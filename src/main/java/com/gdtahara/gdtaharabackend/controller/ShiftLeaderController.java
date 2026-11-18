// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/ShiftLeaderController.java
// (ฉบับแก้ไข เพิ่ม Endpoint ดึงประวัติ NG)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.dto.StockSummaryDto;
import com.gdtahara.gdtaharabackend.service.ShiftLeaderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/shift-leader")
@PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'Document', 'ADMIN')")
    public ResponseEntity<MaterialStockTransaction> createStockTransaction(@Valid @RequestBody StockTransactionRequestDto request, Principal principal) {
        return ResponseEntity.ok(shiftLeaderService.recordStockTransaction(request, principal.getName()));
    }

    @PutMapping("/stock-transactions/{id}")
    @PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'Document', 'ADMIN')")
    public ResponseEntity<MaterialStockTransaction> updateStockTransaction(@PathVariable Long id, @Valid @RequestBody StockTransactionRequestDto request, Principal principal) {
        return ResponseEntity.ok(shiftLeaderService.updateStockTransaction(id, request, principal.getName()));
    }

    @PostMapping("/reports/{reportId}/ng-logs")
    public ResponseEntity<NgLog> createNgLog(@PathVariable Long reportId, @Valid @RequestBody NgLogRequestDto request, Principal principal) {
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
    @PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'Document', 'ADMIN')")
    public ResponseEntity<List<MaterialStockCardDto>> getMaterialStocks() {
        return ResponseEntity.ok(shiftLeaderService.getMaterialStocks());
    }

    /** Lightweight: 2 SQL queries, no history payload — ใช้โหลดหน้าแรก */
    @GetMapping("/stock-summary")
    @PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'Document', 'ADMIN')")
    public ResponseEntity<List<StockSummaryDto>> getStockSummaries() {
        return ResponseEntity.ok(shiftLeaderService.getStockSummaries());
    }

    /** On-demand history for ONE material — เรียกเมื่อกด "ดูประวัติ" */
    @GetMapping("/stock-history/{materialId}")
    @PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin', 'Production Control', 'Document', 'ADMIN')")
    public ResponseEntity<MaterialStockCardDto> getStockHistory(@PathVariable Long materialId) {
        return ResponseEntity.ok(shiftLeaderService.getStockCardForMaterial(materialId));
    }

}