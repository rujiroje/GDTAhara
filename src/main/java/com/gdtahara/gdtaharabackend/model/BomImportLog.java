package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bom_import_log")
public class BomImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false, length = 200)
    private String filename;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "factory_code", length = 20)
    private String factoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    private User importedBy;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "rows_added", nullable = false)
    private Integer rowsAdded = 0;

    @Column(name = "rows_updated", nullable = false)
    private Integer rowsUpdated = 0;

    @Column(name = "rows_skipped", nullable = false)
    private Integer rowsSkipped = 0;

    @Column(name = "rows_error", nullable = false)
    private Integer rowsError = 0;

    @Column(name = "errors_json", columnDefinition = "NVARCHAR(MAX)")
    private String errorsJson;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @PrePersist
    protected void onCreate() {
        this.importedAt = LocalDateTime.now();
    }
}
