package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.OeeResultDto;
import com.gdtahara.gdtaharabackend.service.OeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/oee")
public class OeeController {

    private final OeeService oeeService;

    public OeeController(OeeService oeeService) {
        this.oeeService = oeeService;
    }

    // GET /api/oee/{machineId}?date=2026-04-21
    @GetMapping("/{machineId}")
    @PreAuthorize("hasAnyRole('Shift Leader','Production Control','DataAdmin','Management','CM Operator')")
    public ResponseEntity<OeeResultDto> getOee(
            @PathVariable Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(oeeService.calculate(machineId, target));
    }
}
