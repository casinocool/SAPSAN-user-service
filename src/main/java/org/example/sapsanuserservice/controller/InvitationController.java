package org.example.sapsanuserservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sapsanuserservice.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @RequestHeader("X-User-Institute") String institutes,
            @RequestHeader("X-User-Direction") String directions,
            @RequestHeader("X-User-Department") String departments
    ) {
        try {
            var result = invitationService.inviteGroup(groupNumber, institutes, directions, departments);
            return ResponseEntity.ok(result);
        } catch (InvitationService.ApiException ex) {
            return ResponseEntity.status(ex.status).body(
                    new InvitationService.InvitationResult(Collections.emptyList(),
                            List.of(ex.getMessage()))
            );
        }
    }
}