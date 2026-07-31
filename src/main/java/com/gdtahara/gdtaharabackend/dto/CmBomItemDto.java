package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmBomItemDto {
    private Integer itemNumber;
    private String rmCode;
    private String materialName;
    private String materialType;
    private String unit;
    private BigDecimal qtyPer;
    private BigDecimal suggestedQty;
    private Long materialId;
    private List<String> availableLots;
    private boolean materialFound;
    /** HIBE / VERP — deducted automatically from BOM×actual; no stock check */
    private boolean autoDeduct;
}
