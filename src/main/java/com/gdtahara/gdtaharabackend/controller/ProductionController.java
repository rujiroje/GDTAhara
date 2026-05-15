// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/ProductionController.java
// (ฉบับแก้ไข เพิ่มสิทธิ์ให้ CM Operator)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.service.ProductionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/production") // เปลี่ยนจาก "/api/pc" เป็น "/api/production"
public class ProductionController {

    private static final Logger logger = LoggerFactory.getLogger(ProductionController.class);
    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/dashboard-summary") // เปลี่ยนจาก "/production/dashboard-summary" เป็น "/dashboard-summary"
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<PcDashboardSummaryDto>> getDashboardSummary() {
        logger.info("Fetching dashboard summary...");
        try {
            return ResponseEntity.ok(productionService.getDashboardSummary());
        } catch (Exception e) {
            logger.error("Error fetching dashboard summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/reports/{id}/report-summary")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<ReportSummaryDto> getReportSummary(
            @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        logger.info("Fetching report summary for report ID: {} (langHeader={}, langParam={})", id, acceptLanguage, langParam);
        try {
            String langPref = (langParam != null && !langParam.isBlank()) ? langParam
                    : (acceptLanguage != null && !acceptLanguage.isBlank() ? acceptLanguage : "th");
            String lang = langPref.toLowerCase().startsWith("en") ? "en" : "th";
            ReportSummaryDto summary = productionService.getReportSummary(id, lang);
            return ResponseEntity.ok(summary);
        } catch (EntityNotFoundException e) {
            logger.warn("Report not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching report summary for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductionReportDto>> getAllReports() {
        logger.info("Fetching all production reports...");
        try {
            return ResponseEntity.ok(productionService.getAllProductionReports());
        } catch (Exception e) {
            logger.error("Error fetching all production reports: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/machines")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<MachineSimpleDto>> getMachinesForDropdown() {
        logger.info("Fetching machine list for dropdown...");
        try {
            return ResponseEntity.ok(productionService.getMachineList());
        } catch (Exception e) {
            logger.error("Error fetching machine list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductSimpleDto>> getProductsForDropdown() {
        logger.info("Fetching product list for dropdown...");
        try {
            return ResponseEntity.ok(productionService.getProductList());
        } catch (Exception e) {
            logger.error("Error fetching product list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/active")
    // **[แก้ไข]** เพิ่ม 'CM Operator' เข้าไปใน Role ที่สามารถเข้าถึงได้
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<ProductionReportDto>> getActiveReports(@RequestParam(name = "scope", required = false) String scope) {
        logger.info("Fetching active production reports with scope: {}", scope);
        try {
            if ("today".equals(scope)) {
                return ResponseEntity.ok(productionService.getTodaysActiveProductionReports());
            }
            return ResponseEntity.ok(productionService.getActiveProductionReports());
        } catch (Exception e) {
            logger.error("Error fetching active production reports: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<?> createReport(@RequestBody ReportCreateRequest request, Principal principal) {
        logger.info("Creating a new production report by user: {}", principal.getName());
        try {
            ProductionReportDto createdReport = productionService.createProductionReport(request, principal.getName());
            return ResponseEntity.ok(createdReport);
        } catch (Exception e) {
            logger.error("Error creating production report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/reports/{id}")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<?> updateReport(@PathVariable Long id, @RequestBody ReportCreateRequest request) {
        logger.info("Updating production report with ID: {}", id);
        try {
            return ResponseEntity.ok(productionService.updateProductionReport(id, request));
        } catch (Exception e) {
            logger.error("Error updating production report with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/reports/{id}")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        logger.info("Deleting production report with ID: {}", id);
        try {
            productionService.deleteProductionReport(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting production report with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/reports/{id}/finalize")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
    public ResponseEntity<Void> finalizeReport(@PathVariable Long id) {
        logger.info("Finalizing production report with ID: {}", id);
        try {
            productionService.finalizeReport(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error finalizing production report with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }



    @GetMapping("/reports/{id}/detailed")
    @PreAuthorize("hasAnyRole('Production Control', 'Shift Leader', 'Operator', 'Technician', 'QA', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DetailedProductionReportDto> getDetailedReport(
            @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        logger.info("Fetching detailed report with ID: {} (langHeader={}, langParam={})", id, acceptLanguage, langParam);
        try {
            String langPref = (langParam != null && !langParam.isBlank()) ? langParam
                    : (acceptLanguage != null && !acceptLanguage.isBlank() ? acceptLanguage : "th");
            String lang = langPref.toLowerCase().startsWith("en") ? "en" : "th";
            return ResponseEntity.ok(productionService.getDetailedReport(id, lang));
        } catch (Exception e) {
            logger.error("Error fetching detailed report with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/reports/production-historical")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<HistoricalReportSummaryDto>> getProductionHistoricalReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) Long productId) {
        
        logger.info("Getting historical reports from {} to {}, machineId={}, productId={}", 
                    startDate, endDate, machineId, productId);
                    
        try {
            // ตรวจสอบว่า startDate และ endDate ถูกต้อง
            if (startDate.isAfter(endDate)) {
                logger.error("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            // ดึงข้อมูลจาก Service
            List<HistoricalReportSummaryDto> historicalReports = productionService.getHistoricalReports(startDate, endDate, machineId, productId);
            
            if (historicalReports == null) {
                logger.warn("Historical reports result is null");
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            logger.info("Found {} historical reports", historicalReports.size());
            
            if (historicalReports.isEmpty()) {
                logger.info("No historical reports found for the specified criteria");
            } else {
                logger.debug("First report: {}", historicalReports.get(0));
            }

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(historicalReports.size()))
                    .body(historicalReports);
        } catch (Exception e) {
            logger.error("Error getting historical reports", e);
            return ResponseEntity.status(500)
                    .header("X-Error-Message", e.getMessage())
                    .body(new ArrayList<>());
        }
    }
    
    @GetMapping("/reports/summary/daily")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DailyProductionSummaryDto> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        logger.info("Fetching daily production summary for date: {}, machineId: {}, productId: {}, langHeader={}, langParam={}", date, machineId, productId, acceptLanguage, langParam);
        try {
            String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
            String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
            return ResponseEntity.ok(productionService.getDailyProductionSummary(date, machineId, productId, lang));
        } catch (Exception e) {
            logger.error("Error fetching daily production summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/summary/daily-shift")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DailyShiftSummaryDto> getDailySummaryByShift(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        logger.info("Fetching daily production summary by shift for date: {}, machineId: {}, productId: {}, langHeader={}, langParam={}", date, machineId, productId, acceptLanguage, langParam);
        try {
            DailyShiftSummaryDto dto = productionService.getDailyProductionSummaryByShift(date, machineId, productId);
            String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
            String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
            try {
                var dayNg = productionService.getShiftSpecificNgSummary(date, true, machineId, productId, lang);
                var nightNg = productionService.getShiftSpecificNgSummary(date, false, machineId, productId, lang);
                if (dto != null) {
                    if (dto.getDayShiftData() != null) dto.getDayShiftData().setNgSummary(dayNg);
                    if (dto.getNightShiftData() != null) dto.getNightShiftData().setNgSummary(nightNg);
                }
            } catch (Exception ignore) { }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching daily production summary by shift: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/summary/daily-by-shift")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<DailyShiftSummaryDto> getDailySummaryByShiftAlias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestParam(value = "lang", required = false) String langParam) {
        logger.info("Fetching daily production summary by shift (alias) for date: {}, machineId: {}, productId: {}, langHeader={}, langParam={}", date, machineId, productId, acceptLanguage, langParam);
        try {
            DailyShiftSummaryDto dto = productionService.getDailyProductionSummaryByShift(date, machineId, productId);
            String pref = (langParam != null && !langParam.isBlank()) ? langParam : acceptLanguage;
            String lang = (pref != null && pref.toLowerCase().startsWith("en")) ? "en" : "th";
            try {
                var dayNg = productionService.getShiftSpecificNgSummary(date, true, machineId, productId, lang);
                var nightNg = productionService.getShiftSpecificNgSummary(date, false, machineId, productId, lang);
                if (dto != null) {
                    if (dto.getDayShiftData() != null) dto.getDayShiftData().setNgSummary(dayNg);
                    if (dto.getNightShiftData() != null) dto.getNightShiftData().setNgSummary(nightNg);
                }
            } catch (Exception ignore) { }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching daily production summary by shift (alias): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }



    @GetMapping("/reports/available-dates")
    @PreAuthorize("hasAnyRole('Production Control', 'DataAdmin', 'Management', 'Document', 'CM Operator')")
    public ResponseEntity<List<String>> getAvailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String orderNumber) {
        logger.info("Fetching available dates between {} and {} with filters machineId='{}', productId='{}', orderNumber='{}'",
                startDate, endDate, machineId, productId, orderNumber);
        try {
            List<String> availableDates = productionService.getAvailableDates(startDate, endDate, machineId, productId, orderNumber);
            logger.info("Found {} available dates", availableDates.size());
            return ResponseEntity.ok(availableDates);
        } catch (Exception e) {
            logger.error("Error fetching available dates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/material-usage")
    @PreAuthorize("hasAnyRole('Production Control', 'Technician', 'DataAdmin')")
    public ResponseEntity<MaterialUsageLogDto> logMaterialUsage(@RequestBody MaterialUsageLogRequestDto request) {
        logger.info("Logging material usage...");
        try {
            MaterialUsageLogDto loggedUsage = productionService.logMaterialUsage(request);
            return ResponseEntity.ok(loggedUsage);
        } catch (EntityNotFoundException e) {
            logger.error("Material usage logging failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}