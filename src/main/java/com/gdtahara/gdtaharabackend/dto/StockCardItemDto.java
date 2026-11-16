package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockCardItemDto {
    private String movementType; // "IN", "OUT", "CORRECTION"
    private String materialCode;
    private String lotRef;
    private BigDecimal qtyKg;       // positive for IN/OUT
    private BigDecimal negativeQty; // negative for CORRECTION
    private LocalDateTime movementAt;
    private String actor;
    private String machineId;
    private String tankId;
    private String note;
    private Boolean isCorrection;
    private Long sourceId;
    private String sourceTable;
    private BigDecimal runningBalance; // computed in service
}
