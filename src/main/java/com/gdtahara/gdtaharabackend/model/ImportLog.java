package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "import_log")
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, length = 200)
    private String filename;

    @Column(name = "factory_code", length = 20)
    private String factoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    private User importedBy;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "rows_added", nullable = false)
    private Integer rowsAdded;

    @Column(name = "rows_skipped_past", nullable = false)
    private Integer rowsSkippedPast;

    @Column(name = "rows_skipped_started", nullable = false)
    private Integer rowsSkippedStarted;

    @Column(name = "rows_updated", nullable = false)
    private Integer rowsUpdated;

    @Column(name = "errors_json", columnDefinition = "NVARCHAR(MAX)")
    private String errorsJson;

    @PrePersist
    protected void onCreate() {
        if (this.importedAt == null) this.importedAt = LocalDateTime.now();
        if (this.rowsAdded == null) this.rowsAdded = 0;
        if (this.rowsSkippedPast == null) this.rowsSkippedPast = 0;
        if (this.rowsSkippedStarted == null) this.rowsSkippedStarted = 0;
        if (this.rowsUpdated == null) this.rowsUpdated = 0;
    }
}
