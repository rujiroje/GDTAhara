package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MaterialUsageSummaryDto {
    private String materialName;
    private BigDecimal totalQuantity;
    private String unit;
}