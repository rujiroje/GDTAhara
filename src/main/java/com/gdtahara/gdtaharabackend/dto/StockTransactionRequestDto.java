package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockTransactionRequestDto {
    @NotNull(message = "Material is required")
    private Long materialId;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "IN|OUT", message = "Transaction type must be IN or OUT")
    private String transactionType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private String lotNumber;
    private Long productionReportId;
}