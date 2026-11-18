package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.SubLotRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubLotServiceTest {

    @Mock private SubLotRepository subLotRepository;
    @Mock private ProductionReportRepository productionReportRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private SubLotService service;

    private ProductionReport stubReport;
    private User stubUser;

    @BeforeEach
    void setUp() {
        service = new SubLotService(subLotRepository, productionReportRepository,
                userRepository, auditLogService);

        stubReport = new ProductionReport();
        stubReport.setStatus("In Progress");
        stubReport.setParentLotNumber("PL-MC01-20260601-D");

        stubUser = new User();
        stubUser.setId(10L);
        stubUser.setUsername("op01");
    }

    @Test
    void confirmBox_generatesIncrementalNumber() {
        when(productionReportRepository.findById(1L)).thenReturn(Optional.of(stubReport));
        when(userRepository.findByUsername("op01")).thenReturn(Optional.of(stubUser));

        SubLot saved = new SubLot();
        saved.setId(1L);
        when(subLotRepository.save(any())).thenReturn(saved);

        // count=0 → seq=1 → B0001
        when(subLotRepository.countByProductionReportId(1L)).thenReturn(0L);
        service.confirmBox(1L, 100, BigDecimal.TEN, "PALLET-A", "op01");

        // count=5 → seq=6 → B0006
        when(subLotRepository.countByProductionReportId(1L)).thenReturn(5L);
        service.confirmBox(1L, 200, BigDecimal.ONE, "PALLET-B", "op01");

        ArgumentCaptor<SubLot> captor = ArgumentCaptor.forClass(SubLot.class);
        verify(subLotRepository, times(2)).save(captor.capture());

        List<SubLot> captured = captor.getAllValues();
        assertThat(captured.get(0).getSubLotNumber()).endsWith("-B0001");
        assertThat(captured.get(1).getSubLotNumber()).endsWith("-B0006");
    }

    @Test
    void confirmBox_throwsWhenReportNotFound() {
        when(productionReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.confirmBox(99L, 100, BigDecimal.TEN, "PALLET", "op01"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void confirmBox_writesAuditLog() {
        when(productionReportRepository.findById(1L)).thenReturn(Optional.of(stubReport));
        when(userRepository.findByUsername("op01")).thenReturn(Optional.of(stubUser));
        when(subLotRepository.countByProductionReportId(1L)).thenReturn(0L);

        SubLot saved = new SubLot();
        saved.setId(5L);
        when(subLotRepository.save(any())).thenReturn(saved);

        service.confirmBox(1L, 100, BigDecimal.TEN, "PALLET-A", "op01");

        verify(auditLogService).log(eq("CREATE"), eq("SubLot"), eq(5L), isNull(), any());
    }

    @Test
    void markAsLabeled_updatesStatus() {
        SubLot subLot = new SubLot();
        subLot.setId(1L);
        subLot.setStatus("draft");

        when(subLotRepository.findById(1L)).thenReturn(Optional.of(subLot));
        when(subLotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsLabeled(1L, "ZPL-REF-001", "op01");

        ArgumentCaptor<SubLot> captor = ArgumentCaptor.forClass(SubLot.class);
        verify(subLotRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("labeled");
        assertThat(captor.getValue().getZplLabelPrintedRef()).isEqualTo("ZPL-REF-001");
        verify(auditLogService).log(eq("UPDATE"), eq("SubLot"), eq(1L), any(), any());
    }
}
