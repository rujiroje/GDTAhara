package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.DowntimeEventRequestDto;
import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.dto.ParameterRecordRequest;
import com.gdtahara.gdtaharabackend.dto.ScrapWeightLogRequestDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock private ProductionReportRepository productionReportRepository;
    @Mock private DowntimeEventRepository downtimeEventRepository;
    @Mock private ScrapWeightLogRepository scrapWeightLogRepository;
    @Mock private ParameterRecordRepository parameterRecordRepository;
    @Mock private NgLogRepository ngLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private NgTypeRepository ngTypeRepository;
    @Mock private AuditLogService auditLogService;

    private TechnicianService technicianService;

    private ProductionReport stubReport;
    private User stubTechnician;

    @BeforeEach
    void setUp() {
        technicianService = new TechnicianService(
                productionReportRepository, downtimeEventRepository,
                scrapWeightLogRepository, parameterRecordRepository,
                ngLogRepository, userRepository, ngTypeRepository,
                auditLogService);

        stubReport = new ProductionReport();
        stubTechnician = new User();
        stubTechnician.setId(55L);
        stubTechnician.setUsername("tech01");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- saveParameterRecord: mass-assignment guard ---

    @Test
    void saveParameterRecord_stripsIdFromRequestBody() {
        ParameterRecord incoming = new ParameterRecord();
        incoming.setId(999L);
        incoming.setReportId(1L);

        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));
        ParameterRecord saved = new ParameterRecord();
        saved.setId(1L);
        saved.setReportId(1L);
        when(parameterRecordRepository.save(any(ParameterRecord.class))).thenReturn(saved);

        technicianService.saveParameterRecord(incoming, "tech01");

        ArgumentCaptor<ParameterRecord> captor = ArgumentCaptor.forClass(ParameterRecord.class);
        verify(parameterRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void saveParameterRecord_forcesTechnicianIdFromUsername() {
        ParameterRecord incoming = new ParameterRecord();
        incoming.setTechnicianId(777L);

        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));
        ParameterRecord saved = new ParameterRecord();
        saved.setId(2L);
        when(parameterRecordRepository.save(any(ParameterRecord.class))).thenReturn(saved);

        technicianService.saveParameterRecord(incoming, "tech01");

        ArgumentCaptor<ParameterRecord> captor = ArgumentCaptor.forClass(ParameterRecord.class);
        verify(parameterRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getTechnicianId()).isEqualTo(55L);
    }

    @Test
    void saveParameterRecord_writesAuditLog() {
        ParameterRecord incoming = new ParameterRecord();
        incoming.setReportId(10L);

        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));
        ParameterRecord saved = new ParameterRecord();
        saved.setId(3L);
        when(parameterRecordRepository.save(any())).thenReturn(saved);

        technicianService.saveParameterRecord(incoming, "tech01");

        verify(auditLogService).log(eq("CREATE"), eq("ParameterRecord"), eq(3L), isNull(), any());
    }

    // --- recordDowntime: audit log ---

    @Test
    void recordDowntime_writesAuditLog() {
        when(productionReportRepository.findById(1L)).thenReturn(Optional.of(stubReport));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));

        DowntimeEvent savedEntity = mock(DowntimeEvent.class);
        when(savedEntity.getId()).thenReturn(10L);
        when(downtimeEventRepository.save(any(DowntimeEvent.class))).thenReturn(savedEntity);

        DowntimeEventRequestDto dto = new DowntimeEventRequestDto();
        dto.setStartTime(LocalDateTime.now());
        dto.setReason("Power failure");

        technicianService.recordDowntime(1L, dto, "tech01");

        verify(auditLogService).log(eq("CREATE"), eq("DowntimeEvent"), eq(10L), isNull(), any());
    }

    // --- recordScrapWeight: audit log ---

    @Test
    void recordScrapWeight_writesAuditLog() {
        when(productionReportRepository.findById(1L)).thenReturn(Optional.of(stubReport));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));

        ScrapWeightLog savedEntity = mock(ScrapWeightLog.class);
        when(savedEntity.getId()).thenReturn(20L);
        when(scrapWeightLogRepository.save(any(ScrapWeightLog.class))).thenReturn(savedEntity);

        ScrapWeightLogRequestDto dto = new ScrapWeightLogRequestDto();
        dto.setWeightKg(BigDecimal.valueOf(2.5));

        technicianService.recordScrapWeight(1L, dto, "tech01");

        verify(auditLogService).log(eq("CREATE"), eq("ScrapWeightLog"), eq(20L), isNull(), any());
    }

    // --- recordTechnicianNg: audit log ---

    @Test
    void recordTechnicianNg_writesAuditLog() {
        NgType ngType = new NgType();
        ngType.setId(5L);

        when(productionReportRepository.findById(1L)).thenReturn(Optional.of(stubReport));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));
        when(ngTypeRepository.findById(5L)).thenReturn(Optional.of(ngType));

        NgLog savedEntity = mock(NgLog.class);
        when(savedEntity.getId()).thenReturn(30L);
        when(ngLogRepository.save(any(NgLog.class))).thenReturn(savedEntity);

        NgLogRequestDto dto = new NgLogRequestDto();
        dto.setNgTypeId(5L);
        dto.setQuantity(3);

        technicianService.recordTechnicianNg(1L, dto, "tech01");

        verify(auditLogService).log(eq("CREATE"), eq("NgLog"), eq(30L), isNull(), any());
    }

    // --- updateParameterRecord: IDOR ownership check (M-6) ---

    @Test
    void updateParameterRecord_owner_succeeds() {
        // Owner technician (id=55) updating their own record (technicianId=55)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tech01", null, "ROLE_Technician"));

        ParameterRecord record = new ParameterRecord();
        record.setId(10L);
        record.setTechnicianId(55L);

        when(parameterRecordRepository.findById(10L)).thenReturn(Optional.of(record));
        when(userRepository.findByUsername("tech01")).thenReturn(Optional.of(stubTechnician));
        when(parameterRecordRepository.save(any())).thenReturn(record);

        ParameterRecordRequest request = new ParameterRecordRequest();
        request.setParameters(new ParameterRecordRequest.ParameterData());

        technicianService.updateParameterRecord(10L, request, "tech01");

        verify(parameterRecordRepository).save(any());
    }

    @Test
    void updateParameterRecord_dataAdmin_succeeds_regardless_of_ownership() {
        // DataAdmin (different user, id=99) can update any record
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin01");

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin01", null, "ROLE_DataAdmin"));

        ParameterRecord record = new ParameterRecord();
        record.setId(20L);
        record.setTechnicianId(55L); // owned by tech01, not admin01

        when(parameterRecordRepository.findById(20L)).thenReturn(Optional.of(record));
        when(parameterRecordRepository.save(any())).thenReturn(record);

        ParameterRecordRequest request = new ParameterRecordRequest();
        request.setParameters(new ParameterRecordRequest.ParameterData());

        technicianService.updateParameterRecord(20L, request, "admin01");

        verify(parameterRecordRepository).save(any());
        // userRepository must NOT be called — isAdmin check short-circuits
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void updateParameterRecord_differentTechnician_throwsAccessDenied() {
        // tech02 (id=66) trying to update a record owned by tech01 (id=55)
        User otherTech = new User();
        otherTech.setId(66L);
        otherTech.setUsername("tech02");

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("tech02", null, "ROLE_Technician"));

        ParameterRecord record = new ParameterRecord();
        record.setId(30L);
        record.setTechnicianId(55L); // owned by tech01

        when(parameterRecordRepository.findById(30L)).thenReturn(Optional.of(record));
        when(userRepository.findByUsername("tech02")).thenReturn(Optional.of(otherTech));

        ParameterRecordRequest request = new ParameterRecordRequest();
        request.setParameters(new ParameterRecordRequest.ParameterData());

        assertThatThrownBy(() -> technicianService.updateParameterRecord(30L, request, "tech02"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("you do not own this parameter record");

        verify(parameterRecordRepository, never()).save(any());
    }
}
