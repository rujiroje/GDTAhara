package com.gdtahara.gdtaharabackend.dto;

public class NgRecordingDto {
    private Long reportId;
    private String ngType;
    private int ngCount;
    private String description;
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public String getNgType() {
        return ngType;
    }
    
    public void setNgType(String ngType) {
        this.ngType = ngType;
    }
    
    public int getNgCount() {
        return ngCount;
    }
    
    public void setNgCount(int ngCount) {
        this.ngCount = ngCount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}