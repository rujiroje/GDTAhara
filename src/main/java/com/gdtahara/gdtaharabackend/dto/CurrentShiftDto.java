package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CurrentShiftDto {
    private String shiftCode;
    private LocalDate shiftDate;
    private String startTime;
    private String endTime;
}
