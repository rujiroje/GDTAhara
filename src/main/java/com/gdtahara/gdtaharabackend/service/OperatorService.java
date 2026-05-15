// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/OperatorService.java
// (ฉบับแก้ไข เพิ่มเมธอด getHourlyNgSummary)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.PackagingLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProblemAlertRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OperatorService {

    private final ProductionReportRepository productionReportRepository;
    private final NgLogRepository ngLogRepository;
    private final LabelStockRepository labelStockRepository;
    private final PackagingLogRepository packagingLogRepository;
    private final ProblemAlertRepository problemAlertRepository;
    private final UserRepository userRepository;
    private final NgTypeRepository ngTypeRepository;

    public OperatorService(ProductionReportRepository productionReportRepository, NgLogRepository ngLogRepository, LabelStockRepository labelStockRepository, PackagingLogRepository packagingLogRepository, ProblemAlertRepository problemAlertRepository, UserRepository userRepository, NgTypeRepository ngTypeRepository) {
        this.productionReportRepository = productionReportRepository;
        this.ngLogRepository = ngLogRepository;
        this.labelStockRepository = labelStockRepository;
        this.packagingLogRepository = packagingLogRepository;
        this.problemAlertRepository = problemAlertRepository;
        this.userRepository = userRepository;
        this.ngTypeRepository = ngTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<NgType> getOperatorNgTypes() {
        System.out.println("🔍 Getting ALL NG Types for Operator buttons...");
        
        // แก้ไข: ส่ง NG Types ทั้งหมดให้ Operator สำหรับปุ่มบันทึก
        List<NgType> allNgTypes = ngTypeRepository.findAll();
        System.out.println("📊 Total NG Types for Operator: " + allNgTypes.size());
        
        return allNgTypes;
    }

    // **[ใหม่]** เมธอดสำหรับดึงรายการ active reports สำหรับ Operator
    @Transactional(readOnly = true)
    public List<ProductionReportSimpleViewDto> getActiveReportsForOperator() {
        try {
            // หา reports ที่มีสถานะที่ถือว่าใช้งานได้ (In Progress/Active โดยนโยบายใหม่)
            List<ProductionReport> inProgressReports = productionReportRepository.findByStatusIn(
                java.util.List.of("IN_PROGRESS", "In Progress", "ACTIVE")
            );
            
            if (inProgressReports.isEmpty()) {
                // ถ้าไม่มี ให้หา reports ทั้งหมดที่สร้างวันนี้
                LocalDate today = LocalDate.now();
                List<ProductionReport> todayReports = productionReportRepository.findByStartDate(today);
        if (todayReports.isEmpty()) {
            // ถ้ายังไม่มี ให้ใช้ native latest 5 rows (raw) แล้ว map
            return productionReportRepository.findLatest5Raw().stream()
                .map(r -> {
                    java.time.LocalDate startDate = toLocalDate(r[2]);
                    java.time.LocalDate endDate = toLocalDate(r[3]);
                    return new ProductionReportSimpleViewDto(
                        (Long) r[0],
                        r[1] != null ? r[1].toString() : null,
                        startDate,
                        endDate,
                        (String) r[5],
                        (String) r[6],
                        null
                    );
                })
                .collect(Collectors.toList());
        } else {
                    return todayReports.stream()
                            .map(this::convertToSimpleDto)
                            .collect(Collectors.toList());
                }
            } else {
                return inProgressReports.stream()
                        .map(this::convertToSimpleDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching active reports for operator: " + e.getMessage());
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
    
    // เพิ่ม method สำหรับ debug
    @Transactional(readOnly = true)
    public List<NgType> getAllNgTypesForDebug() {
        return ngTypeRepository.findAll();
    }

    // **[แก้ไข]** เมธอดสำหรับดึงข้อมูลสรุป NG รายชั่วโมงของ Operator คนนั้นเอง
    @Transactional(readOnly = true)
    public Map<String, Long> getHourlyNgSummary(Long reportId, String username) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfHour = now.truncatedTo(ChronoUnit.HOURS);

        // **[ใหม่]** Filter เฉพาะ NG ที่ Operator คนนั้นเองบันทึก
        User operator = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        List<NgLog> hourlyLogs = ngLogRepository.findByReportIdAndTimestampBetween(reportId, startOfHour, now)
                .stream()
                .filter(log -> log.getUser().getId().equals(operator.getId())) // Filter เฉพาะ Operator คนนั้น
                .collect(Collectors.toList());

        System.out.println("🕐 Hourly NG Summary for " + username + ": " + hourlyLogs.size() + " logs");

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
        return ngLogRepository.save(ngLog);
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
        return packagingLogRepository.save(packagingLog);
    }

    public void createProblemAlert(Long reportId, ProblemAlertRequestDto request, String username) {
        ProductionReport report = productionReportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Production Report not found"));
        User operator = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        ProblemAlert alert = new ProblemAlert();
        alert.setReport(report);
        alert.setOperator(operator);
        alert.setMessage(request.getMessage());
        problemAlertRepository.save(alert);
    }

    public int getNextBoxNumber(Long reportId, String lotNumber) {
        PackagingLog lastLog = packagingLogRepository.findTopByReportIdAndLotNumberOrderByBoxNoDesc(reportId, lotNumber);
        return (lastLog != null) ? lastLog.getBoxNo() + 1 : 1;
    }
}