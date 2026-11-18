// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ScrapWeightLogRequestDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScrapWeightLogRequestDto {
    private String scrapType;
    private String matType;

    @NotNull(message = "Weight (kg) is required")
    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    private BigDecimal weightKg;
}