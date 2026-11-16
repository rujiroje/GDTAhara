package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TankStatusDto {
    private String tankId;
    private String machineId;
    private String tankCode;
    private String tankName;
    private String defaultMaterialCode;
    private BigDecimal capacityKg;
    private String physicalPosition;

    private String currentShiftCode;
    private LocalDate currentShiftDate;

    private BigDecimal latestMeterKg;
    private LocalDateTime latestReadingAt;
    private String latestReadingType;

    private BigDecimal shiftStartMeterKg;
    private BigDecimal totalRefillsKg;
    private Integer refillCount;
    private Boolean shiftStartRecorded;
    private Boolean shiftEndRecorded;
}
