package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockOverviewDto {
    private List<StockOverviewItemDto> materials;
    private BigDecimal totalBalanceKg;
    private Integer totalActiveLots;
    private Integer alertCount;
}
