package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // 1. เพิ่ม NoArgsConstructor
import java.time.LocalDate;

@Data
@AllArgsConstructor // 2. AllArgsConstructor จะสร้าง Constructor ให้เราอัตโนมัติ
@NoArgsConstructor  // 3. เพิ่ม NoArgsConstructor เพื่อให้มีความยืดหยุ่น
public class ProductionReportSimpleViewDto {
    private Long id; // แก้ไขจากไฟล์เดิม อาจจะต้องเพิ่ม ID เข้ามา
    private LocalDate startDate;
    private String machineName;
    private String productName;
}