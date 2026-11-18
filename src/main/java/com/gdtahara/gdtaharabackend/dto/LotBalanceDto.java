package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LotBalanceDto {
    private String lotRef;
    private BigDecimal balanceKg;
    private LocalDateTime receivedAt;
    private LocalDateTime lastActivityAt;
}
