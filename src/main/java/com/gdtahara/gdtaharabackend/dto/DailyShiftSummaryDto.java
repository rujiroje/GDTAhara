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
     * ข้อมูลเครื่องจักรและผลิตภัณฑ์
     */
    private MachineProductInfoDto machineInfo;

    /**
     * ข้อมูลการผลิตกะกลางวัน
     */
    private ShiftDataDto dayShiftData;

    /**
     * ข้อมูลการผลิตกะกลางคืน
     */
    private ShiftDataDto nightShiftData;
}
