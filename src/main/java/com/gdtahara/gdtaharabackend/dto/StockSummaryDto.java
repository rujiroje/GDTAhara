package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockSummaryDto {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal currentBalance;
}
