// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/OperatorService.java
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.PackagingLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProblemAlertRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")

@Service
@Transactional
public class OperatorService {

    private static final Logger logger = LoggerFactory.getLogger(OperatorService.class);

    private final ProductionReportRepository productionReportRepository;
    private final NgLogRepository ngLogRepository;
    private final LabelStockRepository labelStockRepository;
    private final PackagingLogRepository packagingLogRepository;
    private final ProblemAlertRepository problemAlertRepository;
    private final UserRepository userRepository;
    private final NgTypeRepository ngTypeRepository;
    private final AuditLogService auditLogService;

    public OperatorService(ProductionReportRepository productionReportRepository, NgLogRepository ngLogRepository, LabelStockRepository labelStockRepository, PackagingLogRepository packagingLogRepository, ProblemAlertRepository problemAlertRepository, UserRepository userRepository, NgTypeRepository ngTypeRepository, AuditLogService auditLogService) {
        this.productionReportRepository = productionReportRepository;
        this.ngLogRepository = ngLogRepository;
        this.labelStockRepository = labelStockRepository;
        this.packagingLogRepository = packagingLogRepository;
        this.problemAlertRepository = problemAlertRepository;
        this.userRepository = userRepository;
        this.ngTypeRepository = ngTypeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<NgType> getOperatorNgTypes() {
        List<NgType> allNgTypes = ngTypeRepository.findAll();
        logger.debug("Total NG Types for Operator: {}", allNgTypes.size());
        return allNgTypes;
    }

    @Transactional(readOnly = true)
    public List<ProductionReportSimpleViewDto> getActiveReportsForOperator() {
        try {
            // Only return reports whose date range spans TODAY — never show future or expired orders
            LocalDate today = LocalDate.now();
            var terminalStatuses = java.util.List.of(
                    "COMPLETED","COMPLETE","DONE","FINISHED","CANCELLED","CANCELED","CLOSED","FINALIZED");
            List<ProductionReport> inProgressReports = productionReportRepository.findActiveReportsFast(
                    java.util.List.of(), terminalStatuses, today);

            return inProgressReports.stream()
                    .map(this::convertToSimpleDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching active reports for operator: {}", e.getMessage(), e);
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

    @Transactional(readOnly = true)
    public Map<String, Long> getHourlyNgSummary(Long reportId, String username) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfHour = now.truncatedTo(ChronoUnit.HOURS);

        User operator = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        List<NgLog> hourlyLogs = ngLogRepository.findByReportIdAndTimestampBetween(reportId, startOfHour, now)
                .stream()
                .filter(log -> log.getUser().getId().equals(operator.getId()))
                .collect(Collectors.toList());

        logger.debug("Hourly NG Summary for {}: {} logs", username, hourlyLogs.size());

        return hourlyLogs.stream()
                .collect(Collectors.groupingBy(
                    log -> log.getNgType().getNgDescriptionTh(),
                    Collectors.summingLong(NgLog::getQuantity)
                ));
    }

    public NgLog recordNgLog(Long reportId, NgLogRequestDto request, String username) {
        ProductionReport report = productionReportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Production Report not found"));
        User operator = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        NgType ngType = ngTypeRepository.findById(request.getNgTypeId()).orElseThrow(() -> new EntityNotFoundException("NG Type not found"));

        NgLog ngLog = new NgLog();
        ngLog.setReport(report);
        ngLog.setUser(operator);
        ngLog.setNgType(ngType);
        ngLog.setQuantity(request.getQuantity());
        ngLog.setSource(request.getSource());
        NgLog saved = ngLogRepository.save(ngLog);
        auditLogService.log("CREATE", "NgLog", saved.getId(), null,
                Map.of("id", saved.getId(), "reportId", String.valueOf(reportId),
                        "ngTypeId", String.valueOf(request.getNgTypeId()),
                        "quantity", String.valueOf(request.getQuantity()),
                        "source", String.valueOf(request.getSource())));
        return saved;
    }

    public PackagingLog recordPackaging(Long reportId, PackagingLogRequestDto request, String username) {
        ProductionReport report = productionReportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Production Report not found"));
        User operator = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        LabelStock stock = labelStockRepository.findByProductId(report.getProduct().getId()).orElseThrow(() -> new EntityNotFoundException("ไม่พบสต็อกป้ายสำหรับผลิตภัณฑ์นี้"));
        if (stock.getCurrentStock() <= 0) {
            throw new IllegalStateException("ป้ายหมดสต็อก ไม่สามารถบันทึกได้");
        }
        stock.setCurrentStock(stock.getCurrentStock() - 1);
        labelStockRepository.save(stock);

        PackagingLog packagingLog = new PackagingLog();
        packagingLog.setReport(report);
        packagingLog.setOperator(operator);
        packagingLog.setLotNumber(request.getLotNumber());
        packagingLog.setBoxNo(request.getBoxNo());
        PackagingLog savedPkg = packagingLogRepository.save(packagingLog);
        auditLogService.log("CREATE", "PackagingLog", savedPkg.getId(), null,
                Map.of("id", savedPkg.getId(), "reportId", String.valueOf(reportId),
                        "lotNumber", String.valueOf(request.getLotNumber()),
                        "boxNo", String.valueOf(request.getBoxNo())));
        return savedPkg;
    }

    public void createProblemAlert(Long reportId, ProblemAlertRequestDto request, String username) {
        ProductionReport report = productionReportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Production Report not found"));
        User operator = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        ProblemAlert alert = new ProblemAlert();
        alert.setReport(report);
        alert.setOperator(operator);
        alert.setMessage(request.getMessage());
        ProblemAlert savedAlert = problemAlertRepository.save(alert);
        auditLogService.log("CREATE", "ProblemAlert", savedAlert.getId(), null,
                Map.of("id", savedAlert.getId(), "reportId", String.valueOf(reportId),
                        "message", String.valueOf(request.getMessage())));
    }

    public int getNextBoxNumber(Long reportId, String lotNumber) {
        PackagingLog lastLog = packagingLogRepository.findTopByReportIdAndLotNumberOrderByBoxNoDesc(reportId, lotNumber);
        return (lastLog != null) ? lastLog.getBoxNo() + 1 : 1;
    }
}
