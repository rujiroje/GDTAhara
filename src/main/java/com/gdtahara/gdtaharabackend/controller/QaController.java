// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/QaController.java
// (**แก้ไข** เพิ่ม endpoints ครบถ้วนสำหรับ QA)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.service.QaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@PreAuthorize("hasAnyRole('QA','DataAdmin')")
public class QaController {

    private static final Logger logger = LoggerFactory.getLogger(QaController.class);

    @Autowired
    private QaService qaService;

    // **[ใหม่]** Get active reports for QA
    @GetMapping("/reports/active")
    public ResponseEntity<List<ProductionReportSimpleViewDto>> getActiveReports() {
        try {
            List<ProductionReportSimpleViewDto> reports = qaService.getActiveReportsForQa();
            logger.debug("Found {} active reports for QA", reports.size());
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            logger.error("Error getting active reports for QA: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // **[ใหม่]** Get NG Types for QA
    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getNgTypesForQa() {
        try {
            List<NgType> ngTypes = qaService.getQaNgTypes();
            logger.debug("Found {} NG Types for QA", ngTypes.size());
            return ResponseEntity.ok(ngTypes);
        } catch (Exception e) {
            logger.error("Error getting NG Types for QA: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // **[ใหม่]** Get QA history for a specific report
    @GetMapping("/reports/{reportId}/qa-history")
    public ResponseEntity<?> getQaHistory(@PathVariable Long reportId) {
        try {
            var history = qaService.getQaHistoryForReport(reportId);
            logger.debug("Found {} QA history records for report {}", history.size(), reportId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error getting QA history for report {}: {}", reportId, e.getMessage(), e);
            return ResponseEntity.status(500).body("Error fetching QA history: " + e.getMessage());
        }
    }

    // **[ใหม่]** Update existing QA NG Log
    @PutMapping("/ng-logs/{ngLogId}")
    public ResponseEntity<?> updateQaNgLog(@PathVariable Long ngLogId, @RequestBody Map<String, Object> updateRequest, Principal principal) {
        try {
            Integer newQuantity = (Integer) updateRequest.get("quantity");
            if (newQuantity == null || newQuantity < 0) {
                return ResponseEntity.badRequest().body("จำนวนต้องเป็นตัวเลขที่ไม่ติดลบ");
            }
            qaService.updateQaNgLog(ngLogId, newQuantity, principal.getName());
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            logger.warn("Security error updating QA NG log {}: {}", ngLogId, e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating QA NG log {}: {}", ngLogId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<?> recordQaNgLog(@PathVariable Long reportId, @RequestBody NgLogRequestDto ngLogRequest, Principal principal) {
        try {
            qaService.recordQaNg(reportId, ngLogRequest, principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error recording QA NG log for report {}: {}", reportId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}