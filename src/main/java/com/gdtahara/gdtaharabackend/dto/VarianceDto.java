package com.gdtahara.gdtaharabackend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class VarianceDto {
    private Long reportId;
    private Long planId;
    private String fgCode;

    private String rmCode;
    private String rmName;
    private String materialType;
    private String unit;
    private Boolean isScrap;

    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private BigDecimal variance;
    private BigDecimal variancePercent;

    /** GREEN / YELLOW / RED */
    private String status;
}
