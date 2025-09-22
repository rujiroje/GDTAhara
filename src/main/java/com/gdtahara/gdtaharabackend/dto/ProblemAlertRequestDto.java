package com.gdtahara.gdtaharabackend.dto;

public class ProblemAlertRequestDto {
    private Long reportId;
    private String problemType;
    private String description;
    private String severity;
    private String message;
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public String getProblemType() {
        return problemType;
    }
    
    public void setProblemType(String problemType) {
        this.problemType = problemType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}