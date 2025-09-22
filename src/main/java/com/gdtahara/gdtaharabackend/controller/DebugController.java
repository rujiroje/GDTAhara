package com.gdtahara.gdtaharabackend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.PackagingLogRepository;
import com.gdtahara.gdtaharabackend.repository.NgLogRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.service.ProductionService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private PackagingLogRepository packagingLogRepository;
    
    @Autowired
    private NgLogRepository ngLogRepository;
    
    @Autowired
    private ProductionReportRepository productionReportRepository;
    
    @Autowired
    private ProductionService productionService;

    @GetMapping("/validate-token")
    public String validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        return "Token received: " + authorizationHeader;
    }
    
    /**
     * 🔍 Debug endpoint เพื่อตรวจสอบข้อมูลใน packaging_logs และ ng_logs
     */
    @GetMapping("/data-check")
    public ResponseEntity<Map<String, Object>> checkData() {
        Map<String, Object> result = new HashMap<>();
        
        // ตรวจสอบข้อมูลรายงานทั้งหมด
        var reports = productionReportRepository.findAll();
        result.put("totalReports", reports.size());
        
        // ตรวจสอบข้อมูลใน packaging_logs
        long totalPackagingLogs = packagingLogRepository.count();
        result.put("totalPackagingLogs", totalPackagingLogs);
        
        // ตรวจสอบข้อมูลใน ng_logs
        long totalNgLogs = ngLogRepository.count();
        result.put("totalNgLogs", totalNgLogs);
        
        // ตรวจสอบข้อมูลแต่ละรายงาน
        Map<String, Object> reportDetails = new HashMap<>();
        for (var report : reports) {
            Map<String, Object> details = new HashMap<>();
            details.put("reportId", report.getId());
            details.put("machineName", report.getMachine() != null ? report.getMachine().getMachineName() : "N/A");
            details.put("productName", report.getProduct() != null ? report.getProduct().getProductName() : "N/A");
            details.put("targetQty", report.getTargetQty());
            details.put("status", report.getStatus());
            
            // นับข้อมูลจริงจาก repositories
            long goodQty = packagingLogRepository.countByReportId(report.getId());
            long ngQty = ngLogRepository.countByReportId(report.getId());
            
            details.put("goodQty", goodQty);
            details.put("ngQty", ngQty);
            details.put("totalProduced", goodQty + ngQty);
            
            // คำนวณ Yield
            if (goodQty + ngQty > 0) {
                double yield = ((double) goodQty / (goodQty + ngQty)) * 100;
                details.put("yield", String.format("%.2f%%", yield));
            } else {
                details.put("yield", "0.00%");
            }
            
            reportDetails.put("report_" + report.getId(), details);
        }
        
        result.put("reportDetails", reportDetails);
        result.put("message", "🔍 Data check completed successfully");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🎯 Debug endpoint เพื่อทดสอบ getReportSummary method
     */
    @GetMapping("/report-summary/{id}")
    public ResponseEntity<Object> debugReportSummary(@PathVariable Long id) {
        try {
            var summary = productionService.getReportSummary(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", summary,
                "message", "✅ Report summary retrieved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "❌ Error retrieving report summary"
            ));
        }
    }
    
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("authorities", auth.getAuthorities());
            }
            response.put("principalType", principal.getClass().getSimpleName());
        }
        
        return response;
    }
}
