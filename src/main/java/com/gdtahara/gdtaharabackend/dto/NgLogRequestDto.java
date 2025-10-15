// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/NgLogRequestDto.java
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class NgLogRequestDto {
    private Long ngTypeId;
    private Integer quantity;
    private String source;
}