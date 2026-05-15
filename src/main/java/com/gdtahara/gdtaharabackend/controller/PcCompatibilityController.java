package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.service.ProductionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;

@RestController
@RequestMapping("/api/pc")
public class PcCompatibilityController {

    // No per-request logging needed here; keep lightweight
    private final ProductionService productionService;
    private static final Logger logger = LoggerFactory.getLogger(PcCompatibilityController.class);

    public PcCompatibilityController(ProductionService productionService) {
        this.productionService = productionService;
    }

    // Dashboard summary
    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<PcDashboardSummaryDto>> getDashboardSummary() {
        List<PcDashboardSummaryDto> list = productionService.getDashboardSummary();
        logger.info("PC dashboard-summary returned {} item(s)", list.size());
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore().cachePrivate());
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return ResponseEntity.ok().headers(headers).body(list);
    }

    // List all reports
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductionReportDto>> getAllReports() {
        return ResponseEntity.ok(productionService.getAllProductionReports());
    }

    // Active reports (with optional scope)
    @GetMapping("/reports/active")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductionReportDto>> getActiveReports(@RequestParam(name = "scope", required = false) String scope) {
        if ("today".equals(scope)) {
            List<ProductionReportDto> list = productionService.getTodaysActiveProductionReports();
            logger.info("PC active reports (today) returned {} item(s)", list.size());
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noStore().cachePrivate());
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            return ResponseEntity.ok().headers(headers).body(list);
        }
        List<ProductionReportDto> list = productionService.getActiveProductionReports();
        logger.info("PC active reports returned {} item(s)", list.size());
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore().cachePrivate());
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return ResponseEntity.ok().headers(headers).body(list);
    }

    // Machines & Products
    @GetMapping("/machines")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<MachineSimpleDto>> getMachinesForDropdown() {
        return ResponseEntity.ok(productionService.getMachineList());
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductSimpleDto>> getProductsForDropdown() {
        return ResponseEntity.ok(productionService.getProductList());
    }

    // Some frontend variants call /pc/production/machines|products
    @GetMapping("/production/machines")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<MachineSimpleDto>> getMachinesForDropdownAlt() {
        return ResponseEntity.ok(productionService.getMachineList());
    }

    @GetMapping("/production/products")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductSimpleDto>> getProductsForDropdownAlt() {
        return ResponseEntity.ok(productionService.getProductList());
    }

    // Additional aliases for older FE paths
    // GET /api/pc/production/reports -> same as /api/pc/reports
    @GetMapping("/production/reports")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductionReportDto>> getProductionReportsAlias() {
        return ResponseEntity.ok(productionService.getAllProductionReports());
    }

    // GET /api/pc/production/dashboard-summary -> same as /api/pc/dashboard-summary
    @GetMapping("/production/dashboard-summary")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<PcDashboardSummaryDto>> getProductionDashboardSummaryAlias() {
        List<PcDashboardSummaryDto> list = productionService.getDashboardSummary();
        logger.info("PC production/dashboard-summary returned {} item(s)", list.size());
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore().cachePrivate());
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return ResponseEntity.ok().headers(headers).body(list);
    }

    // --- FE alias for report summary/detailed under /production/... (to avoid 404) ---
    // GET /api/pc/production/reports/{id}/summary -> same as /api/pc/reports/{id}/summary
    @GetMapping("/production/reports/{id}/summary")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'DataAdmin', 'Management', 'Document')")
    public ResponseEntity<ReportSummaryDto> getProductionReportSummaryAlias(@PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        return ResponseEntity.ok(productionService.getReportSummary(id, lang));
    }

    // GET /api/pc/production/reports/{id}/detailed -> same as /api/pc/reports/{id}/detailed
    @GetMapping("/production/reports/{id}/detailed")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DetailedProductionReportDto> getProductionReportDetailedAlias(@PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        return ResponseEntity.ok(productionService.getDetailedReport(id, lang));
    }

    // Create/Update/Finalize report
    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<?> createReport(@RequestBody ReportCreateRequest request, Principal principal) {
        try {
            ProductionReportDto created = productionService.createProductionReport(request, principal != null ? principal.getName() : "");
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/reports/{id}")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody ReportCreateRequest request) {
        try {
            return ResponseEntity.ok(productionService.updateProductionReport(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reports/{id}/finalize")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<Void> finalizeReport(@PathVariable Long id) {
        try {
            productionService.finalizeReport(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Summaries
    @GetMapping("/reports/{id}/summary")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'DataAdmin', 'Management', 'Document')")
    public ResponseEntity<ReportSummaryDto> getReportSummary(@PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        return ResponseEntity.ok(productionService.getReportSummary(id, lang));
    }

    @GetMapping("/reports/{id}/detailed")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DetailedProductionReportDto> getDetailedReport(@PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        return ResponseEntity.ok(productionService.getDetailedReport(id, lang));
    }

    // --- Aliases for daily-shift summary under PC paths (for legacy FE compatibility) ---
    @GetMapping("/reports/summary/daily-shift")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DailyShiftSummaryDto> getDailySummaryByShift(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        DailyShiftSummaryDto dto = productionService.getDailyProductionSummaryByShift(date, machineId, productId);
        // Adjust NG summaries by language preference
        try {
            var dayNg = productionService.getShiftSpecificNgSummary(date, true, machineId, productId, lang);
            var nightNg = productionService.getShiftSpecificNgSummary(date, false, machineId, productId, lang);
            if (dto != null) {
                if (dto.getDayShiftData() != null) dto.getDayShiftData().setNgSummary(dayNg);
                if (dto.getNightShiftData() != null) dto.getNightShiftData().setNgSummary(nightNg);
            }
        } catch (Exception ignore) { }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/production/reports/summary/daily-shift")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DailyShiftSummaryDto> getDailySummaryByShiftAlias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
        String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
        DailyShiftSummaryDto dto = productionService.getDailyProductionSummaryByShift(date, machineId, productId);
        try {
            var dayNg = productionService.getShiftSpecificNgSummary(date, true, machineId, productId, lang);
            var nightNg = productionService.getShiftSpecificNgSummary(date, false, machineId, productId, lang);
            if (dto != null) {
                if (dto.getDayShiftData() != null) dto.getDayShiftData().setNgSummary(dayNg);
                if (dto.getNightShiftData() != null) dto.getNightShiftData().setNgSummary(nightNg);
            }
        } catch (Exception ignore) { }
        return ResponseEntity.ok(dto);
    }
}
