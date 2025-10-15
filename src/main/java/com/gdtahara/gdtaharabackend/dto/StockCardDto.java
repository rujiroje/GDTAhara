// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/StockCardDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockCardDto {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal currentStock;
    private List<MaterialStockTransactionDto> history;
}