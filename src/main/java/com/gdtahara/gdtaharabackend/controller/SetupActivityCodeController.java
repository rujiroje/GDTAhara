package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.CreateActivityCodeRequest;
import com.gdtahara.gdtaharabackend.dto.SetupActivityCodeDto;
import com.gdtahara.gdtaharabackend.service.SetupTimeLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/setup-activity-codes")
@RequiredArgsConstructor
@Tag(name = "Setup Activity Code", description = "Activity code master — list (all roles) / create / toggle (Admin only)")
public class SetupActivityCodeController {

    private final SetupTimeLogService setupTimeLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
    public ResponseEntity<List<SetupActivityCodeDto>> getActive() {
        return ResponseEntity.ok(setupTimeLogService.getActiveCodes());
    }

    @PostMapping
    @PreAuthorize("hasRole('DataAdmin')")
    public ResponseEntity<SetupActivityCodeDto> create(
            @RequestBody CreateActivityCodeRequest req, Principal principal) {
        return ResponseEntity.ok(setupTimeLogService.createCode(req, principal.getName()));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('DataAdmin')")
    public ResponseEntity<SetupActivityCodeDto> toggle(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(setupTimeLogService.toggleCodeActive(id, active));
    }
}
