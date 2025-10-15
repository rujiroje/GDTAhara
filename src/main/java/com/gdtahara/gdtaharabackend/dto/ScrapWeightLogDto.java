package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapWeightLogDto {
    private LocalDateTime timestamp;
    private BigDecimal weightKg;
    private String scrapType; // รายการ/สาเหตุของเศษ เช่น PE/PP หรือรายละเอียดที่บันทึก
    private String matType;   // ประเภทวัสดุ (ถ้ามี)
    private String technicianName; // ผู้บันทึก

    // ช่วยให้ FE ตรวจจับว่าเป็น Technician
    private String recordedByRole = "Technician";
    private boolean isTechnician = true;

    // ช่องที่ FE ใช้สำหรับรวมตามสาเหตุ
    public String getReason() { return scrapType; }
    public String getDescription() { return scrapType; }
}
