package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.ProductionPlanResponse;
import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductionPlanService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanService.class);

    private final ProductionPlanRepository productionPlanRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final MachineSetupJobService setupJobService;

    public ProductionPlanService(ProductionPlanRepository productionPlanRepository,
                                 MachineRepository machineRepository,
                                 ProductRepository productRepository,
                                 UserRepository userRepository,
                                 AuditLogService auditLogService,
                                 MachineSetupJobService setupJobService) {
        this.productionPlanRepository = productionPlanRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.setupJobService = setupJobService;
    }

    public record ShiftSplit(int dayTarget, int nightTarget) {}

    @Transactional
    public ProductionPlan createPlan(LocalDate planDate, Long machineId, Long productId,
                                     Integer targetQty, BigDecimal manpowerDRatio,
                                     BigDecimal manpowerNRatio, String sapWoNumber,
                                     String source, String excelFileRef, String username) {
        logger.info("Creating production plan: machine={} product={} date={} by {}", machineId, productId, planDate, username);

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + machineId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        productionPlanRepository.findByMachineIdAndPlanDateAndProductId(machineId, planDate, productId)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "A plan already exists for machine " + machineId + " on " + planDate + " for product " + productId);
                });

        BigDecimal dRatio = manpowerDRatio != null ? manpowerDRatio : new BigDecimal("0.50");
        BigDecimal nRatio = manpowerNRatio != null ? manpowerNRatio : new BigDecimal("0.50");

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanDate(planDate);
        plan.setMachine(machine);
        plan.setProduct(product);
        plan.setTargetQty(targetQty);
        plan.setManpowerDRatio(dRatio);
        plan.setManpowerNRatio(nRatio);
        plan.setSapWoNumber(sapWoNumber);
        plan.setSource(source);
        plan.setExcelFileRef(excelFileRef);
        plan.setStatus("draft");
        plan.setCreatedBy(user);

        ProductionPlan saved = productionPlanRepository.save(plan);
        auditLogService.log("CREATE", "ProductionPlan", saved.getId(), null,
                Map.of("planDate", planDate.toString(),
                        "machineId", String.valueOf(machineId),
                        "productId", String.valueOf(productId),
                        "createdBy", username));

        // Trigger setup-job scan for the affected day window (±1 day) so that any
        // product change between adjacent plans is detected immediately on manual entry.
        triggerSetupJobScan(planDate, saved.getId(), "createPlan");

        logger.info("Created production plan id={}", saved.getId());
        return saved;
    }

    @Transactional
    public ProductionPlan updatePlan(Long planId, Integer newTargetQty,
                                     BigDecimal manpowerDRatio, BigDecimal manpowerNRatio,
                                     String status, String username) {
        logger.info("Updating plan {} by {}", planId, username);

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Production plan not found: " + planId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DataAdmin")
                            || a.getAuthority().equals("ROLE_Production Control"));
        if (!isPrivileged) {
            User caller = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
            if (!caller.getId().equals(plan.getCreatedBy().getId())) {
                throw new AccessDeniedException(
                        "Access denied: only the plan creator, Production Control, or DataAdmin may update this plan");
            }
        }

        Map<String, Object> before = Map.of(
                "targetQty", String.valueOf(plan.getTargetQty()),
                "status", plan.getStatus() != null ? plan.getStatus() : "");

        if (newTargetQty != null) plan.setTargetQty(newTargetQty);
        if (manpowerDRatio != null) plan.setManpowerDRatio(manpowerDRatio);
        if (manpowerNRatio != null) plan.setManpowerNRatio(manpowerNRatio);
        if (status != null) plan.setStatus(status);

        ProductionPlan saved = productionPlanRepository.save(plan);
        auditLogService.log("UPDATE", "ProductionPlan", planId, before,
                Map.of("targetQty", String.valueOf(saved.getTargetQty()),
                        "status", saved.getStatus() != null ? saved.getStatus() : "",
                        "updatedBy", username));

        // Re-scan setup jobs in case target-qty or product-context changed.
        LocalDate pd = saved.getPlanDate();
        if (pd != null) {
            triggerSetupJobScan(pd, planId, "updatePlan");
        }

        logger.info("Updated plan {}", planId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProductionPlan> getPlansForDate(LocalDate planDate) {
        return productionPlanRepository.findByPlanDateBetween(planDate, planDate);
    }

    // Maps inside the transaction so LAZY associations are resolved before session closes
    @Transactional(readOnly = true)
    public List<ProductionPlanResponse> getPlansForDateAsDto(LocalDate planDate) {
        return productionPlanRepository.findByPlanDateBetween(planDate, planDate)
                .stream().map(ProductionPlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionPlan> getPlansForMachineInRange(Long machineId, LocalDate from, LocalDate to) {
        return productionPlanRepository.findByMachineIdAndPlanDateBetweenOrderByPlanDate(machineId, from, to);
    }

    @Transactional(readOnly = true)
    public List<ProductionPlan> getPlansForMonth(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        return productionPlanRepository.findByPlanDateBetween(first, last);
    }

    @Transactional(readOnly = true)
    public Optional<ProductionPlan> findById(Long id) {
        return productionPlanRepository.findById(id);
    }

    public ShiftSplit splitTargetByShift(ProductionPlan plan) {
        BigDecimal dRatio = plan.getManpowerDRatio() != null ? plan.getManpowerDRatio() : BigDecimal.ZERO;
        BigDecimal nRatio = plan.getManpowerNRatio() != null ? plan.getManpowerNRatio() : BigDecimal.ZERO;
        BigDecimal total = dRatio.add(nRatio);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new ShiftSplit(plan.getTargetQty(), 0);
        }

        int dayTarget = dRatio
                .multiply(BigDecimal.valueOf(plan.getTargetQty()))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
        int nightTarget = plan.getTargetQty() - dayTarget;
        return new ShiftSplit(dayTarget, nightTarget);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void triggerSetupJobScan(LocalDate planDate, Long contextId, String caller) {
        try {
            setupJobService.scanAndCreateSetupJobs(
                    planDate.minusDays(1), planDate.plusDays(1));
        } catch (Exception e) {
            logger.warn("Setup job scan failed after {} (id={}): {}", caller, contextId, e.getMessage());
        }
    }
}
