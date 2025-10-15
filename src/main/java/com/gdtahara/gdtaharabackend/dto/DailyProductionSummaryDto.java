package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO สำหรับสรุปข้อมูลการผลิตรายวัน
 */
@Data
public class DailyProductionSummaryDto {

    /**
     * วันที่ของรายงาน
     */
    private LocalDate date;

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
     * สรุปประเภทของเสีย
     */
    private List<NgSummaryDto> ngSummary;

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

    /**
     * รายการบันทึกการบรรจุภายในช่วงวันผลิต (03:00-03:00) สำหรับการตรวจสอบข้อมูลจริง
     */
    private List<PackagingLogViewDto> packagingLogs;

    // ข้อมูลกะทำงาน
    private String nightShiftSupervisor;
    private String nightShiftWorkers;
    private Integer nightShiftCount;
    
    private String morningShiftSupervisor;
    private String morningShiftWorkers;
    private Integer morningShiftCount;
    
    private String eveningShiftSupervisor;
    private String eveningShiftWorkers;
    private Integer eveningShiftCount;

    /**
     * รายการรายงานประจำวัน (เพื่อแสดงรายละเอียดเครื่องจักรและผลิตภัณฑ์)
     */
    private List<DailyReportSummaryDto> dailyReports;
}