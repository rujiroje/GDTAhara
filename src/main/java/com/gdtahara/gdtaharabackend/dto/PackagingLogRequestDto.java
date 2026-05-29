// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/PackagingLogRequestDto.java
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PackagingLogRequestDto {
    @NotBlank(message = "Lot number is required")
    private String lotNumber;

    @Positive(message = "Box number must be positive")
    private Integer boxNo;
}