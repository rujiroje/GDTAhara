package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Verifies N+1 fix: getAllProductionReports() must call findAllWithFetch(), not findAll().
 */
@ExtendWith(MockitoExtension.class)
class ProductionServiceQueryTest {

    @Mock private ProductionReportRepository productionReportRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PackagingLogRepository packagingLogRepository;
    @Mock private NgLogRepository ngLogRepository;
    @Mock private MaterialUsageLogRepository materialUsageLogRepository;
    @Mock private DowntimeEventRepository downtimeEventRepository;
    @Mock private ScrapWeightLogRepository scrapWeightLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ProductionService productionService;

    @Test
    void getAllProductionReports_usesFetchJoin_notFindAll() {
        when(productionReportRepository.findAllWithFetch()).thenReturn(List.of());

        productionService.getAllProductionReports();

        // Must use the fetch-join query to avoid N+1
        verify(productionReportRepository).findAllWithFetch();
        // Must NOT fall back to the bare findAll() that triggers N+1
        verify(productionReportRepository, never()).findAll();
    }
}
