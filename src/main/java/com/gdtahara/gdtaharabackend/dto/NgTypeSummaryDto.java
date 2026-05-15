package com.gdtahara.gdtaharabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NgTypeSummaryDto {
    private String ngDescription;
    private Long count;
    private double percentage;
    private long totalQuantity;
    // น้ำหนักของเสีย (กก.) ใช้สำหรับ Technician scrap summary ตามกะ
    private BigDecimal weightKg;
    // สำหรับ FE: เวลา/ช่วงเวลาที่ต้องการแสดง (เช่น เวลาเริ่มกะ)
    private String timeDisplay;

    // Constructor สำหรับ backward compatibility (2 parameters)
    public NgTypeSummaryDto(String ngDescription, Long count) {
        this.ngDescription = ngDescription;
        this.count = count;
        this.percentage = 0.0;
        this.totalQuantity = count != null ? count : 0L;
        this.weightKg = BigDecimal.ZERO;
    }

    // Full constructor without timeDisplay (for legacy calls)
    public NgTypeSummaryDto(String ngDescription, Long count, double percentage, Long totalQuantity) {
        this.ngDescription = ngDescription;
        this.count = count;
        this.percentage = percentage;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0L;
        this.weightKg = BigDecimal.ZERO;
        this.timeDisplay = null;
    }

    // JSON aliases for frontend compatibility
    @JsonProperty("description")
    public String getDescriptionAlias() { return ngDescription; }

    @JsonProperty("name")
    public String getNameAlias() { return ngDescription; }

    @JsonProperty("label")
    public String getLabelAlias() { return ngDescription; }

    @JsonProperty("quantity")
    public Long getQuantityAlias() { return count; }

    @JsonProperty("qty")
    public Long getQtyAlias() { return count; }

    @JsonProperty("value")
    public Long getValueAlias() { return count; }

    @JsonProperty("percent")
    public double getPercentAlias() { return percentage; }

    // Weight aliases for FE convenience
    @JsonProperty("weightKg")
    public BigDecimal getWeightKgAlias() { return weightKg; }

    @JsonProperty("weight")
    public BigDecimal getWeightAlias() { return weightKg; }

    @JsonProperty("scrapWeightKg")
    public BigDecimal getScrapWeightKgAlias() { return weightKg; }

    @JsonProperty("scrapWeight")
    public BigDecimal getScrapWeightAlias() { return weightKg; }

    // Aliases สำหรับเวลาที่แสดงในตาราง FE
    @JsonProperty("time")
    public String getTimeAlias() { return timeDisplay; }

    @JsonProperty("timestamp")
    public String getTimestampAlias() { return timeDisplay; }
}