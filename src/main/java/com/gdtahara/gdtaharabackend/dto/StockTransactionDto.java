// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/StockTransactionDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionDto {
    private String timestamp;
    private String transactionType; // IN or OUT
    private BigDecimal quantity;
    private String lotNumber;
    private String reportIdentifier; // MachineName - ProductName หรือ "Stock-In"
    private String userName; // ผู้ทำรายการ
}