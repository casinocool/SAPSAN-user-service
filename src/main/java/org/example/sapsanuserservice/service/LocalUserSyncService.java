package org.example.sapsanuserservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sapsanuserservice.entity.enums.UserStatus;
import org.example.sapsanuserservice.entity.enums.UserType;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.repository.internal.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalUserSyncService {

    private final UserRepository userRepository;

    /**
     * Возвращает локального User для текущего JWT. Если его ещё нет —
     * создаёт на лету по данным из токена. Используется для учителей и админов,
     * которых никто не приглашает явно.
     */
    @Transactional
    public User getOrCreate(JwtAuthenticationToken auth) {
        UUID keycloakId = UUID.fromString(auth.getName());
        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> createFromJwt(auth.getToken(), keycloakId));
    }

    private User createFromJwt(Jwt jwt, UUID keycloakId) {
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        UserType type = detectType(jwt);

        log.info("Создаём локального пользователя для {} (type={}) из JWT", email, type);

        return userRepository.save(User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .userType(type)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private UserType detectType(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.List<?> roles) {
            for (Object r : roles) {
                if ("ROLE_ADMIN".equals(r)) return UserType.ADMIN;
                if ("ROLE_TEACHER".equals(r)) return UserType.TEACHER;
            }
        }
        return UserType.STUDENT;
    }
}