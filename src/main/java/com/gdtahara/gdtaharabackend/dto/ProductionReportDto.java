package com.gdtahara.gdtaharabackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionReportDto {

    private Long id;
    private String reportNumber;
    private String shift;
    private String status;

    private String machineId;
    private String machineName;

    private String productId;
    private String productName;
    private String productCode;

    private String createdBy;
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long parameterRecordCount;
    private Long qualityCheckCount;

    private String orderNumber;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer targetQty;
    private boolean finalizable;
    private boolean editable;
    private boolean deletable;

    public String getDisplayDate() {
        return createdAt != null ? createdAt.toLocalDate().toString() : "";
    }

    public String getDisplayTime() {
        return createdAt != null ? createdAt.toLocalTime().toString() : "";
    }

    public String getStatusDisplayName() {
        if (status == null) return "ไม่ระบุ";
        switch (status.toUpperCase()) {
            case "DRAFT": return "ร่าง";
            case "IN_PROGRESS": return "กำลังดำเนินการ";
            case "COMPLETED": return "เสร็จสิ้น";
            case "ACTIVE": return "พร้อมทำงาน";
            case "INACTIVE": return "ปิดงาน";
            case "CANCELLED": return "ยกเลิก";
            case "ON_HOLD": return "หยุดชั่วคราว";
            default: return status;
        }
    }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }
    public boolean isFinalizable() { return finalizable; }
    public void setFinalizable(boolean finalizable) { this.finalizable = finalizable; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public boolean isDeletable() { return deletable; }
    public void setDeletable(boolean deletable) { this.deletable = deletable; }
}
