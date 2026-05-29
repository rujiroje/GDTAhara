package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.StockTransactionRequestDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftLeaderServiceTest {

    @Mock private ProductionReportRepository productionReportRepository;
    @Mock private PackagingLogRepository packagingLogRepository;
    @Mock private NgLogRepository ngLogRepository;
    @Mock private DowntimeEventRepository downtimeEventRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialStockTransactionRepository transactionRepository;
    @Mock private NgTypeRepository ngTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MaterialUsageLogRepository materialUsageLogRepository;
    @Mock private ScrapWeightLogRepository scrapWeightLogRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LabelStockRepository labelStockRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ShiftLeaderService shiftLeaderService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- updateStockTransaction: IDOR ownership check (M-5) ---

    @Test
    void updateStockTransaction_ownerCanUpdate() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("shiftA", "x", "ROLE_Shift Leader"));

        User owner = new User();
        owner.setUsername("shiftA");

        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setId(1L);
        transaction.setUser(owner);
        transaction.setQuantity(BigDecimal.valueOf(10));
        transaction.setLotNumber("LOT-001");

        Material material = new Material();
        material.setId(5L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(materialRepository.findById(5L)).thenReturn(Optional.of(material));
        when(transactionRepository.save(any())).thenReturn(transaction);

        StockTransactionRequestDto request = new StockTransactionRequestDto();
        request.setMaterialId(5L);
        request.setQuantity(BigDecimal.valueOf(20));
        request.setLotNumber("LOT-002");
        request.setTransactionType("IN");

        shiftLeaderService.updateStockTransaction(1L, request, "shiftA");

        verify(transactionRepository).save(any(MaterialStockTransaction.class));
        verify(auditLogService).log(eq("UPDATE"), eq("MaterialStockTransaction"), eq(1L), any(), any());
    }

    @Test
    void updateStockTransaction_dataAdminCanUpdateAnyTransaction() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "x", "ROLE_DataAdmin"));

        User otherUser = new User();
        otherUser.setUsername("shiftB");

        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setId(2L);
        transaction.setUser(otherUser); // owned by shiftB, not admin
        transaction.setQuantity(BigDecimal.valueOf(5));
        transaction.setLotNumber("LOT-A");

        Material material = new Material();
        material.setId(7L);

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(transaction));
        when(materialRepository.findById(7L)).thenReturn(Optional.of(material));
        when(transactionRepository.save(any())).thenReturn(transaction);

        StockTransactionRequestDto request = new StockTransactionRequestDto();
        request.setMaterialId(7L);
        request.setQuantity(BigDecimal.valueOf(15));
        request.setLotNumber("LOT-B");
        request.setTransactionType("OUT");

        shiftLeaderService.updateStockTransaction(2L, request, "admin");

        verify(transactionRepository).save(any(MaterialStockTransaction.class));
    }

    @Test
    void updateStockTransaction_otherUserDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("shiftC", "x", "ROLE_Shift Leader"));

        User owner = new User();
        owner.setUsername("shiftB"); // owned by shiftB

        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setId(3L);
        transaction.setUser(owner);
        transaction.setQuantity(BigDecimal.valueOf(8));
        transaction.setLotNumber("LOT-X");

        when(transactionRepository.findById(3L)).thenReturn(Optional.of(transaction));

        StockTransactionRequestDto request = new StockTransactionRequestDto();
        request.setMaterialId(9L);
        request.setQuantity(BigDecimal.valueOf(5));
        request.setLotNumber("LOT-Y");
        request.setTransactionType("IN");

        assertThatThrownBy(() ->
                shiftLeaderService.updateStockTransaction(3L, request, "shiftC"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("you do not own this transaction");

        verify(transactionRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any());
    }
}
