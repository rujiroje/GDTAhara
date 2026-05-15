package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.exception.ResourceNotFoundException;
import com.gdtahara.gdtaharabackend.model.PmSchedule;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.PmScheduleRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pm-schedules")
public class PmScheduleController {

    private final PmScheduleRepository pmRepo;
    private final MachineRepository machineRepo;
    private final UserRepository userRepo;

    public PmScheduleController(PmScheduleRepository pmRepo,
                                MachineRepository machineRepo,
                                UserRepository userRepo) {
        this.pmRepo = pmRepo;
        this.machineRepo = machineRepo;
        this.userRepo = userRepo;
    }

    // GET /api/pm-schedules — ทั้งหมด
    @GetMapping
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getAll() {
        return ResponseEntity.ok(pmRepo.findByIsActiveTrueOrderByNextDueDateAsc());
    }

    // GET /api/pm-schedules/urgent — DUE + OVERDUE
    @GetMapping("/urgent")
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getUrgent() {
        return ResponseEntity.ok(pmRepo.findUrgent());
    }

    // GET /api/pm-schedules/machine/{machineId}
    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('Technician','Shift Leader','Production Control','DataAdmin','Management')")
    public ResponseEntity<List<PmSchedule>> getByMachine(@PathVariable Long machineId) {
        return ResponseEntity.ok(pmRepo.findByMachineIdAndIsActiveTrueOrderByNextDueDateAsc(machineId));
    }

    // POST /api/pm-schedules — สร้างงาน PM ใหม่
    @PostMapping
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<PmSchedule> create(@RequestBody PmSchedule req) {
        req.setId(null);
        if (req.getMachine() != null && req.getMachine().getId() != null)
            req.setMachine(machineRepo.findById(req.getMachine().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine not found")));
        if (req.getAssignedTo() != null && req.getAssignedTo().getId() != null)
            req.setAssignedTo(userRepo.findById(req.getAssignedTo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        return ResponseEntity.ok(pmRepo.save(req));
    }

    // POST /api/pm-schedules/{id}/done — บันทึกว่าทำ PM เสร็จแล้ว
    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
    public ResponseEntity<PmSchedule> markDone(@PathVariable Long id) {
        PmSchedule pm = pmRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PM Schedule not found: " + id));
        pm.setLastDoneDate(LocalDate.now());
        pm.setNextDueDate(LocalDate.now().plusDays(pm.getIntervalDays()));
        pm.setStatus("DONE");
        return ResponseEntity.ok(pmRepo.save(pm));
    }

    // DELETE /api/pm-schedules/{id} — soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        PmSchedule pm = pmRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PM Schedule not found: " + id));
        pm.setIsActive(false);
        pmRepo.save(pm);
        return ResponseEntity.noContent().build();
    }
}
