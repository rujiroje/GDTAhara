package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalReportDto {
    private Long id;
    private String orderNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String machineName;
    private String productName;
    private String status;
}
