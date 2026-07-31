// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/StockOutRequestDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockOutRequestDto {
    @NotNull(message = "Production report is required")
    private Long productionReportId;

    @NotNull(message = "Material is required")
    private Long materialId;

    private String lotNumber;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    /** true = HIBE/VERP auto-deduct; skip stock-balance check */
    private boolean autoDeduct;
}