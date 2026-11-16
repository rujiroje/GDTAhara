package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.service.CmMeterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cm")
@PreAuthorize("hasAnyRole('CM Operator', 'DataAdmin')")
public class CmMeterController {

    private final CmMeterService service;

    public CmMeterController(CmMeterService service) {
        this.service = service;
    }

    // GET /api/cm/shift — กะปัจจุบัน
    @GetMapping("/shift")
    public ResponseEntity<CurrentShiftDto> getCurrentShift() {
        return ResponseEntity.ok(service.getCurrentShift());
    }

    // GET /api/cm/tanks/machine/{machineId} — ถังของเครื่อง
    @GetMapping("/tanks/machine/{machineId}")
    public ResponseEntity<List<TankStatusDto>> getTanksByMachine(@PathVariable String machineId) {
        return ResponseEntity.ok(service.getTanksByMachine(machineId));
    }

    // GET /api/cm/tanks/{tankId}/status — สถานะถังปัจจุบัน
    @GetMapping("/tanks/{tankId}/status")
    public ResponseEntity<TankStatusDto> getTankStatus(@PathVariable String tankId) {
        return ResponseEntity.ok(service.getTankStatus(tankId));
    }

    // POST /api/cm/tanks/{tankId}/meter — บันทึกค่ามิเตอร์
    @PostMapping("/tanks/{tankId}/meter")
    public ResponseEntity<Void> recordMeterReading(
            @PathVariable String tankId,
            @RequestBody TankMeterEntryDto dto,
            Principal principal) {
        service.recordMeterReading(tankId, dto, principal.getName());
        return ResponseEntity.ok().build();
    }

    // POST /api/cm/tanks/{tankId}/refill — บันทึกการเติมวัตถุดิบ
    @PostMapping("/tanks/{tankId}/refill")
    public ResponseEntity<Void> recordRefill(
            @PathVariable String tankId,
            @RequestBody RefillRequestDto dto,
            Principal principal) {
        service.recordRefill(tankId, dto, principal.getName());
        return ResponseEntity.ok().build();
    }

    // GET /api/cm/lots?materialCode=xxx — Lot ที่ยังมีวัตถุดิบเหลือ
    @GetMapping("/lots")
    public ResponseEntity<List<AvailableLotDto>> getAvailableLots(
            @RequestParam String materialCode) {
        return ResponseEntity.ok(service.getAvailableLots(materialCode));
    }

    // GET /api/cm/tanks/{tankId}/report?shiftDate=2026-11-16&shiftCode=A — รายงานกะ
    @GetMapping("/tanks/{tankId}/report")
    public ResponseEntity<ShiftReportDto> getShiftReport(
            @PathVariable String tankId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam String shiftCode) {
        return ResponseEntity.ok(service.getShiftReport(tankId, shiftDate, shiftCode));
    }

    // POST /api/cm/refills/{refillId}/correct — แก้ไขรายการเติม
    @PostMapping("/refills/{refillId}/correct")
    public ResponseEntity<Void> recordCorrection(
            @PathVariable Long refillId,
            @RequestBody CorrectionRequestDto dto,
            Principal principal) {
        service.recordCorrection(refillId, dto, principal.getName());
        return ResponseEntity.ok().build();
    }
}
