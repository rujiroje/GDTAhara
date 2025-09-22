package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO หลักสำหรับหน้า Dashboard ของ Shift Leader
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftLeaderDashboardDto {
    private String machineName;
    private String productName;
    private String productionDate;

    private ShiftDataDto dayShiftData;
    private ShiftDataDto nightShiftData;
}
