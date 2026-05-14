package org.example.sapsanuserservice.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users/debug")
public class DebugController {

    @GetMapping("/token")
    public Map<String, Object> echoTokenInfo(JwtAuthenticationToken auth) {
        Map<String, Object> debugInfo = new HashMap<>();

        // 1. Извлекаем ID (Subject из Keycloak)
        debugInfo.put("X-User-Id (from sub)", auth.getName());

        // 2. Извлекаем Роли
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));
        debugInfo.put("X-User-Roles (processed)", roles);

        // 3. Извлекаем кастомные атрибуты (которые раньше прокидывал Gateway)
        Map<String, Object> claims = auth.getToken().getClaims();

        debugInfo.put("X-User-Group", claims.get("group"));
        debugInfo.put("X-User-Institute", claims.get("institute"));
        debugInfo.put("X-User-Direction", claims.get("direction"));
        debugInfo.put("X-User-Department", claims.get("department"));

        // 4. Почта и ФИО для полноты картины
        debugInfo.put("Email", claims.get("email"));
        debugInfo.put("Preferred Username", claims.get("preferred_username"));

        return debugInfo;
    }
}