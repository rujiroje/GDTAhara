// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/ShiftLeaderController.java
// (ฉบับแก้ไข เพิ่ม Endpoint ดึงประวัติ NG)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.service.ShiftLeaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/shift-leader")
@PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control')")
public class ShiftLeaderController {

    private final ShiftLeaderService shiftLeaderService;

    public ShiftLeaderController(ShiftLeaderService shiftLeaderService) {
        this.shiftLeaderService = shiftLeaderService;
    }

    @GetMapping("/active-reports")
    public ResponseEntity<List<ProductionReportSimpleViewDto>> getActiveReports() { return ResponseEntity.ok(shiftLeaderService.getActiveReportsForShiftLeader()); }
    @GetMapping("/dashboard/{reportId}")
    public ResponseEntity<ShiftLeaderDashboardDto> getDashboardData(@PathVariable Long reportId) { return ResponseEntity.ok(shiftLeaderService.getDashboardData(reportId)); }

    @PostMapping("/stock-transactions")
    public ResponseEntity<MaterialStockTransaction> createStockTransaction(@RequestBody StockTransactionRequestDto request, Principal principal) {
        return ResponseEntity.ok(shiftLeaderService.recordStockTransaction(request, principal.getName()));
    }
    
    @PutMapping("/stock-transactions/{id}")
    public ResponseEntity<MaterialStockTransaction> updateStockTransaction(@PathVariable Long id, @RequestBody StockTransactionRequestDto request) {
        return ResponseEntity.ok(shiftLeaderService.updateStockTransaction(id, request));
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<NgLog> createNgLog(@PathVariable Long reportId, @RequestBody NgLogRequestDto request, Principal principal) {
        return ResponseEntity.ok(shiftLeaderService.recordNgLog(reportId, request, principal.getName()));
    }

    // **[ใหม่]** เพิ่ม Endpoint สำหรับดึงประวัติ NG Log ของ Shift Leader
    @GetMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<List<NgLog>> getNgLogs(@PathVariable Long reportId) {
        return ResponseEntity.ok(shiftLeaderService.getNgLogsForReport(reportId));
    }

    // **[เพิ่มใหม่]** Endpoints สำหรับการจัดการ Label Stock
    @GetMapping("/label-stocks")
    public ResponseEntity<List<LabelStockDto>> getLabelStocks() {
        return ResponseEntity.ok(shiftLeaderService.getLabelStocks());
    }

    @PostMapping("/label-stocks/{productId}/add")
    public ResponseEntity<String> addLabelStock(@PathVariable Long productId, @RequestBody AddLabelStockRequest request) {
        shiftLeaderService.addLabelStock(productId, request.getQuantityToAdd());
        return ResponseEntity.ok("Label stock added successfully");
    }

    // **[เพิ่มใหม่]** Endpoints สำหรับการจัดการ Material Stock
    @GetMapping("/material-stocks")
    public ResponseEntity<List<MaterialStockCardDto>> getMaterialStocks() {
        return ResponseEntity.ok(shiftLeaderService.getMaterialStocks());
    }

    // **[เพิ่มใหม่]** Endpoint สำหรับสร้างข้อมูลตัวอย่างในการทดสอบ
    // เพิ่ม simple endpoint ที่ไม่ใช้ database queries ที่ซับซ้อน
    @GetMapping("/simple-test")
    public ResponseEntity<Map<String, Object>> simpleTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Simple test endpoint working");
        result.put("timestamp", LocalDateTime.now());
        result.put("backend", "connected");
        return ResponseEntity.ok(result);
    }

    // เพิ่ม endpoint สำหรับตรวจสอบการทำงาน
    @GetMapping("/test-dashboard")
    public ResponseEntity<Map<String, Object>> testDashboard() {
        Map<String, Object> result = new HashMap<>();
        try {
            // ตรวจสอบข้อมูลแต่ละส่วน
            List<ProductionReportSimpleViewDto> activeReports = shiftLeaderService.getActiveReportsForShiftLeader();
            List<String> ngTypes = shiftLeaderService.getNgTypes();
            List<String> machines = shiftLeaderService.getMachines();
            
            result.put("activeReports", activeReports);
            result.put("activeReportsCount", activeReports.size());
            result.put("ngTypes", ngTypes);
            result.put("ngTypesCount", ngTypes.size());
            result.put("machines", machines);
            result.put("machinesCount", machines.size());
            result.put("status", "success");
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(result);
        }
    }

    // เพิ่ม endpoint สำหรับสร้างข้อมูลทดสอบที่ปลอดภัย
    @PostMapping("/create-test-data")
    public ResponseEntity<Map<String, Object>> createTestData() {
        Map<String, Object> response = new HashMap<>();
        try {
            // สร้างข้อมูลทดสอบพื้นฐาน
            String result = shiftLeaderService.createSampleDataForTesting();
            response.put("status", "success");
            response.put("message", result);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error creating test data: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ปิดใช้งาน sample data endpoint - ใช้เฉพาะข้อมูลจริงเท่านั้น
    // @PostMapping("/create-sample-data")
    // public ResponseEntity<Map<String, String>> createSampleData() {
    //     Map<String, String> response = new HashMap<>();
    //     response.put("status", "disabled");
    //     response.put("message", "Sample data creation has been disabled. Use only real data.");
    //     return ResponseEntity.ok(response);
    // }

    // --- Centralized error handling for clearer client responses ---
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}