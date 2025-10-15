// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/DowntimeReasonSummaryDto.java
// สำหรับสรุปประเภทสาเหตุ Downtime
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeReasonSummaryDto {
    
    private String reason;
    private long totalMinutes;
    private double percentage;
    private int eventCount; // จำนวนครั้งที่เกิดขึ้น
    private String formattedDuration; // เช่น "2 ชั่วโมง 30 นาที"
}