// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ProductSimpleDto.java
// (ไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDto {
    private Long id;
    private String productName;
}