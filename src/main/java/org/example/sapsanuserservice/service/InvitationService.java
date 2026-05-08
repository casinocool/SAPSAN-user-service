package org.example.sapsanuserservice.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.entity.enums.UserStatus;
import org.example.sapsanuserservice.entity.enums.UserType;
import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.entity.external.ExternalStudentEntity;
import org.example.sapsanuserservice.repository.external.ExternalStudentRepository;
import org.example.sapsanuserservice.repository.internal.GroupRepository;
import org.example.sapsanuserservice.repository.internal.UserRepository;
import org.example.sapsanuserservice.util.KeycloakUtil;
import org.example.sapsanuserservice.utilits.PasswordGenerator;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final Keycloak keycloak;
    private final ExternalStudentRepository externalRepo;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordGenerator passwordGenerator;
    private final JavaMailSender mailSender;

    private static final String REALM = "sapsan";

    /**
     * ГЛАВНЫЙ МЕТОД: Синхронизация группы
     * @param groupNumber номер группы для импорта
     * @param encodedDirections направления из заголовка X-User-Direction
     */
    public void inviteGroup(String groupNumber, String encodedDirections) {
        // 1. ПРОВЕРКА ПРАВ ПРЕПОДАВАТЕЛЯ
        String decodedDirections = URLDecoder.decode(encodedDirections, StandardCharsets.UTF_8);
        List<String> teacherDirections = Arrays.asList(decodedDirections.split(", "));

        // Ищем группу в нашей иерархии, чтобы понять, к какому направлению она относится
        Group group = groupRepository.findByNumber(groupNumber)
                .orElseThrow(() -> new RuntimeException("Группа " + groupNumber + " не найдена в справочнике системы"));

        String groupDirection = group.getDepartment().getDirection().getName();

        if (!teacherDirections.contains(groupDirection)) {
            log.error("Доступ запрещен: Преподаватель с направлениями {} пытается импортировать группу направления {}",
                    teacherDirections, groupDirection);
            throw new RuntimeException("У вас нет прав на управление этим направлением!");
        }

        // 2. ПОЛУЧЕНИЕ СПИСКА СТУДЕНТОВ
        List<ExternalStudentEntity> students = externalRepo.findAllByGroupNumber(groupNumber);
        log.info("Начало импорта группы {}. Найдено студентов: {}", groupNumber, students.size());

        // 3. ЦИКЛ ОБРАБОТКИ
        for (ExternalStudentEntity extStudent : students) {
            try {
                // Вызываем метод обработки одного студента в НОВОЙ транзакции
                processSingleStudent(extStudent);
            } catch (Exception e) {
                log.error("Ошибка при импорте студента {}: {}", extStudent.getEmail(), e.getMessage());
            }
        }
    }

    /**
     * Метод для обработки одного студента.
     * REQUIRES_NEW гарантирует, что ошибка одного студента не откатит всю группу.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleStudent(ExternalStudentEntity extStudent) {
        // Проверяем, есть ли он уже у нас
        if (userRepository.findByEmail(extStudent.getEmail()).isPresent()) {
            log.info("Студент {} уже существует в БД, пропускаем", extStudent.getEmail());
            return;
        }

        char[] tempPass = passwordGenerator.generate(12);

        // Парсим ФИО
        String[] nameParts = extStudent.getFullName().trim().split("\\s+");
        String lastName = nameParts.length > 0 ? nameParts[0] : "Unknown";
        String firstName = nameParts.length > 1 ? nameParts[1] : "";
        String middleName = nameParts.length > 2 ? nameParts[2] : "";

        // 1. СОЗДАНИЕ В KEYCLOAK
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

        Response response = keycloak.realm(REALM).users().create(kcUser);

        try {
            if (response.getStatus() == 201) {
                String kcId = KeycloakUtil.getCreatedId(response);

                // 2. УСТАНОВКА ПАРОЛЯ
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(new String(tempPass));
                cred.setTemporary(true);
                keycloak.realm(REALM).users().get(kcId).resetPassword(cred);

                // 3. НАЗНАЧЕНИЕ РОЛИ
                var role = keycloak.realm(REALM).roles().get("ROLE_STUDENT").toRepresentation();
                keycloak.realm(REALM).users().get(kcId).roles().realmLevel().add(List.of(role));

                // 4. СОХРАНЕНИЕ В ЛОКАЛЬНУЮ БД
                User localUser = User.builder()
                        .keycloakId(UUID.fromString(kcId))
                        .email(extStudent.getEmail())
                        .firstName(firstName)
                        .lastName(lastName)
                        .middleName(middleName)
                        .userType(UserType.STUDENT)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(localUser);

                // 5. ОТПРАВКА ПИСЬМА
                sendEmail(extStudent.getEmail(), tempPass);

                log.info("Студент {} успешно импортирован (ID: {})", extStudent.getEmail(), kcId);
            } else if (response.getStatus() == 409) {
                log.warn("Конфликт: Студент {} уже есть в Keycloak", extStudent.getEmail());
            }
        } finally {
            response.close();
            Arrays.fill(tempPass, '0');
        }
    }

    private void sendEmail(String email, char[] password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("noreply@sapsan.local");
        message.setSubject("Ваш аккаунт в системе SAPSAN");
        message.setText("Добро пожаловать!\n\nВаш временный пароль для входа: " + new String(password) +
                "\n\nПосле первого входа система потребует сменить пароль.");
        mailSender.send(message);
    }
}