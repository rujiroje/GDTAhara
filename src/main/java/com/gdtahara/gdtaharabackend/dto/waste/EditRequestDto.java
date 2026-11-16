package com.gdtahara.gdtaharabackend.dto.waste;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EditRequestDto(
    String requestedMachineId,
    @NotBlank String requestedWoNumber,
    String requestedWasteTypeCode,
    @NotNull @DecimalMin("0.001") BigDecimal requestedWeightKg,
    String requestedNote,
    @NotBlank String reason,
    String requestPhotoIds
) {}
