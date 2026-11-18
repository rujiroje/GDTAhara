package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TankMeterEntryDto {
    private BigDecimal meterValueKg;
    private String readingType; // SHIFT_START, SHIFT_END, ADHOC
    private String shiftCode;
    private LocalDate shiftDate;
    private String note;
}
