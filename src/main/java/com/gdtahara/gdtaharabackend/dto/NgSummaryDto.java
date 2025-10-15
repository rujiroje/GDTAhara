package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

/**
 * DTO สำหรับสรุปประเภทของเสีย
 */
@Data
public class NgSummaryDto {
    
    /**
     * คำอธิบายประเภทของเสีย
     */
    private String ngDescription;
    
    /**
     * จำนวนชิ้นของเสีย
     */
    private Long count;
    
    /**
     * น้ำหนักของเสีย (กิโลกรัม)
     */
    private java.math.BigDecimal weightKg;
    
    public NgSummaryDto() {}
    
    public NgSummaryDto(String ngDescription, Long count) {
        this.ngDescription = ngDescription;
        this.count = count;
        this.weightKg = java.math.BigDecimal.ZERO;
    }
    
    public NgSummaryDto(String ngDescription, Long count, java.math.BigDecimal weightKg) {
        this.ngDescription = ngDescription;
        this.count = count;
        this.weightKg = weightKg;
    }
}