package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.exception.ResourceNotFoundException;
import com.gdtahara.gdtaharabackend.model.PmSchedule;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.PmScheduleRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import com.gdtahara.gdtaharabackend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm-schedules")
public class PmScheduleController {

    private final PmScheduleRepository pmRepo;
    private final MachineRepository machineRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;

    public PmScheduleController(PmScheduleRepository pmRepo,
                                MachineRepository machineRepo,
                                UserRepository userRepo,
                                AuditLogService auditLogService) {
        this.pmRepo = pmRepo;
        this.machineRepo = machineRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getAll() {
        return ResponseEntity.ok(pmRepo.findByIsActiveTrueOrderByNextDueDateAsc());
    }

    @GetMapping("/urgent")
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getUrgent() {
        return ResponseEntity.ok(pmRepo.findUrgent());
    }

    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getByMachine(@PathVariable Long machineId) {
        return ResponseEntity.ok(pmRepo.findByMachineIdAndIsActiveTrueOrderByNextDueDateAsc(machineId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<PmSchedule> create(@RequestBody PmSchedule req, Principal principal) {
        req.setId(null);
        if (req.getMachine() != null && req.getMachine().getId() != null)
            req.setMachine(machineRepo.findById(req.getMachine().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine not found")));
        if (req.getAssignedTo() != null && req.getAssignedTo().getId() != null)
            req.setAssignedTo(userRepo.findById(req.getAssignedTo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        PmSchedule saved = pmRepo.save(req);
        auditLogService.log("CREATE", "PmSchedule", saved.getId(), null,
                Map.of("id", saved.getId(), "status", String.valueOf(saved.getStatus())));
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
    public ResponseEntity<PmSchedule> markDone(@PathVariable Long id, Principal principal) {
        PmSchedule pm = pmRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PM Schedule not found: " + id));
        String oldStatus = pm.getStatus();
        pm.setLastDoneDate(LocalDate.now());
        pm.setNextDueDate(LocalDate.now().plusDays(pm.getIntervalDays()));
        pm.setStatus("DONE");
        PmSchedule saved = pmRepo.save(pm);
        auditLogService.log("UPDATE", "PmSchedule", id,
                Map.of("id", id, "status", String.valueOf(oldStatus)),
                Map.of("id", id, "status", "DONE"));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Principal principal) {
        PmSchedule pm = pmRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PM Schedule not found: " + id));
        pm.setIsActive(false);
        pmRepo.save(pm);
        auditLogService.log("DEACTIVATE", "PmSchedule", id, Map.of("id", id), null);
        return ResponseEntity.noContent().build();
    }
}
