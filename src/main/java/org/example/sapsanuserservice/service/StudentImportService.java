package org.example.sapsanuserservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.enums.UserStatus;
import org.example.sapsanuserservice.entity.enums.UserType;
import org.example.sapsanuserservice.entity.external.ExternalStudentEntity;
import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.entity.internal.StudentGroupHistory;
import org.example.sapsanuserservice.entity.internal.User;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentImportService {

    private static final String REALM = "sapsan";
    private static final String STUDENT_ROLE = "ROLE_STUDENT";
    private static final String MAIL_FROM = "noreply@sapsan.local";

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final StudentGroupHistoryRepository studentGroupHistoryRepository;
    private final PasswordGenerator passwordGenerator;
    private final JavaMailSender mailSender;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleStudent(ExternalStudentEntity extStudent, Group group) {
        // Проверяем, есть ли уже этот студент в этой группе
        boolean alreadyInGroup = userRepository.findByEmail(extStudent.getEmail())
                .map(u -> studentGroupHistoryRepository.existsByUserAndGroup(u, group))
                .orElse(false);

        if (alreadyInGroup) {
            log.info("Студент {} уже в группе {}, пропускаем", extStudent.getEmail(), group.getNumber());
            return;
        }

        char[] tempPass = passwordGenerator.generate(12);
        try {
            String[] parts = extStudent.getFullName().trim().split("\\s+");
            String lastName = parts.length > 0 ? parts[0] : "Unknown";
            String firstName = parts.length > 1 ? parts[1] : "";
            String middleName = parts.length > 2 ? parts[2] : "";

            // 1. Создаем пользователя в Keycloak
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

            if (response.getStatus() == 201) {
                String kcId = KeycloakUtil.getCreatedId(response);

                // 2. Устанавливаем временный пароль
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(new String(tempPass));
                cred.setTemporary(true);
                keycloak.realm(REALM).users().get(kcId).resetPassword(cred);

                // 3. Назначаем роль STUDENT
                var role = keycloak.realm(REALM).roles().get(STUDENT_ROLE).toRepresentation();
                keycloak.realm(REALM).users().get(kcId).roles().realmLevel().add(List.of(role));

                // 4. Сохраняем в нашу БД
                User localUser = userRepository.save(User.builder()
                        .keycloakId(UUID.fromString(kcId))
                        .email(extStudent.getEmail())
                        .firstName(firstName).lastName(lastName).middleName(middleName)
                        .userType(UserType.STUDENT).status(UserStatus.ACTIVE).build());

                studentGroupHistoryRepository.save(StudentGroupHistory.builder()
                        .user(localUser).group(group).active(true).build());

                sendEmailAsync(extStudent.getEmail(), tempPass);
            } else if (response.getStatus() != 409) {
                throw new RuntimeException("Keycloak error status: " + response.getStatus());
            }
            response.close();
        } finally {
            Arrays.fill(tempPass, '0');
        }
    }

    @Async
    public void sendEmailAsync(String email, char[] password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(MAIL_FROM);
        message.setTo(email);
        message.setSubject("Ваш аккаунт SAPSAN");
        message.setText("Ваш временный пароль: " + new String(password));
        mailSender.send(message);
    }
}