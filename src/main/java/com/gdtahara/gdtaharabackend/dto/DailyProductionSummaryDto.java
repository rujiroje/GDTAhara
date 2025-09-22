package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO สำหรับสรุปข้อมูลการผลิตรายวัน
 */
@Data
public class DailyProductionSummaryDto {

    /**
     * ยอดผลิตดีทั้งหมด (ชิ้น)
     */
    private long totalGoodQty;

    /**
     * ยอดของเสียทั้งหมด (ชิ้น)
     */
    private long totalNgQty;

    /**
     * จำนวนกล่องที่ผลิตได้ทั้งหมด
     */
    private long totalGoodBoxes;

    /**
     * น้ำหนักของเสียทั้งหมด (กิโลกรัม)
     */
    private BigDecimal totalScrapWeight;

    /**
     * ข้อมูลเครื่องจักรและผลิตภัณฑ์
     */
    private MachineProductInfoDto machineInfo;

    /**
     * สรุปประเภทของเสีย
     */
    private List<NgTypeSummaryDto> ngSummary;

    /**
     * บันทึกการใช้วัตถุดิบ
     */
    private List<MaterialUsageLogDto> materialUsageLogs;

    /**
     * บันทึกเหตุการณ์ Downtime
     */
    private List<DowntimeEventSummaryDto> downtimeEvents;

    /**
     * ข้อมูลการผลิตกะกลางวัน
     */
    private ShiftDataDto dayShiftData;

    /**
     * ข้อมูลการผลิตกะกลางคืน
     */
    private ShiftDataDto nightShiftData;
}