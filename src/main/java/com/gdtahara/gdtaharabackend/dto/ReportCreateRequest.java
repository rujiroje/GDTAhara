package com.gdtahara.gdtaharabackend.dto;

import java.time.LocalDate;

public class ReportCreateRequest {
    private String orderNumber;
    private Long machineId;
    private Long productId;
    private int targetQty;
    private LocalDate startDate;
    private LocalDate endDate;
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public Long getMachineId() {
        return machineId;
    }
    
    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public int getTargetQty() {
        return targetQty;
    }
    
    public void setTargetQty(int targetQty) {
        this.targetQty = targetQty;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}