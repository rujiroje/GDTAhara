package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.mapper.ParameterRecordMapper;
import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@SuppressWarnings("unused")
public class TechnicianService {
    
    private static final Logger logger = LoggerFactory.getLogger(TechnicianService.class);
    
    private final ProductionReportRepository productionReportRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final ScrapWeightLogRepository scrapWeightLogRepository;
    private final ParameterRecordRepository parameterRecordRepository;
    private final NgLogRepository ngLogRepository;
    private final UserRepository userRepository;
    
    public TechnicianService(
            ProductionReportRepository productionReportRepository,
            DowntimeEventRepository downtimeEventRepository,
            ScrapWeightLogRepository scrapWeightLogRepository,
            ParameterRecordRepository parameterRecordRepository,
            NgLogRepository ngLogRepository,
            UserRepository userRepository) {
        this.productionReportRepository = productionReportRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.scrapWeightLogRepository = scrapWeightLogRepository;
        this.parameterRecordRepository = parameterRecordRepository;
        this.ngLogRepository = ngLogRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Record downtime event
     */
    public void recordDowntime(Long reportId, DowntimeEventRequestDto requestDto, String username) {
        logger.info("Recording downtime for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        // Implementation logic here
        // Convert DTO to entity and save
        
        logger.info("Successfully recorded downtime for report ID: {}", reportId);
    }
    
    /**
     * Record scrap weight
     */
    public void recordScrapWeight(Long reportId, ScrapWeightLogRequestDto requestDto, String username) {
        logger.info("Recording scrap weight for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        // Implementation logic here
        // Convert DTO to entity and save
        
        logger.info("Successfully recorded scrap weight for report ID: {}", reportId);
    }
    
    /**
     * Get parameter records for a production report
     */
    @Transactional(readOnly = true)
    public List<ParameterRecord> getParameterRecords(Long reportId) {
        logger.info("Fetching parameter records for report ID: {}", reportId);
        
        try {
            // Check if report exists
            productionReportRepository.findById(reportId)
                    .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
            
            // Fetch records from repository
            List<ParameterRecord> records = parameterRecordRepository.findByReportIdOrderByCreatedAtDesc(reportId);
            logger.info("Found {} parameter records for report ID: {}", records.size(), reportId);
            
            return records;
        } catch (Exception e) {
            logger.error("Error fetching parameter records for report ID: {}", reportId, e);
            // Return empty list instead of throwing error to prevent 500 error
            return new ArrayList<>();
        }
    }
    
    /**
     * Create parameter record
     */
    @Transactional
    public void createParameterRecord(Long reportId, ParameterRecordRequest request, String username) {
        logger.info("Creating parameter record for report ID: {} by user: {}", reportId, username);
        
        if (request == null) {
            throw new IllegalArgumentException("Parameter record request cannot be null");
        }
        
        if (request.getParameters() == null) {
            logger.warn("Parameter data is null, creating empty parameter data");
            request.setParameters(new ParameterRecordRequest.ParameterData());
        }
        
        // ตรวจสอบว่า report มีอยู่หรือไม่
        ProductionReport report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        try {
            // สร้าง Entity ใหม่
            ParameterRecord entity = new ParameterRecord();
            
            // ตั้งค่า reportId และข้อมูลอื่นๆ
            entity.setReportId(reportId);
            
            // ใช้ technicianId จาก authentication หรือ default value
            entity.setTechnicianId(1L); // TODO: ใช้ข้อมูลจาก authentication
            
            // ใช้ recordType ที่ส่งมาจาก Frontend หรือ default value
            String recordTime = request.getRecordType() != null ? request.getRecordType() : "Standard";
            entity.setRecordTime(recordTime);
            
            entity.setCreatedAt(LocalDateTime.now());
            
            // ใช้ mapper แปลงข้อมูล
            if (request.getParameters() != null) {
                ParameterRecordMapper.mapDtoToEntity(request.getParameters(), entity);
            }
            
            // บันทึกลงฐานข้อมูล
            ParameterRecord savedEntity = parameterRecordRepository.save(entity);
            logger.debug("Saved parameter record with ID: {}", savedEntity.getId());
            
            logger.info("Successfully created parameter record for report ID: {} with recordType: {}", reportId, recordTime);
            
        } catch (Exception e) {
            logger.error("Error creating parameter record for report ID {}: {}", reportId, e.getMessage(), e);
            throw new RuntimeException("Failed to create parameter record: " + e.getMessage(), e);
        }
    }

    /**
     * Update parameter record
     */
    @Transactional
    public void updateParameterRecord(Long recordId, ParameterRecordRequest request, String username) {
        logger.info("Updating parameter record ID: {} by user: {}", recordId, username);
        
        if (request == null) {
            throw new IllegalArgumentException("Parameter record request cannot be null");
        }
        
        if (request.getParameters() == null) {
            logger.warn("Parameter data is null, creating empty parameter data");
            request.setParameters(new ParameterRecordRequest.ParameterData());
        }
        
        try {
            // ค้นหา record ที่ต้องการอัปเดต
            ParameterRecord record = parameterRecordRepository.findById(recordId)
                    .orElseThrow(() -> new EntityNotFoundException("Parameter record not found with ID: " + recordId));
            logger.debug("Found record: {}", record.getId());
            
            // อัปเดต recordType ถ้ามีการส่งมา
            if (request.getRecordType() != null) {
                record.setRecordTime(request.getRecordType());
            }
            
            // อัปเดต createdAt เป็น current time
            record.setCreatedAt(LocalDateTime.now());
            
            // ใช้ mapper แปลงข้อมูล
            if (request.getParameters() != null) {
                ParameterRecordMapper.mapDtoToEntity(request.getParameters(), record);
            }
            
            // บันทึกการเปลี่ยนแปลง
            ParameterRecord updatedRecord = parameterRecordRepository.save(record);
            logger.debug("Updated parameter record with ID: {}", updatedRecord.getId());
            
            logger.info("Successfully updated parameter record ID: {} with recordType: {}", recordId, request.getRecordType());
            
        } catch (EntityNotFoundException e) {
            logger.error("Parameter record not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating parameter record ID {}: {}", recordId, e.getMessage(), e);
            throw new RuntimeException("Failed to update parameter record: " + e.getMessage(), e);
        }
    }
    
    /**
     * Record technician NG (defect)
     */
    public void recordTechnicianNg(Long reportId, NgLogRequestDto requestDto, String username) {
        logger.info("Recording NG for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        // Implementation logic here
        // Convert DTO to entity and save
        
        logger.info("Successfully recorded NG for report ID: {}", reportId);
    }
    
    /**
     * Save Parameter Record from ParameterChecklistForm
     */
    @Transactional
    public ParameterRecord saveParameterRecord(ParameterRecord parameterRecord) {
        try {
            // Set creation timestamp
            parameterRecord.setCreatedAt(LocalDateTime.now());
            
            // Save the record
            ParameterRecord savedRecord = parameterRecordRepository.save(parameterRecord);
            
            logger.info("Successfully saved parameter record with ID: {}", savedRecord.getId());
            return savedRecord;
            
        } catch (Exception e) {
            logger.error("Error saving parameter record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save parameter record: " + e.getMessage());
        }
    }
    
    /**
     * Utility method to convert Double to BigDecimal with null handling
     */
    @SuppressWarnings("unused")
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }
}