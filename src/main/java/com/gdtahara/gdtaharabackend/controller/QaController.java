// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/QaController.java
// (**แก้ไข** เพิ่ม endpoints ครบถ้วนสำหรับ QA)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.service.QaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@PreAuthorize("hasAnyAuthority('ROLE_QA', 'ROLE_DataAdmin')")
public class QaController {

    @Autowired
    private QaService qaService;

    // **[ใหม่]** Get active reports for QA
    @GetMapping("/reports/active")
    public ResponseEntity<List<ProductionReportSimpleViewDto>> getActiveReports() {
        try {
            System.out.println("🔍 QA getting active reports...");
            List<ProductionReportSimpleViewDto> reports = qaService.getActiveReportsForQa();
            System.out.println("📊 Found " + reports.size() + " active reports for QA");
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.out.println("❌ Error getting active reports for QA: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // **[ใหม่]** Get NG Types for QA
    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getNgTypesForQa() {
        try {
            System.out.println("🔍 QA getting NG Types...");
            List<NgType> ngTypes = qaService.getQaNgTypes();
            System.out.println("📊 Found " + ngTypes.size() + " NG Types for QA");
            return ResponseEntity.ok(ngTypes);
        } catch (Exception e) {
            System.out.println("❌ Error getting NG Types for QA: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // **[ใหม่]** Get QA history for a specific report
    @GetMapping("/reports/{reportId}/qa-history")
    public ResponseEntity<?> getQaHistory(@PathVariable Long reportId) {
        try {
            System.out.println("🔍 QA getting history for report " + reportId);
            var history = qaService.getQaHistoryForReport(reportId);
            System.out.println("📊 Found " + history.size() + " QA history records");
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.out.println("❌ Error getting QA history: " + e.getMessage());
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
            
            System.out.println("🔄 QA updating NG log " + ngLogId + " with quantity " + newQuantity + " by " + principal.getName());
            
            qaService.updateQaNgLog(ngLogId, newQuantity, principal.getName());
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            System.out.println("🚫 Security error: " + e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error updating QA NG log: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<?> recordQaNgLog(@PathVariable Long reportId, @RequestBody NgLogRequestDto ngLogRequest, Principal principal) {
        try {
            System.out.println("💾 QA recording NG log for report " + reportId + " by " + principal.getName());
            qaService.recordQaNg(reportId, ngLogRequest, principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("❌ Error recording QA NG log: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}