package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO สำหรับสรุปรายงานการผลิตรายวัน
 */
@Data
public class DailyReportDto {

    /**
     * ยอดผลิตดีทั้งหมด (ชิ้น)
     */
    private long totalGoodQty;

    /**
     * ยอดของเสียทั้งหมด (ชิ้น)
     */
    private long totalNgQty;

    /**
     * ค่า Yield (เปอร์เซ็นต์)
     */
    private String yield;

    /**
     * รายการการใช้วัตถุดิบ
     */
    private List<MaterialUsageLogDto> materialUsages;

    /**
     * รายการเหตุการณ์ Downtime
     */
    private List<DowntimeEventSummaryDto> downtimeEvents;
}