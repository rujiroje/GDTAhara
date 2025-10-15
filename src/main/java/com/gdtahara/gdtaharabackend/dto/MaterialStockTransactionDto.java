// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/MaterialStockTransactionDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialStockTransactionDto {
    private Long id;
    private String timestamp;
    private String transactionType;
    private BigDecimal quantity;
    private String lotNumber;
    private String productionInfo; // e.g., "Order 123, RBL101, Aji130"
    private String userName;
}