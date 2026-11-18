package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.CmBomItemDto;
import com.gdtahara.gdtaharabackend.dto.StockOutRequestDto;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.service.CmOperatorService;
import com.gdtahara.gdtaharabackend.service.MaterialStockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cm-operator")
@PreAuthorize("hasAnyRole('CM Operator', 'DataAdmin')")
public class CmOperatorController {

    private final CmOperatorService cmOperatorService;
    private final MaterialStockService materialStockService;

    public CmOperatorController(CmOperatorService cmOperatorService, MaterialStockService materialStockService) {
        this.cmOperatorService = cmOperatorService;
        this.materialStockService = materialStockService;
    }

    /** Single stock-out (kept for backwards compatibility). */
    @PostMapping("/stock-out")
    public ResponseEntity<?> recordStockOut(@Valid @RequestBody StockOutRequestDto request, Principal principal) {
        try {
            MaterialStockTransaction tx = cmOperatorService.recordStockOut(request, principal.getName());
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Batch stock-out — submit all BOM materials at once. */
    @PostMapping("/stock-out/batch")
    public ResponseEntity<?> batchRecordStockOut(@Valid @RequestBody List<StockOutRequestDto> requests, Principal principal) {
        try {
            List<MaterialStockTransaction> txs = cmOperatorService.batchRecordStockOut(requests, principal.getName());
            return ResponseEntity.ok(txs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** BOM raw-material items for a report (non-UNBW, non-scrap), with materialId + available lots. */
    @GetMapping("/bom-items/{reportId}")
    public ResponseEntity<List<CmBomItemDto>> getBomItems(@PathVariable Long reportId) {
        return ResponseEntity.ok(cmOperatorService.getBomItemsForReport(reportId));
    }

    /** Available lot numbers for a material (IN transactions). */
    @GetMapping("/materials/{materialId}/lot-numbers")
    public ResponseEntity<List<String>> getAvailableLots(@PathVariable Long materialId) {
        return ResponseEntity.ok(materialStockService.getAvailableLotNumbers(materialId));
    }
}
