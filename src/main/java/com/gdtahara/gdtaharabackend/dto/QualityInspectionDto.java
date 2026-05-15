package com.gdtahara.gdtaharabackend.dto;

public class QualityInspectionDto {
    private Long reportId;
    private String inspectionType;
    private String result;
    private String comments;
    private String inspectorName;
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public String getInspectionType() {
        return inspectionType;
    }
    
    public void setInspectionType(String inspectionType) {
        this.inspectionType = inspectionType;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    public String getInspectorName() {
        return inspectorName;
    }
    
    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
    }
}