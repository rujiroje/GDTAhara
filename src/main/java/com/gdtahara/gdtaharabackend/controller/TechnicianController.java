package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import com.gdtahara.gdtaharabackend.service.TechnicianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping({"/api/technician", "/api/production/technician"})
public class TechnicianController {

    private static final Logger logger = LoggerFactory.getLogger(TechnicianController.class);

    @Autowired
    private TechnicianService technicianService;

    @PostMapping("/reports/{reportId}/downtime")
    @PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
    public ResponseEntity<?> recordDowntime(@PathVariable Long reportId, @RequestBody DowntimeEventRequestDto requestDto, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        logger.info("API: recordDowntime reportId={} by {}", reportId, username);
        try {
            technicianService.recordDowntime(reportId, requestDto, username);
            return ResponseEntity.ok("บันทึก Downtime สำเร็จ");
        } catch (Exception e) {
            logger.error("Failed to record downtime: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/scrap-weight")
    @PreAuthorize("hasAnyRole('Technician','Production Control','DataAdmin')")
    public ResponseEntity<?> recordScrapWeight(@PathVariable Long reportId, @RequestBody ScrapWeightLogRequestDto requestDto, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        logger.info("API: recordScrapWeight reportId={} by {}", reportId, username);
        try {
            technicianService.recordScrapWeight(reportId, requestDto, username);
            return ResponseEntity.ok("บันทึกน้ำหนักของเสียสำเร็จ");
        } catch (Exception e) {
            logger.error("Failed to record scrap weight: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reports/{reportId}/parameters")
    public ResponseEntity<List<ParameterRecordViewDto>> getParameterRecords(@PathVariable Long reportId) {
        var records = technicianService.getParameterRecords(reportId);
        var view = records.stream()
                .map(ParameterRecordViewDto::fromEntity)
                .toList();
        return ResponseEntity.ok(view);
    }

    @PostMapping("/reports/{reportId}/parameters")
    public ResponseEntity<?> createParameterRecord(@PathVariable Long reportId, @RequestBody ParameterRecordRequest request, Principal principal) {
        try {
            String username = (principal != null) ? principal.getName() : "anonymous";
            technicianService.createParameterRecord(reportId, request, username);
            return ResponseEntity.ok("บันทึกค่าพารามิเตอร์สำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/reports/parameters/{recordId}")
    public ResponseEntity<?> updateParameterRecord(@PathVariable Long recordId, @RequestBody ParameterRecordRequest request, Principal principal) {
        try {
            String username = (principal != null) ? principal.getName() : "anonymous";
            technicianService.updateParameterRecord(recordId, request, username);
            return ResponseEntity.ok("อัปเดตค่าพารามิเตอร์สำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<?> recordNgLog(@PathVariable Long reportId, @RequestBody NgLogRequestDto requestDto, Principal principal) {
        try {
            String username = (principal != null) ? principal.getName() : "anonymous";
            technicianService.recordTechnicianNg(reportId, requestDto, username);
            return ResponseEntity.ok("บันทึกของเสียสำเร็จ");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/parameter-records")
    public ResponseEntity<?> createParameterChecklistRecord(@RequestBody ParameterRecord parameterRecord, Principal principal) {
        try {
            // ถ้ามี field ผู้บันทึก ให้ตั้งค่า username ที่นี่ก่อนบันทึก
            ParameterRecord savedRecord = technicianService.saveParameterRecord(parameterRecord);
            return ResponseEntity.ok(savedRecord);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}