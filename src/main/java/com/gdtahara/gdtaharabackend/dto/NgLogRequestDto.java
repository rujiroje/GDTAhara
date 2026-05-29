// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/NgLogRequestDto.java
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NgLogRequestDto {
    @NotNull(message = "NG type is required")
    private Long ngTypeId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be zero or positive")
    private Integer quantity;

    private String source;
}