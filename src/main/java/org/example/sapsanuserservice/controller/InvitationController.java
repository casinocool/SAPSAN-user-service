    package org.example.sapsanuserservice.controller;

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
        public ResponseEntity<String> inviteGroup(@PathVariable String groupNumber){
            invitationService.inviteGroup(groupNumber);
            return ResponseEntity.ok("Приглашения для группы" + groupNumber+ " отправлены!");
        }
    }
