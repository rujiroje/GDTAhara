package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.MachineStatusLogDto;
import com.gdtahara.gdtaharabackend.dto.MachineStatusUpdateRequest;
import com.gdtahara.gdtaharabackend.service.MachineStatusService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/machine-status")
public class MachineStatusController {

    private final MachineStatusService service;

    public MachineStatusController(MachineStatusService service) {
        this.service = service;
    }

    // Operator กดเปลี่ยนสถานะเครื่อง
    // POST /api/machine-status
    // Body: { "machineId": 1, "status": "RUNNING", "reason": "..." }
    @PostMapping
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','CM Operator')")
    public ResponseEntity<MachineStatusLogDto> updateStatus(
            @RequestBody MachineStatusUpdateRequest req,
            Principal principal) {
        return ResponseEntity.ok(service.updateStatus(req, principal.getName()));
    }

    // ดูสถานะปัจจุบันของเครื่อง
    // GET /api/machine-status/{machineId}/current
    @GetMapping("/{machineId}/current")
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','Management','QA','Technician','CM Operator')")
    public ResponseEntity<MachineStatusLogDto> getCurrent(@PathVariable Long machineId) {
        MachineStatusLogDto dto = service.getCurrentStatus(machineId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    // ดู timeline ย้อนหลัง
    // GET /api/machine-status/{machineId}/timeline?from=...&to=...
    @GetMapping("/{machineId}/timeline")
    @PreAuthorize("hasAnyRole('Shift Leader','Production Control','DataAdmin','Management','CM Operator')")
    public ResponseEntity<List<MachineStatusLogDto>> getTimeline(
            @PathVariable Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(service.getTimeline(machineId, from, to));
    }

    // ดู log ล่าสุด
    // GET /api/machine-status/{machineId}/recent?limit=10
    @GetMapping("/{machineId}/recent")
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','Management','QA','Technician','CM Operator')")
    public ResponseEntity<List<MachineStatusLogDto>> getRecent(
            @PathVariable Long machineId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.getRecent(machineId, limit));
    }
}
