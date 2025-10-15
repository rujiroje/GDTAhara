// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/PackagingLogRequestDto.java
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class PackagingLogRequestDto {
    private String lotNumber;
    private Integer boxNo;
}