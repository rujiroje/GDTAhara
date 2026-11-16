package com.gdtahara.gdtaharabackend.dto.waste;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record EditRequestDto(
    String requestedMachineId,
    String requestedWoNumber,
    String requestedWasteTypeCode,
    @DecimalMin("0.001") BigDecimal requestedWeightKg,
    String requestedNote,
    @NotBlank String reason,
    String requestPhotoIds
) {}
