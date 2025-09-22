package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MaterialWithStockDto {
    private Long id;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal currentStock;
}