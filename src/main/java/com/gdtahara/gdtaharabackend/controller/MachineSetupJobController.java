package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.AssignTechnicianRequest;
import com.gdtahara.gdtaharabackend.dto.CompleteSetupRequest;
import com.gdtahara.gdtaharabackend.dto.SetupJobResponse;
import com.gdtahara.gdtaharabackend.dto.SkipSetupRequest;
import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import com.gdtahara.gdtaharabackend.service.MachineSetupJobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/setup-jobs")
@PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
@Tag(name = "Machine Setup Job", description = "Setup job lifecycle — scan, assign, start, complete, skip")
public class MachineSetupJobController {

    private static final Logger logger = LoggerFactory.getLogger(MachineSetupJobController.class);

    private final MachineSetupJobService machineSetupJobService;

    public MachineSetupJobController(MachineSetupJobService machineSetupJobService) {
        this.machineSetupJobService = machineSetupJobService;
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<List<SetupJobResponse>> scan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        logger.info("Scan setup jobs {} to {}", from, to);
        List<MachineSetupJob> jobs = machineSetupJobService.scanAndCreateSetupJobs(from, to);
        return ResponseEntity.ok(jobs.stream().map(SetupJobResponse::from).toList());
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<SetupJobResponse> assign(
            @PathVariable Long id, @Valid @RequestBody AssignTechnicianRequest req, Principal principal) {
        MachineSetupJob job = machineSetupJobService.assignTechnician(
                id, req.getTechnicianUserId(), principal.getName());
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<SetupJobResponse> start(@PathVariable Long id, Principal principal) {
        MachineSetupJob job = machineSetupJobService.startSetup(id, principal.getName());
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<SetupJobResponse> complete(
            @PathVariable Long id, @RequestBody CompleteSetupRequest req, Principal principal) {
        MachineSetupJob job = machineSetupJobService.completeSetup(
                id, req.isMoldChanged(), req.getMoldCodeFrom(), req.getMoldCodeTo(),
                req.isTempAdjusted(), req.isCycleAdjusted(), req.isBlowPinAligned(),
                req.isFpiPassed(), req.getNotes(), principal.getName());
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }

    @PutMapping("/{id}/skip")
    public ResponseEntity<SetupJobResponse> skip(
            @PathVariable Long id, @Valid @RequestBody SkipSetupRequest req, Principal principal) {
        MachineSetupJob job = machineSetupJobService.skipSetup(
                id, req.getSkipReason(), principal.getName());
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }

    @GetMapping("/technician/{userId}/pending")
    public ResponseEntity<List<SetupJobResponse>> pendingForTechnician(@PathVariable Long userId) {
        return ResponseEntity.ok(machineSetupJobService.getPendingJobsForTechnician(userId).stream()
                .map(SetupJobResponse::from).toList());
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<SetupJobResponse> byPlan(@PathVariable Long planId) {
        MachineSetupJob job = machineSetupJobService.getJobForPlan(planId)
                .orElseThrow(() -> new EntityNotFoundException("No active setup job for plan: " + planId));
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }
}
