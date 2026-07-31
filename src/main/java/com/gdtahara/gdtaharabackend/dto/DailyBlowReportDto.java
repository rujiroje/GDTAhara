package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyBlowReportDto {

    // ── Header ───────────────────────────────────────────────────────────────
    private String reportDate;
    private String machineName;
    private String productCode;
    private String productName;
    private String orderNumber;
    private String parentLotNumber;
    private Integer targetQty;
    private String shift;

    // ── Section 1: Productivity ───────────────────────────────────────────────
    private long totalBoxesPacked;
    private long totalNgQty;

    // ── Section 2: Packing Materials (UNBW BOM × boxes packed) ───────────────
    private List<PackingMaterialRow> packingMaterials;

    // ── Section 3: Raw Material Usage ────────────────────────────────────────
    private List<RawMaterialRow> rawMaterials;

    // ── Section 4: Labor (users who recorded data today) ─────────────────────
    private List<String> activeUsers;

    // ── Section 5: NG Details + Scrap Weight ─────────────────────────────────
    private List<NgRow> ngDetails;
    private List<ScrapRow> scrapDetails;
    private List<ScrapComparisonRow> scrapComparison;

    // ── Section 6: Downtime ───────────────────────────────────────────────────
    private List<DowntimeRow> downtimeEvents;
    private long totalDowntimeMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // Nested row types
    // ─────────────────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PackingMaterialRow {
        private String rmCode;
        private String rmName;
        private String unit;
        private BigDecimal qtyPerBox;
        private long boxesPacked;
        private BigDecimal totalQty;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RawMaterialRow {
        private String materialCode;
        private String materialName;
        private String materialType;
        private String unit;
        private BigDecimal totalUsed;
        private String lotNumber;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class NgRow {
        private String ngType;
        private String ngDescription;
        private String source;
        private long quantity;
        private String note;
        private String timestamp;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ScrapRow {
        private String scrapType;
        private String matType;
        private BigDecimal weightKg;
        private String technicianName;
        private String timestamp;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ScrapComparisonRow {
        private String materialType;
        private String rmName;
        private BigDecimal expectedKg;  // BOM standard × boxes packed / baseQty
        private BigDecimal actualKg;    // sum from ScrapWeightLog
        private BigDecimal varianceKg;  // actual − expected (positive = excess scrap)
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DowntimeRow {
        private String category;
        private String downtimeType;
        private String reason;
        private String solution;
        private String startTime;
        private String endTime;
        private long durationMinutes;
    }
}
