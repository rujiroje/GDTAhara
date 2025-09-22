// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/QaService.java
// (**แก้ไข** เพิ่ม methods ครบถ้วนสำหรับ QA)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ProductionReportSimpleViewDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
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

@Service
public class QaService {
    @Autowired private ProductionReportRepository reportRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NgLogRepository ngLogRepository;

    // **[ใหม่]** เมธอดสำหรับดึงรายการ active reports สำหรับ QA
    @Transactional(readOnly = true)
    public List<ProductionReportSimpleViewDto> getActiveReportsForQa() {
        try {
            // หา reports ที่มีสถานะ "In Progress" ก่อน
            List<ProductionReport> inProgressReports = reportRepository.findByStatus("In Progress");
            
            if (inProgressReports.isEmpty()) {
                // ถ้าไม่มี ให้หา reports ทั้งหมดที่สร้างวันนี้
                LocalDate today = LocalDate.now();
                List<ProductionReport> todayReports = reportRepository.findByStartDate(today);
                if (todayReports.isEmpty()) {
                    // ถ้ายังไม่มี ให้หา reports ล่าสุด 5 รายการ
                    List<ProductionReport> recentReports = reportRepository.findTop5ByOrderByCreatedAtDesc();
                    return recentReports.stream()
                            .map(this::convertToSimpleDto)
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
            System.out.println("Error fetching active reports for QA: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private ProductionReportSimpleViewDto convertToSimpleDto(ProductionReport report) {
        return new ProductionReportSimpleViewDto(
                report.getId(),
                report.getStartDate(),
                report.getMachine() != null ? report.getMachine().getMachineName() : "Unknown Machine",
                report.getProduct() != null ? report.getProduct().getProductName() : "Unknown Product"
        );
    }

    // **[ใหม่]** เมธอดสำหรับดึง NG Types สำหรับ QA
    @Transactional(readOnly = true)
    public List<NgType> getQaNgTypes() {
        System.out.println("🔍 Getting NG Types for QA...");
        
        // ดึง NG Types ทั้งหมด แล้ว filter เฉพาะของ QA
        List<NgType> allNgTypes = ngTypeRepository.findAll();
        System.out.println("📊 Total NG Types in database: " + allNgTypes.size());
        
        List<NgType> qaNgTypes = allNgTypes.stream()
            .filter(ng -> {
                // Filter ด้วย ngType field ถ้ามี
                if (ng.getNgType() != null && ng.getNgType().trim().equalsIgnoreCase("QA")) {
                    System.out.println("✅ Found QA NG Type: " + ng.getNgDescriptionTh());
                    return true;
                }
                return false;
            })
            .collect(Collectors.toList());
        
        // ถ้าไม่มี QA NG Types ให้ส่งทั้งหมดแทน
        if (qaNgTypes.isEmpty()) {
            System.out.println("⚠️ No specific QA NG Types found, returning all NG Types");
            return allNgTypes;
        }
        
        System.out.println("🎯 Final QA NG Types count: " + qaNgTypes.size());
        qaNgTypes.forEach(ng -> 
            System.out.println("  • " + ng.getNgCode() + ": " + ng.getNgDescriptionTh())
        );
        
        return qaNgTypes;
    }

    // **[ใหม่]** เมธอดสำหรับดึงประวัติการตรวจสอบคุณภาพในใบสั่งผลิต
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQaHistoryForReport(Long reportId) {
        System.out.println("🔍 Getting QA history for report " + reportId);
        
        // ดึง NG Logs ที่มี source เป็น "QA_Process"
        List<NgLog> qaLogs = ngLogRepository.findByReportId(reportId)
                .stream()
                .filter(log -> "QA_Process".equals(log.getSource()))
                .collect(Collectors.toList());
        
        System.out.println("📊 Found " + qaLogs.size() + " QA logs for report " + reportId);
        
        // แปลงเป็น Map เพื่อส่งข้อมูลที่ต้องการ
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        return qaLogs.stream()
                .map(log -> {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("id", log.getId());
                    historyItem.put("timestamp", log.getTimestamp().format(formatter));
                    historyItem.put("qaUser", log.getUser().getUsername());
                    historyItem.put("ngType", log.getNgType().getNgDescriptionTh());
                    historyItem.put("quantity", log.getQuantity());
                    historyItem.put("canEdit", true); // QA สามารถแก้ไขได้ทั้งหมด (จะ filter ใน frontend)
                    return historyItem;
                })
                .sorted((a, b) -> b.get("timestamp").toString().compareTo(a.get("timestamp").toString())) // เรียงจากใหม่ไปเก่า
                .collect(Collectors.toList());
    }

    // **[ใหม่]** แก้ไข NG Log (เฉพาะเจ้าของ)
    @Transactional
    public void updateQaNgLog(Long ngLogId, Integer newQuantity, String username) {
        NgLog ngLog = ngLogRepository.findById(ngLogId)
                .orElseThrow(() -> new RuntimeException("NG Log not found"));
        
        // ตรวจสอบว่าเป็นเจ้าของ log นี้หรือไม่
        if (!ngLog.getUser().getUsername().equals(username)) {
            throw new SecurityException("คุณสามารถแก้ไขได้เฉพาะรายการที่ตัวเองบันทึกเท่านั้น");
        }
        
        // ตรวจสอบว่าเป็น QA source หรือไม่
        if (!"QA_Process".equals(ngLog.getSource())) {
            throw new SecurityException("สามารถแก้ไขได้เฉพาะรายการ QA เท่านั้น");
        }
        
        System.out.println("🔄 Updating NG Log ID " + ngLogId + " from " + ngLog.getQuantity() + " to " + newQuantity);
        
        ngLog.setQuantity(newQuantity);
        ngLogRepository.save(ngLog);
        
        System.out.println("✅ QA NG log updated successfully");
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
        ngLog.setSource("QA_Process"); // ระบุ Source เป็น QA
        
        ngLogRepository.save(ngLog);
        System.out.println("✅ QA NG log recorded: " + ngType.getNgDescriptionTh() + " x" + ngLogRequest.getQuantity());
    }
}