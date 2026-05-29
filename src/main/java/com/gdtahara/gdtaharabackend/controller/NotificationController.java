package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.ProblemAlertViewDto;
import com.gdtahara.gdtaharabackend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('Shift Leader','Technician','DataAdmin')")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/alerts/active")
    public ResponseEntity<List<ProblemAlertViewDto>> getActiveAlerts() {
        return ResponseEntity.ok(notificationService.getActiveAlerts());
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id, Principal principal) {
        try {
            notificationService.acknowledgeAlert(id, principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
