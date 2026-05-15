package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecipeDto {
    private Long id;
    private String recipeCode;
    private String recipeName;
    private Long productId;
    private String productName;
    private Long machineId;
    private String machineName;
    private String version;
    private Boolean isActive;
    private BigDecimal targetCycleTimeSec;
    private BigDecimal targetTempZone1;
    private BigDecimal targetTempZone2;
    private BigDecimal targetTempHead;
    private BigDecimal targetBlowPressure;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
