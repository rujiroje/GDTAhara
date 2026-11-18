// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/QaService.java
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")

@Service
public class QaService {

    private static final Logger logger = LoggerFactory.getLogger(QaService.class);

    @Autowired private ProductionReportRepository reportRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NgLogRepository ngLogRepository;
    @Autowired private AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ProductionReportSimpleViewDto> getActiveReportsForQa() {
        try {
            // Only return reports whose date range spans TODAY
            LocalDate today = LocalDate.now();
            var terminalStatuses = java.util.List.of(
                    "COMPLETED","COMPLETE","DONE","FINISHED","CANCELLED","CANCELED","CLOSED","FINALIZED","INACTIVE");
            List<ProductionReport> active = reportRepository.findActiveReportsFast(
                    java.util.List.of(), terminalStatuses, today);
            return active.stream().map(this::convertToSimpleDto).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching active reports for QA: {}", e.getMessage(), e);
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
    public List<NgType> getQaNgTypes() {
        List<NgType> allNgTypes = ngTypeRepository.findAll();
        logger.debug("Total NG Types in database: {}", allNgTypes.size());

        List<NgType> qaNgTypes = allNgTypes.stream()
            .filter(ng -> ng.getNgType() != null && ng.getNgType().trim().equalsIgnoreCase("QA"))
            .collect(Collectors.toList());

        if (qaNgTypes.isEmpty()) {
            logger.debug("No specific QA NG Types found, returning all NG Types");
            return allNgTypes;
        }

        logger.debug("Final QA NG Types count: {}", qaNgTypes.size());
        return qaNgTypes;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQaHistoryForReport(Long reportId) {
        logger.debug("Getting QA history for report {}", reportId);

        List<NgLog> qaLogs = ngLogRepository.findByReportId(reportId)
                .stream()
                .filter(log -> "QA_Process".equals(log.getSource()))
                .collect(Collectors.toList());

        logger.debug("Found {} QA logs for report {}", qaLogs.size(), reportId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return qaLogs.stream()
                .map(log -> {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("id", log.getId());
                    historyItem.put("timestamp", log.getTimestamp().format(formatter));
                    historyItem.put("qaUser", log.getUser().getUsername());
                    historyItem.put("ngType", log.getNgType().getNgDescriptionTh());
                    historyItem.put("quantity", log.getQuantity());
                    historyItem.put("canEdit", true);
                    return historyItem;
                })
                .sorted((a, b) -> b.get("timestamp").toString().compareTo(a.get("timestamp").toString()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateQaNgLog(Long ngLogId, Integer newQuantity, String username) {
        NgLog ngLog = ngLogRepository.findById(ngLogId)
                .orElseThrow(() -> new RuntimeException("NG Log not found"));

        if (!ngLog.getUser().getUsername().equals(username)) {
            throw new SecurityException("คุณสามารถแก้ไขได้เฉพาะรายการที่ตัวเองบันทึกเท่านั้น");
        }

        if (!"QA_Process".equals(ngLog.getSource())) {
            throw new SecurityException("สามารถแก้ไขได้เฉพาะรายการ QA เท่านั้น");
        }

        Integer oldQty = ngLog.getQuantity();
        ngLog.setQuantity(newQuantity);
        ngLogRepository.save(ngLog);
        logger.info("QA NG log {} updated successfully", ngLogId);
        auditLogService.log("UPDATE", "NgLog", ngLogId,
                Map.of("id", ngLogId, "quantity", String.valueOf(oldQty)),
                Map.of("id", ngLogId, "quantity", String.valueOf(newQuantity), "source", "QA_Process"));
    }

    @Transactional
    public void recordQaNg(Long reportId, NgLogRequestDto ngLogRequest, String username) {
        ProductionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Production Report not found"));
        NgType ngType = ngTypeRepository.findById(ngLogRequest.getNgTypeId())
                .orElseThrow(() -> new RuntimeException("NG Type not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        NgLog ngLog = new NgLog();
        ngLog.setReport(report);
        ngLog.setNgType(ngType);
        ngLog.setUser(user);
        ngLog.setQuantity(ngLogRequest.getQuantity());
        ngLog.setSource("QA_Process");

        NgLog saved = ngLogRepository.save(ngLog);
        logger.info("QA NG log recorded: {} x{}", ngType.getNgDescriptionTh(), ngLogRequest.getQuantity());
        auditLogService.log("CREATE", "NgLog", saved.getId(), null,
                Map.of("id", saved.getId(), "reportId", String.valueOf(reportId),
                        "ngTypeId", String.valueOf(ngLogRequest.getNgTypeId()),
                        "quantity", String.valueOf(ngLogRequest.getQuantity()), "source", "QA_Process"));
    }
}
