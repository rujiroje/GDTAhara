// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/ScrapWeightLog.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scrap_weight_logs")
public class ScrapWeightLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "report_id", nullable = false)
    private ProductionReport report;

    @ManyToOne
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(name = "scrap_type", nullable = false)
    private String scrapType;

    @Column(name = "mat_type")
    private String matType;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}