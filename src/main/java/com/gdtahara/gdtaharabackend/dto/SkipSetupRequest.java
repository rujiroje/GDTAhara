package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkipSetupRequest {
    @NotBlank
    private String skipReason;
}
