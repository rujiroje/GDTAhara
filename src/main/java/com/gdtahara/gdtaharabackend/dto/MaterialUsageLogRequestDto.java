package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUsageLogRequestDto {
    private Long reportId;
    private String materialCode;
    private String lotNumber;
    private Double quantityKg;
    private String technicianUsername;
}
