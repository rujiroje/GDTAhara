package com.gdtahara.gdtaharabackend.dto.waste;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DocResponseDto(
    @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
    String docNote
) {}
