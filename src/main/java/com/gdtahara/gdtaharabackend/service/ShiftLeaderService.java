package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShiftLeaderService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ShiftLeaderService.class);

    private final ProductionReportRepository productionReportRepository;
    private final PackagingLogRepository packagingLogRepository;
    private final NgLogRepository ngLogRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockTransactionRepository transactionRepository;
    private final NgTypeRepository ngTypeRepository;
    private final UserRepository userRepository;
    private final MaterialUsageLogRepository materialUsageLogRepository;
    private final ScrapWeightLogRepository scrapWeightLogRepository;
    private final ProductRepository productRepository;
    private final LabelStockRepository labelStockRepository;
    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;

    public ShiftLeaderService(ProductionReportRepository productionReportRepository, PackagingLogRepository packagingLogRepository, NgLogRepository ngLogRepository, DowntimeEventRepository downtimeEventRepository, MaterialRepository materialRepository, MaterialStockTransactionRepository transactionRepository, NgTypeRepository ngTypeRepository, UserRepository userRepository, MaterialUsageLogRepository materialUsageLogRepository, ScrapWeightLogRepository scrapWeightLogRepository, ProductRepository productRepository, LabelStockRepository labelStockRepository, MachineRepository machineRepository, AuditLogService auditLogService) {
        this.productionReportRepository = productionReportRepository;
        this.packagingLogRepository = packagingLogRepository;
        this.ngLogRepository = ngLogRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.materialRepository = materialRepository;
        this.transactionRepository = transactionRepository;
        this.ngTypeRepository = ngTypeRepository;
        this.userRepository = userRepository;
        this.materialUsageLogRepository = materialUsageLogRepository;
        this.scrapWeightLogRepository = scrapWeightLogRepository;
        this.productRepository = productRepository;
        this.labelStockRepository = labelStockRepository;
        this.machineRepository = machineRepository;
        this.auditLogService = auditLogService;
    }

    public List<ProductionReportSimpleViewDto> getActiveReportsForShiftLeader() {
        LocalDate today = LocalDate.now();
        try {
            var activeStatuses = java.util.List.of("IN PROGRESS", "IN_PROGRESS", "ACTIVE");
            var terminalStatuses = java.util.List.of("COMPLETED", "CANCELLED", "CANCELED", "FINISHED", "CLOSED");
            List<ProductionReport> active = productionReportRepository.findActiveReportsFast(activeStatuses, terminalStatuses, today);
            logger.info("[SL] ActiveReportsFast today={} -> {} item(s)", today, (active != null ? active.size() : 0));

            if (active != null && !active.isEmpty()) {
                return active.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
            }

            List<ProductionReport> todayReports = productionReportRepository.findByStartDate(today);
            logger.info("[SL] TodayReports startDate={} -> {} item(s)", today, (todayReports != null ? todayReports.size() : 0));
            if (todayReports != null && !todayReports.isEmpty()) {
                return todayReports.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
            }

            List<ProductionReport> dateActive = productionReportRepository.findActiveOnDate(today, null);
            logger.info("[SL] DateActive overlap today={} -> {} item(s)", today, (dateActive != null ? dateActive.size() : 0));
            if (dateActive != null && !dateActive.isEmpty()) {
                return dateActive.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
            }

            return productionReportRepository.findLatest5Raw().stream()
                .map(r -> new ProductionReportSimpleViewDto(
                        (Long) r[0],
                        r[1] != null ? r[1].toString() : null,
                        toLocalDate(r[2]),
                        toLocalDate(r[3]),
                        (String) r[5],
                        (String) r[6],
                        null
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("[SL] Error fetching active reports: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ProductionReportSimpleViewDto convertToSimpleDto(ProductionReport report) {
        return new ProductionReportSimpleViewDto(
                report.getId(),
                report.getOrderNumber(),
                report.getStartDate(),
                report.getEndDate(),
                report.getMachine() != null ? report.getMachine().getMachineName() : "Unknown Machine",
                report.getProduct() != null ? report.getProduct().getProductName() : "Unknown Product",
                report.getMachine() != null ? String.valueOf(report.getMachine().getId()) : null
        );
    }

    private java.time.LocalDate toLocalDate(Object obj) {
        if (obj instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (obj instanceof java.time.LocalDate ld) return ld;
        return null;
    }

    public ShiftLeaderDashboardDto getDashboardData(Long reportId) {
        ProductionReport report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + reportId));

        LocalDate today = LocalDate.now();

        LocalDateTime dayShiftStart = today.atTime(3, 0);
        LocalDateTime dayShiftEnd = today.atTime(15, 0);
        LocalDateTime nightShiftStart = today.atTime(15, 0);
        LocalDateTime nightShiftEnd = today.plusDays(1).atTime(3, 0);

        try {
            ShiftDataDto dayShiftData = calculateShiftData(report, dayShiftStart, dayShiftEnd);
            ShiftDataDto nightShiftData = calculateShiftData(report, nightShiftStart, nightShiftEnd);

            return new ShiftLeaderDashboardDto(
                (report.getMachine() != null && report.getMachine().getMachineName() != null) ? report.getMachine().getMachineName() : "Unknown Machine",
                (report.getProduct() != null && report.getProduct().getProductName() != null) ? report.getProduct().getProductName() : "Unknown Product",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dayShiftData,
                nightShiftData
            );
        } catch (Exception e) {
            ShiftDataDto emptyShift = createEmptyShiftData();
            return new ShiftLeaderDashboardDto(
                (report.getMachine() != null && report.getMachine().getMachineName() != null) ? report.getMachine().getMachineName() : "Unknown Machine",
                (report.getProduct() != null && report.getProduct().getProductName() != null) ? report.getProduct().getProductName() : "Unknown Product",
                today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                emptyShift,
                emptyShift
            );
        }
    }

    @Transactional(readOnly = true)
    public List<NgLog> getNgLogsForReport(Long reportId) {
        return ngLogRepository.findByReportIdAndSource(reportId, "ShiftLeader");
    }

    @Transactional
    public MaterialStockTransaction recordStockTransaction(StockTransactionRequestDto request, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Material material = materialRepository.findById(request.getMaterialId()).orElseThrow(() -> new EntityNotFoundException("Material not found"));

        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setMaterial(material);
        transaction.setUser(user);
        transaction.setTransactionType(request.getTransactionType());
        transaction.setQuantity(request.getQuantity());
        transaction.setLotNumber(request.getLotNumber());

        if (request.getProductionReportId() != null) {
            ProductionReport report = productionReportRepository.findById(request.getProductionReportId()).orElse(null);
            transaction.setProductionReport(report);
        }

        MaterialStockTransaction saved = transactionRepository.save(transaction);
        auditLogService.log("CREATE", "MaterialStockTransaction", saved.getId(), null,
                java.util.Map.of("materialId", String.valueOf(request.getMaterialId()),
                        "transactionType", String.valueOf(request.getTransactionType()),
                        "quantity", String.valueOf(request.getQuantity()),
                        "createdBy", username));
        return saved;
    }

    @Transactional
    public MaterialStockTransaction updateStockTransaction(Long transactionId, StockTransactionRequestDto request, String username) {
        MaterialStockTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DataAdmin"));
        if (!isAdmin) {
            boolean isOwner = transaction.getUser() != null
                    && username.equals(transaction.getUser().getUsername());
            if (!isOwner) {
                throw new AccessDeniedException("Access denied: you do not own this transaction");
            }
        }

        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new EntityNotFoundException("Material not found"));

        BigDecimal oldQty = transaction.getQuantity();
        String oldLot = transaction.getLotNumber();

        transaction.setMaterial(material);
        transaction.setQuantity(request.getQuantity());
        transaction.setLotNumber(request.getLotNumber());

        MaterialStockTransaction saved = transactionRepository.save(transaction);
        auditLogService.log("UPDATE", "MaterialStockTransaction", transactionId,
                java.util.Map.of("id", transactionId, "quantity", String.valueOf(oldQty), "lotNumber", String.valueOf(oldLot)),
                java.util.Map.of("id", transactionId, "quantity", String.valueOf(request.getQuantity()),
                        "lotNumber", String.valueOf(request.getLotNumber())));
        return saved;
    }

    @Transactional
    public NgLog recordNgLog(Long reportId, NgLogRequestDto request, String username) {
        ProductionReport report = productionReportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Report not found"));
        NgType ngType = ngTypeRepository.findById(request.getNgTypeId()).orElseThrow(() -> new EntityNotFoundException("NG Type not found"));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));

        NgLog ngLog = new NgLog();
        ngLog.setReport(report);
        ngLog.setNgType(ngType);
        ngLog.setUser(user);
        ngLog.setQuantity(request.getQuantity());
        ngLog.setSource("ShiftLeader");

        NgLog savedLog = ngLogRepository.save(ngLog);
        auditLogService.log("CREATE", "NgLog", savedLog.getId(), null,
                java.util.Map.of("id", savedLog.getId(), "reportId", String.valueOf(reportId),
                        "ngTypeId", String.valueOf(request.getNgTypeId()),
                        "quantity", String.valueOf(request.getQuantity()), "source", "ShiftLeader"));
        return savedLog;
    }

    private ShiftDataDto calculateShiftData(ProductionReport report, LocalDateTime startTime, LocalDateTime endTime) {
        if (report == null) {
            return createEmptyShiftData();
        }
        long reportId = report.getId();

        try {
            LocalDateTime endInclusive = endTime != null ? endTime.minusNanos(1) : startTime;
            List<PackagingLog> packagingLogs = packagingLogRepository.findByReportIdAndTimestampBetween(reportId, startTime, endInclusive);
            List<NgLog> ngLogs = ngLogRepository.findByReportIdAndTimestampBetween(reportId, startTime, endInclusive);

            List<DowntimeEventSummaryDto> downtimeHistory = Collections.emptyList();
            try {
                List<DowntimeEvent> events = downtimeEventRepository.findByReportIdAndStartTimeBetween(reportId, startTime, endInclusive);
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                downtimeHistory = events.stream().map(event -> {
                    String startTimeStr = event.getStartTime() != null ? event.getStartTime().format(timeFmt) : "";
                    String endTimeStr = event.getEndTime() != null ? event.getEndTime().format(timeFmt) : "กำลังดำเนินการ";
                    String durationStr;
                    if (event.getEndTime() != null && event.getStartTime() != null) {
                        long minutes = Duration.between(event.getStartTime(), event.getEndTime()).toMinutes();
                        durationStr = minutes + " นาที";
                    } else {
                        durationStr = "กำลังดำเนินการ";
                    }
                    String reason = event.getReason() != null ? event.getReason() : "ไม่ระบุ";
                    String technician = (event.getTechnician() != null && event.getTechnician().getUsername() != null)
                            ? event.getTechnician().getUsername() : "ไม่ระบุ";
                    return new DowntimeEventSummaryDto(startTimeStr, endTimeStr, durationStr, reason, technician, event.getSolution() != null ? event.getSolution() : "");
                }).collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("Cannot fetch downtime events: {}", e.getMessage());
            }

            List<MaterialUsageLog> materialUsageLogsRaw = materialUsageLogRepository.findByReportIdAndTimestampBetween(reportId, startTime, endTime);
            List<ScrapWeightLog> scrapWeightLogs = scrapWeightLogRepository.findByReportIdInAndTimestampBetween(Collections.singletonList(reportId), startTime, endInclusive);

            long goodBoxes = packagingLogs.size();
            Integer qtyPerBox = (report.getProduct() != null) ? report.getProduct().getQtyPerBox() : null;
            long goodPieces = (qtyPerBox != null && qtyPerBox > 0) ? goodBoxes * qtyPerBox : 0;
            long ngPieces = ngLogs.stream().mapToLong(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();
            long totalPieces = goodPieces + ngPieces;
            String yield = (totalPieces > 0) ? String.format("%.2f%%", ((double) goodPieces / totalPieces) * 100) : "0.00%";
            BigDecimal totalScrapWeight = scrapWeightLogs.stream()
                .map(log -> log.getWeightKg() != null ? log.getWeightKg() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            logger.debug("[SL] calcShiftData: boxes={}, qtyPerBox={}, goodPieces={}, ngPieces={}, totalScrapKg={}", goodBoxes, qtyPerBox, goodPieces, ngPieces, totalScrapWeight);

            Map<String, Long> ngSummaryMap = ngLogs.stream()
                .collect(Collectors.groupingBy(
                    log -> (log.getNgType() != null && log.getNgType().getNgDescriptionTh() != null) ? log.getNgType().getNgDescriptionTh() : "ไม่ระบุ",
                    Collectors.summingLong(l -> l.getQuantity() != null ? l.getQuantity() : 0)
                ));

            List<NgTypeSummaryDto> ngSummary = ngSummaryMap.entrySet().stream()
                .map(entry -> new NgTypeSummaryDto(entry.getKey(), entry.getValue(), 0.0, entry.getValue()))
                .collect(Collectors.toList());

            List<MaterialUsageLogDto> materialUsageLogs = materialUsageLogsRaw.stream().map(log ->
                new MaterialUsageLogDto(
                    log.getTimestamp(),
                    log.getMaterialCode(),
                    log.getLotNumber(),
                    log.getQuantityKg(),
                    (log.getTechnician() != null ? log.getTechnician().getUsername() : "ไม่ระบุ")
                )
            ).collect(Collectors.toList());

            ShiftDataDto shiftData = new ShiftDataDto();
            shiftData.setGoodProductionBoxes(goodBoxes);
            shiftData.setGoodProductionPieces(goodPieces);
            shiftData.setNgProductionPieces(ngPieces);
            shiftData.setTotalProductionPieces(totalPieces);
            shiftData.setYieldPercentage(yield);
            shiftData.setNgSummary(ngSummary);
            shiftData.setDowntimeHistory(downtimeHistory);
            shiftData.setMaterialUsageLogs(materialUsageLogs);
            shiftData.setDebugInfo("");
            shiftData.setDebugPackagingLogCount(0L);
            shiftData.setTotalScrapWeight(totalScrapWeight);
            return shiftData;
        } catch (Exception e) {
            logger.error("Error calculating shift data: {}", e.getMessage(), e);
            return createEmptyShiftData();
        }
    }

    public List<LabelStockDto> getLabelStocks() {
        return productRepository.findAll().stream().map(product -> {
            LabelStock stock = labelStockRepository.findByProductId(product.getId()).orElse(null);
            Long currentStock = stock != null ? stock.getCurrentStock().longValue() : 0L;
            return new LabelStockDto(product.getId(), product.getProductCode(), product.getProductName(), currentStock);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addLabelStock(Long productId, Integer quantityToAdd) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        LabelStock stock = labelStockRepository.findByProductId(productId).orElse(null);
        if (stock == null) {
            stock = new LabelStock();
            stock.setProduct(product);
            stock.setCurrentStock(0);
        }

        stock.setCurrentStock(stock.getCurrentStock() + quantityToAdd);
        labelStockRepository.save(stock);
    }

    public List<MaterialStockCardDto> getMaterialStocks() {
        return materialRepository.findAll().stream().map(material -> {
            List<MaterialStockTransactionDto> history = transactionRepository.findByMaterialIdOrderByTimestampDesc(material.getId())
                    .stream()
                    .map(this::convertToTransactionDto)
                    .collect(Collectors.toList());

            Double currentStock = calculateCurrentStock(material.getId());

            return new MaterialStockCardDto(
                material.getId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                currentStock,
                history
            );
        }).collect(Collectors.toList());
    }

    private MaterialStockTransactionDto convertToTransactionDto(MaterialStockTransaction transaction) {
        String productionInfo = transaction.getProductionReport() != null
            ? transaction.getProductionReport().getProduct().getProductName() + " - " + transaction.getProductionReport().getMachine().getMachineName()
            : "";

        return new MaterialStockTransactionDto(
            transaction.getId(),
            transaction.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            transaction.getTransactionType(),
            transaction.getQuantity(),
            transaction.getLotNumber(),
            productionInfo,
            transaction.getUser().getUsername()
        );
    }

    private Double calculateCurrentStock(Long materialId) {
        List<MaterialStockTransaction> transactions = transactionRepository.findByMaterialIdOrderByTimestampDesc(materialId);
        BigDecimal stock = BigDecimal.ZERO;
        for (MaterialStockTransaction transaction : transactions) {
            if ("IN".equals(transaction.getTransactionType())) {
                stock = stock.add(transaction.getQuantity());
            } else if ("OUT".equals(transaction.getTransactionType())) {
                stock = stock.subtract(transaction.getQuantity());
            }
        }
        return stock.doubleValue();
    }

    private ShiftDataDto createEmptyShiftData() {
        ShiftDataDto emptyShift = new ShiftDataDto();
        emptyShift.setGoodProductionBoxes(0L);
        emptyShift.setGoodProductionPieces(0L);
        emptyShift.setNgProductionPieces(0L);
        emptyShift.setTotalProductionPieces(0L);
        emptyShift.setYieldPercentage("0.00%");
        emptyShift.setNgSummary(Collections.emptyList());
        emptyShift.setDowntimeHistory(Collections.emptyList());
        emptyShift.setMaterialUsageLogs(Collections.emptyList());
        emptyShift.setDebugInfo("");
        emptyShift.setDebugPackagingLogCount(0L);
        emptyShift.setTotalScrapWeight(BigDecimal.ZERO);
        return emptyShift;
    }

    @Transactional
    public List<String> getNgTypes() {
        try {
            return Arrays.asList(
                "Dirty", "Scratch", "Dent", "Color Defect",
                "Size Error", "Shape Error", "Other"
            );
        } catch (Exception e) {
            logger.error("Error getting NG types: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<String> getMachines() {
        try {
            List<Machine> machines = machineRepository.findAll();
            if (machines.isEmpty()) {
                return Arrays.asList("Machine 1", "Machine 2", "Machine 3");
            }
            return machines.stream()
                    .map(Machine::getMachineName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting machines: {}", e.getMessage(), e);
            return Arrays.asList("Machine 1", "Machine 2", "Machine 3");
        }
    }

    public String createSampleDataForTesting() {
        try {
            createSampleDataForTestingInternal();
            return "Sample data created successfully with test machines, products, and production reports.";
        } catch (Exception e) {
            logger.error("Error creating sample data: {}", e.getMessage(), e);
            return "Error creating sample data: " + e.getMessage();
        }
    }

    private void createSampleDataForTestingInternal() {
        try {
            createSampleNgTypes();
            createSampleProductionReport();
        } catch (Exception e) {
            logger.warn("Could not create sample data: {}", e.getMessage());
        }
    }

    private void createSampleNgTypes() {
        String[] ngDescriptions = {
            "บิ่น/แตก (Crack)",
            "สีไม่เท่า (Color variation)",
            "ขนาดไม่ได้มาตรฐาน (Size defect)",
            "รอยย่น (Wrinkle)",
            "ข้อผิดพลาดการประกอบ (Assembly error)"
        };

        for (String description : ngDescriptions) {
            NgType ngType = new NgType();
            ngType.setNgDescriptionTh(description);
            ngType.setNgType("Shift Leader");
            ngTypeRepository.save(ngType);
        }
    }

    private void createSampleProductionReport() {
        if (productRepository.count() > 0 && machineRepository.count() > 0) {
            Product product = productRepository.findAll().get(0);
            Machine machine = machineRepository.findAll().get(0);

            ProductionReport report = new ProductionReport();
            report.setProduct(product);
            report.setMachine(machine);
            report.setStatus("In Progress");
            report.setStartDate(LocalDate.now());
            report.setEndDate(LocalDate.now().plusDays(1));
            report.setTargetQty(1000);

            productionReportRepository.save(report);
        }
    }
}
