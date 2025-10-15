// File: src/main/java/com/gdtahara/gdtaharabackend/dto/DowntimeEventSummaryDto.java
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeEventSummaryDto {
    private String startTime;
    private String endTime;
    private String duration;
    private String reason;
    private String technicianName;
    // วิธีแก้ไขปัญหา (การแก้ไข)
    private String solution;
}