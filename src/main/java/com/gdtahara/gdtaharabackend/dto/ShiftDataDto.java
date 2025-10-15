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
    private List<PackagingLogViewDto> packagingLogs;
    private String debugInfo;
    private long debugPackagingLogCount;
    private BigDecimal totalScrapWeight;

    // เพิ่ม fields สำหรับ frontend compatibility
    private long targetQty;
    private long goodQty;
    private long ngQty;
    private double scrapWeight;
    private String orderNumber;
    private String machineName;
    private String productName;
    private String shiftLeader;
    private String workers;
    private int workerCount;

    // Technician-specific rollups
    // - ปริมาณของเสียที่ Technician บันทึก (ชิ้น)
    private long technicianNgQty;
    // - Downtime ที่มี Technician รับผิดชอบ (นาที)
    private long technicianDowntimeMinutes;
    // - น้ำหนักเศษที่ Technician บันทึก (กก.) — โดยทั่วไปเท่ากับ totalScrapWeight
    private BigDecimal technicianScrapWeight;
}