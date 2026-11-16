// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/PackagingLogRequestDto.java
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PackagingLogRequestDto {
    @NotBlank(message = "Lot number is required")
    private String lotNumber;

    // boxNo is ignored — server generates it atomically to prevent race conditions
    private Integer boxNo;
}