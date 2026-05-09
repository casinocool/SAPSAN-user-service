package org.example.sapsanuserservice.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.enums.UserStatus;
import org.example.sapsanuserservice.entity.enums.UserType;
import org.example.sapsanuserservice.entity.external.ExternalStudentEntity;
import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.entity.internal.StudentGroupHistory;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.repository.external.ExternalStudentRepository;
import org.example.sapsanuserservice.repository.internal.GroupRepository;
import org.example.sapsanuserservice.repository.internal.StudentGroupHistoryRepository;
import org.example.sapsanuserservice.repository.internal.UserRepository;
import org.example.sapsanuserservice.util.KeycloakUtil;
import org.example.sapsanuserservice.utilits.PasswordGenerator;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final String REALM = "sapsan";
    private static final String STUDENT_ROLE = "ROLE_STUDENT";

    private final Keycloak keycloak;
    private final ExternalStudentRepository externalRepo;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final StudentGroupHistoryRepository studentGroupHistoryRepository;
    private final PasswordGenerator passwordGenerator;
    private final JavaMailSender mailSender;

    // ============================
    // Public метод для контроллера
    // ============================
    public InvitationResult inviteGroup(
            String groupNumber,
            String teacherInstitutesHeader,
            String teacherDirectionsHeader,
            String teacherDepartmentsHeader
    ) {
        Group group = groupRepository.findByNumber(groupNumber)
                .orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                        "Группа " + groupNumber + " не найдена"));

        checkTeacherCanInviteGroup(group, teacherInstitutesHeader, teacherDirectionsHeader, teacherDepartmentsHeader);

        List<ExternalStudentEntity> students = externalRepo.findAllByGroupNumber(groupNumber);

        log.info("Начало импорта группы {}. Найдено студентов: {}", groupNumber, students.size());

        List<String> success = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (ExternalStudentEntity extStudent : students) {
            try {
                processSingleStudent(extStudent, group);
                success.add(extStudent.getEmail());
            } catch (Exception e) {
                log.error("Ошибка при импорте студента {}: {}", extStudent.getEmail(), e.getMessage(), e);
                failed.add(extStudent.getEmail() + " : " + e.getMessage());
            }
        }

        log.info("Импорт группы {} завершён. Успешно: {}, Ошибки: {}", groupNumber, success.size(), failed.size());

        return new InvitationResult(success, failed);
    }

    // ============================
    // Проверка преподавателя
    // ============================
    private void checkTeacherCanInviteGroup(Group group,
                                            String institutesHeader,
                                            String directionsHeader,
                                            String departmentsHeader) {

        List<String> teacherInstitutes = parseHeaderList(institutesHeader);
        List<String> teacherDirections = parseHeaderList(directionsHeader);
        List<String> teacherDepartments = parseHeaderList(departmentsHeader);

        String groupDepartment = group.getDepartment().getName();
        String groupDirection = group.getDepartment().getDirection().getName();
        String groupInstitute = group.getDepartment().getDirection().getInstitute().getName();

        log.info("Проверка прав преподавателя. Teacher: institutes={}, directions={}, departments={}. " +
                        "Group: institute='{}', direction='{}', department='{}'",
                teacherInstitutes, teacherDirections, teacherDepartments,
                groupInstitute, groupDirection, groupDepartment
        );

        boolean hasInstitute = teacherInstitutes.stream().anyMatch(n -> n.equalsIgnoreCase(groupInstitute));
        boolean hasDirection = teacherDirections.stream().anyMatch(n -> n.equalsIgnoreCase(groupDirection));
        boolean hasDepartment = teacherDepartments.stream().anyMatch(n -> n.equalsIgnoreCase(groupDepartment));

        if (!hasInstitute || !hasDirection || !hasDepartment) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN,
                    "Вы не можете добавить эту группу: институт, направление или кафедра не совпадают");
        }
    }

    // ============================
    // Создание одного студента
    // ============================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleStudent(ExternalStudentEntity extStudent, Group group) {

        Optional<User> existing = userRepository.findByEmail(extStudent.getEmail())
                .filter(u -> studentGroupHistoryRepository.existsByUserAndGroup(u, group));

        if (existing.isPresent()) {
            log.info("Студент {} уже существует и прикреплён к группе {}, пропускаем", extStudent.getEmail(), group.getNumber());
            return;
        }

        char[] tempPass = passwordGenerator.generate(12);

        try {
            String[] parts = extStudent.getFullName().trim().split("\\s+");
            String lastName = parts.length > 0 ? parts[0] : "Unknown";
            String firstName = parts.length > 1 ? parts[1] : "";
            String middleName = parts.length > 2 ? parts[2] : "";

            UserRepresentation kcUser = new UserRepresentation();
            kcUser.setUsername(extStudent.getEmail());
            kcUser.setEmail(extStudent.getEmail());
            kcUser.setFirstName(firstName);
            kcUser.setLastName(lastName);
            kcUser.setEnabled(true);
            kcUser.setEmailVerified(true);

            kcUser.setAttributes(Map.of(
                    "institute", List.of(extStudent.getInstituteName()),
                    "direction", List.of(extStudent.getDirectionName()),
                    "department", List.of(extStudent.getDepartmentName()),
                    "group", List.of(extStudent.getGroupNumber()),
                    "middleName", List.of(middleName)
            ));

            var response = keycloak.realm(REALM).users().create(kcUser);

            try {
                if (response.getStatus() == 201) {
                    String kcId = KeycloakUtil.getCreatedId(response);

                    CredentialRepresentation credential = new CredentialRepresentation();
                    credential.setType(CredentialRepresentation.PASSWORD);
                    credential.setValue(new String(tempPass));
                    credential.setTemporary(true);

                    keycloak.realm(REALM).users().get(kcId).resetPassword(credential);

                    var studentRole = keycloak.realm(REALM).roles().get(STUDENT_ROLE).toRepresentation();
                    keycloak.realm(REALM).users().get(kcId).roles().realmLevel().add(List.of(studentRole));

                    User localUser = User.builder()
                            .keycloakId(UUID.fromString(kcId))
                            .email(extStudent.getEmail())
                            .firstName(firstName)
                            .lastName(lastName)
                            .middleName(middleName)
                            .userType(UserType.STUDENT)
                            .status(UserStatus.ACTIVE)
                            .build();

                    User savedUser = userRepository.save(localUser);

                    StudentGroupHistory history = StudentGroupHistory.builder()
                            .user(savedUser)
                            .group(group)
                            .active(true)
                            .build();

                    studentGroupHistoryRepository.save(history);

                    sendEmailAsync(extStudent.getEmail(), tempPass);

                    log.info("Студент {} успешно импортирован", extStudent.getEmail());

                } else if (response.getStatus() == 409) {
                    log.warn("Студент {} уже существует в Keycloak", extStudent.getEmail());
                } else {
                    throw new ApiException(500, "Ошибка создания пользователя " + extStudent.getEmail() +
                            " в Keycloak. Status: " + response.getStatus());
                }
            } finally {
                response.close();
            }

        } finally {
            Arrays.fill(tempPass, '0');
        }
    }

    // ============================
    // Async отправка email
    // ============================
    @Async
    public void sendEmailAsync(String email, char[] password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("noreply@sapsan.local");
        message.setSubject("Ваш аккаунт в системе SAPSAN");
        message.setText("Добро пожаловать!\n\nВаш временный пароль для входа: "
                + new String(password)
                + "\n\nПосле первого входа система потребует сменить пароль.");
        mailSender.send(message);
    }

    // ============================
    // Парсинг headers с запятыми
    // ============================
    private List<String> parseHeaderList(String value) {
        if (value == null || value.isBlank()) return List.of();

        String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);

        return Arrays.stream(decoded.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    // ============================
    // Результат метода для фронтенда
    // ============================
    public static class InvitationResult {
        public final List<String> success;
        public final List<String> failed;

        public InvitationResult(List<String> success, List<String> failed) {
            this.success = success;
            this.failed = failed;
        }
    }

    // ============================
    // Кастомное исключение для API
    // ============================
    public static class ApiException extends RuntimeException {
        public final int status;

        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}