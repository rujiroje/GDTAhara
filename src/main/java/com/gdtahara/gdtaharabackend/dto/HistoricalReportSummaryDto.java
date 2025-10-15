package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO สำหรับรวบรวมข้อมูลสรุปในหน้ารายงานย้อนหลัง
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalReportSummaryDto {
    private Long id;
    private String orderNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String machineName;
    private String productName;
    private Long goodQty;
    private Long ngQty;
    private String yield;
    private Long totalBoxes;
    private BigDecimal totalScrapWeight;
    private String status;
}