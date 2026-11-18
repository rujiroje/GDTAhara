package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.MaterialReorderConfig;
import com.gdtahara.gdtaharabackend.repository.MaterialReorderConfigRepository;
import com.gdtahara.gdtaharabackend.service.CmStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cm/stock")
@PreAuthorize("hasAnyRole('CM Operator', 'DataAdmin')")
@RequiredArgsConstructor
public class CmStockController {

    private final CmStockService stockService;
    private final MaterialReorderConfigRepository reorderConfigRepo;

    // ── 1. Overview — all materials summary ──────────────────────────────────

    @GetMapping("/overview")
    public ResponseEntity<StockOverviewDto> getOverview() {
        return ResponseEntity.ok(stockService.getOverview());
    }

    // ── 2. Material detail — per-lot breakdown + trend ───────────────────────

    @GetMapping("/{materialCode}")
    public ResponseEntity<MaterialDetailDto> getMaterialDetail(
            @PathVariable String materialCode) {
        return ResponseEntity.ok(stockService.getMaterialDetail(materialCode));
    }

    // ── 3. Stock card — transaction log with filters ─────────────────────────

    @GetMapping("/{materialCode}/stock-card")
    public ResponseEntity<StockCardResponseDto> getStockCard(
            @PathVariable String materialCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) String lotRef) {
        return ResponseEntity.ok(stockService.getStockCard(materialCode, from, to, machineId, lotRef));
    }

    // ── 4. Multi-material summary comparison ─────────────────────────────────

    @GetMapping("/multi-material-summary")
    public ResponseEntity<List<MultiMaterialSummaryItemDto>> getMultiMaterialSummary(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(stockService.getMultiMaterialSummary(days));
    }

    // ── 5. Active low-stock alerts ───────────────────────────────────────────

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockAlertDto>> getLowStockAlerts() {
        return ResponseEntity.ok(stockService.getActiveLowStockAlerts());
    }

    // ── 6. Reorder config (admin read + update) ──────────────────────────────

    @GetMapping("/reorder-config")
    @PreAuthorize("hasAnyRole('DataAdmin')")
    public ResponseEntity<List<MaterialReorderConfig>> getReorderConfig() {
        return ResponseEntity.ok(reorderConfigRepo.findAll());
    }

    @PatchMapping("/reorder-config/{materialCode}")
    @PreAuthorize("hasRole('DataAdmin')")
    public ResponseEntity<MaterialReorderConfig> updateReorderConfig(
            @PathVariable String materialCode,
            @RequestBody Map<String, Object> patch,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
                    org.springframework.security.core.userdetails.UserDetails user) {
        MaterialReorderConfig cfg = reorderConfigRepo.findById(materialCode)
                .orElseGet(() -> { MaterialReorderConfig c = new MaterialReorderConfig(); c.setMaterialCode(materialCode); return c; });
        if (patch.containsKey("reorderPointKg"))
            cfg.setReorderPointKg(new java.math.BigDecimal(patch.get("reorderPointKg").toString()));
        if (patch.containsKey("safetyStockKg"))
            cfg.setSafetyStockKg(new java.math.BigDecimal(patch.get("safetyStockKg").toString()));
        if (patch.containsKey("reorderQtyKg"))
            cfg.setReorderQtyKg(new java.math.BigDecimal(patch.get("reorderQtyKg").toString()));
        if (patch.containsKey("leadTimeDays"))
            cfg.setLeadTimeDays(Integer.parseInt(patch.get("leadTimeDays").toString()));
        if (patch.containsKey("isActive"))
            cfg.setIsActive(Boolean.parseBoolean(patch.get("isActive").toString()));
        cfg.setUpdatedBy(user.getUsername());
        cfg.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(reorderConfigRepo.save(cfg));
    }
}
