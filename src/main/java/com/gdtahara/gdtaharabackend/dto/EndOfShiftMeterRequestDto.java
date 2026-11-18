package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EndOfShiftMeterRequestDto {
    private BigDecimal meterValueKg;
    private String shiftCode;
    private LocalDate shiftDate;
    private String note;
}
