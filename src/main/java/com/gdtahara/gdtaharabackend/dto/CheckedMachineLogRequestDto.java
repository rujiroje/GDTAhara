package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO สำหรับรับข้อมูลการบันทึกค่าพารามิเตอร์ของเครื่องจักร
 */
@Data
public class CheckedMachineLogRequestDto {

    /**
     * เวลาที่บันทึก (รูปแบบ: ISO 8601)
     */
    @NotBlank(message = "Time record is required.")
    private String timeRecord;

    /**
     * พารามิเตอร์ของเครื่องจักร (Key: Parameter ID, Value: ค่า)
     */
    @NotNull(message = "Parameters map cannot be null.")
    private Map<Long, BigDecimal> parameters;
}