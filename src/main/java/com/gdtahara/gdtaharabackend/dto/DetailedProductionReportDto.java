package com.gdtahara.gdtaharabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class DetailedProductionReportDto {
    // Header
    private Long reportId;
    private String orderNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String machineName;
    private String productCode;
    private String productName;
    private Integer cavity;
    private Integer qtyPerBox;
    private Integer targetQty;

    // Totals
    private Long goodBoxes;         // จำนวนกล่อง
    private Long goodQty;           // ชิ้นดี = กล่อง x qtyPerBox
    private Long totalNgQty;        // ของเสียรวม (ชิ้น)
    private String yield;           // %Yield (string พร้อม %)
    private BigDecimal totalScrapWeightKg; // น้ำหนักเศษรวม

    // Sections
    private List<MaterialUsageLogDto> materialUsages;  // วัตถุดิบ/LOT/น้ำหนัก
    private List<PackagingLogViewDto> packagingLogs;    // กล่อง/LOT/เวลา/Operator
    private List<NgLogSummaryDto> ngLogs;               // รายการ NG
    private List<DowntimeEventSummaryDto> downtimeEvents; // Downtime
    private List<ScrapWeightLogDto> scrapWeightLogs;    // รายการชั่งน้ำหนักของเสีย (Technician)

    // Optional sections (QA/Parameter/Signatures) - filled as available
    private ParameterRecordDto parameters;   // ค่าพารามิเตอร์รวม (optional view model)
    private QaSection qaSection;             // QA/PD/PM test (placeholder fields)
    private SignatureSection signatures;     // ผู้บันทึก/Leader/Supervisor

    @Data
    @NoArgsConstructor
    public static class QaSection {
        private String pdTestResult;
        private String qaTestResult;
        private String pmCheckResult;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    public static class SignatureSection {
        private String documentBy;  // ผู้กรอกเอกสาร
        private String leaderUpBy;  // หัวหน้างาน
        private String supervisorBy; // ผู้ตรวจสอบ
    }

    // Frontend compatibility aliases for scrap weight total
    @JsonProperty("totalScrapWeight")
    public BigDecimal getTotalScrapWeightAlias() {
        return totalScrapWeightKg;
    }

    @JsonProperty("scrapWeight")
    public BigDecimal getScrapWeightAlias() {
        return totalScrapWeightKg;
    }

    // FE alias for scrap logs (kept same name, explicit getter for stability)
    @JsonProperty("scrapWeightLogs")
    public List<ScrapWeightLogDto> getScrapWeightLogsAlias() {
        return scrapWeightLogs;
    }
}
