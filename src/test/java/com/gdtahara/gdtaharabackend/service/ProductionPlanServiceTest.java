package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

    @Mock private ProductionPlanRepository productionPlanRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MachineSetupJobService setupJobService;

    private ProductionPlanService service;

    private Machine stubMachine;
    private Product stubProduct;
    private User    stubUser;

    @BeforeEach
    void setUp() {
        service = new ProductionPlanService(
                productionPlanRepository, machineRepository,
                productRepository, userRepository,
                auditLogService, setupJobService);

        stubMachine = new Machine();
        stubMachine.setId(1L);

        stubProduct = new Product();
        stubProduct.setId(1L);

        stubUser = new User();
        stubUser.setId(10L);
        stubUser.setUsername("planner01");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── createPlan ────────────────────────────────────────────────────────────

    @Test
    void createPlan_savesAndAudits() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(stubMachine));
        when(productRepository.findById(1L)).thenReturn(Optional.of(stubProduct));
        when(userRepository.findByUsername("planner01")).thenReturn(Optional.of(stubUser));
        when(productionPlanRepository.findByMachineIdAndPlanDateAndProductId(any(), any(), any()))
                .thenReturn(Optional.empty());

        ProductionPlan saved = new ProductionPlan();
        saved.setId(1L);
        when(productionPlanRepository.save(any())).thenReturn(saved);

        ProductionPlan result = service.createPlan(
                LocalDate.of(2026, 6, 1), 1L, 1L, 500,
                new BigDecimal("0.50"), new BigDecimal("0.50"),
                null, "manual", null, "planner01");

        assertThat(result.getId()).isEqualTo(1L);
        verify(productionPlanRepository).save(any(ProductionPlan.class));
        verify(auditLogService).log(eq("CREATE"), eq("ProductionPlan"), eq(1L), isNull(), any());
    }

    @Test
    void createPlan_throwsWhenDuplicate() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(stubMachine));
        when(productRepository.findById(1L)).thenReturn(Optional.of(stubProduct));
        when(userRepository.findByUsername("planner01")).thenReturn(Optional.of(stubUser));
        when(productionPlanRepository.findByMachineIdAndPlanDateAndProductId(
                1L, LocalDate.of(2026, 6, 1), 1L))
                .thenReturn(Optional.of(new ProductionPlan()));

        assertThatThrownBy(() ->
                service.createPlan(LocalDate.of(2026, 6, 1), 1L, 1L, 500,
                        new BigDecimal("0.50"), new BigDecimal("0.50"),
                        null, "manual", null, "planner01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(productionPlanRepository, never()).save(any());
        verify(setupJobService, never()).scanAndCreateSetupJobs(any(), any());
    }

    @Test
    void createPlan_triggersSetupJobScan() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(stubMachine));
        when(productRepository.findById(1L)).thenReturn(Optional.of(stubProduct));
        when(userRepository.findByUsername("planner01")).thenReturn(Optional.of(stubUser));
        when(productionPlanRepository.findByMachineIdAndPlanDateAndProductId(any(), any(), any()))
                .thenReturn(Optional.empty());

        ProductionPlan saved = new ProductionPlan();
        saved.setId(7L);
        when(productionPlanRepository.save(any())).thenReturn(saved);

        LocalDate planDate = LocalDate.of(2026, 6, 3);
        service.createPlan(planDate, 1L, 1L, 1200,
                null, null, null, "manual", null, "planner01");

        // Scan must cover the adjacent day window so product-change detection works
        verify(setupJobService).scanAndCreateSetupJobs(
                planDate.minusDays(1), planDate.plusDays(1));
    }

    @Test
    void createPlan_setupJobScanFailure_doesNotPropagateException() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(stubMachine));
        when(productRepository.findById(1L)).thenReturn(Optional.of(stubProduct));
        when(userRepository.findByUsername("planner01")).thenReturn(Optional.of(stubUser));
        when(productionPlanRepository.findByMachineIdAndPlanDateAndProductId(any(), any(), any()))
                .thenReturn(Optional.empty());

        ProductionPlan saved = new ProductionPlan();
        saved.setId(8L);
        when(productionPlanRepository.save(any())).thenReturn(saved);

        // Setup job scan throws — createPlan must still return successfully
        doThrow(new RuntimeException("scan error"))
                .when(setupJobService).scanAndCreateSetupJobs(any(), any());

        ProductionPlan result = service.createPlan(
                LocalDate.of(2026, 6, 4), 1L, 1L, 1200,
                null, null, null, "manual", null, "planner01");

        assertThat(result.getId()).isEqualTo(8L);
        verify(productionPlanRepository).save(any());   // plan was still saved
    }

    // ── updatePlan ────────────────────────────────────────────────────────────

    @Test
    void updatePlan_ownerCanUpdate() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("planner01", null, "ROLE_Operator"));

        ProductionPlan plan = new ProductionPlan();
        plan.setId(1L);
        plan.setCreatedBy(stubUser);
        plan.setStatus("draft");
        plan.setTargetQty(100);
        plan.setManpowerDRatio(new BigDecimal("0.50"));
        plan.setManpowerNRatio(new BigDecimal("0.50"));
        // planDate intentionally left null → scan is skipped (null-guard in service)

        when(productionPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findByUsername("planner01")).thenReturn(Optional.of(stubUser));
        when(productionPlanRepository.save(any())).thenReturn(plan);

        service.updatePlan(1L, 200, null, null, "active", "planner01");

        verify(productionPlanRepository).save(any());
        verify(auditLogService).log(eq("UPDATE"), eq("ProductionPlan"), eq(1L), any(), any());
        // planDate is null → scan is not called
        verify(setupJobService, never()).scanAndCreateSetupJobs(any(), any());
    }

    @Test
    void updatePlan_triggersSetupJobScan() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("planner01", null, "ROLE_Production Control",
                        "ROLE_Operator"));

        LocalDate planDate = LocalDate.of(2026, 6, 10);
        ProductionPlan plan = new ProductionPlan();
        plan.setId(2L);
        plan.setPlanDate(planDate);
        plan.setCreatedBy(stubUser);
        plan.setStatus("draft");
        plan.setTargetQty(500);
        plan.setManpowerDRatio(new BigDecimal("0.50"));
        plan.setManpowerNRatio(new BigDecimal("0.50"));

        when(productionPlanRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(productionPlanRepository.save(any())).thenReturn(plan);

        service.updatePlan(2L, 600, null, null, "active", "planner01");

        verify(productionPlanRepository).save(any());
        verify(setupJobService).scanAndCreateSetupJobs(
                planDate.minusDays(1), planDate.plusDays(1));
    }

    @Test
    void updatePlan_otherUserDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("other", null, "ROLE_Operator"));

        User creator = new User();
        creator.setId(10L);
        creator.setUsername("planner01");

        User other = new User();
        other.setId(99L);
        other.setUsername("other");

        ProductionPlan plan = new ProductionPlan();
        plan.setId(1L);
        plan.setCreatedBy(creator);
        plan.setStatus("draft");
        plan.setTargetQty(100);
        plan.setManpowerDRatio(new BigDecimal("0.50"));
        plan.setManpowerNRatio(new BigDecimal("0.50"));

        when(productionPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() ->
                service.updatePlan(1L, 200, null, null, "active", "other"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(productionPlanRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        verify(setupJobService, never()).scanAndCreateSetupJobs(any(), any());
    }

    // ── splitTargetByShift ────────────────────────────────────────────────────

    @Test
    void splitTargetByShift_calculates50_50_correctly() {
        ProductionPlan plan = new ProductionPlan();
        plan.setTargetQty(100);
        plan.setManpowerDRatio(new BigDecimal("0.50"));
        plan.setManpowerNRatio(new BigDecimal("0.50"));

        ProductionPlanService.ShiftSplit result = service.splitTargetByShift(plan);

        assertThat(result.dayTarget()).isEqualTo(50);
        assertThat(result.nightTarget()).isEqualTo(50);
        assertThat(result.dayTarget() + result.nightTarget()).isEqualTo(100);
    }
}
