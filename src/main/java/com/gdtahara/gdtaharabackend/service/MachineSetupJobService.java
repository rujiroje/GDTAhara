package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.DowntimeEvent;
import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.DowntimeEventRepository;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.MachineSetupJobRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MachineSetupJobService {

    private static final Logger logger = LoggerFactory.getLogger(MachineSetupJobService.class);
    private static final LocalTime DEFAULT_REQUIRED_BEFORE = LocalTime.of(7, 0);

    private final MachineSetupJobRepository machineSetupJobRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public MachineSetupJobService(MachineSetupJobRepository machineSetupJobRepository,
                                   ProductionPlanRepository productionPlanRepository,
                                   DowntimeEventRepository downtimeEventRepository,
                                   MachineRepository machineRepository,
                                   ProductRepository productRepository,
                                   UserRepository userRepository,
                                   AuditLogService auditLogService) {
        this.machineSetupJobRepository = machineSetupJobRepository;
        this.productionPlanRepository = productionPlanRepository;
        this.downtimeEventRepository = downtimeEventRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Scan plans in the given date range and create setup jobs wherever the product changes
     * between adjacent plan dates on the same machine. Preserves history per re-trigger rules.
     */
    @Transactional
    public List<MachineSetupJob> scanAndCreateSetupJobs(LocalDate fromDate, LocalDate toDate) {
        logger.info("Scanning for setup jobs: {} to {}", fromDate, toDate);

        List<ProductionPlan> plans = productionPlanRepository.findByPlanDateBetween(fromDate, toDate);

        // Group by machine, sort each group by plan_date ascending
        Map<Long, List<ProductionPlan>> plansByMachine = plans.stream()
                .collect(Collectors.groupingBy(p -> p.getMachine().getId()));
        plansByMachine.values().forEach(list ->
                list.sort(Comparator.comparing(ProductionPlan::getPlanDate)));

        List<MachineSetupJob> created = new ArrayList<>();

        for (List<ProductionPlan> machinePlans : plansByMachine.values()) {
            Long prevProductId = null;
            for (ProductionPlan plan : machinePlans) {
                Long currentProductId = plan.getProduct().getId();
                if (!currentProductId.equals(prevProductId)) {
                    MachineSetupJob job = createSetupJobIfNeeded(plan, prevProductId);
                    if (job != null) created.add(job);
                }
                prevProductId = currentProductId;
            }
        }

        logger.info("Scan complete: {} new setup jobs created for range {} - {}", created.size(), fromDate, toDate);
        return created;
    }

    @Transactional
    public MachineSetupJob assignTechnician(Long jobId, Long technicianUserId, String username) {
        logger.info("Assigning technician {} to setup job {} by {}", technicianUserId, jobId, username);

        MachineSetupJob job = machineSetupJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MachineSetupJob not found: " + jobId));
        User technician = userRepository.findById(technicianUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + technicianUserId));

        job.setAssignedTo(technician);
        MachineSetupJob saved = machineSetupJobRepository.save(job);
        auditLogService.log("UPDATE", "MachineSetupJob", jobId, null,
                Map.of("assignedTo", String.valueOf(technicianUserId), "updatedBy", username));

        logger.info("Assigned technician {} to job {}", technicianUserId, jobId);
        return saved;
    }

    @Transactional
    public MachineSetupJob startSetup(Long jobId, String username) {
        logger.info("Starting setup job {} by {}", jobId, username);

        MachineSetupJob job = machineSetupJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MachineSetupJob not found: " + jobId));

        if (!"PENDING".equals(job.getStatus())) {
            throw new IllegalStateException(
                    "Setup job " + jobId + " is not PENDING (current: " + job.getStatus() + ")");
        }

        job.setStatus("IN_PROGRESS");
        job.setStartedAt(LocalDateTime.now());

        MachineSetupJob saved = machineSetupJobRepository.save(job);
        auditLogService.log("UPDATE", "MachineSetupJob", jobId,
                Map.of("status", "PENDING"),
                Map.of("status", "IN_PROGRESS", "updatedBy", username));

        logger.info("Setup job {} started by {}", jobId, username);
        return saved;
    }

    @Transactional
    public MachineSetupJob completeSetup(Long jobId, boolean moldChanged,
                                          String moldCodeFrom, String moldCodeTo,
                                          boolean tempAdjusted, boolean cycleAdjusted,
                                          boolean blowPinAligned, boolean fpiPassed,
                                          String notes, String username) {
        logger.info("Completing setup job {} by {}", jobId, username);

        MachineSetupJob job = machineSetupJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MachineSetupJob not found: " + jobId));

        if (!"IN_PROGRESS".equals(job.getStatus())) {
            throw new IllegalStateException(
                    "Setup job " + jobId + " is not IN_PROGRESS (current: " + job.getStatus() + ")");
        }
        if (!fpiPassed) {
            throw new IllegalStateException(
                    "FPI (first-piece inspection) must pass before completing setup job " + jobId);
        }

        User completer = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        LocalDateTime completedAt = LocalDateTime.now();
        int durationMin = job.getStartedAt() != null
                ? (int) Duration.between(job.getStartedAt(), completedAt).toMinutes()
                : 0;

        job.setMoldChanged(moldChanged);
        job.setMoldCodeFrom(moldCodeFrom);
        job.setMoldCodeTo(moldCodeTo);
        job.setTempAdjusted(tempAdjusted);
        job.setCycleAdjusted(cycleAdjusted);
        job.setBlowPinAligned(blowPinAligned);
        job.setFpiPassed(fpiPassed);
        job.setNotes(notes);
        job.setStatus("COMPLETED");
        job.setCompletedAt(completedAt);
        job.setCompletedBy(completer);
        job.setDurationMin(durationMin);

        MachineSetupJob saved = machineSetupJobRepository.save(job);

        auditLogService.log("UPDATE", "MachineSetupJob", jobId,
                Map.of("status", "IN_PROGRESS"),
                Map.of("status", "COMPLETED",
                        "durationMin", String.valueOf(durationMin),
                        "updatedBy", username));

        // Auto-create DowntimeEvent so the setup duration flows into OEE Availability.
        // Note: DowntimeEvent.report is nullable=false in the entity but nullable in this
        // auto-create scenario — wrapped in try-catch to prevent transaction rollback
        // until the schema is corrected (report_id should be nullable for SETUP category).
        try {
            String fromCode = job.getFromProduct() != null
                    ? job.getFromProduct().getProductCode() : "NULL";
            String toCode = job.getToProduct() != null
                    ? job.getToProduct().getProductCode() : "?";

            DowntimeEvent event = new DowntimeEvent();
            event.setCategory("SETUP");
            event.setStartTime(job.getStartedAt());
            event.setEndTime(completedAt);
            event.setReason("Machine setup: " + fromCode + " → " + toCode);
            event.setSolution("Auto-recorded from MachineSetupJob #" + jobId);
            event.setTechnician(completer);

            downtimeEventRepository.save(event);
            logger.info("Auto-created DowntimeEvent for setup job {} ({} min)", jobId, durationMin);
        } catch (Exception e) {
            logger.warn("Could not auto-create DowntimeEvent for setup job {} — will need manual entry: {}",
                    jobId, e.getMessage());
        }

        logger.info("Setup job {} completed in {} min by {}", jobId, durationMin, username);
        return saved;
    }

    @Transactional
    public MachineSetupJob skipSetup(Long jobId, String skipReason, String username) {
        logger.info("Skipping setup job {} by {}", jobId, username);

        MachineSetupJob job = machineSetupJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MachineSetupJob not found: " + jobId));

        if ("COMPLETED".equals(job.getStatus())) {
            throw new IllegalStateException("Cannot skip a COMPLETED setup job: " + jobId);
        }
        if ("SUPERSEDED".equals(job.getStatus())) {
            throw new IllegalStateException("Cannot skip a SUPERSEDED setup job: " + jobId);
        }

        String previousStatus = job.getStatus();
        job.setStatus("SKIPPED");
        job.setSkipReason(skipReason);

        MachineSetupJob saved = machineSetupJobRepository.save(job);
        auditLogService.log("UPDATE", "MachineSetupJob", jobId,
                Map.of("status", previousStatus != null ? previousStatus : ""),
                Map.of("status", "SKIPPED",
                        "skipReason", skipReason != null ? skipReason : "",
                        "updatedBy", username));

        logger.info("Setup job {} skipped by {}", jobId, username);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MachineSetupJob> getPendingJobsForTechnician(Long userId) {
        return machineSetupJobRepository.findByAssignedToIdAndStatusIn(userId, List.of("PENDING", "IN_PROGRESS"));
    }

    @Transactional(readOnly = true)
    public Optional<MachineSetupJob> getJobForPlan(Long planId) {
        return machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(
                planId, List.of("PENDING", "IN_PROGRESS", "COMPLETED"));
    }

    // --- private helpers ---

    private MachineSetupJob createSetupJobIfNeeded(ProductionPlan plan, Long fromProductId) {
        Optional<MachineSetupJob> existingOpt = machineSetupJobRepository
                .findFirstByProductionPlanIdAndStatusIn(plan.getId(),
                        List.of("IN_PROGRESS", "PENDING", "COMPLETED", "SKIPPED"));

        if (existingOpt.isPresent()) {
            MachineSetupJob existing = existingOpt.get();
            String status = existing.getStatus();

            if ("IN_PROGRESS".equals(status)) {
                logger.warn("IN_PROGRESS setup job #{} for plan {} on machine {} — flagged for Technician review, no automatic action",
                        existing.getId(), plan.getId(), plan.getMachine().getId());
                return null;
            }

            if ("COMPLETED".equals(status)) {
                // If this COMPLETED job already covers the same product, the setup is done — skip
                if (existing.getToProduct() != null
                        && existing.getToProduct().getId().equals(plan.getProduct().getId())) {
                    logger.debug("COMPLETED job #{} already covers toProduct={} for plan {} — skipping",
                            existing.getId(), plan.getProduct().getId(), plan.getId());
                    return null;
                }
                // Product changed again — create an additional job; do NOT touch the COMPLETED record
                return buildAndSaveSetupJob(plan, fromProductId);
            }

            if ("PENDING".equals(status) || "SKIPPED".equals(status)) {
                existing.setStatus("SUPERSEDED");
                machineSetupJobRepository.save(existing);
                logger.info("Superseded {} job #{} for plan {}", status, existing.getId(), plan.getId());
                return buildAndSaveSetupJob(plan, fromProductId);
            }
        }

        return buildAndSaveSetupJob(plan, fromProductId);
    }

    private MachineSetupJob buildAndSaveSetupJob(ProductionPlan plan, Long fromProductId) {
        Product fromProduct = fromProductId != null
                ? productRepository.findById(fromProductId).orElse(null)
                : null;

        MachineSetupJob job = new MachineSetupJob();
        job.setMachine(plan.getMachine());
        job.setFromProduct(fromProduct);
        job.setToProduct(plan.getProduct());
        job.setProductionPlan(plan);
        job.setPlanDate(plan.getPlanDate());
        job.setRequiredBefore(DEFAULT_REQUIRED_BEFORE);
        job.setStatus("PENDING");
        job.setMoldChanged(false);
        job.setTempAdjusted(false);
        job.setCycleAdjusted(false);
        job.setBlowPinAligned(false);
        job.setFpiPassed(false);

        MachineSetupJob saved = machineSetupJobRepository.save(job);
        auditLogService.log("CREATE", "MachineSetupJob", saved.getId(), null,
                Map.of("machineId", String.valueOf(plan.getMachine().getId()),
                        "planId", String.valueOf(plan.getId()),
                        "planDate", plan.getPlanDate().toString()));

        logger.info("Created setup job {} for machine {} plan_date {}",
                saved.getId(), plan.getMachine().getId(), plan.getPlanDate());
        return saved;
    }
}
