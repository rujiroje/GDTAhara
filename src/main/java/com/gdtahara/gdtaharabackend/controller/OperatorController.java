// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/OperatorController.java
// (ฉบับแก้ไข เพิ่ม Endpoint ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.PackagingLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProblemAlertRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.model.PackagingLog;
import com.gdtahara.gdtaharabackend.service.OperatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator")
@PreAuthorize("hasAnyRole('Operator', 'DataAdmin')")
public class OperatorController {

    private static final Logger logger = LoggerFactory.getLogger(OperatorController.class);

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    // **[ใหม่]** Endpoint สำหรับดึงรายการ active reports สำหรับ Operator
    @GetMapping("/reports/active")
    public ResponseEntity<List<ProductionReportSimpleViewDto>> getActiveReports(@RequestParam(required = false) String scope) {
        try {
            List<ProductionReportSimpleViewDto> activeReports = operatorService.getActiveReportsForOperator();
            return ResponseEntity.ok(activeReports);
        } catch (Exception e) {
            logger.error("Error getting active reports for operator: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    // **[ใหม่]** Test endpoint สำหรับ Operator
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testOperator() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Operator backend is working");
        result.put("timestamp", LocalDateTime.now());
        result.put("role", "Operator");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getNgTypesForOperator() {
        try {
            List<NgType> operatorNgTypes = operatorService.getOperatorNgTypes();
            logger.debug("Returning {} NG types for operator", operatorNgTypes.size());
            return ResponseEntity.ok(operatorNgTypes);
        } catch (Exception e) {
            logger.error("Error getting NG types for operator: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    // **[แก้ไข]** Endpoint สำหรับดึงยอดรวม NG รายชั่วโมงของ Operator คนนั้นเอง
    @GetMapping("/reports/{reportId}/hourly-ng-summary")
    public ResponseEntity<Map<String, Long>> getHourlyNgSummary(@PathVariable Long reportId, Principal principal) {
        return ResponseEntity.ok(operatorService.getHourlyNgSummary(reportId, principal.getName()));
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<?> recordNgLog(@PathVariable Long reportId, @Valid @RequestBody NgLogRequestDto request, Principal principal) {
        try {
            NgLog createdLog = operatorService.recordNgLog(reportId, request, principal.getName());
            return ResponseEntity.ok(createdLog);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/packaging-logs")
    public ResponseEntity<?> recordPackaging(@PathVariable Long reportId, @Valid @RequestBody PackagingLogRequestDto request, Principal principal) {
        try {
            PackagingLog createdLog = operatorService.recordPackaging(reportId, request, principal.getName());
            return ResponseEntity.ok(createdLog);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{reportId}/alert")
    public ResponseEntity<?> createProblemAlert(@PathVariable Long reportId, @RequestBody ProblemAlertRequestDto request, Principal principal) {
        try {
            operatorService.createProblemAlert(reportId, request, principal.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reports/{reportId}/next-box-no")
    public ResponseEntity<Integer> getNextBoxNumber(@PathVariable Long reportId, @RequestParam String lotNumber) {
        int nextBoxNo = operatorService.getNextBoxNumber(reportId, lotNumber);
        return ResponseEntity.ok(nextBoxNo);
    }
}