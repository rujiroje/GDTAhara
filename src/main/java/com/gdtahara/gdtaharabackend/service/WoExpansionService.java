package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Expands a monthly/weekly WO (ProductionReport) into per-day ProductionPlan rows.
 * Runs in its own REQUIRES_NEW transaction so WO-creation is never rolled back by
 * a plan-insert failure.
 */
@Service
public class WoExpansionService {

    private static final Logger logger = LoggerFactory.getLogger(WoExpansionService.class);

    private final ProductionPlanRepository planRepository;
    private final UserRepository           userRepository;
    private final MachineSetupJobService   setupJobService;

    public WoExpansionService(ProductionPlanRepository planRepository,
                               UserRepository userRepository,
                               MachineSetupJobService setupJobService) {
        this.planRepository  = planRepository;
        this.userRepository  = userRepository;
        this.setupJobService = setupJobService;
    }

    /** Lightweight result DTO — counts for logging/return. */
    public record ExpansionResult(int created, int skipPast, int skipExisting) {}

    /**
     * Expand a WO into one ProductionPlan row per calendar day in [startDate, endDate].
     * Days that already have a plan (from Excel import or manual entry) are skipped.
     * Past days (before today) are skipped.
     * Always returns gracefully — never throws.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExpansionResult expandWoToDailyPlans(ProductionReport wo, String username) {
        try {
            return doExpand(wo, username);
        } catch (Exception e) {
            logger.warn("WO expansion failed for WO id={} order={}: {}",
                    wo.getId(), wo.getOrderNumber(), e.getMessage());
            return new ExpansionResult(0, 0, 0);
        }
    }

    // ── private implementation ────────────────────────────────────────────────

    private ExpansionResult doExpand(ProductionReport wo, String username) {
        // Guard: all required fields must be present
        if (wo.getMachine() == null || wo.getProduct() == null
                || wo.getStartDate() == null || wo.getEndDate() == null) {
            logger.warn("WO id={} missing machine/product/dates — skipping expansion", wo.getId());
            return new ExpansionResult(0, 0, 0);
        }

        LocalDate startDate = wo.getStartDate();
        LocalDate endDate   = wo.getEndDate();
        LocalDate today     = LocalDate.now();

        // Calculate per-day target
        long days       = startDate.datesUntil(endDate.plusDays(1)).count();
        int  rawTarget  = wo.getTargetQty() != null ? wo.getTargetQty() : 0;
        int  dailyTarget = days > 0 ? (int) Math.round((double) rawTarget / days) : 0;

        // Resolve plan creator: use requesting user, fall back to WO's PC
        User creator = resolveCreator(username, wo);
        if (creator == null) {
            logger.warn("WO id={}: cannot resolve a valid creator — skipping expansion", wo.getId());
            return new ExpansionResult(0, 0, 0);
        }

        // source field is VARCHAR(20) — keep it within the DB column limit
        String orderRef = wo.getOrderNumber() != null ? wo.getOrderNumber()
                                                      : String.valueOf(wo.getId());
        String source = "WO:" + orderRef;
        if (source.length() > 20) source = source.substring(0, 20);

        int created = 0, skipPast = 0, skipExisting = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Skip past dates — we never backfill historical plans
            if (date.isBefore(today)) {
                skipPast++;
                continue;
            }

            Long machineId  = wo.getMachine().getId();
            Long productId  = wo.getProduct().getId();

            // Skip if a plan already exists (respect Excel / manual entries)
            if (planRepository.findByMachineIdAndPlanDateAndProductId(machineId, date, productId)
                    .isPresent()) {
                skipExisting++;
                continue;
            }

            ProductionPlan plan = new ProductionPlan();
            plan.setPlanDate(date);
            plan.setMachine(wo.getMachine());
            plan.setProduct(wo.getProduct());
            plan.setTargetQty(dailyTarget);
            plan.setSource(source);
            plan.setStatus("draft");
            plan.setManpowerDRatio(new BigDecimal("0.50"));
            plan.setManpowerNRatio(new BigDecimal("0.50"));
            plan.setCreatedBy(creator);
            planRepository.save(plan);
            created++;
        }

        logger.info("WO {} expanded → created={} skipPast={} skipExisting={}",
                wo.getOrderNumber(), created, skipPast, skipExisting);

        // Trigger setup-job detection for adjacent days (product change detection)
        try {
            setupJobService.scanAndCreateSetupJobs(
                    startDate.minusDays(1), endDate.plusDays(1));
        } catch (Exception e) {
            logger.warn("Setup job scan failed after expanding WO id={}: {}",
                    wo.getId(), e.getMessage());
        }

        return new ExpansionResult(created, skipPast, skipExisting);
    }

    private User resolveCreator(String username, ProductionReport wo) {
        if (username != null && !username.isBlank()) {
            User u = userRepository.findByUsername(username).orElse(null);
            if (u != null) return u;
        }
        // Fallback: use the WO's PC
        return wo.getPc();
    }
}
