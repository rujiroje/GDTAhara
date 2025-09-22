package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUsageLogDto {
    private LocalDateTime timestamp;
    private String materialCode;
    private String lotNumber;
    private BigDecimal quantityKg;
    private String technicianName;
}