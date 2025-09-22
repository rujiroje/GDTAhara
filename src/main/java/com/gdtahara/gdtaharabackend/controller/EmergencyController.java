package com.gdtahara.gdtaharabackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@ConditionalOnProperty(name = "app.mode", havingValue = "emergency")
public class EmergencyController {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyController.class);

    @GetMapping("/emergency/repository-fix-guide")
    public String repositoryFixGuide() {
        logger.info("Repository fix guide requested");
        return """
            🔧 Repository Fix Guide for ParameterRecord Issue
            
            ❌ Current Issue:
            No property 'reportId' found for type 'ParameterRecord'
            
            ✅ Solutions:
            
            1. OPTION 1: Fix Repository Method Name
               Change: findByReportIdOrderByTimestampDesc(Long reportId)
               To one of:
               - findByReport_IdOrderByTimestampDesc(Long reportId)
               - findByProductionReport_IdOrderByTimestampDesc(Long reportId)
               - findByReportIdOrderByCreatedAtDesc(Long reportId)
            
            2. OPTION 2: Use @Query Annotation
               @Query("SELECT p FROM ParameterRecord p WHERE p.report.id = :reportId ORDER BY p.timestamp DESC")
               List<ParameterRecord> findByReportIdOrderByTimestampDesc(@Param("reportId") Long reportId);
            
            3. OPTION 3: Use Native Query
               @Query(value = "SELECT * FROM parameter_records WHERE report_id = :reportId ORDER BY timestamp DESC", nativeQuery = true)
               List<ParameterRecord> findByReportIdOrderByTimestampDesc(@Param("reportId") Long reportId);
            
            📋 Steps to Fix:
            1. Stop the application
            2. Edit ParameterRecordRepository.java
            3. Apply one of the solutions above
            4. Restart without --emergency flag
            
            💡 Need to check entity field names first?
            Visit: /emergency/entity-info
            """;
    }

    @GetMapping("/emergency/entity-info")
    public String entityInfo() {
        return """
            📊 Entity Information Guide
            
            🔍 To check ParameterRecord entity field names:
            
            1. Open: src/main/java/com/gdtahara/gdtaharabackend/model/ParameterRecord.java
            
            2. Look for field names like:
               - reportId (if exists)
               - report (with @ManyToOne annotation)
               - productionReport
               - productionReportId
               - timestamp (or createdAt, recordedAt)
            
            3. Common patterns:
               Entity field: report → Repository: findByReport_Id
               Entity field: productionReport → Repository: findByProductionReport_Id
               Entity field: reportId → Repository: findByReportId
            
            📝 Example Entity Structure:
            @Entity
            public class ParameterRecord {
                @ManyToOne
                @JoinColumn(name = "report_id")
                private ProductionReport report;  // Use: findByReport_Id
                
                private LocalDateTime timestamp;   // Use: OrderByTimestamp
                // OR
                private LocalDateTime createdAt;   // Use: OrderByCreatedAt
            }
            
            🎯 Match your repository method name with actual entity field names!
            """;
    }

    @GetMapping("/emergency/quick-commands")
    public String quickCommands() {
        return """
            ⚡ Quick Fix Commands
            
            🔧 Emergency Mode (Current):
            mvn spring-boot:run -Dspring-boot.run.arguments=--emergency
            
            🔧 Normal Mode (After fixing repository):
            mvn spring-boot:run
            
            🔧 Debug Mode (To see more details):
            mvn spring-boot:run -Dspring-boot.run.arguments=--debug
            
            🔧 Clean Build:
            mvn clean compile
            
            🔧 Reset Database (if needed):
            mvn spring-boot:run -Dspring.jpa.hibernate.ddl-auto=create-drop
            
            📋 Recommended Fix Sequence:
            1. Check entity field names: /emergency/entity-info
            2. Fix repository method name: /emergency/repository-fix-guide
            3. Test fix: mvn clean compile
            4. Start normally: mvn spring-boot:run
            """;
    }
}
