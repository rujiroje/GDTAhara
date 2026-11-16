package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LowStockAlertDto {
    private Long id;
    private String materialCode;
    private BigDecimal balanceAtAlert;
    private BigDecimal reorderPoint;
    private BigDecimal estDaysRemaining;
    private LocalDate alertDate;
    private Boolean telegramSent;
    private LocalDateTime telegramSentAt;
    private LocalDateTime resolvedAt;
}
