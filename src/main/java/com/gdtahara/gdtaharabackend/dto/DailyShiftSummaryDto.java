package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO สำหรับสรุปข้อมูลการผลิตรายวันแบบแยกกะ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyShiftSummaryDto {

    /**
     * วันที่ผลิต (รูปแบบ: YYYY-MM-DD)
     */
    private String productionDate;

    /**
     * ข้อมูลการผลิตกะกลางวัน
     */
    private ShiftDataDto dayShiftData;

    /**
     * ข้อมูลการผลิตกะกลางคืน
     */
    private ShiftDataDto nightShiftData;

    /**
     * เลขที่สั่งผลิต
     */
    private String orderNumber;

    /**
     * ชื่อเครื่องจักร
     */
    private String machineName;

    /**
     * ชื่อผลิตภัณฑ์
     */
    private String productName;
}
