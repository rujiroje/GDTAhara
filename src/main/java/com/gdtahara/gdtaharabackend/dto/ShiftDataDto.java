package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO สำหรับเก็บข้อมูลสรุปของแต่ละกะ (กลางวัน/กลางคืน)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDataDto {
    private long goodProductionBoxes;
    private long goodProductionPieces;
    private long ngProductionPieces;
    private long totalProductionPieces;
    private String yieldPercentage;
    private List<NgTypeSummaryDto> ngSummary;
    private List<DowntimeEventSummaryDto> downtimeHistory;
    private List<MaterialUsageLogDto> materialUsageLogs;
    private String debugInfo;
    private long debugPackagingLogCount;
    private BigDecimal totalScrapWeight; // Added
}