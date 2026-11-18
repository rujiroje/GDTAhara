package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockCardResponseDto {
    private String materialCode;
    private String materialName;
    private List<StockCardItemDto> movements;
    private BigDecimal totalInKg;
    private BigDecimal totalOutKg;
    private BigDecimal totalCorrectionKg;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
}
