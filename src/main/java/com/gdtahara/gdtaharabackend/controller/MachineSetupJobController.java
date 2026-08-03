package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import com.gdtahara.gdtaharabackend.service.MachineSetupJobService;
import com.gdtahara.gdtaharabackend.service.SetupTimeLogService;
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
@Tag(name = "Machine Setup Job", description = "Setup job lifecycle — scan, assign, start, complete, skip + time log")
public class MachineSetupJobController {

    private static final Logger logger = LoggerFactory.getLogger(MachineSetupJobController.class);

    private final MachineSetupJobService machineSetupJobService;
    private final SetupTimeLogService setupTimeLogService;

    public MachineSetupJobController(MachineSetupJobService machineSetupJobService,
                                     SetupTimeLogService setupTimeLogService) {
        this.machineSetupJobService = machineSetupJobService;
        this.setupTimeLogService = setupTimeLogService;
    }

    // ── Job lifecycle ─────────────────────────────────────────────────────────

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
        if (setupTimeLogService.hasRunningLogs(id)) {
            throw new IllegalStateException(
                    "มี Activity ที่ยังไม่สิ้นสุด — กรุณากด 'จบ' ก่อน หรือใช้ 'จบทั้งหมด' แล้วจึงกดเสร็จสิ้น");
        }
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

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<List<SetupJobResponse>> allPending() {
        return ResponseEntity.ok(machineSetupJobService.getAllPendingJobs().stream()
                .map(SetupJobResponse::from).toList());
    }

    @GetMapping("/plan/{planId}")
    public ResponseEntity<SetupJobResponse> byPlan(@PathVariable Long planId) {
        MachineSetupJob job = machineSetupJobService.getJobForPlan(planId)
                .orElseThrow(() -> new EntityNotFoundException("No active setup job for plan: " + planId));
        return ResponseEntity.ok(SetupJobResponse.from(job));
    }

    // ── Time Log endpoints ────────────────────────────────────────────────────

    @GetMapping("/{id}/time-logs")
    public ResponseEntity<List<SetupTimeLogDto>> getTimeLogs(@PathVariable Long id) {
        return ResponseEntity.ok(setupTimeLogService.getTimeLogs(id));
    }

    @PostMapping("/{id}/time-logs")
    public ResponseEntity<SetupTimeLogDto> startLog(
            @PathVariable Long id,
            @RequestBody SetupTimeLogRequest req,
            Principal principal) {
        return ResponseEntity.ok(setupTimeLogService.startLog(id, req, principal.getName()));
    }

    @PutMapping("/{id}/time-logs/{logId}/end")
    public ResponseEntity<SetupTimeLogDto> endLog(
            @PathVariable Long id,
            @PathVariable Long logId,
            Principal principal) {
        return ResponseEntity.ok(setupTimeLogService.endLog(id, logId, principal.getName()));
    }

    @DeleteMapping("/{id}/time-logs/{logId}")
    public ResponseEntity<Void> deleteLog(
            @PathVariable Long id,
            @PathVariable Long logId,
            Principal principal) {
        setupTimeLogService.deleteLog(id, logId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/time-logs/end-all")
    public ResponseEntity<List<SetupTimeLogDto>> endAllLogs(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(setupTimeLogService.endAllRunningLogs(id, principal.getName()));
    }
}
