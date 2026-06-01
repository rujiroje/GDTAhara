package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.DowntimeEvent;
import com.gdtahara.gdtaharabackend.model.Machine;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineSetupJobServiceTest {

    @Mock private MachineSetupJobRepository machineSetupJobRepository;
    @Mock private ProductionPlanRepository productionPlanRepository;
    @Mock private DowntimeEventRepository downtimeEventRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private MachineSetupJobService service;

    private Machine machine;
    private Product productA;
    private Product productB;
    private Product productC;
    private ProductionPlan plan1; // day1, productA
    private ProductionPlan plan2; // day2, productB

    @BeforeEach
    void setUp() {
        service = new MachineSetupJobService(machineSetupJobRepository, productionPlanRepository,
                downtimeEventRepository, machineRepository, productRepository,
                userRepository, auditLogService);

        machine = new Machine();
        machine.setId(1L);
        machine.setMachineCode("MC01");

        productA = new Product();
        productA.setId(10L);
        productA.setProductCode("PROD-A");

        productB = new Product();
        productB.setId(20L);
        productB.setProductCode("PROD-B");

        productC = new Product();
        productC.setId(30L);
        productC.setProductCode("PROD-C");

        plan1 = new ProductionPlan();
        plan1.setId(1L);
        plan1.setMachine(machine);
        plan1.setProduct(productA);
        plan1.setPlanDate(LocalDate.of(2026, 6, 1));

        plan2 = new ProductionPlan();
        plan2.setId(2L);
        plan2.setMachine(machine);
        plan2.setProduct(productB);
        plan2.setPlanDate(LocalDate.of(2026, 6, 2));
    }

    // Helper: a completed job stub with a given toProduct
    private MachineSetupJob completedJobFor(ProductionPlan plan, Product toProduct) {
        MachineSetupJob job = new MachineSetupJob();
        job.setId(99L);
        job.setStatus("COMPLETED");
        job.setToProduct(toProduct);
        job.setProductionPlan(plan);
        return job;
    }

    // ---------------------------------------------------------------
    // scan: product changes → 1 new job (for day2)
    // ---------------------------------------------------------------

    @Test
    void scanAndCreateSetupJobs_createsJobOnProductChange() {
        // plan1 (null→A) already has a COMPLETED job matching toProduct=A → skip
        // plan2 (A→B) has no existing job → create
        when(productionPlanRepository.findByPlanDateBetween(any(), any()))
                .thenReturn(List.of(plan1, plan2));
        when(machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(eq(1L), any()))
                .thenReturn(Optional.of(completedJobFor(plan1, productA)));
        when(machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(eq(2L), any()))
                .thenReturn(Optional.empty());
        when(productRepository.findById(productA.getId()))
                .thenReturn(Optional.of(productA)); // fromProduct for plan2 job
        when(machineSetupJobRepository.save(any())).thenAnswer(inv -> {
            MachineSetupJob j = inv.getArgument(0);
            j.setId(100L);
            return j;
        });

        List<MachineSetupJob> created = service.scanAndCreateSetupJobs(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(created.get(0).getPlanDate()).isEqualTo(LocalDate.of(2026, 6, 2));
    }

    // ---------------------------------------------------------------
    // scan: same product → 0 new jobs
    // ---------------------------------------------------------------

    @Test
    void scanAndCreateSetupJobs_doesNotCreateJobIfSameProduct() {
        plan2.setProduct(productA); // both days have the same product

        when(productionPlanRepository.findByPlanDateBetween(any(), any()))
                .thenReturn(List.of(plan1, plan2));
        // plan1 (null→A): existing COMPLETED job covers toProduct=A → skip
        when(machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(eq(1L), any()))
                .thenReturn(Optional.of(completedJobFor(plan1, productA)));
        // plan2 has same product as plan1 → not checked at all

        List<MachineSetupJob> created = service.scanAndCreateSetupJobs(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));

        assertThat(created).isEmpty();
        verify(machineSetupJobRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // scan: COMPLETED job for plan2 with OLD product (B); plan2 now has C → create additional
    // ---------------------------------------------------------------

    @Test
    void scanAndCreateSetupJobs_preservesCompletedJob() {
        plan2.setProduct(productC); // plan2 product changed to C (was B in completed job)

        when(productionPlanRepository.findByPlanDateBetween(any(), any()))
                .thenReturn(List.of(plan1, plan2));
        // plan1: COMPLETED for null→A → skip
        when(machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(eq(1L), any()))
                .thenReturn(Optional.of(completedJobFor(plan1, productA)));
        // plan2: COMPLETED for A→B, but plan now says C → create additional
        when(machineSetupJobRepository.findFirstByProductionPlanIdAndStatusIn(eq(2L), any()))
                .thenReturn(Optional.of(completedJobFor(plan2, productB)));
        when(productRepository.findById(productA.getId()))
                .thenReturn(Optional.of(productA)); // fromProduct for plan2 new job
        when(machineSetupJobRepository.save(any())).thenAnswer(inv -> {
            MachineSetupJob j = inv.getArgument(0);
            if (j.getId() == null) j.setId(101L);
            return j;
        });

        List<MachineSetupJob> created = service.scanAndCreateSetupJobs(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getStatus()).isEqualTo("PENDING");

        // Verify the COMPLETED job (id=99) was NOT saved (it remains untouched)
        ArgumentCaptor<MachineSetupJob> captor = ArgumentCaptor.forClass(MachineSetupJob.class);
        verify(machineSetupJobRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotEqualTo(99L); // new entity, not the existing COMPLETED one
    }

    // ---------------------------------------------------------------
    // completeSetup → auto-creates DowntimeEvent with category=SETUP
    // ---------------------------------------------------------------

    @Test
    void completeSetup_writesDowntimeEvent() {
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(30);

        User completer = new User();
        completer.setId(10L);
        completer.setUsername("tech01");

        MachineSetupJob job = new MachineSetupJob();
        job.setId(1L);
        job.setStatus("IN_PROGRESS");
        job.setStartedAt(startedAt);
        job.setFromProduct(productA);
        job.setToProduct(productB);

        when(machineSetupJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(completer));
        when(machineSetupJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeSetup(1L, true, "MOLD-A", "MOLD-B",
                true, true, true, true, "All done", "tech01");

        // DowntimeEvent was saved with category=SETUP
        ArgumentCaptor<DowntimeEvent> eventCaptor = ArgumentCaptor.forClass(DowntimeEvent.class);
        verify(downtimeEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCategory()).isEqualTo("SETUP");

        // MachineSetupJob durationMin is non-negative
        ArgumentCaptor<MachineSetupJob> jobCaptor = ArgumentCaptor.forClass(MachineSetupJob.class);
        verify(machineSetupJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getDurationMin()).isGreaterThanOrEqualTo(0);
    }

    // ---------------------------------------------------------------
    // completeSetup → audit log written
    // ---------------------------------------------------------------

    @Test
    void completeSetup_writesAuditLog() {
        User completer = new User();
        completer.setId(10L);
        completer.setUsername("tech01");

        MachineSetupJob job = new MachineSetupJob();
        job.setId(1L);
        job.setStatus("IN_PROGRESS");
        job.setStartedAt(LocalDateTime.now().minusMinutes(10));
        job.setToProduct(productB);

        when(machineSetupJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(completer));
        when(machineSetupJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(downtimeEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeSetup(1L, false, null, null,
                true, true, true, true, null, "tech01");

        verify(auditLogService).log(eq("UPDATE"), eq("MachineSetupJob"), eq(1L), any(), any());
    }

    // ---------------------------------------------------------------
    // skipSetup → throws when job is already COMPLETED
    // ---------------------------------------------------------------

    @Test
    void skipSetup_throwsWhenAlreadyCompleted() {
        MachineSetupJob job = new MachineSetupJob();
        job.setId(1L);
        job.setStatus("COMPLETED");

        when(machineSetupJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() ->
                service.skipSetup(1L, "too late", "tech01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");

        verify(machineSetupJobRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any());
    }
}
