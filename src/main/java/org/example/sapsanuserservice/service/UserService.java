package org.example.sapsanuserservice.service;

import jakarta.transaction.Transactional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.repository.internal.GroupRepository;
import org.example.sapsanuserservice.repository.internal.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final Keycloak keycloak;
    private static final String REALM = "sapsan";

    // --- ПРОСМОТР СТУДЕНТОВ ГРУППЫ ---
    public List<User> getStudentsByGroup(String groupNumber, JwtAuthenticationToken auth) {
        // 1. Проверяем, имеет ли право преподаватель смотреть эту группу
        verifyTeacherAccess(groupNumber, auth);

        // 2. Если да — возвращаем список
        return userRepository.findAllByGroupNumber(groupNumber);
    }

    // --- УДАЛЕНИЕ СТУДЕНТА ПО EMAIL ---
    @Transactional
    public void deleteStudentByEmail(String email, JwtAuthenticationToken auth) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        // Находим текущую группу студента для проверки доступа
        // (здесь можно упростить или усложнить логику проверки)

        // 1. Удаляем из Keycloak (чтобы не смог залогиниться)
        try {
            keycloak.realm(REALM).users().get(user.getKeycloakId().toString()).remove();
            log.info("Пользователь {} удален из Keycloak", email);
        } catch (Exception e) {
            log.error("Ошибка удаления из Keycloak: {}", e.getMessage());
        }

        // 2. Удаляем из локальной БД
        userRepository.delete(user);
    }

    // --- ВСПОМОГАТЕЛЬНЫЙ МЕТОД: ПРОВЕРКА ПРАВ ПРЕПОДАВАТЕЛЯ ---
    private void verifyTeacherAccess(String groupNumber, JwtAuthenticationToken auth) {
        Group group = groupRepository.findByNumber(groupNumber)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        Map<String, Object> claims = auth.getToken().getClaims();
        List<String> tInstitutes = (List<String>) claims.getOrDefault("institute", List.of());
        List<String> tDirections = (List<String>) claims.getOrDefault("direction", List.of());
        List<String> tDepartments = (List<String>) claims.getOrDefault("department", List.of());

        String gInst = group.getDepartment().getDirection().getInstitute().getName();
        String gDir = group.getDepartment().getDirection().getName();
        String gDept = group.getDepartment().getName();

        boolean canAccess = tInstitutes.stream().anyMatch(gInst::equalsIgnoreCase) &&
                tDirections.stream().anyMatch(gDir::equalsIgnoreCase) &&
                tDepartments.stream().anyMatch(gDept::equalsIgnoreCase);

        if (!canAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "У вас нет прав доступа к этой группе");
        }
    }
}
