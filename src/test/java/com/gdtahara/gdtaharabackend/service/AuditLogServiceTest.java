package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.AuditLog;
import com.gdtahara.gdtaharabackend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // Minimal no-op PlatformTransactionManager — executes the callback without real TX
        PlatformTransactionManager noopTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(
                    org.springframework.transaction.TransactionDefinition def) {
                return new SimpleTransactionStatus(true);
            }
            @Override public void commit(TransactionStatus status) {}
            @Override public void rollback(TransactionStatus status) {}
        };
        auditLogService = new AuditLogService(auditLogRepository, noopTxManager);
    }

    @Test
    void log_savesAuditLogRow() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log("CREATE", "ProductionReport", 42L, null, "some-after");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("ProductionReport");
        assertThat(saved.getEntityId()).isEqualTo(42L);
    }

    @Test
    void log_doesNotPropagate_whenRepositoryThrows() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        // Must not throw — AuditLogService must never affect the business transaction
        assertThatNoException().isThrownBy(() ->
                auditLogService.log("DELETE", "NgLog", 7L, null, null));
    }

    @Test
    void log_setsCorrectActionAndEntityType() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log("UPDATE", "ParameterRecord", 99L, "before-val", "after-val");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("UPDATE");
        assertThat(captor.getValue().getEntityType()).isEqualTo("ParameterRecord");
        assertThat(captor.getValue().getEntityId()).isEqualTo(99L);
    }
}
