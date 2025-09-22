package com.gdtahara.gdtaharabackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReportSummaryDto {
    private Long reportId;
    private String orderNumber;
    private String status;
    private LocalDateTime createdAt;
    private String machineName;
    private String productName;
    private int targetQty;
    private long goodQty;
    private long totalNgQty;
    private String yield;
    private List<Object> downtimeEvents;
    private List<Object> ngLogs;
    private List<Object> materialUsageLogs;
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getMachineName() {
        return machineName;
    }
    
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public int getTargetQty() {
        return targetQty;
    }
    
    public void setTargetQty(int targetQty) {
        this.targetQty = targetQty;
    }
    
    public long getGoodQty() {
        return goodQty;
    }
    
    public void setGoodQty(long goodQty) {
        this.goodQty = goodQty;
    }
    
    public long getTotalNgQty() {
        return totalNgQty;
    }
    
    public void setTotalNgQty(long totalNgQty) {
        this.totalNgQty = totalNgQty;
    }
    
    public String getYield() {
        return yield;
    }
    
    public void setYield(String yield) {
        this.yield = yield;
    }
    
    public List<Object> getDowntimeEvents() {
        return downtimeEvents;
    }
    
    public void setDowntimeEvents(List<Object> downtimeEvents) {
        this.downtimeEvents = downtimeEvents;
    }
    
    public List<Object> getNgLogs() {
        return ngLogs;
    }
    
    public void setNgLogs(List<Object> ngLogs) {
        this.ngLogs = ngLogs;
    }
    
    public List<Object> getMaterialUsageLogs() {
        return materialUsageLogs;
    }
    
    public void setMaterialUsageLogs(List<Object> materialUsageLogs) {
        this.materialUsageLogs = materialUsageLogs;
    }
}