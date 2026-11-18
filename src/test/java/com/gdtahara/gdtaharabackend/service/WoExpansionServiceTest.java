package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WoExpansionServiceTest {

    @Mock private ProductionPlanRepository planRepository;
    @Mock private UserRepository           userRepository;
    @Mock private MachineSetupJobService   setupJobService;

    private WoExpansionService service;

    private Machine machine;
    private Product product;
    private User    creator;

    @BeforeEach
    void setUp() {
        service = new WoExpansionService(planRepository, userRepository, setupJobService);

        machine = new Machine();
        machine.setId(1L);
        machine.setMachineCode("MC01");

        product = new Product();
        product.setId(10L);

        creator = new User();
        creator.setId(1L);
        creator.setUsername("pc_user");
    }

    // Builds a fully-populated WO with the given date range and total target.
    private ProductionReport buildWo(LocalDate start, LocalDate end, int targetQty) {
        ProductionReport wo = new ProductionReport();
        wo.setId(100L);
        wo.setOrderNumber("WO-001");
        wo.setMachine(machine);
        wo.setProduct(product);
        wo.setStartDate(start);
        wo.setEndDate(end);
        wo.setTargetQty(targetQty);
        wo.setPc(creator);
        return wo;
    }

    // ---------------------------------------------------------------
    // 1. All-future WO: 5 days, target 5000 → 5 plans, dailyTarget=1000
    // ---------------------------------------------------------------
    @Test
    void expand_createsDailyPlans_andTriggersScan() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end   = start.plusDays(4); // 5 days

        when(userRepository.findByUsername("pc_user")).thenReturn(Optional.of(creator));
        when(planRepository.findByMachineIdAndPlanDateAndProductId(eq(1L), any(), eq(10L)))
                .thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> {
            ProductionPlan p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });

        WoExpansionService.ExpansionResult result =
                service.expandWoToDailyPlans(buildWo(start, end, 5000), "pc_user");

        assertThat(result.created()).isEqualTo(5);
        assertThat(result.skipPast()).isEqualTo(0);
        assertThat(result.skipExisting()).isEqualTo(0);

        // every saved plan must carry dailyTarget = round(5000/5) = 1000
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(planRepository, times(5)).save(captor.capture());
        captor.getAllValues().forEach(p -> {
            assertThat(p.getTargetQty()).isEqualTo(1000);
            assertThat(p.getSource()).startsWith("WO:");
            assertThat(p.getStatus()).isEqualTo("draft");
        });

        // scan must be called with the extended range
        verify(setupJobService).scanAndCreateSetupJobs(
                eq(start.minusDays(1)), eq(end.plusDays(1)));
    }

    // ---------------------------------------------------------------
    // 2. One day already has a plan → skip it, don't overwrite
    // ---------------------------------------------------------------
    @Test
    void expand_skipsExistingPlan() {
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = d1.plusDays(1); // this one already exists
        LocalDate d3 = d1.plusDays(2);

        when(userRepository.findByUsername("pc_user")).thenReturn(Optional.of(creator));
        when(planRepository.findByMachineIdAndPlanDateAndProductId(1L, d1, 10L))
                .thenReturn(Optional.empty());
        when(planRepository.findByMachineIdAndPlanDateAndProductId(1L, d2, 10L))
                .thenReturn(Optional.of(new ProductionPlan())); // already exists
        when(planRepository.findByMachineIdAndPlanDateAndProductId(1L, d3, 10L))
                .thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WoExpansionService.ExpansionResult result =
                service.expandWoToDailyPlans(buildWo(d1, d3, 3000), "pc_user");

        assertThat(result.created()).isEqualTo(2);
        assertThat(result.skipExisting()).isEqualTo(1);
        verify(planRepository, times(2)).save(any());
    }

    // ---------------------------------------------------------------
    // 3. Range starts in the past → past days are skipped
    // ---------------------------------------------------------------
    @Test
    void expand_skipsPastDates() {
        // Range: [today-3 .. today+1] = 5 days; 3 past, 2 current/future
        LocalDate start = LocalDate.now().minusDays(3);
        LocalDate end   = LocalDate.now().plusDays(1);

        when(userRepository.findByUsername("pc_user")).thenReturn(Optional.of(creator));
        // only future/today dates reach planRepository lookup
        when(planRepository.findByMachineIdAndPlanDateAndProductId(eq(1L), any(), eq(10L)))
                .thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WoExpansionService.ExpansionResult result =
                service.expandWoToDailyPlans(buildWo(start, end, 5000), "pc_user");

        assertThat(result.skipPast()).isEqualTo(3);
        assertThat(result.created()).isEqualTo(2); // today + tomorrow
    }

    // ---------------------------------------------------------------
    // 4. Missing required fields → early return, no side-effects
    // ---------------------------------------------------------------
    @Test
    void expand_missingMachine_returnsEmptyWithoutSideEffects() {
        ProductionReport wo = new ProductionReport();
        wo.setId(1L);
        wo.setProduct(product);
        wo.setStartDate(LocalDate.now().plusDays(1));
        wo.setEndDate(LocalDate.now().plusDays(5));
        // machine deliberately left null

        WoExpansionService.ExpansionResult result = service.expandWoToDailyPlans(wo, "pc_user");

        assertThat(result.created()).isEqualTo(0);
        verifyNoInteractions(planRepository, userRepository, setupJobService);
    }

    // ---------------------------------------------------------------
    // 5. Username not found → falls back to wo.getPc(), still expands
    // ---------------------------------------------------------------
    @Test
    void expand_unknownUsername_fallsBackToWoPc() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end   = start.plusDays(1); // 2 days

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(planRepository.findByMachineIdAndPlanDateAndProductId(eq(1L), any(), eq(10L)))
                .thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WoExpansionService.ExpansionResult result =
                service.expandWoToDailyPlans(buildWo(start, end, 2000), "unknown");

        // Falls back to wo.getPc() (creator) → still creates plans
        assertThat(result.created()).isEqualTo(2);
    }
}
