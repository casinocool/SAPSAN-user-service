package org.example.sapsanuserservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.dto.DepartmentDto;
import org.example.sapsanuserservice.dto.LectureDto;
import org.example.sapsanuserservice.entity.internal.Lecture;
import org.example.sapsanuserservice.repository.internal.DepartmentRepository;
import org.example.sapsanuserservice.repository.internal.DirectionRepository;
import org.example.sapsanuserservice.service.InvitationService.ApiException;
import org.example.sapsanuserservice.service.LectureService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;
    private final DepartmentRepository departmentRepository;

    /** Список доступных лекций. Фильтрация — внутри сервиса по ролям. */
    @GetMapping
    public List<LectureDto> list(JwtAuthenticationToken auth) {
        return lectureService.findAccessible(auth).stream().map(LectureDto::from).toList();
    }

    /** Кафедры, в которые текущий преподаватель может загружать. Нужны фронту для select'а. */
    @GetMapping("/my-departments")
    @PreAuthorize("hasRole('TEACHER')")
    public List<DepartmentDto> myDepartments(JwtAuthenticationToken auth) {
        Object raw = auth.getToken().getClaim("department");
        List<String> names;
        if (raw instanceof List<?> l) names = (List<String>) l;
        else if (raw instanceof String s) names = List.of(s);
        else names = Collections.emptyList();

        if (names.isEmpty()) return List.of();
        return departmentRepository.findAllByNameInIgnoreCase(names).stream()
                .map(DepartmentDto::from).toList();
    }

    /** Загрузка — только учитель, и только в своё направление. */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public LectureDto upload(@RequestParam("departmentId") Long departmentId,
                             @RequestParam("title") String title,
                             @RequestParam("file") MultipartFile file,
                             JwtAuthenticationToken auth) throws IOException {
        Lecture saved = lectureService.upload(departmentId, title, file, auth);
        return LectureDto.from(saved);
    }

    /** Скачивание — любой, у кого есть доступ к направлению этой лекции. */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id,
                                                        JwtAuthenticationToken auth) {
        Lecture lecture = lectureService.findForDownload(id, auth);
        ResponseInputStream<GetObjectResponse> stream = lectureService.openStream(lecture);

        String encoded = URLEncoder.encode(lecture.getOriginalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentLength(lecture.getSizeBytes())
                .contentType(MediaType.parseMediaType(
                        lecture.getContentType() != null
                                ? lecture.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, JwtAuthenticationToken auth) {
        lectureService.delete(id, auth);
        return ResponseEntity.noContent().build();
    }

    // Единый обработчик ApiException — отдаёт фронту понятный JSON-ответ
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage()));
    }
}
