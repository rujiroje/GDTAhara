package com.gdtahara.gdtaharabackend.dto;

public class PackagingLogRequestDto {
    private Long reportId;
    private int packageCount;
    private String packageType;
    private String lotNumber;
    private Integer boxNo;
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public int getPackageCount() {
        return packageCount;
    }
    
    public void setPackageCount(int packageCount) {
        this.packageCount = packageCount;
    }
    
    public String getPackageType() {
        return packageType;
    }
    
    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }
    
    public String getLotNumber() {
        return lotNumber;
    }
    
    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }
    
    public Integer getBoxNo() {
        return boxNo;
    }
    
    public void setBoxNo(Integer boxNo) {
        this.boxNo = boxNo;
    }
}