// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ScrapWeightLogRequestDto.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScrapWeightLogRequestDto {
    private String scrapType;
    private String matType;
    private BigDecimal weightKg;
}