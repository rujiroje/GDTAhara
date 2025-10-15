// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/StockOutRequestDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockOutRequestDto {
    private Long productionReportId;
    private Long materialId;
    private String lotNumber;
    private BigDecimal quantity;
}