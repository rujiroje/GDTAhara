package com.gdtahara.gdtaharabackend.dto.waste;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SelfEditDto(
    @NotBlank String machineId,
    String woNumber,
    @NotBlank String wasteTypeCode,
    @NotNull @DecimalMin("0.001") BigDecimal weightKg,
    String note,
    @NotBlank String reason
) {}
