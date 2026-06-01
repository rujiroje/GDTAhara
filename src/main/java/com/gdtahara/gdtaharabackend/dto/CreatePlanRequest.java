package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePlanRequest {
    @NotNull
    private LocalDate planDate;

    @NotNull
    private Long machineId;

    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private Integer targetQty;

    private BigDecimal manpowerDRatio;
    private BigDecimal manpowerNRatio;
    private String sapWoNumber;

    @NotNull
    private String source;

    private String excelFileRef;
}
