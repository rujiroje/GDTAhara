package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MaterialDetailDto {
    private String materialCode;
    private String materialName;
    private BigDecimal balanceKg;
    private BigDecimal reorderPointKg;
    private String stockStatus;
    private List<LotBalanceDto> lots;
    private BigDecimal dailyAvgUsageKg;
    private BigDecimal estDaysRemaining;
}
