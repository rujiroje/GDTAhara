// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/StockCardViewDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
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
public class StockCardViewDto {
    private String materialCode;
    private String materialName;
    private BigDecimal currentBalance;
    private String unit;
    private List<StockTransactionDto> transactions;
}