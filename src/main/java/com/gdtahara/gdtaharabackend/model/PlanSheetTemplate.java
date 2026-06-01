package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "plan_sheet_template")
public class PlanSheetTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factory_code", unique = true, nullable = false, length = 20)
    private String factoryCode;

    @Column(name = "sheet_name_hint", length = 100)
    private String sheetNameHint;

    /** 1-based column index of the machine-code column. */
    @Column(name = "machine_col_index", nullable = false)
    private Integer machineColIndex;

    /** 1-based column index of the product-code / sub-row-type column. */
    @Column(name = "product_col_index", nullable = false)
    private Integer productColIndex;

    /** 1-based row index of the date-header row. */
    @Column(name = "date_header_row", nullable = false)
    private Integer dateHeaderRow;

    /** 1-based row index where data rows begin. */
    @Column(name = "data_start_row", nullable = false)
    private Integer dataStartRow;

    /** Pipe-separated keywords (e.g. "Remark|Actual|Plan|Diff") that mark sub-rows to skip. */
    @Column(name = "skip_row_keywords", length = 500)
    private String skipRowKeywords;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
