package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO สำหรับสรุปรายงานประจำวันแต่ละใบ
 */
@Data
@NoArgsConstructor
public class DailyReportSummaryDto {
    private String machineName;
    private String productName;
    private Long targetQty;
    private Long goodQty;
    private Long ngQty;
    private String status;
    private String orderNumber;
    
    // Constructor for 6 parameters (without orderNumber)
    public DailyReportSummaryDto(String machineName, String productName, 
                                Long targetQty, Long goodQty, Long ngQty, String status) {
        this.machineName = machineName;
        this.productName = productName;
        this.targetQty = targetQty;
        this.goodQty = goodQty;
        this.ngQty = ngQty;
        this.status = status;
    }
    
    // Constructor for all 7 parameters
    public DailyReportSummaryDto(String machineName, String productName,
                                Long targetQty, Long goodQty, Long ngQty, 
                                String status, String orderNumber) {
        this.machineName = machineName;
        this.productName = productName;
        this.targetQty = targetQty;
        this.goodQty = goodQty;
        this.ngQty = ngQty;
        this.status = status;
        this.orderNumber = orderNumber;
    }
}