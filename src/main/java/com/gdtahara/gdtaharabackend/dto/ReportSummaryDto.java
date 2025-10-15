// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ReportSummaryDto.java
// (ฉบับแก้ไขที่ถูกต้อง)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {

    // หมายเลขคำสั่งผลิต (เพิ่มใหม่)
    @JsonAlias({"orderNo","order","prodOrder","productionOrder"})
    private String orderNumber;

    private String machineName;
    private String productName;
    private Integer targetQty;
    private long goodQty;
    private long totalNgQty;
    private String yield;
    
    // เพิ่มข้อมูล OEE
    private String oee;
    private String availability;
    private String performance;
    private String quality;
    
    // เพิ่มข้อมูลสรุปประเภทของเสีย
    private List<NgTypeSummaryDto> ngTypeSummary;

    // เพิ่มฟิลด์ alias เพื่อรองรับ frontend เดิมที่อ่านชื่อ 'ngSummary'
    @JsonProperty("ngSummary")
    private List<NgTypeSummaryDto> ngSummary;
    
    // เพิ่มข้อมูลสรุปประเภท Downtime
    private List<DowntimeReasonSummaryDto> downtimeReasonSummary;
    
    private List<DowntimeEventSummaryDto> downtimeEvents;
    private List<NgLogSummaryDto> ngLogs;
    private List<MaterialUsageLogDto> materialUsageLogs;

    // รวมนน. ของเสีย (กก.) — เพิ่มสำหรับภาพรวมรายงาน
    private BigDecimal totalScrapWeightKg;

    // รายการบันทึกน้ำหนักของเสียแบบละเอียด (Technician)
    private List<ScrapWeightLogDto> scrapWeightLogs;

    // JSON aliases เพื่อให้รองรับชื่อที่ frontend อาจใช้
    @JsonProperty("orderNo")
    public String getOrderNoAlias() { return orderNumber; }
    @JsonProperty("order")
    public String getOrderAlias() { return orderNumber; }
    @JsonProperty("prodOrder")
    public String getProdOrderAlias() { return orderNumber; }
    @JsonProperty("productionOrder")
    public String getProductionOrderAlias() { return orderNumber; }

    @JsonProperty("totalScrapWeight")
    public BigDecimal getTotalScrapWeightAlias() { return totalScrapWeightKg; }

    @JsonProperty("scrapWeight")
    public BigDecimal getScrapWeightAlias() { return totalScrapWeightKg; }

    // Alias for material usages list used by some views
    @JsonProperty("materialUsages")
    public List<MaterialUsageLogDto> getMaterialUsagesAlias() { return materialUsageLogs; }

    // Alias สำหรับ FE ที่อ่านชื่อ scrapWeightLogs จาก summary
    @JsonProperty("scrapWeightLogs")
    public List<ScrapWeightLogDto> getScrapWeightLogsAlias() { return scrapWeightLogs; }
}