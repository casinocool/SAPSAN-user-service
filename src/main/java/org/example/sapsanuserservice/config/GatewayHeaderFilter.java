package org.example.sapsanuserservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rawRolesHeader = request.getHeader("X-User-Roles");

        if (userId != null && rawRolesHeader != null) {
            // 1. Декодируем строку ролей из URL-формата в нормальный UTF-8
            String decodedRoles = URLDecoder.decode(rawRolesHeader, StandardCharsets.UTF_8);

            // 2. Превращаем строку "ROLE_TEACHER, ROLE_STUDENT" в список прав
            List<SimpleGrantedAuthority> authorities = Arrays.stream(decodedRoles.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
