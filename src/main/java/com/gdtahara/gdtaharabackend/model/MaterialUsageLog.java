package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "material_usage_logs")
public class MaterialUsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "report_id", nullable = false)
    private ProductionReport report;

    @ManyToOne @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(name = "material_code", nullable = false)
    private String materialCode;
    
    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "quantity_kg", nullable = false)
    private BigDecimal quantityKg;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}