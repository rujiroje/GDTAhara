package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockOverviewItemDto {
    private String materialCode;
    private String materialName;
    private BigDecimal balanceKg;
    private BigDecimal totalInKg;
    private BigDecimal totalOutKg;
    private Integer lotCount;
    private LocalDateTime lastMovementAt;
    // from material_reorder_config
    private BigDecimal reorderPointKg;
    private BigDecimal safetyStockKg;
    private String stockStatus; // "NORMAL", "LOW", "CRITICAL", "UNKNOWN"
    // from low_stock_alert
    private Boolean hasActiveAlert;
}
