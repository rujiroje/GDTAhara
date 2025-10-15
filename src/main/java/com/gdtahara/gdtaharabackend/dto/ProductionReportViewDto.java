// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ProductionReportViewDto.java
// (ฉบับแก้ไข - เพิ่ม Getter/Setter ด้วยตนเอง)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import java.time.LocalDate;

// ไม่ต้องใช้ @Data แล้ว เพราะเราจะสร้าง Getter/Setter เอง
public class ProductionReportViewDto {
    private Long id;
    private LocalDate productionDate;
    private String machineName;
    private String productName;
    private String status;
    private Integer targetQty;
    private boolean editable;

    // --- Manual Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }

    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
}