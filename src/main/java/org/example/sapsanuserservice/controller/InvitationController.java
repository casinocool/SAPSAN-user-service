package org.example.sapsanuserservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sapsanuserservice.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/invite")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;


    @PostMapping("/group/{groupNumber}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> inviteGroup(
            @PathVariable String groupNumber,
            HttpServletRequest request
    ) {
        String teacherInstitute = request.getHeader("X-User-Institute");
        String teacherDirection = request.getHeader("X-User-Direction");
        String teacherDepartment = request.getHeader("X-User-Department");

        invitationService.inviteGroup(
                groupNumber,
                teacherInstitute,
                teacherDirection,
                teacherDepartment
        );

        return ResponseEntity.ok("Студенты группы " + groupNumber + " добавлены в систему");
    }
}