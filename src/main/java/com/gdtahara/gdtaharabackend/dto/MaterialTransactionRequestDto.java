// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/MaterialTransactionRequestDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MaterialTransactionRequestDto {
    private Long materialId;
    private BigDecimal quantity;
    private String lotNumber;
    private Long productionReportId; // สำหรับการเบิกออก (OUT) เท่านั้น
}