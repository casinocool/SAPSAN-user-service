package org.example.sapsanuserservice.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.external.ExternalStudentEntity;
import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.repository.external.ExternalStudentRepository;
import org.example.sapsanuserservice.repository.internal.GroupRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final ExternalStudentRepository externalRepo;
    private final GroupRepository groupRepository;
    private final StudentImportService studentImportService; // Наш новый помощник

    public InvitationResult inviteGroup(String groupNumber, JwtAuthenticationToken auth) {
        // 1. Ищем группу
        Group group = groupRepository.findByNumber(groupNumber)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                        "Группа " + groupNumber + " не найдена"));

        // 2. Проверяем права преподавателя через данные из JWT
        Map<String, Object> claims = auth.getToken().getClaims();
        checkTeacherCanInviteGroup(group, claims);

        // 3. Получаем список студентов из внешней системы
        List<ExternalStudentEntity> students = externalRepo.findAllByGroupNumber(groupNumber);
        log.info("Импорт группы {}. Студентов: {}", groupNumber, students.size());

        List<String> success = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // 4. Обработка каждого студента
        for (ExternalStudentEntity extStudent : students) {
            try {
                // Вызов метода другого бина — транзакция REQUIRES_NEW сработает!
                studentImportService.processSingleStudent(extStudent, group);
                success.add(extStudent.getEmail());
            } catch (Exception e) {
                log.error("Ошибка студента {}: {}", extStudent.getEmail(), e.getMessage());
                failed.add(extStudent.getEmail() + " : " + e.getMessage());
            }
        }

        return new InvitationResult(success, failed);
    }

    private void checkTeacherCanInviteGroup(Group group, Map<String, Object> claims) {
        // В Keycloak атрибуты прилетают как списки (List)
        List<String> tInstitutes = getClaimAsList(claims, "institute");
        List<String> tDirections = getClaimAsList(claims, "direction");
        List<String> tDepartments = getClaimAsList(claims, "department");

        String gDept = group.getDepartment().getName();
        String gDir = group.getDepartment().getDirection().getName();
        String gInst = group.getDepartment().getDirection().getInstitute().getName();

        boolean ok = tInstitutes.stream().anyMatch(gInst::equalsIgnoreCase) &&
                tDirections.stream().anyMatch(gDir::equalsIgnoreCase) &&
                tDepartments.stream().anyMatch(gDept::equalsIgnoreCase);

        if (!ok) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN,
                    "Доступ запрещен: группа не относится к вашему институту/кафедре");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getClaimAsList(Map<String, Object> claims, String key) {
        Object val = claims.get(key);
        if (val instanceof List) return (List<String>) val;
        if (val instanceof String) return List.of((String) val);
        return Collections.emptyList();
    }

    public static class InvitationResult {
        public final List<String> success;
        public final List<String> failed;
        public InvitationResult(List<String> success, List<String> failed) {
            this.success = success;
            this.failed = failed;
        }
    }

    public static class ApiException extends RuntimeException {
        public final int status;
        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}