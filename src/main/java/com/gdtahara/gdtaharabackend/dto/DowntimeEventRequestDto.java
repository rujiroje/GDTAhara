// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/DowntimeEventRequestDto.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO สำหรับรับข้อมูลการบันทึกเหตุการณ์ Downtime
 */
@Data
public class DowntimeEventRequestDto {

    /**
     * เวลาที่เริ่ม Downtime
     */
    @NotNull(message = "Start time is required.")
    private LocalDateTime startTime;

    /**
     * เวลาที่สิ้นสุด Downtime
     */
    @NotNull(message = "End time is required.")
    private LocalDateTime endTime;

    /**
     * สาเหตุของ Downtime
     */
    @NotBlank(message = "Reason is required.")
    private String reason;

    /**
     * วิธีแก้ไขปัญหา
     */
    @NotBlank(message = "Solution is required.")
    private String solution;
}