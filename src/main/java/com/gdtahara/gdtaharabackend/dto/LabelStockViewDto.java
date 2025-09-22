// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/LabelStockViewDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabelStockViewDto {
    private Long productId;
    private String productCode;
    private String productName;
    private Integer currentStock;
}