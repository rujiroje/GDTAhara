package com.gdtahara.gdtaharabackend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class MaterialRequirementDto {
    private Long planId;
    private LocalDate planDate;
    private String machineName;
    private String fgCode;
    private Integer targetQty;

    private Long bomId;
    private Long bomItemId;
    private String rmCode;
    private String rmName;
    private String materialType;
    private String unit;
    private BigDecimal quantityPer;
    private BigDecimal lossPercent;
    private Boolean isScrap;

    private BigDecimal plannedQuantity;
    private BigDecimal plannedLossQuantity;
    private BigDecimal totalRequired;

    // Stock comparison — populated by service after aggregation; null if rmCode not in materials table
    private BigDecimal currentStock;
    private BigDecimal shortfall;  // max(0, totalRequired - currentStock)
}
