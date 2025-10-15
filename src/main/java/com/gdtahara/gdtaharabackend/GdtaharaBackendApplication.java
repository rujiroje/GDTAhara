package com.gdtahara.gdtaharabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@RestController
public class GdtaharaBackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(GdtaharaBackendApplication.class);

    public static void main(String[] args) {
        logger.info("🚀 Starting GD Tahara Backend Application...");
        try {
            SpringApplication.run(GdtaharaBackendApplication.class, args);
            logger.info("✅ GD Tahara Backend Application started successfully!");
        } catch (Exception e) {
            logger.error("❌ Failed to start GD Tahara Backend Application: {}", e.getMessage(), e);
            
            // Additional error details for debugging
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
            
            // Check for specific repository issues
            if (e.getMessage().contains("ProductionReportRepository")) {
                logger.error("🔍 Repository Issue Detected:");
                logger.error("   - Check ProductionReport entity for missing fields");
                logger.error("   - Verify repository method names match entity properties");
                logger.error("   - Ensure database schema is up to date");
            }
            
            // Enhanced debugging for ProductionReport entity issues
            if (e.getMessage().contains("reportNumber")) {
                logger.error("🔥 URGENT FIX REQUIRED: ProductionReportRepository.findByReportNumber method issue");
                logger.error("   ❌ Current Problem: No 'reportNumber' field exists in ProductionReport entity");
                logger.error("   ");
                logger.error("   🔧 IMMEDIATE SOLUTIONS:");
                logger.error("   1️⃣  Remove/Comment out problematic method in ProductionReportRepository:");
                logger.error("       // Optional<ProductionReport> findByReportNumber(String reportNumber);");
                logger.error("   ");
                logger.error("   2️⃣  OR use existing field like 'orderNumber':");
                logger.error("       Optional<ProductionReport> findByOrderNumber(String orderNumber);");
                logger.error("   ");
                logger.error("   3️⃣  OR add reportNumber field to ProductionReport entity");
                logger.error("   ");
                logger.error("   📍 Files to check:");
                logger.error("       - ProductionReportRepository.java (remove findByReportNumber method)");
                logger.error("       - CmOperatorService.java (update method calls)");
            }
            
            if (e.getMessage().contains("cmOperatorService")) {
                logger.error("🔍 CmOperatorService dependency issue - check ProductionReportRepository methods");
            }
            
            // เพิ่มการตรวจสอบ SQL Server authentication issues
            if (e.getMessage().contains("Cannot open database") || e.getMessage().contains("Login failed")) {
                logger.error("🔐 SQL SERVER AUTHENTICATION ISSUE:");
                logger.error("   ❌ Cannot access database 'GDTaharaDB' with user 'sa'");
                logger.error("   ");
                logger.error("   🔧 Check SQL Server:");
                logger.error("       1. Database 'GDTaharaDB' exists on server 10.1.53.33");
                logger.error("       2. User 'sa' has correct password: tst123##");
                logger.error("       3. User 'sa' has permissions to access GDTaharaDB");
                logger.error("       4. SQL Server allows mixed authentication mode");
                logger.error("   ");
                logger.error("   💡 Quick fixes:");
                logger.error("       - Create database if not exists: CREATE DATABASE GDTaharaDB");
                logger.error("       - Reset sa password in SQL Server Management Studio");
                logger.error("       - Grant permissions: USE GDTaharaDB; EXEC sp_adduser 'sa'");
            }

            // เพิ่มการตรวจสอบ database connection issues
            if (e.getMessage().contains("TCP/IP connection") || e.getMessage().contains("Connection refused")) {
                logger.error("🔌 DATABASE CONNECTION ISSUE:");
                logger.error("   ❌ Cannot connect to SQL Server at specified host/port");
                logger.error("   🔧 Check:");
                logger.error("       - SQL Server is running");
                logger.error("       - Network connectivity to database server");
                logger.error("       - Database credentials in application.properties");
                logger.error("       - Firewall settings");
            }
            
            System.exit(1);
        }
    }

    @GetMapping("/health")
    public String healthCheck() {
        logger.debug("Health check endpoint accessed");
        return "GD Tahara Backend is running! 🎉";
    }

    @GetMapping("/")
    public String welcome() {
        return "Welcome to GD Tahara Backend API! Visit /test-api.html for API testing.";
    }

    @Bean
    public CommandLineRunner startupInfo() {
        return args -> {
            logger.info("=================================================");
            logger.info("🏭 GD Tahara Backend Application");
            logger.info("📋 Version: 1.0.0");
            logger.info("🌐 API Test Page: http://localhost:8080/test-api.html");
            logger.info("❤️  Health Check: http://localhost:8080/health");
            logger.info("=================================================");
            logger.info("🔧 FIXED COMPILATION ERRORS:");
            logger.info("   ✅ Removed duplicate methods in ProductionService");
            logger.info("   ✅ Fixed entity field access (getId() instead of getName())");
            logger.info("   ✅ Added convertToDto method");
            logger.info("   ✅ Fixed type mismatches");
            logger.info("=================================================");
            logger.info("✅ ENDPOINTS STATUS:");
            logger.info("   ✅ /api/pc/production/machines");
            logger.info("   ✅ /api/pc/production/products");
            logger.info("   ✅ /api/pc/production/reports");
            logger.info("   ✅ /api/pc/production/dashboard-summary");
            logger.info("   ✅ /api/pc/debug/service-test - For debugging");
            logger.info("=================================================");
            logger.info("🔍 TEST SERVICE:");
            logger.info("   Visit: http://localhost:8080/api/pc/debug/service-test");
            logger.info("   To see actual data from ProductionService");
            logger.info("=================================================");
        };
    }
}