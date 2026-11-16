package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MultiMaterialSummaryItemDto {
    private String materialCode;
    private String materialName;
    private BigDecimal balanceKg;
    private BigDecimal usedKg;
    private BigDecimal receivedKg;
    private BigDecimal changeKg;
    private String trend; // "UP", "DOWN", "STABLE"
    private String stockStatus;
}
