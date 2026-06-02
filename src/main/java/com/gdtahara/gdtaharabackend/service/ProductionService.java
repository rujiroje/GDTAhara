package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductionService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionService.class);

    @Autowired
    private ProductionReportRepository productionReportRepository;
    
    @Autowired
    private MachineRepository machineRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private PackagingLogRepository packagingLogRepository;
    
    @Autowired
    private NgLogRepository ngLogRepository;
    
    @Autowired
    private MaterialUsageLogRepository materialUsageLogRepository;

    @Autowired
    private DowntimeEventRepository downtimeEventRepository;

    @Autowired
    private ScrapWeightLogRepository scrapWeightLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    // @Lazy breaks any potential circular dependency and defers proxy creation
    @Autowired @Lazy
    private WoExpansionService woExpansionService;

    @Transactional(readOnly = true)
    public List<ProductionReportDto> getAllProductionReports() {
        try {
            logger.info("🔍 ProductionService.getAllProductionReports() called");
            List<ProductionReport> reports = productionReportRepository.findAllWithFetch();
            logger.info("📊 Found {} production reports", reports.size());
            
            return reports.stream()
                    .map(this::convertToProductionReportDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error in getAllProductionReports(): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public ProductionReportDto createProductionReport(ReportCreateRequest request) {
        // Delegate to the username-aware method to ensure pc is set and validations are applied
        return createProductionReport(request, null);
    }

    @Transactional
    public ProductionReportDto createProductionReport(ReportCreateRequest request, String username) {
        try {
            // Basic validations (provide specific messages back to caller)
            if (request == null) {
                throw new IllegalArgumentException("คำขอว่าง (request = null)");
            }
            logger.info("🆕 Creating new production report: {} for user: {}", request.getOrderNumber(), username);
            if (request.getOrderNumber() == null || request.getOrderNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("กรุณาระบุหมายเลขคำสั่งผลิต (orderNumber)");
            }
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new IllegalArgumentException("กรุณาระบุช่วงวันที่เริ่มและสิ้นสุด (startDate/endDate)");
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("ช่วงวันที่ไม่ถูกต้อง: startDate อยู่หลัง endDate");
            }
            if (request.getMachineId() == null) {
                throw new IllegalArgumentException("กรุณาเลือกเครื่องจักร (machineId)");
            }
            if (request.getProductId() == null) {
                throw new IllegalArgumentException("กรุณาเลือกผลิตภัณฑ์ (productId)");
            }
            if (request.getTargetQty() == null) {
                throw new IllegalArgumentException("กรุณาระบุเป้าหมายการผลิต (targetQty)");
            }
            if (request.getTargetQty() < 0) {
                throw new IllegalArgumentException("เป้าหมายการผลิต (targetQty) ต้องเป็นเลขศูนย์หรือมากกว่า");
            }

            // Lookups
            Machine machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new EntityNotFoundException("ไม่พบเครื่องจักร (machineId=" + request.getMachineId() + ")"));
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("ไม่พบผลิตภัณฑ์ (productId=" + request.getProductId() + ")"));

            // Duplicate date/machine check — prevent two active reports on the same machine in overlapping period
            List<ProductionReport> conflicts = productionReportRepository.findActiveOverlappingByMachine(
                    request.getMachineId(), request.getStartDate(), request.getEndDate());
            if (!conflicts.isEmpty()) {
                ProductionReport conflict = conflicts.get(0);
                throw new IllegalArgumentException(
                        "ไม่สามารถสร้างใบสั่งผลิตได้: เครื่องจักร \"" + machine.getMachineName() + "\" " +
                        "มีใบสั่งผลิต [" + conflict.getOrderNumber() + "] " +
                        "อยู่แล้วในช่วงวันที่ " + conflict.getStartDate() + " ถึง " + conflict.getEndDate() +
                        " (สถานะ: " + conflict.getStatus() + ") กรุณาเลือกช่วงวันที่อื่น หรือยกเลิกใบสั่งผลิตเดิมก่อน");
            }

            // PC user is mandatory (pc_id is NOT NULL). Try principal first, then fallback by role.
            User pcUser = null;
            try {
                if (username != null && !username.isBlank()) {
                    pcUser = userRepository.findByUsername(username).orElse(null);
                }
                if (pcUser == null) {
                    // Fallback: first Production Control, else first DataAdmin
                    List<User> pcs = userRepository.findByRole("Production Control");
                    if (pcs != null && !pcs.isEmpty()) {
                        pcUser = pcs.get(0);
                    } else {
                        List<User> admins = userRepository.findByRole("DataAdmin");
                        if (admins != null && !admins.isEmpty()) {
                            pcUser = admins.get(0);
                        }
                    }
                }
            } catch (Exception ignore) {
                // keep pcUser as null
            }
            if (pcUser == null) {
                throw new IllegalStateException("ไม่พบผู้ใช้สำหรับกำหนด PC (pc_id). กรุณาเข้าสู่ระบบใหม่หรือติดต่อผู้ดูแลระบบ");
            }

            // Build and persist entity
            ProductionReport report = new ProductionReport();
            report.setOrderNumber(request.getOrderNumber());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setTargetQty(request.getTargetQty());
            // Derive initial status based on date window policy
            String initialStatus = deriveStatusForDates(request.getStartDate(), request.getEndDate(), null);
            report.setStatus(initialStatus);
            report.setMachine(machine);
            report.setProduct(product);
            report.setPc(pcUser);

            // Auto-generate parentLotNumber to satisfy uk_pr_parent_lot_number.
            // SQL Server UNIQUE allows only ONE NULL; subsequent inserts with NULL fail.
            // Format: {machineCode}-{yyyyMMdd}-{sanitisedOrderNumber}  e.g. "RBL101-20260602-PO3520"
            String machinePart = machine.getMachineCode() != null
                    ? machine.getMachineCode() : String.valueOf(machine.getId());
            String datePart    = request.getStartDate().toString().replace("-", "");
            String orderPart   = request.getOrderNumber().trim()
                    .replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            String lotNum = machinePart + "-" + datePart + "-" + orderPart;
            if (lotNum.length() > 95) lotNum = lotNum.substring(0, 95);
            report.setParentLotNumber(lotNum);

            ProductionReport saved = productionReportRepository.save(report);
            logger.info("✅ Production report created with ID: {} by PC: {}", saved.getId(), pcUser.getUsername());
            auditLogService.log("CREATE", "ProductionReport", saved.getId(), null,
                    java.util.Map.of("id", saved.getId(), "orderNumber", String.valueOf(saved.getOrderNumber()),
                            "status", String.valueOf(saved.getStatus())));

            // Expand WO into daily ProductionPlans (REQUIRES_NEW — never rolls back WO creation)
            try {
                WoExpansionService.ExpansionResult exp =
                        woExpansionService.expandWoToDailyPlans(saved, username);
                logger.info("WO daily-plan expansion: created={} skipPast={} skipExisting={}",
                        exp.created(), exp.skipPast(), exp.skipExisting());
            } catch (Exception ex) {
                logger.warn("Daily-plan expansion failed for WO id={}: {}", saved.getId(), ex.getMessage());
            }

            return convertToProductionReportDto(saved);
        } catch (IllegalArgumentException | EntityNotFoundException | IllegalStateException ex) {
            // surface clear message to controller (HTTP 400)
            logger.warn("⚠️ Validation failed creating production report: {}", ex.getMessage());
            throw ex;
        } catch (Exception e) {
            // include cause message for easier troubleshooting
            logger.error("❌ Error creating production report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create production report: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ProductionReportDto updateProductionReport(Long id, ReportCreateRequest request) {
        return updateProductionReport(id, request, null);
    }

    @Transactional
    public ProductionReportDto updateProductionReport(Long id, ReportCreateRequest request, String username) {
        try {
            logger.info("🔄 Updating production report ID: {}", id);

            ProductionReport report = productionReportRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Production report not found with id: " + id));

            String oldStatus = report.getStatus();
            String oldOrder = report.getOrderNumber();

            report.setOrderNumber(request.getOrderNumber());
            report.setStartDate(request.getStartDate());
            report.setEndDate(request.getEndDate());
            report.setTargetQty(request.getTargetQty());

            Machine machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new EntityNotFoundException("Machine not found with id: " + request.getMachineId()));
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

            // Duplicate date/machine check on update — exclude this report from conflict search
            List<ProductionReport> conflicts = productionReportRepository.findActiveOverlappingByMachineExcluding(
                    request.getMachineId(), request.getStartDate(), request.getEndDate(), id);
            if (!conflicts.isEmpty()) {
                ProductionReport conflict = conflicts.get(0);
                throw new IllegalArgumentException(
                        "ไม่สามารถแก้ไขใบสั่งผลิตได้: เครื่องจักร \"" + machine.getMachineName() + "\" " +
                        "มีใบสั่งผลิต [" + conflict.getOrderNumber() + "] " +
                        "อยู่แล้วในช่วงวันที่ " + conflict.getStartDate() + " ถึง " + conflict.getEndDate() +
                        " (สถานะ: " + conflict.getStatus() + ") กรุณาเลือกช่วงวันที่อื่น");
            }

            report.setMachine(machine);
            report.setProduct(product);
            String existing = report.getStatus();
            if (!isTerminalStatus(existing)) {
                report.setStatus(deriveStatusForDates(report.getStartDate(), report.getEndDate(), existing));
            }

            ProductionReport savedReport = productionReportRepository.save(report);
            logger.info("✅ Production report updated successfully");
            auditLogService.log("UPDATE", "ProductionReport", id,
                    java.util.Map.of("id", id, "orderNumber", String.valueOf(oldOrder), "status", String.valueOf(oldStatus)),
                    java.util.Map.of("id", savedReport.getId(), "orderNumber", String.valueOf(savedReport.getOrderNumber()),
                            "status", String.valueOf(savedReport.getStatus())));

            // Re-expand WO into daily plans (skips existing, so safe to call on update)
            try {
                WoExpansionService.ExpansionResult exp =
                        woExpansionService.expandWoToDailyPlans(savedReport, username);
                logger.info("WO daily-plan re-expansion: created={} skipPast={} skipExisting={}",
                        exp.created(), exp.skipPast(), exp.skipExisting());
            } catch (Exception ex) {
                logger.warn("Daily-plan expansion failed for WO id={}: {}", id, ex.getMessage());
            }

            return convertToProductionReportDto(savedReport);
        } catch (Exception e) {
            logger.error("❌ Error updating production report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update production report", e);
        }
    }

    @Transactional
    public void deleteProductionReport(Long id) {
        deleteProductionReport(id, null);
    }

    @Transactional
    public void deleteProductionReport(Long id, String username) {
        try {
            logger.info("🗑️ Deleting production report ID: {}", id);

            ProductionReport report = productionReportRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Production report not found with id: " + id));

            auditLogService.log("DELETE", "ProductionReport", id,
                    java.util.Map.of("id", id, "orderNumber", String.valueOf(report.getOrderNumber()),
                            "status", String.valueOf(report.getStatus())), null);
            productionReportRepository.delete(report);
            logger.info("✅ Production report deleted successfully");
        } catch (Exception e) {
            logger.error("❌ Error deleting production report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete production report", e);
        }
    }

    @Transactional
    public ProductionReportDto finalizeProductionReport(Long id) {
        try {
            logger.info("🏁 Finalizing production report ID: {}", id);
            
            ProductionReport report = productionReportRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Production report not found with id: " + id));
            
            // ปรับสถานะให้สอดคล้องกับข้อมูลในฐานข้อมูล
            report.setStatus("COMPLETED");
            ProductionReport savedReport = productionReportRepository.save(report);
            logger.info("✅ Production report finalized successfully");
            
            return convertToProductionReportDto(savedReport);
        } catch (Exception e) {
            logger.error("❌ Error finalizing production report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to finalize production report", e);
        }
    }

    @Transactional
    public void finalizeReport(Long id) {
        finalizeReport(id, null);
    }

    @Transactional
    public void finalizeReport(Long id, String username) {
        finalizeProductionReport(id);
        auditLogService.log("FINALIZE", "ProductionReport", id,
                java.util.Map.of("id", id), java.util.Map.of("id", id, "status", "COMPLETED"));
    }

    @Transactional(readOnly = true)
    public List<PcDashboardSummaryDto> getDashboardSummary() {
        try {
            logger.info("📊 Getting dashboard summary - START");
            
            // Dashboard แสดงเฉพาะงานที่กำลังทำอยู่: ใช้การตรวจแบบยืดหยุ่น
            List<ProductionReport> activeReports = getActiveReportsRobust();
            logger.info("📊 Active reports for dashboard: {} record(s)", activeReports.size());
            
            // Log each report for debugging
            activeReports.forEach(report -> {
                logger.info("📄 Processing report ID: {}, Machine: {}, Product: {}, Status: {}", 
                    report.getId(), 
                    report.getMachine() != null ? report.getMachine().getMachineName() : "null",
                    report.getProduct() != null ? report.getProduct().getProductName() : "null",
                    report.getStatus());
            });
            
            List<PcDashboardSummaryDto> results = activeReports.stream()
                    .map(report -> {
                        Long reportId = report.getId();
                        String machineName = report.getMachine() != null ? report.getMachine().getMachineName() : "ไม่ระบุ";
                        String productName = report.getProduct() != null ? report.getProduct().getProductName() : "ไม่ระบุ";
                        Integer targetQty = report.getTargetQty() != null ? report.getTargetQty() : 0;

                        // Calculate good quantity = number of packaging logs (boxes) * qtyPerBox (fallback 0 if null)
                        long boxCount = 0L;
                        try {
                            Long counted = packagingLogRepository.countPackagesByReportId(reportId);
                            boxCount = counted != null ? counted : 0L;
                            logger.debug("📦 Report {}: box count = {}", reportId, boxCount);
                        } catch (Exception ex) {
                            logger.warn("⚠️ Cannot count packaging logs for report {}: {}", reportId, ex.getMessage());
                        }
                        
                        int qtyPerBox = 1; // Default to 1 if null
                        if (report.getProduct() != null && report.getProduct().getQtyPerBox() != null) {
                            qtyPerBox = report.getProduct().getQtyPerBox();
                        }
                        long goodQty = boxCount * qtyPerBox;

                        // Calculate NG quantity (sum of ng logs)
                        long ngQty = 0L;
                        try {
                            Long summed = ngLogRepository.sumQuantityByReportId(reportId);
                            ngQty = summed != null ? summed : 0L;
                            logger.debug("🔥 Report {}: ng quantity = {}", reportId, ngQty);
                        } catch (Exception ex) {
                            logger.warn("⚠️ Cannot sum NG logs for report {}: {}", reportId, ex.getMessage());
                        }

            PcDashboardSummaryDto dto = new PcDashboardSummaryDto(
                reportId,
                machineName,
                productName,
                targetQty,
                goodQty,
                ngQty
            );
            // ใส่เลขที่คำสั่งผลิต
            dto.setOrderNumber(report.getOrderNumber());
            // ใส่สถานะตามนโยบายแสดงผล (IN_PROGRESS/ACTIVE/INACTIVE)
            dto.setStatus(deriveDisplayStatus(report));
                        
                        logger.info("✅ Created dashboard summary DTO: {}", dto);
                        return dto;
                    })
                    .sorted((dto1, dto2) -> {
                        // เรียงลำดับตาม machine name (A-Z), ถ้า machine name เหมือนกันให้เรียงตาม product name
                        int machineComparison = dto1.getMachineName().compareToIgnoreCase(dto2.getMachineName());
                        if (machineComparison != 0) {
                            return machineComparison;
                        }
                        return dto1.getProductName().compareToIgnoreCase(dto2.getProductName());
                    })
                    .collect(Collectors.toList());
                    
            logger.info("📊 Dashboard summary completed - {} DTOs created", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("❌ Error getting dashboard summary: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public ReportSummaryDto getReportSummary(Long id) {
        // default to Thai for backward compatibility
        return getReportSummary(id, "th");
    }

    @Transactional(readOnly = true)
    public ReportSummaryDto getReportSummary(Long id, String lang) {
        try {
            logger.info("📊 Getting report summary for ID: {}", id);
            
            ProductionReport report = productionReportRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Production report not found with id: " + id));
            String machineName = report.getMachine() != null ? report.getMachine().getMachineName() : "ไม่ระบุ";
            String productName = report.getProduct() != null ? report.getProduct().getProductName() : "ไม่ระบุ";
            int target = report.getTargetQty() != null ? report.getTargetQty() : 0;

            // Good quantity = boxes * qtyPerBox
            long boxCount = 0L;
            try {
                Long c = packagingLogRepository.countPackagesByReportId(report.getId());
                boxCount = c != null ? c : 0L;
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot count packaging logs for summary report {}: {}", id, ex.getMessage());
            }
            int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                    ? report.getProduct().getQtyPerBox() : 0;
            long goodQty = boxCount * qtyPerBox;

            // NG quantity
            long ngQty = 0L;
            try {
                Long sum = ngLogRepository.sumQuantityByReportId(report.getId());
                ngQty = sum != null ? sum : 0L;
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot sum NG logs for summary report {}: {}", id, ex.getMessage());
            }

            // Yield calculation (Quality %)
            String yield;
            double qualityPercentage = 0.0;
            if (target > 0) {
                double y = ((double) Math.max(goodQty - ngQty, 0)) / target * 100.0;
                yield = String.format("%.2f%%", y);
                qualityPercentage = y;
            } else {
                yield = "0%";
            }

            // Downtime events calculation
            List<DowntimeEventSummaryDto> downtimeSummaries = new ArrayList<>();
            long totalDowntimeMinutes = 0L;
            try {
                var downtimeEvents = downtimeEventRepository.findByReportId(report.getId());
                downtimeSummaries = downtimeEvents.stream().map(de -> {
                    long minutes = 0L;
                    if (de.getStartTime() != null && de.getEndTime() != null) {
                        minutes = java.time.Duration.between(de.getStartTime(), de.getEndTime()).toMinutes();
                    }
                    String reason = normalizeReason(de.getReason());
            return new DowntimeEventSummaryDto(
                de.getStartTime() != null ? de.getStartTime().toLocalTime().toString() : "",
                de.getEndTime() != null ? de.getEndTime().toLocalTime().toString() : "",
                minutes > 0 ? minutes + " นาที" : "In Progress",
                reason,
                de.getTechnician() != null ? de.getTechnician().getUsername() : "",
                de.getSolution() != null ? de.getSolution() : ""
            );
                }).collect(Collectors.toList());
                
                // คำนวณ total downtime
                totalDowntimeMinutes = downtimeEvents.stream()
                        .filter(de -> de.getStartTime() != null && de.getEndTime() != null)
                        .mapToLong(de -> java.time.Duration.between(de.getStartTime(), de.getEndTime()).toMinutes())
                        .sum();
                        
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot load downtime events for report {}: {}", id, ex.getMessage());
            }

            // NG logs list (language-aware)
            List<NgLogSummaryDto> ngLogSummaries = new ArrayList<>();
            try {
                var ngLogs = ngLogRepository.findByReportId(report.getId());
                ngLogSummaries = ngLogs.stream().map(l -> new NgLogSummaryDto(
                        l.getTimestamp() != null ? l.getTimestamp().toString() : "",
                        l.getNgType() != null ? resolveNgDescription(l.getNgType(), lang) : "",
                        l.getQuantity(),
                        l.getSource(),
                        l.getUser() != null ? l.getUser().getUsername() : ""
                )).collect(Collectors.toList());
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot load NG logs list for report {}: {}", id, ex.getMessage());
            }

            // Material usage list for this report
            List<MaterialUsageLogDto> materialUsage = new ArrayList<>();
            try {
                var materials = materialUsageLogRepository.findByReportId(report.getId());
                materialUsage = materials.stream().map(m -> new MaterialUsageLogDto(
                        m.getTimestamp(),
                        m.getMaterialCode(),
                        m.getLotNumber(),
                        m.getQuantityKg(),
                        m.getTechnician() != null ? m.getTechnician().getUsername() : ""
                )).collect(Collectors.toList());
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot load material usage for report {}: {}", id, ex.getMessage());
            }

            // Total scrap weight for this report
            java.math.BigDecimal totalScrapWeightKg = java.math.BigDecimal.ZERO;
            try {
                java.math.BigDecimal scrap = scrapWeightLogRepository.sumWeightByReportId(report.getId());
                if (scrap != null) totalScrapWeightKg = scrap;
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot sum scrap weight for report {}: {}", id, ex.getMessage());
            }

            // Detailed scrap weight logs for this report (Technician)
            java.util.List<ScrapWeightLogDto> scrapWeightLogs = new java.util.ArrayList<>();
            try {
                var logs = scrapWeightLogRepository.findByReportId(report.getId());
                scrapWeightLogs = logs.stream().map(l -> new ScrapWeightLogDto(
                        l.getTimestamp(),
                        l.getWeightKg(),
                        l.getScrapType(),
                        l.getMatType(),
                        l.getTechnician() != null ? l.getTechnician().getUsername() : "",
                        "Technician",
                        true
                )).collect(Collectors.toList());
            } catch (Exception ex) {
                logger.warn("⚠️ Cannot load scrap weight logs for report {}: {}", id, ex.getMessage());
            }

            // คำนวณ OEE (สมมติเวลาทำงาน 8 ชั่วโมง = 480 นาที)
            long plannedProductionMinutes = 480L; // 8 ชั่วโมง
            long actualRunningMinutes = plannedProductionMinutes - totalDowntimeMinutes;
            
            // Availability % = (Planned Production Time - Downtime) / Planned Production Time * 100
            double availabilityPercentage = actualRunningMinutes > 0 ? 
                    ((double) actualRunningMinutes / plannedProductionMinutes) * 100.0 : 0.0;
            
            // Performance % = (Actual Output / Expected Output based on running time) * 100
            // สมมติว่าเครื่องจักรสามารถผลิตได้ target/480 ชิ้นต่อนาที
            double expectedRatePerMinute = target > 0 ? (double) target / plannedProductionMinutes : 0.0;
            double expectedOutputForRunningTime = expectedRatePerMinute * actualRunningMinutes;
            double performancePercentage = expectedOutputForRunningTime > 0 ? 
                    ((double) goodQty / expectedOutputForRunningTime) * 100.0 : 0.0;
            
            // OEE = Availability × Performance × Quality
            double oeePercentage = (availabilityPercentage * performancePercentage * qualityPercentage) / 10000.0;
            
            // สรุปประเภทของเสีย (ใช้ aggregation จาก repository เพื่อให้คำอธิบายตรง) - language-aware
            List<NgTypeSummaryDto> ngTypeSummaries = new ArrayList<>();
            try {
                List<Object[]> rows = (lang != null && lang.toLowerCase().startsWith("en"))
                        ? ngLogRepository.summarizeNgByDescriptionForReportEn(report.getId())
                        : ngLogRepository.summarizeNgByDescriptionForReport(report.getId());
                if (rows != null) {
                    for (Object[] row : rows) {
                        String desc = row[0] != null ? row[0].toString() : "ไม่ระบุ";
                        long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                        double pct = (ngQty > 0) ? round2(((double) count / ngQty) * 100.0) : 0.0;
                        ngTypeSummaries.add(new NgTypeSummaryDto(desc, count, pct, count));
                    }
                    ngTypeSummaries.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
                }
            } catch (Exception ex) {
                logger.warn("Cannot aggregate NG summary for report {}: {}", id, ex.getMessage());
                ngTypeSummaries = calculateNgTypeSummary(report.getId(), ngQty, lang);
            }
            
            // สรุงประเภท Downtime
            List<DowntimeReasonSummaryDto> downtimeReasonSummaries = calculateDowntimeReasonSummary(report.getId(), totalDowntimeMinutes);

            ReportSummaryDto summary = new ReportSummaryDto();
            summary.setOrderNumber(report.getOrderNumber());
            summary.setMachineName(machineName);
            summary.setProductName(productName);
            summary.setTargetQty(target);
            summary.setGoodQty(goodQty);
            summary.setTotalNgQty(ngQty);
            summary.setYield(yield);
            
            // ตั้งค่า OEE
            summary.setOee(String.format("%.2f%%", oeePercentage));
            summary.setAvailability(String.format("%.2f%%", availabilityPercentage));
            summary.setPerformance(String.format("%.2f%%", performancePercentage));
            summary.setQuality(yield);
            
            // ตั้งค่าสรุป
            summary.setNgTypeSummary(ngTypeSummaries);
            // ตั้งค่า alias เพื่อรองรับ frontend เดิมที่อ่าน 'ngSummary'
            summary.setNgSummary(ngTypeSummaries);
            summary.setDowntimeReasonSummary(downtimeReasonSummaries);
            
            summary.setDowntimeEvents(downtimeSummaries);
            summary.setNgLogs(ngLogSummaries);
            summary.setMaterialUsageLogs(materialUsage);
            summary.setTotalScrapWeightKg(totalScrapWeightKg);
            summary.setScrapWeightLogs(scrapWeightLogs);

            logger.info("✅ Report summary built for ID {} (good={}, ng={}, target={}, yield={})", id, goodQty, ngQty, target, yield);
            return summary;
            
        } catch (EntityNotFoundException e) {
            logger.error("❌ Report not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error getting report summary for ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("เกิดข้อผิดพลาดในการดึงข้อมูลสรุปรายงาน", e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<MachineSimpleDto> getMachineList() {
        try {
            return machineRepository.findAll().stream()
                    .map(m -> new MachineSimpleDto(m.getId(), m.getMachineName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error in getMachineList(): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<ProductSimpleDto> getProductList() {
        try {
            return productRepository.findAll().stream()
                    .map(p -> new ProductSimpleDto(p.getId(), p.getProductName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error in getProductList(): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private ProductionReportDto convertToProductionReportDto(ProductionReport report) {
        ProductionReportDto dto = new ProductionReportDto();
        dto.setId(report.getId());
        dto.setOrderNumber(report.getOrderNumber());
        dto.setStartDate(report.getStartDate());
        dto.setEndDate(report.getEndDate());
        dto.setMachineName(report.getMachine() != null ? report.getMachine().getMachineName() : "ไม่ระบุ");
        dto.setMachineId(report.getMachine() != null ? String.valueOf(report.getMachine().getId()) : null);
        dto.setProductName(report.getProduct() != null ? report.getProduct().getProductName() : "ไม่ระบุ");
        dto.setTargetQty(report.getTargetQty());
        // Derive status by policy (today within window -> IN_PROGRESS; closed -> INACTIVE; else ACTIVE)
        String statusCode = deriveDisplayStatus(report);
        dto.setStatus(statusCode);
        
        // Check if there's any production data
        boolean hasPackagingLogs = false;
        try {
            hasPackagingLogs = packagingLogRepository.countByReportId(report.getId()) > 0;
        } catch (Exception ignore) {}
        boolean hasNgLogs = false;
        try {
            hasNgLogs = ngLogRepository.countByReportId(report.getId()) > 0;
        } catch (Exception ignore) {}
        boolean hasProductionData = hasPackagingLogs || hasNgLogs;
        
        // Business rules for button visibility
        boolean isInProgress = "IN_PROGRESS".equalsIgnoreCase(statusCode);
        boolean isActive = "ACTIVE".equalsIgnoreCase(statusCode); // expired date but not yet closed
        dto.setFinalizable(isInProgress || isActive);
        dto.setEditable(isInProgress && !hasProductionData);
        dto.setDeletable(!hasProductionData);

        return dto;
    }

    // --- New status helpers implementing business rules ---
    private boolean isTerminalStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return java.util.Set.of("COMPLETED", "COMPLETE", "DONE", "FINISHED", "CLOSED", "INACTIVE").contains(s);
    }

    private String deriveStatusForDates(LocalDate start, LocalDate end, String existingStatus) {
        // If existing is terminal, remain terminal (will map to INACTIVE for display)
        if (isTerminalStatus(existingStatus)) {
            return "COMPLETED"; // stored terminal
        }
        LocalDate today = LocalDate.now();
        boolean within = (start == null || !today.isBefore(start)) && (end == null || !today.isAfter(end));
        if (within) return "IN_PROGRESS";
        return "ACTIVE"; // not within the window and not closed
    }

    private String deriveDisplayStatus(ProductionReport report) {
        String raw = report.getStatus() != null ? report.getStatus().trim().toUpperCase() : "";
        if (isTerminalStatus(raw)) {
            return "INACTIVE";
        }
        LocalDate start = report.getStartDate();
        LocalDate end   = report.getEndDate();
        LocalDate today = LocalDate.now();
        if (start != null && today.isBefore(start)) return "PENDING";   // future — not yet started
        if (end   != null && today.isAfter(end))    return "ACTIVE";    // expired — past end date
        return "IN_PROGRESS";                                            // today within range
    }

    @Transactional(readOnly = true)
    public List<Machine> getAllMachines() {
        try {
            logger.info("🔍 ProductionService.getAllMachines() called");
            List<Machine> machines = machineRepository.findAll();
            logger.info("📊 Found {} machines", machines.size());
            return machines;
        } catch (Exception e) {
            logger.error("❌ Error in getAllMachines(): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        try {
            logger.info("🔍 ProductionService.getAllProducts() called");
            List<Product> products = productRepository.findAll();
            logger.info("📊 Found {} products", products.size());
            return products;
        } catch (Exception e) {
            logger.error("❌ Error in getAllProducts(): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Additional methods required by controllers
    @Transactional(readOnly = true)
    public List<ProductionReportDto> getActiveProductionReports() {
        try {
            logger.info("🔍 Getting active production reports");
            // ใช้การตรวจแบบยืดหยุ่น
            List<ProductionReport> reports = getActiveReportsRobust();
            logger.info("🔍 Active reports list size: {}", reports.size());
            return reports.stream()
                    .map(this::convertToProductionReportDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error getting active production reports: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // --- Helpers ---
    private List<ProductionReport> getActiveReportsRobust() {
        logger.info("🔍 Starting getActiveReportsRobust()...");

        java.time.LocalDate today = java.time.LocalDate.now();
        // Terminal statuses — reports with these statuses are excluded even if date spans today
        java.util.List<String> terminalStatuses = java.util.List.of(
                "COMPLETED", "COMPLETE", "DONE", "FINISHED",
                "CANCELLED", "CANCELED", "CLOSED", "FINALIZED");

        try {
            // Primary query: startDate <= today <= endDate AND status not terminal
            List<ProductionReport> fast = productionReportRepository.findActiveReportsFast(
                    java.util.List.of(), // activeStatuses no longer used in query
                    terminalStatuses,
                    today
            );
            logger.info("⚡ Fast active detection returned {} record(s)", fast.size());
            if (!fast.isEmpty()) {
                return fast;
            }
        } catch (Exception ex) {
            logger.warn("⚠️ Fast active detection failed: {} — falling back", ex.getMessage());
        }

        // Fallback: filter all reports in-memory by date range AND status
        logger.info("🔍 Fallback: querying ALL reports and filtering by today={}", today);
        java.util.List<ProductionReport> result = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        try {
            List<ProductionReport> allReports = productionReportRepository.findAll();
            for (ProductionReport report : allReports) {
                if (report == null || report.getId() == null) continue;
                // Date range must span today
                if (report.getStartDate() == null || report.getEndDate() == null) continue;
                if (report.getStartDate().isAfter(today) || report.getEndDate().isBefore(today)) continue;
                // Status must not be terminal
                String upperStatus = report.getStatus() != null ? report.getStatus().trim().toUpperCase() : "";
                if (terminalStatuses.contains(upperStatus)) continue;
                if (seen.add(report.getId())) {
                    result.add(report);
                }
            }
        } catch (Exception e) {
            logger.error("❌ Fallback failed: {}", e.getMessage(), e);
        }

        logger.info("🏁 getActiveReportsRobust() completed - returning {} active reports", result.size());
        return result;
    }
    // Helper methods for summary calculations
    private List<NgTypeSummaryDto> calculateNgTypeSummary(Long reportId, long totalNgQty) {
        return calculateNgTypeSummary(reportId, totalNgQty, "th");
    }

    private List<NgTypeSummaryDto> calculateNgTypeSummary(Long reportId, long totalNgQty, String lang) {
        try {
            if (totalNgQty == 0) {
                return new ArrayList<>();
            }
            
            // Query NG logs grouped by type
            var ngLogs = ngLogRepository.findByReportId(reportId);
            Map<String, Long> ngTypeMap = new HashMap<>();
            for (NgLog log : ngLogs) {
                long qty = log.getQuantity() != null ? log.getQuantity() : 0L;
                String desc = "ไม่ระบุ";
                if (log.getNgType() != null) {
                    desc = resolveNgDescription(log.getNgType(), lang);
                }
                ngTypeMap.merge(desc, qty, Long::sum);
            }
                    
            return ngTypeMap.entrySet().stream()
                    .map(entry -> {
                        String description = entry.getKey();
                        Long count = entry.getValue();
                        double percentage = ((double) count / totalNgQty) * 100.0;
                        return new NgTypeSummaryDto(description, count, percentage, count);
                    })
                    .sorted((a, b) -> Long.compare(b.getCount(), a.getCount())) // เรียงตามจำนวนมากไปน้อย
                    .collect(Collectors.toList());
                    
        } catch (Exception ex) {
            logger.warn("⚠️ Cannot calculate NG type summary for report {}: {}", reportId, ex.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper: choose proper NG description
    private String resolveNgDescription(NgType nt, String lang) {
        if (nt == null) return "ไม่ระบุ";
        String language = lang != null ? lang.toLowerCase() : "th";
        if (language.startsWith("en")) {
            if (nt.getNgDescriptionEn() != null && !nt.getNgDescriptionEn().isBlank()) return nt.getNgDescriptionEn().trim();
            if (nt.getNgDescriptionTh() != null && !nt.getNgDescriptionTh().isBlank()) return nt.getNgDescriptionTh().trim();
        } else {
            if (nt.getNgDescriptionTh() != null && !nt.getNgDescriptionTh().isBlank()) return nt.getNgDescriptionTh().trim();
            if (nt.getNgDescriptionEn() != null && !nt.getNgDescriptionEn().isBlank()) return nt.getNgDescriptionEn().trim();
        }
        if (nt.getNgCode() != null && !nt.getNgCode().isBlank()) return nt.getNgCode().trim();
        if (nt.getNgType() != null && !nt.getNgType().isBlank()) return nt.getNgType().trim();
        return "ไม่ระบุ";
    }
    
    private List<DowntimeReasonSummaryDto> calculateDowntimeReasonSummary(Long reportId, long totalDowntimeMinutes) {
        try {
            if (totalDowntimeMinutes == 0) {
                return new ArrayList<>();
            }
            
            var downtimeEvents = downtimeEventRepository.findByReportId(reportId);
            Map<String, Long> reasonMinutesMap = new HashMap<>();
            Map<String, Integer> reasonCountMap = new HashMap<>();
            
            downtimeEvents.stream()
                    .filter(de -> de.getStartTime() != null && de.getEndTime() != null)
                    .forEach(de -> {
                        String reason = normalizeReason(de.getReason());
                        long minutes = java.time.Duration.between(de.getStartTime(), de.getEndTime()).toMinutes();
                        
                        reasonMinutesMap.merge(reason, minutes, Long::sum);
                        reasonCountMap.merge(reason, 1, Integer::sum);
                    });
                    
            return reasonMinutesMap.entrySet().stream()
                    .map(entry -> {
                        String reason = entry.getKey();
                        long minutes = entry.getValue();
                        int count = reasonCountMap.get(reason);
                        double percentage = ((double) minutes / totalDowntimeMinutes) * 100.0;
                        
                        // Format duration
                        String formattedDuration;
                        if (minutes >= 60) {
                            long hours = minutes / 60;
                            long remainingMinutes = minutes % 60;
                            formattedDuration = hours + " ชั่วโมง " + remainingMinutes + " นาที";
                        } else {
                            formattedDuration = minutes + " นาที";
                        }
                        
                        return new DowntimeReasonSummaryDto(reason, minutes, percentage, count, formattedDuration);
                    })
                    .sorted((a, b) -> Long.compare(b.getTotalMinutes(), a.getTotalMinutes())) // เรียงตามเวลามากไปน้อย
                    .collect(Collectors.toList());
                    
        } catch (Exception ex) {
            logger.warn("⚠️ Cannot calculate downtime reason summary for report {}: {}", reportId, ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<ProductionReportDto> getTodaysActiveProductionReports() {
        return getActiveProductionReports(); // getActiveReportsRobust() already filters by today
    }

    @Transactional(readOnly = true)
    public List<HistoricalReportSummaryDto> getHistoricalReports(java.time.LocalDate startDate, java.time.LocalDate endDate, Long machineId, Long productId) {
        try {
            logger.info("🔍 Getting historical reports from {} to {}, machineId={}, productId={}", 
                       startDate, endDate, machineId, productId);
            
            List<ProductionReport> reports = productionReportRepository.findHistoricalReports(
                startDate, endDate, machineId, productId);
            
            logger.info("📊 Found {} production reports in date range", reports.size());
            
            List<HistoricalReportSummaryDto> summaries = new ArrayList<>();
            
            for (ProductionReport report : reports) {
                try {
                    HistoricalReportSummaryDto dto = new HistoricalReportSummaryDto();
                    dto.setId(report.getId());
                    dto.setOrderNumber(report.getOrderNumber());
                    dto.setStartDate(report.getStartDate());
                    dto.setEndDate(report.getEndDate());
                    dto.setMachineName(report.getMachine() != null ? report.getMachine().getMachineName() : "-");
                    dto.setProductName(report.getProduct() != null ? report.getProduct().getProductName() : "-");
                    
                    // Calculate good quantity from packaging logs
                    Long goodQty = 0L;
                    Long totalBoxes = 0L;
                    try {
                        Long packageCount = packagingLogRepository.countPackagesByReportId(report.getId());
                        if (packageCount != null && packageCount > 0) {
                            totalBoxes = packageCount;
                            Integer qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null) 
                                              ? report.getProduct().getQtyPerBox() : 0;
                            goodQty = packageCount * qtyPerBox;
                        }
                    } catch (Exception ex) {
                        logger.warn("Cannot calculate good quantity for report {}: {}", report.getId(), ex.getMessage());
                    }
                    
                    // Calculate NG quantity
                    Long ngQty = 0L;
                    try {
                        Long ngSum = ngLogRepository.sumQuantityByReportId(report.getId());
                        ngQty = ngSum != null ? ngSum : 0L;
                    } catch (Exception ex) {
                        logger.warn("Cannot calculate NG quantity for report {}: {}", report.getId(), ex.getMessage());
                    }
                    
                    // Calculate yield
                    String yield = "0.00%";
                    Integer targetQty = report.getTargetQty() != null ? report.getTargetQty() : 0;
                    if (targetQty > 0 && goodQty > 0) {
                        double actualGoodQty = Math.max(goodQty - ngQty, 0);
                        double yieldPercent = (actualGoodQty / targetQty) * 100.0;
                        yield = String.format("%.2f%%", yieldPercent);
                    }
                    
                    // Calculate total scrap weight
                    java.math.BigDecimal totalScrapWeight = java.math.BigDecimal.ZERO;
                    try {
                        java.math.BigDecimal scrapWeight = scrapWeightLogRepository.sumWeightByReportId(report.getId());
                        totalScrapWeight = scrapWeight != null ? scrapWeight : java.math.BigDecimal.ZERO;
                    } catch (Exception ex) {
                        logger.warn("Cannot calculate scrap weight for report {}: {}", report.getId(), ex.getMessage());
                    }
                    
                    dto.setGoodQty(goodQty);
                    dto.setNgQty(ngQty);
                    dto.setYield(yield);
                    dto.setTotalBoxes(totalBoxes);
                    dto.setTotalScrapWeight(totalScrapWeight);
                    dto.setStatus(report.getStatus() != null ? report.getStatus() : "Unknown");
                    
                    summaries.add(dto);
                    
                } catch (Exception ex) {
                    logger.error("Error processing report {}: {}", report.getId(), ex.getMessage(), ex);
                }
            }
            
            logger.info("✅ Successfully built {} historical report summaries", summaries.size());
            return summaries;
            
        } catch (Exception e) {
            logger.error("❌ Error getting historical reports: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    // Overloaded method รองรับ machineId
    public DailyProductionSummaryDto getDailyProductionSummary(java.time.LocalDate date, String machineId) {
        logger.info("🔍 Getting daily production summary for date: {} and machineId: {}", date, machineId);
        
        try {
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(date, normalizedMachineId);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(Collectors.toList());

            logger.info("📊 Found {} production reports for date: {} and machineId: {}", reports.size(), date, machineId);

            return processProductionReports(reports, date, "th");
        } catch (Exception e) {
            logger.error("❌ Error in getDailyProductionSummary for date: {} and machineId: {}", date, machineId, e);
            return createEmptySummary(date);
        }
    }

    // New overload: support optional product filter
    public DailyProductionSummaryDto getDailyProductionSummary(java.time.LocalDate date, String machineId, Long productId) {
        logger.info("🔍 Getting daily production summary for date: {}, machineId: {}, productId: {}", date, machineId, productId);
        try {
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(date, normalizedMachineId);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            logger.info("📊 Found {} production reports for date: {}, machineId: {}, productId: {}", reports.size(), date, machineId, productId);
            return processProductionReports(reports, date, "th");
        } catch (Exception e) {
            logger.error("❌ Error in getDailyProductionSummary for date: {}, machineId: {}, productId: {}", date, machineId, productId, e);
            return createEmptySummary(date);
        }
    }

    // Language-aware overloads for daily summary
    @Transactional(readOnly = true)
    public DailyProductionSummaryDto getDailyProductionSummary(java.time.LocalDate date, String machineId, Long productId, String lang) {
        logger.info("🔍 Getting daily production summary (lang-aware) for date: {}, machineId: {}, productId: {}, lang={}", date, machineId, productId, lang);
        try {
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(date, normalizedMachineId);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            String langNorm = (lang != null && lang.toLowerCase().startsWith("en")) ? "en" : "th";
            return processProductionReports(reports, date, langNorm);
        } catch (Exception e) {
            logger.error("❌ Error in getDailyProductionSummary (lang-aware) for date: {}, machineId: {}, productId: {}", date, machineId, productId, e);
            return createEmptySummary(date);
        }
    }

    

    public DailyProductionSummaryDto getDailyProductionSummary(java.time.LocalDate date) {
        logger.info("🔍 Getting daily production summary for date: {}", date);
        
        try {
            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(date, null);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(Collectors.toList());

            logger.info("📊 Found {} production reports for date: {}", reports.size(), date);

            return processProductionReports(reports, date, "th");
        } catch (Exception e) {
            logger.error("❌ Error in getDailyProductionSummary for date: {}", date, e);
            return createEmptySummary(date);
        }
    }
    
    private DailyProductionSummaryDto processProductionReports(List<ProductionReport> reports, java.time.LocalDate date, String lang) {
        try {
            if (reports.isEmpty()) {
                logger.warn("⚠️ No production reports found for date: {}", date);
                return createEmptySummary(date);
            }

            List<Long> reportIds = reports.stream()
                .filter(java.util.Objects::nonNull)
                .map(ProductionReport::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            if (reportIds.isEmpty()) {
                logger.warn("⚠️ No valid production report IDs for date: {}", date);
                return createEmptySummary(date);
            }

            // Production day runs 03:00 -> 03:00 (next day) to match shift definitions
            java.time.LocalDateTime dayShiftStart = date.atTime(3, 0);
            java.time.LocalDateTime dayShiftEnd = date.atTime(15, 0);
            java.time.LocalDateTime nightShiftEndExclusive = date.plusDays(1).atTime(3, 0);
            java.time.LocalDateTime queryEndInclusive = nightShiftEndExclusive.minusNanos(1);

            DailyProductionSummaryDto summary = new DailyProductionSummaryDto();
            summary.setDate(date);

            // Fetch all relevant records in a single round-trip per log type
            List<PackagingLog> packagingLogs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, dayShiftStart, queryEndInclusive);
            List<NgLog> dailyNgLogs = ngLogRepository.findByReportIdInAndTimestampBetween(reportIds, dayShiftStart, queryEndInclusive);
            List<ScrapWeightLog> dailyScrapLogs = scrapWeightLogRepository.findByReportIdInAndTimestampBetween(reportIds, dayShiftStart, queryEndInclusive);
            List<MaterialUsageLog> dailyMaterialLogs = materialUsageLogRepository.findByReportIdInAndTimestampBetween(reportIds, dayShiftStart, queryEndInclusive);
            List<DowntimeEvent> downtimeEvents = downtimeEventRepository.findByReportIdIn(reportIds);

            List<Object[]> aggregatedNgRows = (lang != null && lang.toLowerCase().startsWith("en"))
                ? ngLogRepository.summarizeNgByDescriptionEn(reportIds, dayShiftStart, queryEndInclusive)
                : ngLogRepository.summarizeNgByDescription(reportIds, dayShiftStart, queryEndInclusive);
            if (aggregatedNgRows == null) {
                aggregatedNgRows = java.util.Collections.emptyList();
            }
            java.util.Map<String, Long> ngSummaryMap = new java.util.HashMap<>();
            long totalNgQty = 0L;
            for (Object[] row : aggregatedNgRows) {
                if (row == null || row.length < 2) {
                    continue;
                }
                String description = row[0] != null ? row[0].toString() : "ไม่ระบุ";
                long quantity = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                ngSummaryMap.merge(description, quantity, Long::sum);
                totalNgQty += quantity;
            }

            // Group packaging logs per report
            logger.info("📦 Packaging logs fetched for daily window ({} to {}): {} record(s)", dayShiftStart, queryEndInclusive, packagingLogs.size());
            if (packagingLogs.isEmpty()) {
                // Diagnostics: check calendar-day window to see if logs fall outside 03:00–03:00
                java.time.LocalDateTime calStart = date.atStartOfDay();
                java.time.LocalDateTime calEndIncl = date.plusDays(1).atStartOfDay().minusNanos(1);
                List<PackagingLog> dayLogs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, calStart, calEndIncl);
                logger.info("🔎 Packaging logs for calendar day ({} to {}): {} record(s)", calStart, calEndIncl, dayLogs.size());
                // Also probe min/max timestamp for all logs of these reports (without time filter)
                List<PackagingLog> allLogsForReports = packagingLogRepository.findByReportIdIn(reportIds);
                java.util.Optional<java.time.LocalDateTime> minTs = allLogsForReports.stream().map(PackagingLog::getTimestamp).filter(java.util.Objects::nonNull).min(java.time.LocalDateTime::compareTo);
                java.util.Optional<java.time.LocalDateTime> maxTs = allLogsForReports.stream().map(PackagingLog::getTimestamp).filter(java.util.Objects::nonNull).max(java.time.LocalDateTime::compareTo);
                logger.info("🕒 Packaging logs overall for selected reports: total={}, minTs={}, maxTs={}", allLogsForReports.size(), minTs.orElse(null), maxTs.orElse(null));
            }
            java.util.Map<Long, Long> boxesByReport = packagingLogs.stream()
                .filter(log -> log.getReport() != null && log.getReport().getId() != null)
                .collect(Collectors.groupingBy(log -> log.getReport().getId(), Collectors.counting()));

            long totalGoodBoxes = boxesByReport.values().stream().mapToLong(Long::longValue).sum();
            long totalGoodQty = 0L;
            for (ProductionReport report : reports) {
                long boxes = boxesByReport.getOrDefault(report.getId(), 0L);
                int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                    ? report.getProduct().getQtyPerBox()
                    : 0;
                if (qtyPerBox <= 0) {
                    int fallback = 0;
                    try {
                        if (report.getProduct() != null && report.getProduct().getCavity() != null) {
                            fallback = report.getProduct().getCavity();
                        }
                    } catch (Exception ignore) {}
                    if (fallback <= 0) fallback = 1; // at least 1 piece per box when config is missing
                    logger.warn("qtyPerBox missing/zero for product {} (report {}), using fallback {}",
                        report.getProduct() != null ? report.getProduct().getProductName() : "unknown",
                        report.getId(), fallback);
                    qtyPerBox = fallback;
                }
                totalGoodQty += boxes * (long) qtyPerBox;
            }

            // NG totals and summary per description
            java.util.Map<Long, Long> ngByReport = new java.util.HashMap<>();
            for (NgLog log : dailyNgLogs) {
                if (log == null) continue;
                Long reportId = log.getReport() != null ? log.getReport().getId() : null;
                if (reportId == null) continue;

                long quantity = log.getQuantity() != null ? log.getQuantity() : 0L;
                ngByReport.merge(reportId, quantity, Long::sum);
            }

            // Scrap weight totals
            java.math.BigDecimal totalScrapWeight = dailyScrapLogs.stream()
                .map(log -> log.getWeightKg() != null ? log.getWeightKg() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.util.Map<String, java.math.BigDecimal> scrapWeightMap = new java.util.HashMap<>();
            java.util.Map<String, Long> scrapCountMap = new java.util.HashMap<>();
            for (ScrapWeightLog log : dailyScrapLogs) {
                if (log == null) continue;
                String scrapType = log.getScrapType() != null ? log.getScrapType() : "ไม่ระบุประเภท";
                String matType = log.getMatType() != null ? log.getMatType() : "";
                String combinedType = matType.isEmpty() ? scrapType : matType + " " + scrapType;
                java.math.BigDecimal weight = log.getWeightKg() != null ? log.getWeightKg() : java.math.BigDecimal.ZERO;

                scrapWeightMap.merge(combinedType, weight, java.math.BigDecimal::add);
                scrapCountMap.merge(combinedType, 1L, Long::sum);
            }

            // Material usage details
            java.util.List<MaterialUsageLogDto> allMaterialUsage = dailyMaterialLogs.stream()
                .map(log -> {
                    MaterialUsageLogDto dto = new MaterialUsageLogDto();
                    dto.setTimestamp(log.getTimestamp());
                    dto.setMaterialCode(log.getMaterialCode());
                    dto.setLotNumber(log.getLotNumber());
                    dto.setQuantityKg(log.getQuantityKg());
                    dto.setTechnicianName(log.getTechnician() != null ? log.getTechnician().getUsername() : "");
                    return dto;
                })
                .sorted(java.util.Comparator.comparing(MaterialUsageLogDto::getTimestamp, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .collect(Collectors.toList());

            // Downtime events filtered to the production day window (allow overlaps)
            java.util.List<DowntimeEventSummaryDto> allDowntimeEvents = downtimeEvents.stream()
                .filter(event -> isOverlapping(event.getStartTime(), event.getEndTime(), dayShiftStart, nightShiftEndExclusive))
                .map(event -> {
                    String startTime = event.getStartTime() != null ? event.getStartTime().toLocalTime().toString() : "";
                    String endTime = event.getEndTime() != null ? event.getEndTime().toLocalTime().toString() : "";
                    String durationText = "In Progress";
                    if (event.getStartTime() != null && event.getEndTime() != null) {
                        long minutes = java.time.Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
                        durationText = minutes + " นาที";
                    }
                    String technician = event.getTechnician() != null ? event.getTechnician().getUsername() : "";
                    return new DowntimeEventSummaryDto(startTime, endTime, durationText, normalizeReason(event.getReason()), technician, event.getSolution() != null ? event.getSolution() : "");
                })
                .collect(Collectors.toList());

            // Build NG summary list combining piece and weight based data
            java.util.List<NgSummaryDto> ngSummaryList = new java.util.ArrayList<>();
            ngSummaryMap.forEach((desc, qty) -> ngSummaryList.add(new NgSummaryDto(desc, qty, java.math.BigDecimal.ZERO)));
            scrapWeightMap.forEach((desc, weight) -> {
                Long count = scrapCountMap.getOrDefault(desc, 0L);
                ngSummaryList.add(new NgSummaryDto(desc, count, weight));
            });

            logger.info("✅ Daily good calculation: totalBoxes={}, totalGoodQty={} (pieces)", totalGoodBoxes, totalGoodQty);
            summary.setTotalGoodQty(totalGoodQty);
            summary.setTotalNgQty(totalNgQty);
            summary.setTotalGoodBoxes(totalGoodBoxes);
            summary.setTotalScrapWeight(totalScrapWeight);
            summary.setNgSummary(ngSummaryList);
            summary.setMaterialUsageLogs(allMaterialUsage);
            summary.setDowntimeEvents(allDowntimeEvents);
            // Map packaging logs for inspection on FE
            java.util.List<PackagingLogViewDto> packagingLogViews = packagingLogs.stream()
                .map(p -> new PackagingLogViewDto(
                    p.getTimestamp(),
                    p.getLotNumber(),
                    p.getBoxNo(),
                    p.getOperator() != null ? p.getOperator().getUsername() : ""
                ))
                .sorted(java.util.Comparator.comparing(PackagingLogViewDto::getTimestamp, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .collect(java.util.stream.Collectors.toList());
            summary.setPackagingLogs(packagingLogViews);

            // Build per-report summary using the filtered data
            java.util.List<DailyReportSummaryDto> dailyReportsList = new java.util.ArrayList<>();
            for (ProductionReport report : reports) {
                long boxes = boxesByReport.getOrDefault(report.getId(), 0L);
                int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                    ? report.getProduct().getQtyPerBox()
                    : 0;
                if (qtyPerBox <= 0) {
                    int fallback = 0;
                    try {
                        if (report.getProduct() != null && report.getProduct().getCavity() != null) {
                            fallback = report.getProduct().getCavity();
                        }
                    } catch (Exception ignore) {}
                    if (fallback <= 0) fallback = 1;
                    logger.warn("qtyPerBox missing/zero for product {} (report {}), using fallback {}",
                        report.getProduct() != null ? report.getProduct().getProductName() : "unknown",
                        report.getId(), fallback);
                    qtyPerBox = fallback;
                }
                long goodQty = boxes * (long) qtyPerBox;
                long ngQty = ngByReport.getOrDefault(report.getId(), 0L);

                DailyReportSummaryDto reportSummary = new DailyReportSummaryDto(
                    report.getMachine() != null ? report.getMachine().getMachineName() : "ไม่ระบุ",
                    report.getProduct() != null ? report.getProduct().getProductName() : "ไม่ระบุ",
                    report.getTargetQty() != null ? report.getTargetQty() : 0L,
                    goodQty,
                    ngQty,
                    report.getStatus() != null ? report.getStatus() : "Unknown"
                );
                reportSummary.setOrderNumber(report.getOrderNumber());
                dailyReportsList.add(reportSummary);
            }
            summary.setDailyReports(dailyReportsList);

            // Build worker lists by shift using the filtered log records
            java.util.Set<String> morningWorkers = new java.util.HashSet<>();
            java.util.Set<String> nightWorkers = new java.util.HashSet<>();

            java.util.function.Consumer<String> addMorningWorker = worker -> {
                if (worker != null) {
                    morningWorkers.add(worker);
                }
            };
            java.util.function.Consumer<String> addNightWorker = worker -> {
                if (worker != null) {
                    nightWorkers.add(worker);
                }
            };

            for (PackagingLog log : packagingLogs) {
                if (log.getTimestamp() == null || log.getOperator() == null) continue;
                java.time.LocalDateTime timestamp = log.getTimestamp();
                if (!timestamp.isBefore(dayShiftStart) && timestamp.isBefore(dayShiftEnd)) {
                    addMorningWorker.accept(log.getOperator().getUsername());
                } else if (!timestamp.isBefore(dayShiftEnd) && timestamp.isBefore(nightShiftEndExclusive)) {
                    addNightWorker.accept(log.getOperator().getUsername());
                }
            }

            for (NgLog log : dailyNgLogs) {
                if (log.getTimestamp() == null || log.getUser() == null) continue;
                java.time.LocalDateTime timestamp = log.getTimestamp();
                if (!timestamp.isBefore(dayShiftStart) && timestamp.isBefore(dayShiftEnd)) {
                    addMorningWorker.accept(log.getUser().getUsername());
                } else if (!timestamp.isBefore(dayShiftEnd) && timestamp.isBefore(nightShiftEndExclusive)) {
                    addNightWorker.accept(log.getUser().getUsername());
                }
            }

            for (MaterialUsageLog log : dailyMaterialLogs) {
                if (log.getTimestamp() == null || log.getTechnician() == null) continue;
                java.time.LocalDateTime timestamp = log.getTimestamp();
                if (!timestamp.isBefore(dayShiftStart) && timestamp.isBefore(dayShiftEnd)) {
                    addMorningWorker.accept(log.getTechnician().getUsername());
                } else if (!timestamp.isBefore(dayShiftEnd) && timestamp.isBefore(nightShiftEndExclusive)) {
                    addNightWorker.accept(log.getTechnician().getUsername());
                }
            }

            for (DowntimeEvent event : downtimeEvents) {
                if (event.getStartTime() == null || event.getTechnician() == null) continue;
                java.time.LocalDateTime timestamp = event.getStartTime();
                if (!timestamp.isBefore(dayShiftStart) && timestamp.isBefore(dayShiftEnd)) {
                    addMorningWorker.accept(event.getTechnician().getUsername());
                } else if (!timestamp.isBefore(dayShiftEnd) && timestamp.isBefore(nightShiftEndExclusive)) {
                    addNightWorker.accept(event.getTechnician().getUsername());
                }
            }

            // Fetch shift leaders and organize workers by role
            String morningShiftLeader = "ไม่มีข้อมูล";
            String nightShiftLeader = "ไม่มีข้อมูล";
            try {
                List<User> possibleLeaders = new ArrayList<>();
                List<User> shiftLeaders = userRepository.findByRole("Shift Leader");
                if (shiftLeaders != null && !shiftLeaders.isEmpty()) {
                    possibleLeaders.addAll(shiftLeaders);
                }
                if (possibleLeaders.isEmpty()) {
                    List<User> ldUsers = userRepository.findByRole("LD");
                    if (ldUsers != null && !ldUsers.isEmpty()) {
                        possibleLeaders.addAll(ldUsers);
                    }
                }
                if (possibleLeaders.isEmpty()) {
                    List<User> altShiftLeaders = userRepository.findByRole("SHIFT_LEADER");
                    if (altShiftLeaders != null && !altShiftLeaders.isEmpty()) {
                        possibleLeaders.addAll(altShiftLeaders);
                    }
                }

                if (!possibleLeaders.isEmpty()) {
                    morningShiftLeader = possibleLeaders.get(0).getUsername() + " (" + possibleLeaders.get(0).getRole() + ")";
                    nightShiftLeader = possibleLeaders.size() > 1
                        ? possibleLeaders.get(1).getUsername() + " (" + possibleLeaders.get(1).getRole() + ")"
                        : morningShiftLeader;
                }
            } catch (Exception ex) {
                logger.warn("Cannot get shift leader data: {}", ex.getMessage());
            }

            Map<String, List<String>> morningWorkersByRole = new HashMap<>();
            Map<String, List<String>> nightWorkersByRole = new HashMap<>();
            try {
                List<User> allUsers = userRepository.findAll();
                Map<String, String> userRoleMap = allUsers.stream()
                    .collect(Collectors.toMap(User::getUsername, User::getRole, (existing, replacement) -> existing));

                for (String worker : morningWorkers) {
                    String role = userRoleMap.getOrDefault(worker, "Unknown");
                    morningWorkersByRole.computeIfAbsent(role, k -> new ArrayList<>()).add(worker);
                }

                for (String worker : nightWorkers) {
                    String role = userRoleMap.getOrDefault(worker, "Unknown");
                    nightWorkersByRole.computeIfAbsent(role, k -> new ArrayList<>()).add(worker);
                }
            } catch (Exception ex) {
                logger.warn("Cannot organize workers by role: {}", ex.getMessage());
            }

            String morningWorkersDisplay = formatWorkersByRole(morningWorkersByRole);
            String nightWorkersDisplay = formatWorkersByRole(nightWorkersByRole);

            summary.setMorningShiftSupervisor(morningShiftLeader);
            summary.setMorningShiftWorkers(morningWorkersDisplay.isEmpty() ? "ไม่มีข้อมูล" : morningWorkersDisplay);
            summary.setMorningShiftCount(morningWorkers.size());
            summary.setNightShiftSupervisor(nightShiftLeader);
            summary.setNightShiftWorkers(nightWorkersDisplay.isEmpty() ? "ไม่มีข้อมูล" : nightWorkersDisplay);
            summary.setNightShiftCount(nightWorkers.size());
            summary.setEveningShiftSupervisor(null);
            summary.setEveningShiftWorkers(null);
            summary.setEveningShiftCount(0);

            logger.info("✅ Daily summary built for {} - Good: {}, NG: {}, Boxes: {}, Scrap: {} kg",
                date, totalGoodQty, totalNgQty, totalGoodBoxes, totalScrapWeight);

            return summary;

        } catch (Exception e) {
            logger.error("❌ Error getting daily production summary for {}: {}", date, e.getMessage(), e);
            return createEmptySummary(date);
        }
    }

    private String formatWorkersByRole(Map<String, List<String>> workersByRole) {
        if (workersByRole.isEmpty()) {
            return "";
        }

        // Normalize roles to a canonical display and merge workers across equivalent role names
        Map<String, List<String>> normalized = new java.util.HashMap<>();
        for (Map.Entry<String, List<String>> entry : workersByRole.entrySet()) {
            String rawRole = entry.getKey() != null ? entry.getKey().trim() : "";
            String displayRole = normalizeRole(rawRole);
            // Deduplicate and sort worker names for consistent display
            java.util.Set<String> dedup = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            if (entry.getValue() != null) {
                dedup.addAll(entry.getValue().stream().filter(java.util.Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
            }
            if (!dedup.isEmpty()) {
                normalized.computeIfAbsent(displayRole, k -> new ArrayList<>()).addAll(dedup);
            }
        }

        // Preferred order: CM Operator, Technician, QA, then others alphabetically
        java.util.List<String> preferredOrder = java.util.List.of("CM Operator", "Technician", "QA");
        List<String> orderedRoles = new ArrayList<>();
        for (String pref : preferredOrder) {
            if (normalized.containsKey(pref)) {
                orderedRoles.add(pref);
            }
        }
        // Add remaining roles not in preferred order, sorted alphabetically
        normalized.keySet().stream()
            .filter(r -> !preferredOrder.contains(r))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEach(orderedRoles::add);

        List<String> formattedRoles = new ArrayList<>();
        for (String role : orderedRoles) {
            List<String> workers = normalized.getOrDefault(role, java.util.Collections.emptyList());
            // Ensure stable alphabetical order
            java.util.List<String> sortedWorkers = new java.util.ArrayList<>(new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER) {{ addAll(workers); }});
            String roleDisplay = role + " (" + sortedWorkers.size() + " คน): " + String.join(", ", sortedWorkers);
            formattedRoles.add(roleDisplay);
        }

        return String.join(" | ", formattedRoles);
    }

    // Normalize various stored role names to canonical display names for reports
    private String normalizeRole(String role) {
        if (role == null) return "Unknown";
        String r = role.trim();
        String upper = r.toUpperCase();
        if (upper.equals("CM OPERATOR") || upper.equals("OPERATOR") || upper.equals("CM_OPERATOR") || upper.equals("CM-OPERATOR")) {
            return "CM Operator";
        }
        if (upper.equals("TECHNICIAN") || upper.equals("MECHANIC")) {
            return "Technician";
        }
        if (upper.contains("QA") || upper.contains("QUALITY")) {
            return "QA";
        }
        if (upper.contains("SHIFT LEADER") || upper.equals("LD") || upper.equals("SHIFT_LEADER") || upper.equals("SHIFT-LEADER")) {
            return "Shift Leader";
        }
        if (upper.contains("PRODUCTION CONTROL") || upper.equals("PC")) {
            return "Production Control";
        }
        if (upper.equals("DATAADMIN") || upper.equals("DATA ADMIN") || upper.equals("DATA_ADMIN")) {
            return "Data Admin";
        }
        // Default to original input for unrecognized roles
        return r;
    }

    @Transactional(readOnly = true)
    public DailyShiftSummaryDto getDailyProductionSummaryByShift(LocalDate date, String machineId) {
        return getDailyProductionSummaryByShift(date, machineId, (String) null);
    }

    @Transactional(readOnly = true)
    public DailyShiftSummaryDto getDailyProductionSummaryByShift(LocalDate date, String machineId, String lang) {
        logger.info("🕐 Getting daily production summary by shift for date: {} and machineId: {}", date, machineId);
        
        try {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(targetDate, normalizedMachineId);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, targetDate))
                .collect(Collectors.toList());
            
            DailyShiftSummaryDto shiftSummary = new DailyShiftSummaryDto();
            shiftSummary.setProductionDate(targetDate.toString());
            
            if (reports.isEmpty()) {
                logger.warn("No production reports found for date: {} and machineId: {}", targetDate, machineId);
                // สร้างข้อมูลเปล่าสำหรับทั้งสองกะ แต่ให้มีข้อมูลพื้นฐาน
                ShiftDataDto emptyDayShift = createEmptyShiftDataWithInfo();
                ShiftDataDto emptyNightShift = createEmptyShiftDataWithInfo();
                
                shiftSummary.setDayShiftData(emptyDayShift);
                shiftSummary.setNightShiftData(emptyNightShift);
                return shiftSummary;
            }
            
            // คำนวณข้อมูลทั้งหมดก่อน โดยใช้ machineId
            DailyProductionSummaryDto fullSummary = getDailyProductionSummary(targetDate, normalizedMachineId);
            if (fullSummary == null) {
                fullSummary = createEmptySummary(targetDate);
            }

            ProductionReport firstReport = reports.get(0);

            LocalDateTime dayShiftStart = targetDate.atTime(3, 0);
            LocalDateTime dayShiftEndExclusive = targetDate.atTime(15, 0);
            LocalDateTime nightShiftStart = dayShiftEndExclusive;
            LocalDateTime nightShiftEndExclusive = targetDate.plusDays(1).atTime(3, 0);

            ShiftTotals dayTotals = calculateShiftTotals(reports, dayShiftStart, dayShiftEndExclusive);
            ShiftTotals nightTotals = calculateShiftTotals(reports, nightShiftStart, nightShiftEndExclusive);

            ShiftDataDto dayShift = buildShiftDataDto(dayTotals, firstReport,
                fullSummary.getMorningShiftSupervisor(),
                fullSummary.getMorningShiftWorkers(),
                fullSummary.getMorningShiftCount());
            
            // เพิ่มข้อมูลรายละเอียดจริงจาก database - กรองตามเวลากะกลางวัน (03:00-15:00) และ machineId
            List<NgTypeSummaryDto> dayNgSummary = getShiftSpecificNgSummary(targetDate, true, normalizedMachineId, lang); // true สำหรับกะกลางวัน
            if (dayNgSummary == null) {
                dayNgSummary = new ArrayList<>();
            }
            dayShift.setNgSummary(dayNgSummary);
            
            List<DowntimeEventSummaryDto> dayDowntime = getShiftSpecificDowntime(targetDate, true, normalizedMachineId);
            if (dayDowntime == null) {
                dayDowntime = new ArrayList<>();
            }
            dayShift.setDowntimeHistory(dayDowntime);
            
            List<MaterialUsageLogDto> dayMaterialLogs = getShiftSpecificMaterialUsage(targetDate, true, normalizedMachineId);
            if (dayMaterialLogs == null) {
                dayMaterialLogs = new ArrayList<>();
            }
            dayShift.setMaterialUsageLogs(dayMaterialLogs);
            // Packaging logs for day shift
            List<PackagingLogViewDto> dayPackaging = getShiftSpecificPackagingLogs(targetDate, true, normalizedMachineId);
            dayShift.setPackagingLogs(dayPackaging);

            // Debug info for NG logs window and counts (day shift)
            try {
                List<Long> reportIds = reports.stream().map(ProductionReport::getId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
                LocalDateTime dStart = dayShiftStart;
                LocalDateTime dEndIncl = dayShiftEndExclusive.minusNanos(1);
                List<NgLog> dayNgLogs = reportIds.isEmpty() ? java.util.Collections.emptyList() : ngLogRepository.findByReportIdInAndTimestampBetween(reportIds, dStart, dEndIncl);
                java.time.LocalDateTime minTs = dayNgLogs.stream().map(NgLog::getTimestamp).filter(java.util.Objects::nonNull).min(java.time.LocalDateTime::compareTo).orElse(null);
                java.time.LocalDateTime maxTs = dayNgLogs.stream().map(NgLog::getTimestamp).filter(java.util.Objects::nonNull).max(java.time.LocalDateTime::compareTo).orElse(null);
                String dbg = String.format("day NG count=%d, window=[%s to %s], minTs=%s, maxTs=%s", dayNgLogs.size(), dStart, dEndIncl, minTs, maxTs);
                dayShift.setDebugInfo(dbg);
                dayShift.setDebugPackagingLogCount(dayPackaging != null ? dayPackaging.size() : 0);
            } catch (Exception ignore) {}
            
            ShiftDataDto nightShift = buildShiftDataDto(nightTotals, firstReport,
                fullSummary.getNightShiftSupervisor(),
                fullSummary.getNightShiftWorkers(),
                fullSummary.getNightShiftCount());
            
            // เพิ่มข้อมูลรายละเอียดจริงจาก database - กรองตามเวลากะกลางคืน (15:00-03:00) และ machineId
            List<NgTypeSummaryDto> nightNgSummary = getShiftSpecificNgSummary(targetDate, false, normalizedMachineId, lang); // false สำหรับกะกลางคืน
            if (nightNgSummary == null) {
                nightNgSummary = new ArrayList<>();
            }
            nightShift.setNgSummary(nightNgSummary);
            
            List<DowntimeEventSummaryDto> nightDowntime = getShiftSpecificDowntime(targetDate, false, normalizedMachineId);
            if (nightDowntime == null) {
                nightDowntime = new ArrayList<>();
            }
            nightShift.setDowntimeHistory(nightDowntime);
            
            List<MaterialUsageLogDto> nightMaterialLogs = getShiftSpecificMaterialUsage(targetDate, false, normalizedMachineId);
            if (nightMaterialLogs == null) {
                nightMaterialLogs = new ArrayList<>();
            }
            nightShift.setMaterialUsageLogs(nightMaterialLogs);
            // Packaging logs for night shift
            List<PackagingLogViewDto> nightPackaging = getShiftSpecificPackagingLogs(targetDate, false, normalizedMachineId);
            nightShift.setPackagingLogs(nightPackaging);

            // Debug info for NG logs window and counts (night shift)
            try {
                List<Long> reportIds = reports.stream().map(ProductionReport::getId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
                LocalDateTime nStart = nightShiftStart;
                LocalDateTime nEndIncl = nightShiftEndExclusive.minusNanos(1);
                List<NgLog> nightNgLogs = reportIds.isEmpty() ? java.util.Collections.emptyList() : ngLogRepository.findByReportIdInAndTimestampBetween(reportIds, nStart, nEndIncl);
                java.time.LocalDateTime minTs = nightNgLogs.stream().map(NgLog::getTimestamp).filter(java.util.Objects::nonNull).min(java.time.LocalDateTime::compareTo).orElse(null);
                java.time.LocalDateTime maxTs = nightNgLogs.stream().map(NgLog::getTimestamp).filter(java.util.Objects::nonNull).max(java.time.LocalDateTime::compareTo).orElse(null);
                String dbg = String.format("night NG count=%d, window=[%s to %s], minTs=%s, maxTs=%s", nightNgLogs.size(), nStart, nEndIncl, minTs, maxTs);
                nightShift.setDebugInfo(dbg);
                nightShift.setDebugPackagingLogCount(nightPackaging != null ? nightPackaging.size() : 0);
            } catch (Exception ignore) {}
            
            // เพิ่มข้อมูลรายละเอียดที่ frontend ต้องการ
            shiftSummary.setOrderNumber(firstReport.getOrderNumber());
            if (firstReport.getMachine() != null) {
                shiftSummary.setMachineName(firstReport.getMachine().getMachineName());
            }
            if (firstReport.getProduct() != null) {
                shiftSummary.setProductName(firstReport.getProduct().getProductName());
            }
            
            shiftSummary.setDayShiftData(dayShift);
            shiftSummary.setNightShiftData(nightShift);
            
            logger.info("✅ Shift-based summary built for {} with order: {}", targetDate, firstReport.getOrderNumber());
            
            // Debug logging
            logger.info("Day shift NG summary size: {}", dayShift.getNgSummary() != null ? dayShift.getNgSummary().size() : "null");
            logger.info("Night shift NG summary size: {}", nightShift.getNgSummary() != null ? nightShift.getNgSummary().size() : "null");
            logger.info("Day shift downtime size: {}", dayShift.getDowntimeHistory() != null ? dayShift.getDowntimeHistory().size() : "null");
            logger.info("Night shift downtime size: {}", nightShift.getDowntimeHistory() != null ? nightShift.getDowntimeHistory().size() : "null");
            
            return shiftSummary;
            
        } catch (Exception e) {
            logger.error("❌ Error getting shift-based daily summary for {}: {}", date, e.getMessage(), e);
            // สร้างข้อมูลเปล่าเมื่อเกิดข้อผิดพลาด
            DailyShiftSummaryDto errorSummary = new DailyShiftSummaryDto();
            errorSummary.setProductionDate(date != null ? date.toString() : LocalDate.now().toString());
            errorSummary.setDayShiftData(createEmptyShiftData());
            errorSummary.setNightShiftData(createEmptyShiftData());
            return errorSummary;
        }
    }

    // Overload with product filter
    @Transactional(readOnly = true)
    public DailyShiftSummaryDto getDailyProductionSummaryByShift(LocalDate date, String machineId, Long productId) {
        logger.info("🕐 Getting daily production summary by shift for date: {}, machineId: {}, productId: {}", date, machineId, productId);
        try {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> reports = productionReportRepository.findActiveOnDate(targetDate, normalizedMachineId);
            reports = reports.stream()
                .filter(report -> isReportActiveOnDate(report, targetDate))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            // Reuse existing method flow by temporarily narrowing to selected reports
            if (reports.isEmpty()) {
                return getDailyProductionSummaryByShift(targetDate, normalizedMachineId);
            }

            // Build shift summary similarly to the existing method but using filtered reports
            DailyShiftSummaryDto shiftSummary = new DailyShiftSummaryDto();
            shiftSummary.setProductionDate(targetDate.toString());
            DailyProductionSummaryDto fullSummary = getDailyProductionSummary(targetDate, normalizedMachineId, productId);
            if (fullSummary == null) fullSummary = createEmptySummary(targetDate);

            ProductionReport firstReport = reports.get(0);
            LocalDateTime dayShiftStart = targetDate.atTime(3, 0);
            LocalDateTime dayShiftEndExclusive = targetDate.atTime(15, 0);
            LocalDateTime nightShiftStart = dayShiftEndExclusive;
            LocalDateTime nightShiftEndExclusive = targetDate.plusDays(1).atTime(3, 0);

            ShiftTotals dayTotals = calculateShiftTotals(reports, dayShiftStart, dayShiftEndExclusive);
            ShiftTotals nightTotals = calculateShiftTotals(reports, nightShiftStart, nightShiftEndExclusive);

            ShiftDataDto dayShift = buildShiftDataDto(dayTotals, firstReport,
                fullSummary.getMorningShiftSupervisor(),
                fullSummary.getMorningShiftWorkers(),
                fullSummary.getMorningShiftCount());
            dayShift.setNgSummary(getShiftSpecificNgSummary(targetDate, true, normalizedMachineId, productId));
            dayShift.setDowntimeHistory(getShiftSpecificDowntime(targetDate, true, normalizedMachineId, productId));
            dayShift.setMaterialUsageLogs(getShiftSpecificMaterialUsage(targetDate, true, normalizedMachineId, productId));
            dayShift.setPackagingLogs(getShiftSpecificPackagingLogs(targetDate, true, normalizedMachineId, productId));

            ShiftDataDto nightShift = buildShiftDataDto(nightTotals, firstReport,
                fullSummary.getNightShiftSupervisor(),
                fullSummary.getNightShiftWorkers(),
                fullSummary.getNightShiftCount());
            nightShift.setNgSummary(getShiftSpecificNgSummary(targetDate, false, normalizedMachineId, productId));
            nightShift.setDowntimeHistory(getShiftSpecificDowntime(targetDate, false, normalizedMachineId, productId));
            nightShift.setMaterialUsageLogs(getShiftSpecificMaterialUsage(targetDate, false, normalizedMachineId, productId));
            nightShift.setPackagingLogs(getShiftSpecificPackagingLogs(targetDate, false, normalizedMachineId, productId));

            shiftSummary.setOrderNumber(firstReport.getOrderNumber());
            if (firstReport.getMachine() != null) shiftSummary.setMachineName(firstReport.getMachine().getMachineName());
            if (firstReport.getProduct() != null) shiftSummary.setProductName(firstReport.getProduct().getProductName());
            shiftSummary.setDayShiftData(dayShift);
            shiftSummary.setNightShiftData(nightShift);
            return shiftSummary;
        } catch (Exception e) {
            logger.error("❌ Error getting shift-based daily summary with product filter: {}", e.getMessage(), e);
            DailyShiftSummaryDto errorSummary = new DailyShiftSummaryDto();
            errorSummary.setProductionDate(date != null ? date.toString() : LocalDate.now().toString());
            errorSummary.setDayShiftData(createEmptyShiftData());
            errorSummary.setNightShiftData(createEmptyShiftData());
            return errorSummary;
        }
    }
    
    private ShiftDataDto createEmptyShiftData() {
        ShiftDataDto emptyShift = new ShiftDataDto();
        emptyShift.setGoodProductionBoxes(0L);
        emptyShift.setGoodProductionPieces(0L);
        emptyShift.setNgProductionPieces(0L);
        emptyShift.setTotalProductionPieces(0L);
        emptyShift.setYieldPercentage("0%");
        emptyShift.setTotalScrapWeight(java.math.BigDecimal.ZERO);
        
        // สร้างข้อมูลเปล่าสำหรับการแสดงผล
        emptyShift.setNgSummary(new ArrayList<>());
        emptyShift.setDowntimeHistory(new ArrayList<>());
        emptyShift.setMaterialUsageLogs(new ArrayList<>());
        
        return emptyShift;
    }

    private ShiftDataDto createEmptyShiftDataWithInfo() {
        ShiftDataDto emptyShift = createEmptyShiftData();
        
        // เพิ่มข้อมูลพื้นฐานสำหรับ frontend
        emptyShift.setTargetQty(0L);
        emptyShift.setGoodQty(0L);
        emptyShift.setNgQty(0L);
        emptyShift.setScrapWeight(0.0);
        emptyShift.setOrderNumber("ไม่มีข้อมูล");
        emptyShift.setMachineName("ไม่มีข้อมูล");
        emptyShift.setProductName("ไม่มีข้อมูล");
        emptyShift.setShiftLeader("ไม่ระบุ");
        emptyShift.setWorkers("ไม่ระบุ");
        emptyShift.setWorkerCount(0);
        
        return emptyShift;
    }

    private ShiftTotals calculateShiftTotals(List<ProductionReport> reports, LocalDateTime shiftStart, LocalDateTime shiftEndExclusive) {
        ShiftTotals totals = new ShiftTotals();

        if (reports == null || reports.isEmpty() || shiftStart == null || shiftEndExclusive == null || !shiftEndExclusive.isAfter(shiftStart)) {
            return totals;
        }

        List<Long> reportIds = reports.stream()
            .filter(java.util.Objects::nonNull)
            .map(ProductionReport::getId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

        if (reportIds.isEmpty()) {
            return totals;
        }

        LocalDateTime queryEndInclusive = shiftEndExclusive.minusNanos(1);

        // Aggregate overall targets from active reports (used for per-shift target distribution)
        long overallTarget = 0L;
        for (ProductionReport report : reports) {
            if (report != null && report.getTargetQty() != null) {
                overallTarget += report.getTargetQty();
            }
        }
        totals.setTotalTarget(overallTarget);

        List<PackagingLog> packagingLogs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, shiftStart, queryEndInclusive);
        logger.info("📦 Packaging logs for shift window {} to {}: {} record(s)", shiftStart, queryEndInclusive, packagingLogs.size());
        if (packagingLogs.isEmpty()) {
            // diagnostics: compare with calendar day
            LocalDateTime calStart = shiftStart.toLocalDate().atStartOfDay();
            LocalDateTime calEndIncl = calStart.plusDays(1).minusNanos(1);
            List<PackagingLog> dayLogs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, calStart, calEndIncl);
            logger.info("🔎 [Shift] Packaging logs for calendar day ({} to {}): {} record(s)", calStart, calEndIncl, dayLogs.size());
        }
        java.util.Map<Long, Long> boxesByReport = packagingLogs.stream()
            .filter(log -> log.getReport() != null && log.getReport().getId() != null)
            .collect(Collectors.groupingBy(log -> log.getReport().getId(), Collectors.counting()));

        totals.setGoodBoxes(boxesByReport.values().stream().mapToLong(Long::longValue).sum());

        long goodQty = 0L;
        for (ProductionReport report : reports) {
            long boxes = boxesByReport.getOrDefault(report.getId(), 0L);
            int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                ? report.getProduct().getQtyPerBox()
                : 0;
            if (qtyPerBox <= 0) {
                int fallback = 0;
                try {
                    if (report.getProduct() != null && report.getProduct().getCavity() != null) {
                        fallback = report.getProduct().getCavity();
                    }
                } catch (Exception ignore) {}
                if (fallback <= 0) fallback = 1; // at least count boxes as 1 piece each
                logger.warn("qtyPerBox missing/zero for product {} (report {}), using fallback {}", 
                    report.getProduct() != null ? report.getProduct().getProductName() : "unknown",
                    report.getId(), fallback);
                qtyPerBox = fallback;
            }
            goodQty += boxes * (long) qtyPerBox;
        }
    logger.info("✅ Shift good calculation: boxesTotal={}, goodQty={} (pieces)", totals.getGoodBoxes(), goodQty);
    totals.setGoodQty(goodQty);

        List<NgLog> ngLogs = ngLogRepository.findByReportIdInAndTimestampBetween(reportIds, shiftStart, queryEndInclusive);
        long ngQty = 0L;
        long technicianNgQty = 0L;
        for (NgLog log : ngLogs) {
            long q = log.getQuantity() != null ? log.getQuantity() : 0L;
            ngQty += q;
            // Treat NG recorded by a Technician as technician NG
            try {
                if (log.getUser() != null && log.getUser().getRole() != null) {
                    String role = log.getUser().getRole();
                    if (role.equalsIgnoreCase("Technician") || role.equalsIgnoreCase("MECHANIC")) {
                        technicianNgQty += q;
                    }
                }
            } catch (Exception ignore) { }
        }
        totals.setNgQty(ngQty);
        totals.setTechnicianNgQty(technicianNgQty);

        java.util.List<com.gdtahara.gdtaharabackend.model.ScrapWeightLog> scrapLogs =
            scrapWeightLogRepository.findByReportIdInAndTimestampBetween(reportIds, shiftStart, queryEndInclusive);
        java.math.BigDecimal scrapWeight = scrapLogs.stream()
            .map(log -> log.getWeightKg() != null ? log.getWeightKg() : java.math.BigDecimal.ZERO)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        totals.setScrapWeight(scrapWeight);
        // All scrapWeight logs are recorded by Technician role; keep a mirror value for explicit reporting
        totals.setTechnicianScrapWeight(scrapWeight);

        // Technician downtime minutes: any downtime event with a technician assigned
        long technicianDowntimeMinutes = 0L;
        try {
            List<DowntimeEvent> events = downtimeEventRepository.findByReportIdIn(reportIds);
            for (DowntimeEvent ev : events) {
                if (!isOverlapping(ev.getStartTime(), ev.getEndTime(), shiftStart, shiftEndExclusive)) continue;
                if (ev.getTechnician() != null) {
                    LocalDateTime s = ev.getStartTime();
                    LocalDateTime e = ev.getEndTime() != null ? ev.getEndTime() : shiftEndExclusive;
                    if (s != null) {
                        long minutes = java.time.Duration.between(s, e).toMinutes();
                        technicianDowntimeMinutes += Math.max(minutes, 0);
                    }
                }
            }
        } catch (Exception ignore) { }
        totals.setTechnicianDowntimeMinutes(technicianDowntimeMinutes);

        return totals;
    }

    private ShiftDataDto buildShiftDataDto(ShiftTotals totals, ProductionReport baseReport, String leader, String workers, int workerCount) {
        ShiftDataDto dto = new ShiftDataDto();

        long goodBoxes = totals != null ? totals.getGoodBoxes() : 0L;
        long goodQty = totals != null ? totals.getGoodQty() : 0L;
        long ngQty = totals != null ? totals.getNgQty() : 0L;
        java.math.BigDecimal scrapWeight = (totals != null && totals.getScrapWeight() != null)
            ? totals.getScrapWeight()
            : java.math.BigDecimal.ZERO;

        long totalPieces = goodQty + ngQty;
        double yield = totalPieces > 0 ? (double) goodQty / totalPieces * 100 : 0.0;

        dto.setGoodProductionBoxes(goodBoxes);
        dto.setGoodProductionPieces(goodQty);
        dto.setNgProductionPieces(ngQty);
        dto.setTotalProductionPieces(totalPieces);
        dto.setYieldPercentage(String.format("%.2f%%", yield));
        dto.setTotalScrapWeight(scrapWeight);
        // เป้าหมายต่อกะ = 50% ของเป้าหมายรวมของรายงานที่ใช้งานในวันนั้น
        long perShiftTarget = 0L;
        if (totals != null && totals.getTotalTarget() > 0) {
            // แบ่งเท่า ๆ กันระหว่าง 2 กะ (03:00-15:00 และ 15:00-03:00)
            perShiftTarget = Math.round(totals.getTotalTarget() / 2.0);
        }
        dto.setTargetQty(perShiftTarget);
        dto.setGoodQty(goodQty);
        dto.setNgQty(ngQty);
        dto.setScrapWeight(scrapWeight.doubleValue());

    // Technician rollups onto DTO
    dto.setTechnicianNgQty(totals != null ? totals.getTechnicianNgQty() : 0L);
    dto.setTechnicianDowntimeMinutes(totals != null ? totals.getTechnicianDowntimeMinutes() : 0L);
    dto.setTechnicianScrapWeight(totals != null && totals.getTechnicianScrapWeight() != null ? totals.getTechnicianScrapWeight() : java.math.BigDecimal.ZERO);

        if (baseReport != null) {
            dto.setOrderNumber(baseReport.getOrderNumber());
            if (baseReport.getMachine() != null) {
                dto.setMachineName(baseReport.getMachine().getMachineName());
            }
            if (baseReport.getProduct() != null) {
                dto.setProductName(baseReport.getProduct().getProductName());
            }
        }

        dto.setShiftLeader(leader != null ? leader : "ไม่ระบุ");
        dto.setWorkers(workers != null ? workers : "ไม่ระบุ");
        dto.setWorkerCount(workerCount);

        return dto;
    }
    
    private DailyProductionSummaryDto createEmptySummary(LocalDate date) {
        DailyProductionSummaryDto summary = new DailyProductionSummaryDto();
        summary.setDate(date);
        summary.setTotalGoodQty(0L);
        summary.setTotalNgQty(0L);
        summary.setTotalGoodBoxes(0L);
        summary.setTotalScrapWeight(java.math.BigDecimal.ZERO);
        summary.setMorningShiftSupervisor("ไม่มีข้อมูล");
        summary.setNightShiftSupervisor("ไม่มีข้อมูล");
        summary.setMorningShiftWorkers("ไม่มีข้อมูล");
        summary.setNightShiftWorkers("ไม่มีข้อมูล");
        summary.setMorningShiftCount(0);
        summary.setNightShiftCount(0);
        return summary;
    }

    private boolean isReportActiveOnDate(ProductionReport report, LocalDate targetDate) {
        if (report == null) {
            return false;
        }

        LocalDate startDate = report.getStartDate();
        LocalDate endDate = report.getEndDate();

        if (startDate != null && targetDate.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && targetDate.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    private boolean isOverlapping(LocalDateTime eventStart, LocalDateTime eventEnd, LocalDateTime windowStart, LocalDateTime windowEndExclusive) {
        if (eventStart == null || windowStart == null || windowEndExclusive == null) {
            return false;
        }

        LocalDateTime effectiveEnd = eventEnd != null ? eventEnd : windowEndExclusive;

        return eventStart.isBefore(windowEndExclusive) && effectiveEnd.isAfter(windowStart);
    }

    private static class ShiftTotals {
        private long goodBoxes;
        private long goodQty;
        private long ngQty;
        private java.math.BigDecimal scrapWeight = java.math.BigDecimal.ZERO;
        private long totalTarget;
        private long technicianNgQty;
        private long technicianDowntimeMinutes;
        private java.math.BigDecimal technicianScrapWeight = java.math.BigDecimal.ZERO;

        public long getGoodBoxes() {
            return goodBoxes;
        }

        public void setGoodBoxes(long goodBoxes) {
            this.goodBoxes = goodBoxes;
        }

        public long getGoodQty() {
            return goodQty;
        }

        public void setGoodQty(long goodQty) {
            this.goodQty = goodQty;
        }

        public long getNgQty() {
            return ngQty;
        }

        public void setNgQty(long ngQty) {
            this.ngQty = ngQty;
        }

        public long getTechnicianNgQty() { return technicianNgQty; }
        public void setTechnicianNgQty(long v) { this.technicianNgQty = v; }

        public java.math.BigDecimal getScrapWeight() {
            return scrapWeight;
        }

        public void setScrapWeight(java.math.BigDecimal scrapWeight) {
            this.scrapWeight = scrapWeight != null ? scrapWeight : java.math.BigDecimal.ZERO;
        }

        public long getTechnicianDowntimeMinutes() { return technicianDowntimeMinutes; }
        public void setTechnicianDowntimeMinutes(long v) { this.technicianDowntimeMinutes = v; }

        public java.math.BigDecimal getTechnicianScrapWeight() { return technicianScrapWeight; }
        public void setTechnicianScrapWeight(java.math.BigDecimal v) { this.technicianScrapWeight = v != null ? v : java.math.BigDecimal.ZERO; }

        public long getTotalTarget() {
            return totalTarget;
        }

        public void setTotalTarget(long totalTarget) {
            this.totalTarget = totalTarget;
        }
    }

    /**
     * ทำความสะอาดข้อความเหตุผล Downtime ให้เหมาะกับการแสดงผลภาษาไทย
     * - ตัดช่องว่างซ้ายขวา
     * - แทนที่ค่า null/ว่างด้วย "ไม่ระบุสาเหตุ"
     * - ลบอักขระที่พิมพ์ไม่ได้เพื่อหลีกเลี่ยงการแสดงเป็นเครื่องหมายคำถาม
     */
    private String normalizeReason(String raw) {
        if (raw == null) return "ไม่ระบุสาเหตุ";
        String s = raw.trim();
        if (s.isEmpty()) return "ไม่ระบุสาเหตุ";
        // Remove non-printable control chars
        s = s.replaceAll("[\\p{Cntrl}]", "");
        // ถ้ามีเครื่องหมาย ? จำนวนมาก ให้ถือว่าเป็น mojibake แล้ว fallback
        long qCount = s.chars().filter(ch -> ch == '?').count();
        int letters = (int) s.chars().filter(ch -> !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != '-' && ch != '/').count();
        if (qCount >= 3 || (letters > 0 && ((double) qCount / letters) >= 0.4)) {
            return "ไม่ระบุสาเหตุ";
        }
        return s;
    }

    // Helper: round to 2 decimal places
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Convert NgSummaryDto list to NgTypeSummaryDto list
     */
    @SuppressWarnings("unused")
    private List<NgTypeSummaryDto> convertNgSummaryToNgTypeSummary(List<NgSummaryDto> ngSummaryList) {
        if (ngSummaryList == null || ngSummaryList.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<NgTypeSummaryDto> result = new ArrayList<>();
        for (NgSummaryDto ngSummary : ngSummaryList) {
            NgTypeSummaryDto ngType = new NgTypeSummaryDto();
            ngType.setNgDescription(ngSummary.getNgDescription());
            ngType.setCount(ngSummary.getCount());
            ngType.setTotalQuantity(ngSummary.getCount() != null ? ngSummary.getCount() : 0L);
            // Calculate percentage if possible (we'll use a default percentage)
            ngType.setPercentage(0.0); // This should be calculated based on total production
            result.add(ngType);
        }
        return result;
    }

    /**
     * ดึงข้อมูลของเสียตามกะการทำงาน
     * @param date วันที่
     * @param isDayShift true = กะกลางวัน (03:00-15:00), false = กะกลางคืน (15:00-03:00)
     */
    // Overloaded method รองรับ machineId
    private List<NgTypeSummaryDto> getShiftSpecificNgSummary(LocalDate date, boolean isDayShift, String machineId) {
        return getShiftSpecificNgSummary(date, isDayShift, machineId, "th");
    }

    private List<NgTypeSummaryDto> getShiftSpecificNgSummary(LocalDate date, boolean isDayShift, String machineId, String lang) {
        try {
            LocalDateTime startTime, endTimeExclusive;
            
            if (isDayShift) {
                // กะกลางวัน 03:00-15:00
                startTime = date.atTime(3, 0);
                endTimeExclusive = date.atTime(15, 0);
            } else {
                // กะกลางคืน 15:00-03:00 (วันถัดไป)
                startTime = date.atTime(15, 0);
                endTimeExclusive = date.plusDays(1).atTime(3, 0);
            }
            
            logger.info("🔍 Filtering NG data for {} shift and machineId {}: {} to {}", 
                isDayShift ? "day" : "night", machineId, startTime, endTimeExclusive);
            
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(java.util.stream.Collectors.toList());
            logger.info("📊 Found {} active production reports for date: {} and machineId: {}", activeReports.size(), date, machineId);
            
            return processNgSummaryForShift(activeReports, startTime, endTimeExclusive, isDayShift, date, lang);
        } catch (Exception e) {
            logger.error("❌ Error in getShiftSpecificNgSummary for date: {}, isDayShift: {}, machineId: {}", date, isDayShift, machineId, e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unused")
    private List<NgTypeSummaryDto> getShiftSpecificNgSummary(LocalDate date, boolean isDayShift) {
        return getShiftSpecificNgSummary(date, isDayShift, null);
    }
    
    private List<NgTypeSummaryDto> processNgSummaryForShift(List<ProductionReport> activeReports, LocalDateTime startTime, LocalDateTime endTimeExclusive, boolean isDayShift, LocalDate date) {
        return processNgSummaryForShift(activeReports, startTime, endTimeExclusive, isDayShift, date, "th");
    }

    private List<NgTypeSummaryDto> processNgSummaryForShift(List<ProductionReport> activeReports, LocalDateTime startTime, LocalDateTime endTimeExclusive, boolean isDayShift, LocalDate date, String lang) {
        try {
            // รวมยอดของเสียตามประเภท ของทั้งกะ (ทุก report ที่อยู่ในกะ)
            Map<String, Long> aggregateByType = new HashMap<>();
            Map<String, java.time.LocalDateTime> latestTimestampByType = new HashMap<>();
            // รวมยอดน้ำหนักของเสีย (ชั่งน้ำหนักโดย Technician) แยกตามประเภทภายในช่วงกะ
            Map<String, java.math.BigDecimal> scrapWeightByType = new HashMap<>();
            java.time.LocalDateTime debugMin = null;
            java.time.LocalDateTime debugMax = null;

            if (!activeReports.isEmpty()) {
                for (ProductionReport report : activeReports) {
                    logger.info("🔍 Checking report ID: {} (Order: {}, Status: {})", report.getId(), report.getOrderNumber(), report.getStatus());

                    // Use endInclusive to avoid including the 03:00 of the next shift
                    LocalDateTime endInclusive = endTimeExclusive.minusNanos(1);
                    List<NgLog> ngLogs = ngLogRepository.findByReportIdAndTimestampBetween(report.getId(), startTime, endInclusive);
                    logger.info("📊 Found {} NG logs for report {} in time range {} to {} (inclusive)", ngLogs.size(), report.getId(), startTime, endInclusive);

                    for (NgLog ngLog : ngLogs) {
                        String ngTypeName = ngLog.getNgType() != null ? resolveNgDescription(ngLog.getNgType(), lang) : "ไม่ระบุ";
                        long quantity = ngLog.getQuantity() != null ? ngLog.getQuantity() : 0L;
                        aggregateByType.merge(ngTypeName, quantity, Long::sum);
                        if (ngLog.getTimestamp() != null) {
                            latestTimestampByType.merge(
                                ngTypeName,
                                ngLog.getTimestamp(),
                                (oldV, newV) -> newV.isAfter(oldV) ? newV : oldV
                            );
                        }

                        if (ngLog.getTimestamp() != null) {
                            if (debugMin == null || ngLog.getTimestamp().isBefore(debugMin)) debugMin = ngLog.getTimestamp();
                            if (debugMax == null || ngLog.getTimestamp().isAfter(debugMax)) debugMax = ngLog.getTimestamp();
                        }
                    }

                    // รวม ScrapWeight (Technician) สำหรับ report นี้ในช่วงเวลาเดียวกัน
                    try {
                        List<ScrapWeightLog> scrapLogs = scrapWeightLogRepository.findByReportIdInAndTimestampBetween(
                            java.util.List.of(report.getId()), startTime, endInclusive
                        );
                        for (ScrapWeightLog sw : scrapLogs) {
                            String scrapType = sw.getScrapType() != null ? sw.getScrapType() : "ไม่ระบุประเภท";
                            String matType = sw.getMatType() != null ? sw.getMatType() : "";
                            String combinedType = matType.isEmpty() ? scrapType : (matType + " " + scrapType);
                            java.math.BigDecimal w = sw.getWeightKg() != null ? sw.getWeightKg() : java.math.BigDecimal.ZERO;
                            scrapWeightByType.merge(combinedType, w, java.math.BigDecimal::add);
                        }
                    } catch (Exception ex) {
                        logger.warn("⚠️ Could not aggregate scrap weights for report {}: {}", report.getId(), ex.getMessage());
                    }
                }
            }

            if (aggregateByType.isEmpty()) {
                logger.info("ℹ️ No NG data found for {} shift on {}", isDayShift ? "day" : "night", date);
                return new ArrayList<>();
            }

            // คำนวณ % ของเสีย ภายในกะนั้น ๆ
            long totalNgInShift = aggregateByType.values().stream().mapToLong(Long::longValue).sum();
            List<NgTypeSummaryDto> result = new ArrayList<>();
            java.time.format.DateTimeFormatter thaiDateTime = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

            // เตรียม fallback time: ใช้วันที่ที่ผู้ใช้เรียก + เวลาจากข้อมูลล่าสุดในกะ
            java.time.LocalTime fallbackTimeOfDay;
            if (debugMax != null) {
                fallbackTimeOfDay = debugMax.toLocalTime();
            } else {
                // ถ้าไม่มีข้อมูลเลย ให้ใช้เวลาใกล้สิ้นสุดกะ (เช่น 14:59:59 หรือ 02:59:59)
                fallbackTimeOfDay = endTimeExclusive.minusNanos(1).toLocalTime();
            }
            java.time.LocalDateTime fallbackDateTime = java.time.LocalDateTime.of(date, fallbackTimeOfDay);

            for (Map.Entry<String, Long> entry : aggregateByType.entrySet()) {
                String type = entry.getKey();
                long count = entry.getValue();
                double pct = totalNgInShift > 0 ? round2(((double) count / totalNgInShift) * 100.0) : 0.0;

                NgTypeSummaryDto dto = new NgTypeSummaryDto();
                dto.setNgDescription(type);
                dto.setCount(count);
                dto.setTotalQuantity(count);
                dto.setPercentage(pct);
                // แสดงเวลาของ record สุดท้ายของประเภทนั้นในกะ
                // ถ้าไม่มี ให้ใช้วันที่ที่เรียก + เวลา (ชั่วโมง:นาที:วินาที) จากข้อมูลล่าสุดในกะ
                java.time.LocalDateTime lastTs = latestTimestampByType.get(type);
                dto.setTimeDisplay(
                    lastTs != null
                        ? lastTs.format(thaiDateTime)
                        : fallbackDateTime.format(thaiDateTime)
                );
                dto.setWeightKg(java.math.BigDecimal.ZERO);
                result.add(dto);
            }

            // เติมข้อมูลน้ำหนักของเสียตามประเภท (Technician) เข้าไปในรายการสรุป โดยไม่ทับซ้อนกับชื่อประเภทที่มีอยู่
            for (Map.Entry<String, java.math.BigDecimal> e : scrapWeightByType.entrySet()) {
                String type = e.getKey();
                java.math.BigDecimal weight = e.getValue() != null ? e.getValue() : java.math.BigDecimal.ZERO;
                // ถ้ามีอยู่แล้ว ให้เพิ่ม field weight เข้าไปใน item เดิม
                NgTypeSummaryDto existing = null;
                for (NgTypeSummaryDto item : result) {
                    if (type.equals(item.getNgDescription())) { existing = item; break; }
                }
                if (existing != null) {
                    existing.setWeightKg(weight);
                } else {
                    NgTypeSummaryDto dto = new NgTypeSummaryDto();
                    dto.setNgDescription(type);
                    dto.setCount(0L);
                    dto.setTotalQuantity(0L);
                    dto.setPercentage(0.0);
                    dto.setWeightKg(weight);
                    dto.setTimeDisplay(fallbackDateTime.format(thaiDateTime));
                    result.add(dto);
                }
            }

            // เรียงลำดับมากไปน้อยตามจำนวน
            result.sort((a, b) -> {
                long ac = a.getCount() != null ? a.getCount() : 0L;
                long bc = b.getCount() != null ? b.getCount() : 0L;
                if (bc != ac) return Long.compare(bc, ac);
                // ถ้าจำนวนชิ้นเท่ากัน ให้เรียงตามน้ำหนักจากมากไปน้อย
                java.math.BigDecimal aw = a.getWeightKg() != null ? a.getWeightKg() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal bw = b.getWeightKg() != null ? b.getWeightKg() : java.math.BigDecimal.ZERO;
                int cmp = bw.compareTo(aw);
                if (cmp != 0) return cmp;
                return String.CASE_INSENSITIVE_ORDER.compare(a.getNgDescription() != null ? a.getNgDescription() : "", b.getNgDescription() != null ? b.getNgDescription() : "");
            });

            logger.info("✅ NG summary for {} shift: totalNG={}, types={}, minTs={}, maxTs={}", isDayShift ? "day" : "night", totalNgInShift, result.size(), debugMin, debugMax);
            return result;

        } catch (Exception e) {
            logger.error("Error filtering NG summary for shift", e);
            return new ArrayList<>();
        }
    }

    // Overloaded helpers with product filter
    private List<NgTypeSummaryDto> getShiftSpecificNgSummary(LocalDate date, boolean isDayShift, String machineId, Long productId) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTime = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            return processNgSummaryForShift(activeReports, startTime, endTime, isDayShift, date);
        } catch (Exception e) {
            logger.error("❌ Error in getShiftSpecificNgSummary (with product) for date: {}, isDay: {}, machineId: {}, productId: {}", date, isDayShift, machineId, productId, e);
            return new ArrayList<>();
        }
    }

    // Public facade for controller to get language-aware NG summaries with product filter
    @Transactional(readOnly = true)
    public List<NgTypeSummaryDto> getShiftSpecificNgSummary(LocalDate date, boolean isDayShift, String machineId, Long productId, String lang) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTime = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            return processNgSummaryForShift(activeReports, startTime, endTime, isDayShift, date, lang);
        } catch (Exception e) {
            logger.error("❌ Error in getShiftSpecificNgSummary (public, lang) for date: {}, isDay: {}, machineId: {}, productId: {}", date, isDayShift, machineId, productId, e);
            return new ArrayList<>();
        }
    }

    /**
     * ดึงข้อมูล Downtime ตามกะการทำงาน พร้อม machineId
     */
    private List<DowntimeEventSummaryDto> getShiftSpecificDowntime(LocalDate date, boolean isDayShift, String machineId) {
        try {
            LocalDateTime startTime, endTimeExclusive;
            
            if (isDayShift) {
                startTime = date.atTime(3, 0);
                endTimeExclusive = date.atTime(15, 0);
            } else {
                startTime = date.atTime(15, 0);
                endTimeExclusive = date.plusDays(1).atTime(3, 0);
            }
            
            logger.info("🔍 Filtering Downtime data for {} shift and machineId {}: {} to {}", 
                isDayShift ? "day" : "night", machineId, startTime, endTimeExclusive);
            
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(Collectors.toList());
            
            List<DowntimeEventSummaryDto> downtimeList = new ArrayList<>();

            if (!activeReports.isEmpty()) {
                List<Long> reportIds = activeReports.stream()
                    .map(ProductionReport::getId)
                    .collect(Collectors.toList());

                // ดึงทั้งหมดแล้วกรองด้วย overlap เพื่อไม่พลาดเหตุการณ์ที่เริ่มก่อนช่วงเวลา
                List<DowntimeEvent> events = downtimeEventRepository.findByReportIdIn(reportIds);
                for (DowntimeEvent event : events) {
                    if (!isOverlapping(event.getStartTime(), event.getEndTime(), startTime, endTimeExclusive)) continue;
                    String startTimeStr = event.getStartTime() != null ? event.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
                    String endTimeStr = event.getEndTime() != null ? event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "กำลังดำเนินการ";
                    String duration = "ไม่ระบุ";
                    if (event.getEndTime() != null && event.getStartTime() != null) {
                        long minutes = java.time.Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
                        duration = minutes + " นาที";
                    }
                    String reason = normalizeReason(event.getReason());
                    String technician = event.getTechnician() != null ? event.getTechnician().getUsername() : "ไม่ระบุ";
                    downtimeList.add(new DowntimeEventSummaryDto(startTimeStr, endTimeStr, duration, reason, technician, event.getSolution() != null ? event.getSolution() : ""));
                }
            }
            
            // ถ้าไม่มีข้อมูลจริง ให้แจ้งและคืนข้อมูลเปล่า
            if (downtimeList.isEmpty()) {
                logger.info("ℹ️ No downtime data found for {} shift on {}", isDayShift ? "day" : "night", date);
            } else {
                logger.info("✅ Found {} real downtime events for {} shift", downtimeList.size(), isDayShift ? "day" : "night");
            }
            
            logger.info("🔚 Returning {} downtime events for {} shift", downtimeList.size(), isDayShift ? "day" : "night");
            return downtimeList;
            
        } catch (Exception e) {
            logger.error("Error filtering downtime for shift", e);
            return new ArrayList<>();
        }
    }
    
    // Overloaded with product filter
    private List<DowntimeEventSummaryDto> getShiftSpecificDowntime(LocalDate date, boolean isDayShift, String machineId, Long productId) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTime = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            if (activeReports.isEmpty()) return new ArrayList<>();

            List<Long> reportIds = activeReports.stream().map(ProductionReport::getId).collect(Collectors.toList());
            List<DowntimeEvent> events = downtimeEventRepository.findByReportIdIn(reportIds);
            List<DowntimeEventSummaryDto> result = new ArrayList<>();
            for (DowntimeEvent event : events) {
                if (!isOverlapping(event.getStartTime(), event.getEndTime(), startTime, endTime)) continue;
                String startTimeStr = event.getStartTime() != null ? event.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
                String endTimeStr = event.getEndTime() != null ? event.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "กำลังดำเนินการ";
                String duration = "ไม่ระบุ";
                if (event.getEndTime() != null && event.getStartTime() != null) {
                    long minutes = java.time.Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
                    duration = minutes + " นาที";
                }
                String reason = normalizeReason(event.getReason());
                String technician = event.getTechnician() != null ? event.getTechnician().getUsername() : "ไม่ระบุ";
                result.add(new DowntimeEventSummaryDto(startTimeStr, endTimeStr, duration, reason, technician, event.getSolution() != null ? event.getSolution() : ""));
            }
            return result;
        } catch (Exception e) {
            logger.error("Error filtering downtime for shift (with product)", e);
            return new ArrayList<>();
        }
    }
    @SuppressWarnings("unused")
    private List<DowntimeEventSummaryDto> getShiftSpecificDowntime(LocalDate date, boolean isDayShift) {
        return getShiftSpecificDowntime(date, isDayShift, null);
    }

    /**
     * ดึงข้อมูลการใช้วัตถุดิบตามกะการทำงาน พร้อม machineId
     */
    private List<MaterialUsageLogDto> getShiftSpecificMaterialUsage(LocalDate date, boolean isDayShift, String machineId) {
        try {
            LocalDateTime startTime, endTimeExclusive;
            
            if (isDayShift) {
                startTime = date.atTime(3, 0);
                endTimeExclusive = date.atTime(15, 0);
            } else {
                startTime = date.atTime(15, 0);
                endTimeExclusive = date.plusDays(1).atTime(3, 0);
            }
            
            logger.info("🔍 Filtering Material usage data for {} shift and machineId {}: {} to {}", 
                isDayShift ? "day" : "night", machineId, startTime, endTimeExclusive);
            
            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(Collectors.toList());
            
            List<MaterialUsageLogDto> materialUsageList = new ArrayList<>();

            if (!activeReports.isEmpty()) {
                for (ProductionReport report : activeReports) {
                    LocalDateTime endInclusive = endTimeExclusive.minusNanos(1);
                    List<MaterialUsageLog> usageLogs = materialUsageLogRepository.findByReportIdAndTimestampBetween(
                        report.getId(), startTime, endInclusive
                    );
                    for (MaterialUsageLog log : usageLogs) {
                        MaterialUsageLogDto dto = new MaterialUsageLogDto();
                        dto.setMaterialCode(log.getMaterialCode());
                        dto.setLotNumber(log.getLotNumber());
                        dto.setQuantityKg(log.getQuantityKg());
                        dto.setTechnicianName(log.getTechnician() != null ? log.getTechnician().getUsername() : "ไม่ระบุ");
                        dto.setTimestamp(log.getTimestamp());
                        materialUsageList.add(dto);
                    }
                }
            }
            
            // ถ้าไม่มีข้อมูลจริง ให้แจ้งและคืนข้อมูลเปล่า
            if (materialUsageList.isEmpty()) {
                logger.info("ℹ️ No material usage data found for {} shift on {}", isDayShift ? "day" : "night", date);
            } else {
                logger.info("✅ Found {} real material usage records for {} shift", materialUsageList.size(), isDayShift ? "day" : "night");
            }
            
            logger.info("🔚 Returning {} material usage records for {} shift", materialUsageList.size(), isDayShift ? "day" : "night");
            return materialUsageList;
            
        } catch (Exception e) {
            logger.error("Error filtering material usage for shift", e);
            return new ArrayList<>();
        }
    }
    
    @SuppressWarnings("unused")
    private List<MaterialUsageLogDto> getShiftSpecificMaterialUsage(LocalDate date, boolean isDayShift) {
        return getShiftSpecificMaterialUsage(date, isDayShift, null);
    }

    /**
     * ดึงรายการบันทึกการบรรจุตามกะ เพื่อแสดงตรวจสอบข้อมูลจริง
     */
    private List<PackagingLogViewDto> getShiftSpecificPackagingLogs(LocalDate date, boolean isDayShift, String machineId) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTimeExclusive = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);
            LocalDateTime endInclusive = endTimeExclusive.minusNanos(1);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .collect(Collectors.toList());
            if (activeReports.isEmpty()) return new ArrayList<>();

            List<Long> reportIds = activeReports.stream().map(ProductionReport::getId).collect(Collectors.toList());
            List<PackagingLog> logs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, startTime, endInclusive);
            return logs.stream()
                .map(p -> new PackagingLogViewDto(
                    p.getTimestamp(),
                    p.getLotNumber(),
                    p.getBoxNo(),
                    p.getOperator() != null ? p.getOperator().getUsername() : ""
                ))
                .sorted(java.util.Comparator.comparing(PackagingLogViewDto::getTimestamp, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error filtering packaging logs for shift", e);
            return new ArrayList<>();
        }
    }

    // Overloaded with product filter
    private List<PackagingLogViewDto> getShiftSpecificPackagingLogs(LocalDate date, boolean isDayShift, String machineId, Long productId) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTimeExclusive = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);
            LocalDateTime endInclusive = endTimeExclusive.minusNanos(1);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());
            if (activeReports.isEmpty()) return new ArrayList<>();

            List<Long> reportIds = activeReports.stream().map(ProductionReport::getId).collect(Collectors.toList());
            List<PackagingLog> logs = packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, startTime, endInclusive);
            return logs.stream()
                .map(p -> new PackagingLogViewDto(
                    p.getTimestamp(),
                    p.getLotNumber(),
                    p.getBoxNo(),
                    p.getOperator() != null ? p.getOperator().getUsername() : ""
                ))
                .sorted(java.util.Comparator.comparing(PackagingLogViewDto::getTimestamp, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error filtering packaging logs for shift (with product)", e);
            return new ArrayList<>();
        }
    }

    // Overloaded with product filter
    private List<MaterialUsageLogDto> getShiftSpecificMaterialUsage(LocalDate date, boolean isDayShift, String machineId, Long productId) {
        try {
            LocalDateTime startTime = isDayShift ? date.atTime(3, 0) : date.atTime(15, 0);
            LocalDateTime endTime = isDayShift ? date.atTime(15, 0) : date.plusDays(1).atTime(3, 0);

            String normalizedMachineId = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                ? machineId.trim()
                : null;

            List<ProductionReport> activeReports = productionReportRepository.findActiveOnDate(date, normalizedMachineId).stream()
                .filter(report -> isReportActiveOnDate(report, date))
                .filter(report -> productId == null || (report.getProduct() != null && productId.equals(report.getProduct().getId())))
                .collect(Collectors.toList());

            List<MaterialUsageLogDto> result = new ArrayList<>();
            for (ProductionReport report : activeReports) {
                List<MaterialUsageLog> usageLogs = materialUsageLogRepository.findByReportIdAndTimestampBetween(
                    report.getId(), startTime, endTime
                );
                for (MaterialUsageLog log : usageLogs) {
                    MaterialUsageLogDto dto = new MaterialUsageLogDto();
                    dto.setMaterialCode(log.getMaterialCode());
                    dto.setLotNumber(log.getLotNumber());
                    dto.setQuantityKg(log.getQuantityKg());
                    dto.setTechnicianName(log.getTechnician() != null ? log.getTechnician().getUsername() : "ไม่ระบุ");
                    dto.setTimestamp(log.getTimestamp());
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Error filtering material usage for shift (with product)", e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public DailyShiftSummaryDto getDailySummaryByShift(java.time.LocalDate date) {
        // Return empty summary for now
        return new DailyShiftSummaryDto();
    }

    @Transactional
    public MaterialUsageLogDto logMaterialUsage(MaterialUsageLogRequestDto request) {
        // Return empty log for now
        return new MaterialUsageLogDto();
    }

    @Transactional(readOnly = true)
    public DetailedProductionReportDto getDetailedReport(Long reportId) {
        return getDetailedReport(reportId, "th");
    }

    @Transactional(readOnly = true)
    public DetailedProductionReportDto getDetailedReport(Long reportId, String lang) {
        logger.info("Building detailed report for ID: {}", reportId);
        ProductionReport report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with id: " + reportId));

        DetailedProductionReportDto dto = new DetailedProductionReportDto();
        // Header
        dto.setReportId(report.getId());
        dto.setOrderNumber(report.getOrderNumber());
        dto.setStartDate(report.getStartDate());
        dto.setEndDate(report.getEndDate());
        dto.setMachineName(report.getMachine() != null ? report.getMachine().getMachineName() : "");
        if (report.getProduct() != null) {
            dto.setProductCode(report.getProduct().getProductCode());
            dto.setProductName(report.getProduct().getProductName());
            dto.setCavity(report.getProduct().getCavity());
            dto.setQtyPerBox(report.getProduct().getQtyPerBox());
        }
        dto.setTargetQty(report.getTargetQty());

        // Totals
        long boxCount = 0L;
        try {
            Long c = packagingLogRepository.countPackagesByReportId(reportId);
            boxCount = c != null ? c : 0L;
        } catch (Exception ex) {
            logger.warn("Cannot count packaging logs for report {}: {}", reportId, ex.getMessage());
        }
        int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                ? report.getProduct().getQtyPerBox() : 0;
        long goodQty = boxCount * qtyPerBox;

        long ngQty = 0L;
        try {
            Long sum = ngLogRepository.sumQuantityByReportId(reportId);
            ngQty = sum != null ? sum : 0L;
        } catch (Exception ex) {
            logger.warn("Cannot sum NG logs for report {}: {}", reportId, ex.getMessage());
        }
        String yield = "0%";
        int target = report.getTargetQty() != null ? report.getTargetQty() : 0;
        if (target > 0) {
            double y = ((double) Math.max(goodQty - ngQty, 0)) / target * 100.0;
            yield = String.format("%.2f%%", y);
        }

        dto.setGoodBoxes(boxCount);
        dto.setGoodQty(goodQty);
        dto.setTotalNgQty(ngQty);
        dto.setYield(yield);
        try {
            dto.setTotalScrapWeightKg(scrapWeightLogRepository.sumWeightByReportId(reportId));
        } catch (Exception ex) {
            logger.warn("Cannot sum scrap weight for report {}: {}", reportId, ex.getMessage());
        }

        // Sections: materials
        try {
            var materials = materialUsageLogRepository.findByReportId(reportId);
            List<MaterialUsageLogDto> materialDtos = materials.stream().map(m ->
                new MaterialUsageLogDto(
                    m.getTimestamp(),
                    m.getMaterialCode(),
                    m.getLotNumber(),
                    m.getQuantityKg(),
                    m.getTechnician() != null ? m.getTechnician().getUsername() : ""
                )
            ).collect(java.util.stream.Collectors.toList());
            dto.setMaterialUsages(materialDtos);
        } catch (Exception ex) {
            logger.warn("Cannot load materials for report {}: {}", reportId, ex.getMessage());
            dto.setMaterialUsages(java.util.Collections.emptyList());
        }

        // Sections: packaging logs (view)
        try {
            var logs = packagingLogRepository.findByReportIdIn(java.util.Collections.singletonList(reportId));
            List<PackagingLogViewDto> pkgDtos = logs.stream().map(p ->
                new PackagingLogViewDto(
                    p.getTimestamp(),
                    p.getLotNumber(),
                    p.getBoxNo(),
                    p.getOperator() != null ? p.getOperator().getUsername() : ""
                )
            ).collect(java.util.stream.Collectors.toList());
            dto.setPackagingLogs(pkgDtos);
        } catch (Exception ex) {
            logger.warn("Cannot load packaging logs for report {}: {}", reportId, ex.getMessage());
            dto.setPackagingLogs(java.util.Collections.emptyList());
        }

        // Sections: NG logs
        try {
            var ngs = ngLogRepository.findByReportId(reportId);
            List<NgLogSummaryDto> ngDtos = ngs.stream().map(l -> new NgLogSummaryDto(
                l.getTimestamp() != null ? l.getTimestamp().toString() : "",
                l.getNgType() != null ? resolveNgDescription(l.getNgType(), lang) : "",
                l.getQuantity(),
                l.getSource(),
                l.getUser() != null ? l.getUser().getUsername() : ""
            )).collect(java.util.stream.Collectors.toList());
            dto.setNgLogs(ngDtos);
        } catch (Exception ex) {
            logger.warn("Cannot load NG logs for report {}: {}", reportId, ex.getMessage());
            dto.setNgLogs(java.util.Collections.emptyList());
        }

        // Sections: downtime
        try {
            var dts = downtimeEventRepository.findByReportId(reportId);
            List<DowntimeEventSummaryDto> dtDtos = dts.stream().map(de -> {
                String reason = de.getReason();
                if (reason == null || reason.isBlank()) {
                    reason = "ไม่ระบุสาเหตุ";
                } else {
                    reason = reason.trim();
                }
                return new DowntimeEventSummaryDto(
                    de.getStartTime() != null ? de.getStartTime().toLocalTime().toString() : "",
                    de.getEndTime() != null ? de.getEndTime().toLocalTime().toString() : "",
                    (de.getStartTime() != null && de.getEndTime() != null)
                            ? (java.time.Duration.between(de.getStartTime(), de.getEndTime()).toMinutes() + " นาที")
                            : "In Progress",
                    reason,
                    de.getTechnician() != null ? de.getTechnician().getUsername() : "",
                    de.getSolution() != null ? de.getSolution() : ""
                );
            }).collect(java.util.stream.Collectors.toList());
            dto.setDowntimeEvents(dtDtos);
        } catch (Exception ex) {
            logger.warn("Cannot load downtime events for report {}: {}", reportId, ex.getMessage());
            dto.setDowntimeEvents(java.util.Collections.emptyList());
        }

        // Sections: scrap weight logs (Technician)
        try {
            var logs = scrapWeightLogRepository.findByReportId(reportId);
            List<ScrapWeightLogDto> scrapDtos = logs.stream().map(l ->
                new ScrapWeightLogDto(
                    l.getTimestamp(),
                    l.getWeightKg(),
                    l.getScrapType(),
                    l.getMatType(),
                    l.getTechnician() != null ? l.getTechnician().getUsername() : "",
                    "Technician",
                    true
                )
            ).collect(java.util.stream.Collectors.toList());
            dto.setScrapWeightLogs(scrapDtos);
        } catch (Exception ex) {
            logger.warn("Cannot load scrap weight logs for report {}: {}", reportId, ex.getMessage());
            dto.setScrapWeightLogs(java.util.Collections.emptyList());
        }

        // Optional placeholders
        DetailedProductionReportDto.QaSection qa = new DetailedProductionReportDto.QaSection();
        dto.setQaSection(qa);
        DetailedProductionReportDto.SignatureSection sig = new DetailedProductionReportDto.SignatureSection();
        dto.setSignatures(sig);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        logger.info("🔍 Getting available dates between {} and {} (by actual packaging logs only)", startDate, endDate);
        try {
            // Find reports that overlap the range (any machine/product)
            List<com.gdtahara.gdtaharabackend.model.ProductionReport> reports =
                productionReportRepository.findOverlappingReportsWithFilters(startDate, endDate, null, null, null);

            if (reports == null || reports.isEmpty()) {
                // No reports overlapping the requested range
                return java.util.Collections.emptyList();
            }

            // Collect report IDs
            java.util.List<Long> reportIds = reports.stream()
                .filter(java.util.Objects::nonNull)
                .map(com.gdtahara.gdtaharabackend.model.ProductionReport::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

            if (reportIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            // Query packaging logs within production-day windows covering the requested range: [start@03:00, end+1@03:00)
            java.time.LocalDateTime windowStart = startDate.atTime(3, 0);
            java.time.LocalDateTime windowEndExclusive = endDate.plusDays(1).atTime(3, 0);
            java.time.LocalDateTime queryEndInclusive = windowEndExclusive.minusNanos(1);

            List<com.gdtahara.gdtaharabackend.model.PackagingLog> packs =
                packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, windowStart, queryEndInclusive);

            java.util.Set<java.time.LocalDate> days = new java.util.HashSet<>();
            java.time.LocalTime cutover = java.time.LocalTime.of(3, 0);
            for (com.gdtahara.gdtaharabackend.model.PackagingLog p : packs) {
                if (p == null || p.getTimestamp() == null) continue;
                java.time.LocalDateTime ts = p.getTimestamp();
                java.time.LocalDate prodDate = ts.toLocalTime().isBefore(cutover)
                        ? ts.toLocalDate().minusDays(1)
                        : ts.toLocalDate();
                if (!prodDate.isBefore(startDate) && !prodDate.isAfter(endDate)) {
                    days.add(prodDate);
                }
            }

            List<String> availableDates = days.stream()
                .map(java.time.LocalDate::toString)
                .sorted(java.util.Collections.reverseOrder())
                .collect(java.util.stream.Collectors.toList());
            logger.info("📅 Found {} available dates (by packaging logs)", availableDates.size());
            return availableDates;
        } catch (Exception e) {
            logger.error("❌ Error getting available dates: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableDates(java.time.LocalDate startDate, java.time.LocalDate endDate, String machineId, Long productId, String orderNumber) {
        logger.info("🔍 Getting available dates between {} and {} with filters machineId='{}', productId='{}', orderNumber='{}' (by actual packaging logs only)",
                startDate, endDate, machineId, productId, orderNumber);
        try {
            String normalizedMachine = (machineId != null && !machineId.trim().isEmpty() && !"all".equalsIgnoreCase(machineId))
                    ? machineId.trim()
                    : null;
            // Support both machineName and numeric machine ID for filtering
            if (normalizedMachine != null) {
                try {
                    long maybeId = Long.parseLong(normalizedMachine);
                    var opt = machineRepository.findById(maybeId);
                    if (opt.isPresent() && opt.get().getMachineName() != null) {
                        String resolvedName = opt.get().getMachineName();
                        logger.info("🛠️ Resolved machineId '{}' to machineName '{}' for date filtering", normalizedMachine, resolvedName);
                        normalizedMachine = resolvedName;
                    }
                } catch (NumberFormatException ignore) {
                    // it's already a name
                } catch (Exception ex) {
                    logger.warn("⚠️ Failed to resolve machine by ID '{}': {}", normalizedMachine, ex.getMessage());
                }
            }
            

            // Use overlapping reports with filters first
            List<com.gdtahara.gdtaharabackend.model.ProductionReport> reports =
                productionReportRepository.findOverlappingReportsWithFilters(startDate, endDate, normalizedMachine, productId, (orderNumber != null && !orderNumber.isBlank()) ? orderNumber : null);

            // Fallback by orderNumber when no overlapping reports matched but order filter is given
            if ((reports == null || reports.isEmpty()) && orderNumber != null && !orderNumber.isBlank()) {
                String ord = orderNumber.trim();
                try {
                    java.util.Optional<com.gdtahara.gdtaharabackend.model.ProductionReport> optReport = productionReportRepository.findByOrderNumber(ord);
                    if (optReport.isPresent()) {
                        com.gdtahara.gdtaharabackend.model.ProductionReport pr = optReport.get();
                        // Respect machine/product filters if provided
                        boolean machineOk = (normalizedMachine == null) || (pr.getMachine() != null && normalizedMachine.equals(pr.getMachine().getMachineName()));
                        boolean productOk = (productId == null) || (pr.getProduct() != null && productId.equals(pr.getProduct().getId()));
                        if (machineOk && productOk) {
                            reports = java.util.List.of(pr);
                            logger.info("↩️ Fallback matched orderNumber='{}' to report ID={} despite no overlapping reports", ord, pr.getId());
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("⚠️ Fallback by orderNumber failed: {}", ex.getMessage());
                }
            }

            if (reports == null || reports.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            // Debug: log distinct report IDs and order numbers matched by filters
            java.util.Set<Long> dbgReportIds = reports.stream()
                .filter(java.util.Objects::nonNull)
                .map(com.gdtahara.gdtaharabackend.model.ProductionReport::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            java.util.Set<String> dbgOrderNumbers = reports.stream()
                .map(com.gdtahara.gdtaharabackend.model.ProductionReport::getOrderNumber)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            logger.info("📄 Filtered reports count={}, reportIds={}, orderNumbers={}",
                reports.size(), dbgReportIds, dbgOrderNumbers);

            java.util.List<Long> reportIds = reports.stream()
                .filter(java.util.Objects::nonNull)
                .map(com.gdtahara.gdtaharabackend.model.ProductionReport::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

            if (reportIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            // Compute window [start@03:00, end+1@03:00)
            java.time.LocalDateTime windowStart = startDate.atTime(3, 0);
            java.time.LocalDateTime windowEndExclusive = endDate.plusDays(1).atTime(3, 0);
            java.time.LocalDateTime queryEndInclusive = windowEndExclusive.minusNanos(1);

            List<com.gdtahara.gdtaharabackend.model.PackagingLog> packs =
                packagingLogRepository.findByReportIdInAndTimestampBetweenSafe(reportIds, windowStart, queryEndInclusive);

            // Extra guard: if orderNumber filter is provided, ensure only packaging logs from that order are considered
            if (orderNumber != null && !orderNumber.isBlank()) {
                final String ord = orderNumber.trim();
                packs = packs.stream()
                    .filter(p -> p != null && p.getReport() != null && ord.equals(p.getReport().getOrderNumber()))
                    .collect(java.util.stream.Collectors.toList());
            }

            // Debug: verify all packaging logs belong to the filtered reports/order
            java.util.Set<Long> packReportIds = packs.stream()
                .map(p -> p.getReport() != null ? p.getReport().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            java.util.Set<String> packOrderNumbers = packs.stream()
                .map(p -> p.getReport() != null ? p.getReport().getOrderNumber() : null)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            logger.info("📦 Packaging logs fetched: count={}, reportIds(from packs)={}, orderNumbers(from packs)={}",
                packs.size(), packReportIds, packOrderNumbers);

            java.util.Set<java.time.LocalDate> days = new java.util.HashSet<>();
            java.time.LocalTime cutover = java.time.LocalTime.of(3, 0);
            for (com.gdtahara.gdtaharabackend.model.PackagingLog p : packs) {
                if (p == null || p.getTimestamp() == null) continue;
                java.time.LocalDateTime ts = p.getTimestamp();
                java.time.LocalDate prodDate = ts.toLocalTime().isBefore(cutover)
                        ? ts.toLocalDate().minusDays(1)
                        : ts.toLocalDate();
                if (!prodDate.isBefore(startDate) && !prodDate.isAfter(endDate)) {
                    days.add(prodDate);
                }
            }

            List<String> availableDates = days.stream()
                    .map(java.time.LocalDate::toString)
                    .sorted(java.util.Collections.reverseOrder())
                    .collect(java.util.stream.Collectors.toList());
            logger.info("📅 Found {} available dates (filtered by packaging logs)", availableDates.size());
            return availableDates;
        } catch (Exception e) {
            logger.error("❌ Error getting available dates (filtered): {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}