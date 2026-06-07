package org.example.sapsanuserservice.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.internal.Department;
import org.example.sapsanuserservice.entity.internal.Lecture;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.repository.internal.DepartmentRepository;
import org.example.sapsanuserservice.repository.internal.DirectionRepository;
import org.example.sapsanuserservice.repository.internal.LectureRepository;
import org.example.sapsanuserservice.repository.internal.UserRepository;
import org.example.sapsanuserservice.service.InvitationService.ApiException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureService {

    private final LectureRepository lectureRepository;
    private final DirectionRepository directionRepository;
    private final UserRepository userRepository;
    private final MinioStorageService storage;
    private final LocalUserSyncService localUserSyncService;
    private final DepartmentRepository departmentRepository;

    // -------- ЗАГРУЗКА (преподаватель) --------

    @Transactional
    public Lecture upload(Long departmentId,
                          String title,
                          MultipartFile file,
                          JwtAuthenticationToken auth) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Файл не должен быть пустым");
        }
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Название обязательно");
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                        "Кафедра не найдена: " + departmentId));

        // Учитель может грузить только на «свою» кафедру
        List<String> teacherDepartments = getClaimAsList(auth.getToken(), "department");
        boolean allowed = teacherDepartments.stream()
                .anyMatch(n -> n.equalsIgnoreCase(department.getName()));
        if (!allowed) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN,
                    "Доступ запрещён: кафедра «" + department.getName() + "» не относится к вашим");
        }

        User uploader = currentLocalUser(auth);
        String key = storage.upload(file);

        Lecture lecture = Lecture.builder()
                .title(title.trim())
                .originalFileName(file.getOriginalFilename())
                .minioKey(key)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploader(uploader)
                .department(department)
                .build();

        return lectureRepository.save(lecture);
    }

    // -------- ПРОСМОТР СПИСКА --------

    /**
     * Список лекций, доступных текущему пользователю:
     *  - студент видит лекции направлений, в которых состоит через активные группы;
     *  - учитель видит лекции направлений из своего JWT-claim "direction";
     *  - админ — всё.
     */
    @Transactional(readOnly = true)
    public List<Lecture> findAccessible(JwtAuthenticationToken auth) {
        if (hasRole(auth, "ROLE_ADMIN")) {
            return lectureRepository.findAll(
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }

        List<Long> departmentIds;
        if (hasRole(auth, "ROLE_TEACHER")) {
            List<String> names = getClaimAsList(auth.getToken(), "department");
            if (names.isEmpty()) return List.of();
            departmentIds = departmentRepository.findAllByNameInIgnoreCase(names)
                    .stream().map(Department::getId).toList();
        } else {
            User me = currentLocalUser(auth);
            departmentIds = lectureRepository.findDepartmentIdsForStudent(me.getId());
        }

        if (departmentIds.isEmpty()) return List.of();
        return lectureRepository.findAllByDepartmentIdInOrderByCreatedAtDesc(departmentIds);
    }

    // -------- СКАЧИВАНИЕ --------

    @Transactional(readOnly = true)
    public Lecture findForDownload(Long lectureId, JwtAuthenticationToken auth) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_NOT_FOUND,
                        "Лекция не найдена"));

        if (!canSee(lecture, auth)) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN,
                    "Доступ к этой лекции запрещён");
        }
        return lecture;
    }

    public ResponseInputStream<GetObjectResponse> openStream(Lecture lecture) {
        return storage.download(lecture.getMinioKey());
    }

    // -------- УДАЛЕНИЕ --------

    @Transactional
    public void delete(Long id, JwtAuthenticationToken auth) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_NOT_FOUND, "Лекция не найдена"));

        // Удалять может: автор, любой учитель того же направления, админ
        if (!hasRole(auth, "ROLE_ADMIN")) {
            User me = currentLocalUser(auth);
            boolean isAuthor = lecture.getUploader().getId().equals(me.getId());
            boolean isTeacherOfDirection = hasRole(auth, "ROLE_TEACHER")
                    && getClaimAsList(auth.getToken(), "direction").stream()
                        .anyMatch(n -> n.equalsIgnoreCase(lecture.getDepartment().getName()));
            if (!isAuthor && !isTeacherOfDirection) {
                throw new ApiException(HttpServletResponse.SC_FORBIDDEN,
                        "Нельзя удалять чужие лекции");
            }
        }

        storage.delete(lecture.getMinioKey());
        lectureRepository.delete(lecture);
    }

    // -------- ВСПОМОГАТЕЛЬНОЕ --------

    private boolean canSee(Lecture lecture, JwtAuthenticationToken auth) {
        if (hasRole(auth, "ROLE_ADMIN")) return true;

        Long lectureDept = lecture.getDepartment().getId();

        if (hasRole(auth, "ROLE_TEACHER")) {
            List<String> names = getClaimAsList(auth.getToken(), "department");
            return departmentRepository.findAllByNameInIgnoreCase(names)
                    .stream().anyMatch(d -> d.getId().equals(lectureDept));
        }
        User me = currentLocalUser(auth);
        return lectureRepository.findDepartmentIdsForStudent(me.getId()).contains(lectureDept);
    }

    private User currentLocalUser(JwtAuthenticationToken auth) {
        return localUserSyncService.getOrCreate(auth);
    }

    private boolean hasRole(JwtAuthenticationToken auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }

    @SuppressWarnings("unchecked")
    private List<String> getClaimAsList(Jwt token, String name) {
        Object v = token.getClaim(name);
        if (v instanceof List) return (List<String>) v;
        if (v instanceof String s) return List.of(s);
        return Collections.emptyList();
    }
}
