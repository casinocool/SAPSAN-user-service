package org.example.sapsanuserservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.sapsanuserservice.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users/invite")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/group/{groupNumber}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<InvitationService.InvitationResult> inviteGroup(
            @PathVariable String groupNumber,
            JwtAuthenticationToken auth
    ) {
        try {
            var result = invitationService.inviteGroup(groupNumber, auth);
            return ResponseEntity.ok(result);
        } catch (InvitationService.ApiException ex) {
            return ResponseEntity.status(ex.status).body(
                    new InvitationService.InvitationResult(
                            Collections.emptyList(),
                            List.of(ex.getMessage())
                    )
            );
        } catch (Exception e) {
            // На случай непредвиденных ошибок
            return ResponseEntity.status(500).body(
                    new InvitationService.InvitationResult(
                            Collections.emptyList(),
                            List.of("Внутренняя ошибка сервера: " + e.getMessage())
                    )
            );
        }
    }
}