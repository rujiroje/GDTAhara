package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.ProductionReportDto;
import com.gdtahara.gdtaharabackend.dto.ReportCreateRequest;
import com.gdtahara.gdtaharabackend.dto.ReportSummaryDto;
import com.gdtahara.gdtaharabackend.service.ProductionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

// Dev/sample legacy controller only. Disabled by default to avoid route conflicts with PcCompatibilityController
@Profile("dev-sample")
@PreAuthorize("hasAnyRole('Production Control', 'DataAdmin')")
@RestController
@RequestMapping("/api/dev/pc")
public class ProductionControlController {

    private static final Logger logger = LoggerFactory.getLogger(ProductionControlController.class);

    @Autowired
    private ProductionService productionService;

    // เพิ่ม endpoints ที่ Frontend ต้องการ
    @GetMapping("/production/machines")
    public ResponseEntity<?> getMachines() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting machines list for user: {}", username);
            
            var machines = productionService.getAllMachines();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", machines);
            response.put("status", "success");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting machines: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลเครื่องจักร: " + e.getMessage()));
        }
    }

    @GetMapping("/production/products")
    public ResponseEntity<?> getProducts() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting products list for user: {}", username);
            
            var products = productionService.getAllProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", products);
            response.put("status", "success");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลผลิตภัณฑ์: " + e.getMessage()));
        }
    }



    @GetMapping("/production/dashboard-summary")
    public ResponseEntity<?> getDashboardSummary() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting dashboard summary for user: {}", username);
            
            var summary = productionService.getDashboardSummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", summary);
            response.put("status", "success");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลสรุป: " + e.getMessage()));
        }
    }

    // เก็บทั้ง 2 endpoints สำหรับ reports (เก่าและใหม่)
    @GetMapping("/reports")
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        return searchReports(machineId, productId, startDate, endDate, page, size);
    }

    // เพิ่ม POST endpoint สำหรับการสร้างใบสั่งผลิตใหม่
    @PostMapping("/reports")
    public ResponseEntity<?> createProductionReport(@RequestBody ReportCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Creating production report for user: {}", username);
            logger.info("Request data: orderNumber={}, machineId={}, productId={}, targetQty={}", 
                request.getOrderNumber(), request.getMachineId(), request.getProductId(), request.getTargetQty());
            
            ProductionReportDto createdReport = productionService.createProductionReport(request, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", createdReport);
            response.put("status", "success");
            response.put("message", "สร้างใบสั่งผลิตสำเร็จ");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating production report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการสร้างใบสั่งผลิต: " + e.getMessage()));
        }
    }

    // เพิ่ม PUT endpoint สำหรับการแก้ไขใบสั่งผลิต
    @PutMapping("/reports/{id}")
    public ResponseEntity<?> updateProductionReport(@PathVariable Long id, @RequestBody ReportCreateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Updating production report ID: {} for user: {}", id, username);
            
            ProductionReportDto updatedReport = productionService.updateProductionReport(id, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", updatedReport);
            response.put("status", "success");
            response.put("message", "แก้ไขใบสั่งผลิตสำเร็จ");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating production report ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการแก้ไขใบสั่งผลิต: " + e.getMessage()));
        }
    }

    // เพิ่ม POST endpoint สำหรับการปิดงานใบสั่งผลิต
    @PostMapping("/reports/{id}/finalize")
    public ResponseEntity<?> finalizeProductionReport(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Finalizing production report ID: {} for user: {}", id, username);
            
            productionService.finalizeReport(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "ปิดงานใบสั่งผลิตสำเร็จ");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error finalizing production report ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการปิดงาน: " + e.getMessage()));
        }
    }

    // เพิ่ม DELETE endpoint สำหรับการลบใบสั่งผลิต
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<?> deleteProductionReport(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Deleting production report ID: {} for user: {}", id, username);
            
            productionService.deleteProductionReport(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "ลบใบสั่งผลิตสำเร็จ");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting production report ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการลบใบสั่งผลิต: " + e.getMessage()));
        }
    }

    @GetMapping("/production/reports")
    public ResponseEntity<?> getProductionReports(
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            // ตรวจสอบว่ามีการส่งพารามิเตอร์การค้นหาหรือไม่
            boolean hasSearchCriteria = (machineId != null && !machineId.trim().isEmpty()) ||
                    (productId != null && !productId.trim().isEmpty()) ||
                    (startDate != null && !startDate.trim().isEmpty()) ||
                    (endDate != null && !endDate.trim().isEmpty());

            if (!hasSearchCriteria) {
                // ถ้าไม่มีพารามิเตอร์ค้นหา ให้ส่งข้อมูลทั้งหมด
                logger.info("Getting all production reports for user: {}", username);
                
                var reports = productionService.getAllProductionReports();
                
                Map<String, Object> response = new HashMap<>();
                response.put("data", reports);
                response.put("status", "success");
                response.put("timestamp", java.time.LocalDateTime.now());
                
                return ResponseEntity.ok(response);
            } else {
                // ถ้ามีพารามิเตอร์ค้นหา ให้เรียกใช้ searchReports
                logger.info("Searching reports with criteria - User: {}, MachineId: {}, ProductId: {}, DateRange: {} to {}",
                        username, machineId, productId, startDate, endDate);
                
                return searchReports(machineId, productId, startDate, endDate, page, size);
            }
            
        } catch (Exception e) {
            logger.error("Error getting production reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลรายการผลิต: " + e.getMessage()));
        }
    }

    // เพิ่ม endpoint สำหรับ summary ของแต่ละ report
    @GetMapping("/test-simple")
    public ResponseEntity<?> testSimple() {
        try {
            logger.info("Test simple endpoint called");
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Simple test endpoint working");
            response.put("timestamp", LocalDateTime.now().toString());
            logger.info("Test simple endpoint response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in test simple endpoint: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/test-without-service")
    public ResponseEntity<?> testWithoutService() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Working without service dependency");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-db")
    public ResponseEntity<?> testDatabase() {
        try {
            long reportCount = productionService.getAllProductionReports().size();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Database connection working");
            response.put("reportCount", reportCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Database error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/production/reports/{id}/summary")
    public ResponseEntity<?> getProductionReportSummary(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting production report summary for ID: {} by user: {}", id, username);
            
            ReportSummaryDto summary = productionService.getReportSummary(id);
            // Return plain DTO to match frontend expectation (it calls setSummary(response.data))
            // Axios response.data should be the DTO directly
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error getting production report summary for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลสรุป: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/search")
    public ResponseEntity<?> searchReports(
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Searching reports - User: {}, MachineId: {}, ProductId: {}, DateRange: {} to {}",
                    username, machineId, productId, startDate, endDate);

            // ตรวจสอบว่ามีการส่งพารามิเตอร์การค้นหาหรือไม่
            boolean hasSearchCriteria = (machineId != null && !machineId.trim().isEmpty()) ||
                    (productId != null && !productId.trim().isEmpty()) ||
                    (startDate != null && !startDate.trim().isEmpty()) ||
                    (endDate != null && !endDate.trim().isEmpty());

            if (!hasSearchCriteria) {
                // ส่งข้อมูลสำหรับแสดง search form
                Map<String, Object> response = new HashMap<>();
                response.put("searchRequired", true);
                response.put("message", "กรุณาเลือกเงื่อนไขการค้นหา");
                response.put("searchOptions", Map.of(
                    "machines", productionService.getAllMachines(),
                    "products", productionService.getAllProducts(),
                    "defaultDateRange", Map.of(
                        "startDate", LocalDate.now().minusDays(30).toString(),
                        "endDate", LocalDate.now().toString()
                    )
                ));

                return ResponseEntity.ok(response);
            }

            // ดึงข้อมูลและกรอง
            List<ProductionReportDto> allReports = productionService.getAllProductionReports();
            
            // กรองข้อมูลตามเงื่อนไข
            List<ProductionReportDto> filteredReports = allReports.stream()
                .filter(report -> {
                    boolean matches = true;
                    
                    if (machineId != null && !machineId.trim().isEmpty()) {
                        matches &= machineId.equals(String.valueOf(report.getId())); 
                    }
                    
                    if (productId != null && !productId.trim().isEmpty()) {
                        matches &= productId.equals(String.valueOf(report.getId())); 
                    }
                    
                    return matches;
                })
                .collect(Collectors.toList());
            
            // Pagination
            int totalElements = filteredReports.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            
            List<ProductionReportDto> paginatedReports = fromIndex < totalElements 
                ? filteredReports.subList(fromIndex, toIndex)
                : new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedReports);
            response.put("pagination", Map.of(
                "currentPage", page,
                "totalPages", (int) Math.ceil((double) totalElements / size),
                "totalElements", totalElements,
                "size", paginatedReports.size()
            ));
            response.put("searchCriteria", Map.of(
                "machineId", machineId != null ? machineId : "",
                "productId", productId != null ? productId : "",
                "startDate", startDate != null ? startDate : "",
                "endDate", endDate != null ? endDate : ""
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการค้นหา: " + e.getMessage()));
        }
    }

    // **[ใหม่]** เพิ่ม endpoint สำหรับ CM Operator ดึง active reports
    @GetMapping("/reports/active")
    public ResponseEntity<?> getActiveReports() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting active reports for user: {}", username);
            
            // ดึงรายการ reports ที่มีสถานะ IN_PROGRESS หรือ ACTIVE
            List<ProductionReportDto> allReports = productionService.getAllProductionReports();
            List<ProductionReportDto> activeReports = allReports.stream()
                .filter(report -> "IN_PROGRESS".equals(report.getStatus()) || 
                                 "ACTIVE".equals(report.getStatus()) ||
                                 "In Progress".equals(report.getStatus()) ||
                                 "Active".equals(report.getStatus()))
                .collect(Collectors.toList());
            
            // ถ้าไม่มี active reports ให้ส่ง reports ล่าสุด 5 รายการ
            if (activeReports.isEmpty()) {
                logger.info("No active reports found, returning recent reports");
                activeReports = allReports.stream()
                    .sorted((r1, r2) -> r2.getStartDate().compareTo(r1.getStartDate())) // เรียงตามวันที่ล่าสุด
                    .limit(5)
                    .collect(Collectors.toList());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", activeReports);
            response.put("status", "success");
            response.put("message", activeReports.isEmpty() ? "ไม่มีใบสั่งผลิตที่กำลังทำงาน" : "พบใบสั่งผลิตที่กำลังทำงาน " + activeReports.size() + " รายการ");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Active reports returned: {} items", activeReports.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting active reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลใบสั่งผลิตที่กำลังทำงาน: " + e.getMessage()));
        }
    }

    // เพิ่ม search options endpoint สำหรับ dropdown lists
    @GetMapping("/search-options")
    public ResponseEntity<?> getSearchOptions() {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("machines", productionService.getAllMachines());
            options.put("products", productionService.getAllProducts());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", options);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting search options: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "เกิดข้อผิดพลาดในการดึงข้อมูลตัวเลือก: " + e.getMessage()));
        }
    }

    // เพิ่ม endpoint สำหรับรายงานย้อนหลัง
    @GetMapping("/production/reports/production-historical")
    public ResponseEntity<?> getHistoricalReports(
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            logger.info("Getting historical reports - User: {}, MachineId: {}, ProductId: {}, DateRange: {} to {}",
                    username, machineId, productId, startDate, endDate);

            // ตรวจสอบว่ามีการส่งพารามิเตอร์การค้นหาหรือไม่
            boolean hasSearchCriteria = (startDate != null && !startDate.trim().isEmpty()) ||
                    (endDate != null && !endDate.trim().isEmpty()) ||
                    (machineId != null && !machineId.trim().isEmpty()) ||
                    (productId != null && !productId.trim().isEmpty());

            if (!hasSearchCriteria) {
                // ถ้าไม่มีเงื่อนไขค้นหา ให้แสดงข้อความ
                Map<String, Object> response = new HashMap<>();
                response.put("data", new ArrayList<>());
                response.put("message", "กรุณาระบุเงื่อนไขการค้นหา");
                response.put("status", "success");
                response.put("totalElements", 0);
                return ResponseEntity.ok(response);
            }

            // ดึงข้อมูลทั้งหมดจาก service
            List<ProductionReportDto> allReports = productionService.getAllProductionReports();
            
            // กรองข้อมูลตามเงื่อนไข
            List<ProductionReportDto> filteredReports = allReports.stream()
                .filter(report -> {
                    boolean matches = true;
                    
                    // กรองตาม machineId (ถ้ามี)
                    if (machineId != null && !machineId.trim().isEmpty() && !"all".equals(machineId)) {
                        // ในกรณีนี้ เปรียบเทียบกับ machine name หรือ id
                        matches &= report.getMachineName().contains(machineId) ||
                                  String.valueOf(report.getId()).equals(machineId);
                    }
                    
                    // กรองตาม productId (ถ้ามี)
                    if (productId != null && !productId.trim().isEmpty() && !"all".equals(productId)) {
                        // ในกรณีนี้ เปรียบเทียบกับ product name หรือ id
                        matches &= report.getProductName().contains(productId) ||
                                  String.valueOf(report.getId()).equals(productId);
                    }
                    
                    // กรองตามวันที่ (ถ้ามี) - ในอนาคตจะต้องเพิ่มการตรวจสอบวันที่จริง
                    // ตอนนี้คืนข้อมูลทั้งหมดที่ผ่านเงื่อนไขอื่นๆ
                    
                    return matches;
                })
                .collect(Collectors.toList());
            
            // Pagination
            int totalElements = filteredReports.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            
            List<ProductionReportDto> paginatedReports = fromIndex < totalElements 
                ? filteredReports.subList(fromIndex, toIndex)
                : new ArrayList<>();
            
            // สร้าง response
            Map<String, Object> response = new HashMap<>();
            response.put("data", paginatedReports);
            response.put("status", "success");
            response.put("totalElements", totalElements);
            response.put("currentPage", page);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("searchCriteria", Map.of(
                "machineId", machineId != null ? machineId : "",
                "productId", productId != null ? productId : "",
                "startDate", startDate != null ? startDate : "",
                "endDate", endDate != null ? endDate : ""
            ));
            response.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("Historical reports retrieved: {} total, {} returned", totalElements, paginatedReports.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting historical reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "เกิดข้อผิดพลาดในการดึงข้อมูลรายงานย้อนหลัง: " + e.getMessage(),
                        "status", "error",
                        "timestamp", java.time.LocalDateTime.now()
                    ));
        }
    }

    // Test endpoint
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Production Control API is working!");
        response.put("user", username);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        logger.info("Test endpoint accessed by user: {}", username);
        return ResponseEntity.ok(response);
    }

    // เพิ่ม debug endpoint
    @GetMapping("/debug/service-test")
    public ResponseEntity<?> debugServiceTest() {
        try {
            logger.info("🔍 Testing ProductionService methods directly...");
            
            Map<String, Object> debugInfo = new HashMap<>();
            
            // Test getAllMachines
            var machines = productionService.getAllMachines();
            debugInfo.put("machines_count", machines.size());
            debugInfo.put("machines_sample", machines.isEmpty() ? "No data" : 
                machines.stream().limit(3).collect(java.util.stream.Collectors.toList()));
            
            // Test getAllProducts  
            var products = productionService.getAllProducts();
            debugInfo.put("products_count", products.size());
            debugInfo.put("products_sample", products.isEmpty() ? "No data" :
                products.stream().limit(3).collect(java.util.stream.Collectors.toList()));
            
            // Test getAllProductionReports
            var reports = productionService.getAllProductionReports();
            debugInfo.put("reports_count", reports.size());
            debugInfo.put("reports_sample", reports.isEmpty() ? "No data" :
                reports.stream().limit(3).collect(java.util.stream.Collectors.toList()));
            
            // Test getDashboardSummary
            var summary = productionService.getDashboardSummary();
            debugInfo.put("dashboard_summary", summary);
            
            debugInfo.put("status", "debug_complete");
            debugInfo.put("timestamp", java.time.LocalDateTime.now());
            
            logger.info("✅ Service debug test completed");
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            logger.error("❌ Error in service debug test: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "timestamp", java.time.LocalDateTime.now()));
        }
    }
}