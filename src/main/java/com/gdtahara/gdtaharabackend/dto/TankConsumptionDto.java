package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TankConsumptionDto {
    private String tankId;
    private String tankCode;
    private String machineId;
    private String shiftCode;
    private LocalDate shiftDate;
    private BigDecimal startingMeterKg;
    private BigDecimal totalRefillsKg;
    private BigDecimal endingMeterKg;
    private BigDecimal computedUsedKg;
    private Integer refillCount;
    private Boolean isFinal;
}
