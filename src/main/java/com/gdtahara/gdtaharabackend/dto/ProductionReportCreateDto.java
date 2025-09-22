package com.gdtahara.gdtaharabackend.dto;

public class ProductionReportCreateDto {
    private String orderNumber;
    private String machineName;
    private String productName;
    private String shiftLeader;
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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
    
    public String getShiftLeader() {
        return shiftLeader;
    }
    
    public void setShiftLeader(String shiftLeader) {
        this.shiftLeader = shiftLeader;
    }
}