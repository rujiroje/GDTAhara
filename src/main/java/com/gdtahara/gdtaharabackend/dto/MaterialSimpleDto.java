// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/MaterialSimpleDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSimpleDto {
    private Long id;
    private String materialCode;
    private String materialName;
}