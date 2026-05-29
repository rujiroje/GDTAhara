package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.mapper.ParameterRecordMapper;
import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final NgTypeRepository ngTypeRepository;
    private final AuditLogService auditLogService;

    public TechnicianService(
            ProductionReportRepository productionReportRepository,
            DowntimeEventRepository downtimeEventRepository,
            ScrapWeightLogRepository scrapWeightLogRepository,
            ParameterRecordRepository parameterRecordRepository,
            NgLogRepository ngLogRepository,
            UserRepository userRepository,
            NgTypeRepository ngTypeRepository,
            AuditLogService auditLogService) {
        this.productionReportRepository = productionReportRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.scrapWeightLogRepository = scrapWeightLogRepository;
        this.parameterRecordRepository = parameterRecordRepository;
        this.ngLogRepository = ngLogRepository;
        this.userRepository = userRepository;
        this.ngTypeRepository = ngTypeRepository;
        this.auditLogService = auditLogService;
    }
    
    /**
     * Record downtime event
     */
    @Transactional
    public void recordDowntime(Long reportId, DowntimeEventRequestDto requestDto, String username) {
        logger.info("Recording downtime for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        if (requestDto == null) {
            throw new IllegalArgumentException("Downtime request cannot be null");
        }

        // Validate times
        if (requestDto.getStartTime() == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        if (requestDto.getEndTime() != null && requestDto.getEndTime().isBefore(requestDto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Resolve technician user
        var technician = userRepository.findByUsername(username).orElseGet(() -> {
            // fallback: first Technician, else any user
            try {
                var techs = userRepository.findByRole("Technician");
                if (techs != null && !techs.isEmpty()) return techs.get(0);
                var admins = userRepository.findByRole("DataAdmin");
                if (admins != null && !admins.isEmpty()) return admins.get(0);
            } catch (Exception ignored) {}
            throw new EntityNotFoundException("User not found: " + username);
        });

        try {
            var entity = new com.gdtahara.gdtaharabackend.model.DowntimeEvent();
            entity.setReport(report);
            entity.setTechnician(technician);
            entity.setStartTime(requestDto.getStartTime());
            entity.setEndTime(requestDto.getEndTime());
            entity.setReason(requestDto.getReason());
            // solution is @Transient – safe to set if provided
            entity.setSolution(requestDto.getSolution());

            var saved = downtimeEventRepository.save(entity);
            auditLogService.log("CREATE", "DowntimeEvent", saved.getId(), null,
                    java.util.Map.of("reportId", String.valueOf(reportId),
                            "startTime", String.valueOf(requestDto.getStartTime()),
                            "createdBy", username));
        } catch (Exception e) {
            logger.error("Failed to record downtime for report {}: {}", reportId, e.getMessage(), e);
            throw e;
        }

        logger.info("Successfully recorded downtime for report ID: {}", reportId);
    }
    
    /**
     * Record scrap weight
     */
    @Transactional
    public void recordScrapWeight(Long reportId, ScrapWeightLogRequestDto requestDto, String username) {
        logger.info("Recording scrap weight for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        if (requestDto == null) {
            throw new IllegalArgumentException("Scrap weight request cannot be null");
        }
        if (requestDto.getWeightKg() == null) {
            throw new IllegalArgumentException("Scrap weight (kg) is required");
        }
        if (requestDto.getWeightKg().signum() < 0) {
            throw new IllegalArgumentException("Scrap weight (kg) must be non-negative");
        }

        var technician = userRepository.findByUsername(username).orElseGet(() -> {
            try {
                var techs = userRepository.findByRole("Technician");
                if (techs != null && !techs.isEmpty()) return techs.get(0);
                var admins = userRepository.findByRole("DataAdmin");
                if (admins != null && !admins.isEmpty()) return admins.get(0);
            } catch (Exception ignored) {}
            throw new EntityNotFoundException("User not found: " + username);
        });

        try {
            var entity = new com.gdtahara.gdtaharabackend.model.ScrapWeightLog();
            entity.setReport(report);
            entity.setTechnician(technician);
            entity.setScrapType((requestDto.getScrapType() != null && !requestDto.getScrapType().isBlank()) ? requestDto.getScrapType() : "ไม่ระบุ");
            entity.setMatType((requestDto.getMatType() != null && !requestDto.getMatType().isBlank()) ? requestDto.getMatType() : null);
            entity.setWeightKg(requestDto.getWeightKg());

            var saved = scrapWeightLogRepository.save(entity);
            auditLogService.log("CREATE", "ScrapWeightLog", saved.getId(), null,
                    java.util.Map.of("reportId", String.valueOf(reportId),
                            "weightKg", String.valueOf(requestDto.getWeightKg()),
                            "createdBy", username));
        } catch (Exception e) {
            logger.error("Failed to record scrap weight for report {}: {}", reportId, e.getMessage(), e);
            throw e;
        }

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
            // Resolve technician
            var technician = userRepository.findByUsername(username).orElseGet(() -> {
                try {
                    var techs = userRepository.findByRole("Technician");
                    if (techs != null && !techs.isEmpty()) return techs.get(0);
                    var admins = userRepository.findByRole("DataAdmin");
                    if (admins != null && !admins.isEmpty()) return admins.get(0);
                } catch (Exception ignored) {}
                throw new EntityNotFoundException("User not found: " + username);
            });

            // Determine recordTime key (e.g., "Standard", "10:00")
            String recordTimeKey;
            try {
                String rec = request.getRecordType();
                recordTimeKey = (rec != null && !rec.isBlank()) ? rec : "Standard";
            } catch (Exception ignore) {
                recordTimeKey = "Standard";
            }

            // Try upsert: if a record for (reportId, recordTime) exists, update it; else create
            ParameterRecord entity = parameterRecordRepository
                    .findTopByReportIdAndRecordTime(reportId, recordTimeKey)
                    .orElseGet(ParameterRecord::new);

            // Initialize identity fields for new record
            if (entity.getId() == null) {
                entity.setReportId(reportId);
                entity.setTechnicianId(technician.getId());
                entity.setRecordTime(recordTimeKey);
            }

            // Always refresh createdAt to last-modified moment
            entity.setCreatedAt(LocalDateTime.now());

            // Map DTO -> entity
            if (request.getParameters() != null) {
                ParameterRecordMapper.mapDtoToEntity(request.getParameters(), entity);
            }

            // Save (insert or update)
            ParameterRecord savedEntity = parameterRecordRepository.save(entity);
            logger.debug("Upserted parameter record (reportId={}, recordTime={}) with ID: {}", reportId, recordTimeKey, savedEntity.getId());
            auditLogService.log("UPSERT", "ParameterRecord", savedEntity.getId(), null,
                    java.util.Map.of("reportId", String.valueOf(reportId),
                            "recordTime", recordTimeKey, "createdBy", username));

            logger.info("Successfully upserted parameter record for report ID: {}", reportId);

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

            if (!isAdminOrOwner(record, username)) {
                throw new AccessDeniedException("Access denied: you do not own this parameter record");
            }

            // อัปเดตข้อมูล - recordTime ไม่ได้มีใน ParameterRecordRequest
            // TODO: เพิ่ม recordTime field ใน ParameterRecordRequest หรือส่งมาแยก
            
            // อัปเดต createdAt เป็น current time
            record.setCreatedAt(LocalDateTime.now());
            
            // ใช้ mapper แปลงข้อมูล
            if (request.getParameters() != null) {
                ParameterRecordMapper.mapDtoToEntity(request.getParameters(), record);
            }
            
            // บันทึกการเปลี่ยนแปลง
            ParameterRecord updatedRecord = parameterRecordRepository.save(record);
            logger.debug("Updated parameter record with ID: {}", updatedRecord.getId());
            logger.info("Successfully updated parameter record ID: {}", recordId);
            auditLogService.log("UPDATE", "ParameterRecord", recordId,
                    java.util.Map.of("id", recordId),
                    java.util.Map.of("id", updatedRecord.getId(), "updatedBy", username));
            
        } catch (EntityNotFoundException | AccessDeniedException e) {
            logger.error("Parameter record update rejected: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating parameter record ID {}: {}", recordId, e.getMessage(), e);
            throw new RuntimeException("Failed to update parameter record: " + e.getMessage(), e);
        }
    }

    /**
     * Record technician NG (defect)
     */
    @Transactional
    public void recordTechnicianNg(Long reportId, NgLogRequestDto requestDto, String username) {
        logger.info("Recording NG for report ID: {} by user: {}", reportId, username);
        
        // Check if report exists
        var report = productionReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Production report not found with ID: " + reportId));
        logger.debug("Found report: {}", report.getId());
        
        if (requestDto == null) {
            throw new IllegalArgumentException("NG request cannot be null");
        }
        if (requestDto.getNgTypeId() == null) {
            throw new IllegalArgumentException("NG Type is required");
        }
        if (requestDto.getQuantity() == null || requestDto.getQuantity() < 0) {
            throw new IllegalArgumentException("NG quantity must be zero or positive");
        }

        var user = userRepository.findByUsername(username).orElseGet(() -> {
            try {
                var techs = userRepository.findByRole("Technician");
                if (techs != null && !techs.isEmpty()) return techs.get(0);
                var admins = userRepository.findByRole("DataAdmin");
                if (admins != null && !admins.isEmpty()) return admins.get(0);
            } catch (Exception ignored) {}
            throw new EntityNotFoundException("User not found: " + username);
        });

        var ngType = ngTypeRepository.findById(requestDto.getNgTypeId())
                .orElseThrow(() -> new EntityNotFoundException("NG Type not found with ID: " + requestDto.getNgTypeId()));

        try {
            var entity = new com.gdtahara.gdtaharabackend.model.NgLog();
            entity.setReport(report);
            entity.setNgType(ngType);
            entity.setUser(user);
            entity.setQuantity(requestDto.getQuantity());
            String source = (requestDto.getSource() != null && !requestDto.getSource().isBlank()) ? requestDto.getSource() : "Technician_Process";
            entity.setSource(source);

            var saved = ngLogRepository.save(entity);
            auditLogService.log("CREATE", "NgLog", saved.getId(), null,
                    java.util.Map.of("reportId", String.valueOf(reportId),
                            "ngTypeId", String.valueOf(requestDto.getNgTypeId()),
                            "quantity", String.valueOf(requestDto.getQuantity()),
                            "createdBy", username));
        } catch (Exception e) {
            logger.error("Failed to record NG for report {}: {}", reportId, e.getMessage(), e);
            throw e;
        }

        logger.info("Successfully recorded NG for report ID: {}", reportId);
    }
    
    /**
     * Save Parameter Record from ParameterChecklistForm.
     * technicianId is always resolved from username — caller cannot override it.
     */
    @Transactional
    public ParameterRecord saveParameterRecord(ParameterRecord parameterRecord, String username) {
        // Strip identity fields that should not come from the caller
        parameterRecord.setId(null);

        var technician = userRepository.findByUsername(username).orElseGet(() -> {
            try {
                var techs = userRepository.findByRole("Technician");
                if (techs != null && !techs.isEmpty()) return techs.get(0);
            } catch (Exception ignored) {}
            throw new jakarta.persistence.EntityNotFoundException("User not found: " + username);
        });
        parameterRecord.setTechnicianId(technician.getId());

        try {
            parameterRecord.setCreatedAt(LocalDateTime.now());
            ParameterRecord savedRecord = parameterRecordRepository.save(parameterRecord);
            logger.info("Successfully saved parameter record with ID: {}", savedRecord.getId());
            auditLogService.log("CREATE", "ParameterRecord", savedRecord.getId(), null,
                    java.util.Map.of("id", savedRecord.getId(), "reportId",
                            String.valueOf(savedRecord.getReportId()), "createdBy", username));
            return savedRecord;
        } catch (Exception e) {
            logger.error("Error saving parameter record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save parameter record: " + e.getMessage());
        }
    }
    
    
    /**
     * Utility method to convert Double to BigDecimal with null handling
     */
    private boolean isAdminOrOwner(ParameterRecord record, String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DataAdmin"));
        if (isAdmin) return true;
        var caller = userRepository.findByUsername(username).orElse(null);
        return caller != null && caller.getId().equals(record.getTechnicianId());
    }

    @SuppressWarnings("unused")
    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }
}