package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ShiftReportDto {
    private String tankId;
    private String tankCode;
    private String tankName;
    private String machineId;
    private String shiftCode;
    private LocalDate shiftDate;

    private BigDecimal startingMeterKg;
    private BigDecimal totalRefillsKg;
    private BigDecimal endingMeterKg;
    private BigDecimal computedUsedKg;
    private Integer refillCount;
    private Integer correctionCount;
    private Boolean isFinal;

    private List<RefillHistoryItemDto> refills;
}
