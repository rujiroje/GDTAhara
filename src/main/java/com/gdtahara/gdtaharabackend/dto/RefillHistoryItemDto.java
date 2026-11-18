package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefillHistoryItemDto {
    private Long id;
    private String materialCode;
    private String materialLotCode;
    private BigDecimal qtyKg;
    private Boolean isCorrection;
    private Long correctsRefillId;
    private String refillBy;
    private LocalDateTime refillAt;
    private String note;
}
