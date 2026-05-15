package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OeeResultDto {
    private Long machineId;
    private String machineName;
    private LocalDateTime from;
    private LocalDateTime to;

    // นาทีแต่ละ state
    private long plannedProductionMinutes;  // เวลาที่วางแผนผลิต (ไม่รวม PLANNED_STOP)
    private long runningMinutes;
    private long idleMinutes;
    private long plannedStopMinutes;
    private long unplannedStopMinutes;

    // Production data
    private long totalQty;
    private long goodQty;
    private long ngQty;
    private double idealCycleTimeSec;       // จาก Recipe

    // OEE components (%)
    private double availability;    // runningMinutes / plannedProductionMinutes
    private double performance;     // (totalQty * idealCycleTimeSec) / (runningMinutes * 60)
    private double quality;         // goodQty / totalQty
    private double oee;             // availability * performance * quality
}
