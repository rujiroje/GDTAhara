// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ProductionReportSummaryDto.java
// (ฉบับแก้ไข - เพิ่ม Getter/Setter ด้วยตนเอง)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import java.util.List;

public class ProductionReportSummaryDto {
    private Long reportId;
    private String productionDate;
    private String machineName;
    private String productName;
    private Integer targetQty;
    private long goodQty;
    private int totalNgQty;
    private String yield;
    private List<DowntimeEventSummaryDto> downtimeEvents;
    private List<NgLogSummaryDto> ngLogs;

    // --- Manual Getters and Setters ---
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getProductionDate() { return productionDate; }
    public void setProductionDate(String productionDate) { this.productionDate = productionDate; }

    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }

    public long getGoodQty() { return goodQty; }
    public void setGoodQty(long goodQty) { this.goodQty = goodQty; }

    public int getTotalNgQty() { return totalNgQty; }
    public void setTotalNgQty(int totalNgQty) { this.totalNgQty = totalNgQty; }

    public String getYield() { return yield; }
    public void setYield(String yield) { this.yield = yield; }

    public List<DowntimeEventSummaryDto> getDowntimeEvents() { return downtimeEvents; }
    public void setDowntimeEvents(List<DowntimeEventSummaryDto> downtimeEvents) { this.downtimeEvents = downtimeEvents; }

    public List<NgLogSummaryDto> getNgLogs() { return ngLogs; }
    public void setNgLogs(List<NgLogSummaryDto> ngLogs) { this.ngLogs = ngLogs; }
}