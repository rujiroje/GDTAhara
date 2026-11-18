// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ReportCreateRequest.java
// (ฉบับแก้ไข)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportCreateRequest {

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Machine is required")
    private Long machineId;

    @NotNull(message = "Product is required")
    private Long productId;

    @Positive(message = "Target quantity must be positive")
    private Integer targetQty;
}