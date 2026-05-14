package org.example.sapsanuserservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.sapsanuserservice.entity.internal.User;
import org.example.sapsanuserservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Получить список студентов группы
    @GetMapping("/group/{groupNumber}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<User>> getGroupStudents(
            @PathVariable String groupNumber,
            JwtAuthenticationToken auth) {
        return ResponseEntity.ok(userService.getStudentsByGroup(groupNumber, auth));
    }

    // Удалить конкретного студента
    @DeleteMapping("/{email}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable String email,
            JwtAuthenticationToken auth) {
        userService.deleteStudentByEmail(email, auth);
        return ResponseEntity.noContent().build();
    }

    // Посмотреть данные о себе
    @GetMapping("/me")
    public Map<String, Object> getMe(JwtAuthenticationToken auth) {
        Map<String, Object> claims = auth.getToken().getClaims();
        Map<String, Object> userDetails = new HashMap<>();

        userDetails.put("username", claims.get("preferred_username"));
        userDetails.put("roles", auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList());
        userDetails.put("name", claims.get("given_name") + " " + claims.get("family_name"));
        userDetails.put("group", claims.get("group")); // Придет списком из Keycloak

        return userDetails;
    }
}
