// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/PcDashboardSummaryDto.java
// (ไฟล์ใหม่สำหรับโครงสร้างข้อมูล Dashboard ของ PC)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object สำหรับข้อมูลสรุปในหน้า Dashboard ของ Production Control
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PcDashboardSummaryDto {

    private Long reportId;
    private String machineName;
    private String productName;
    private Integer targetQty;
    private Long currentGoodQty; // ยอดผลิตดี (ชิ้น)
    private Long currentNgQty;   // ยอดของเสีย (ชิ้น)

}