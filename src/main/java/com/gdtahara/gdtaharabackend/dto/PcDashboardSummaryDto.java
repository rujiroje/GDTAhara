// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/PcDashboardSummaryDto.java
// (ไฟล์ใหม่สำหรับโครงสร้างข้อมูล Dashboard ของ PC)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonAlias({"orderNo","order","prodOrder","productionOrder"})
    private String orderNumber; // เพิ่มเลขที่คำสั่งผลิตเพื่อแสดงที่ Dashboard
    private String machineName;
    private String productName;
    private Integer targetQty;
    private Long currentGoodQty; // ยอดผลิตดี (ชิ้น)
    private Long currentNgQty;   // ยอดของเสีย (ชิ้น)
    // สถานะสำหรับแสดงในหน้า PC (IN_PROGRESS/ACTIVE/INACTIVE)
    private String status;

    // แสดงชื่อสถานะภาษาไทยตามนโยบายเดียวกับ ProductionReportDto
    @JsonProperty("statusDisplayName")
    public String getStatusDisplayName() {
        if (status == null) return "ไม่ระบุ";
        switch (status.toUpperCase()) {
            case "IN_PROGRESS": return "กำลังดำเนินการ";
            case "ACTIVE": return "พร้อมทำงาน";
            case "INACTIVE": return "ปิดงาน";
            case "COMPLETED": return "เสร็จสิ้น";
            default: return status;
        }
    }

    public PcDashboardSummaryDto(Long reportId, String machineName, String productName,
                                 Integer targetQty, Long currentGoodQty, Long currentNgQty) {
        this.reportId = reportId;
        this.machineName = machineName;
        this.productName = productName;
        this.targetQty = targetQty;
        this.currentGoodQty = currentGoodQty;
        this.currentNgQty = currentNgQty;
    }
}