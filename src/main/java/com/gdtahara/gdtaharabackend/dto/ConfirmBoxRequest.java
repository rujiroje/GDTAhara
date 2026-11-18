package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmBoxRequest {
    @NotNull
    private Long productionReportId;

    @NotNull
    @Positive
    private Integer boxQuantity;

    private BigDecimal weightKg;
    private String palletNumber;
}
