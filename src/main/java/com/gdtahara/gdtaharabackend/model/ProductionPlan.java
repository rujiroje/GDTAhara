package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "production_plan")
public class ProductionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "target_qty", nullable = false)
    private Integer targetQty;

    @Column(name = "manpower_d_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal manpowerDRatio;

    @Column(name = "manpower_n_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal manpowerNRatio;

    @Column(name = "sap_wo_number", length = 50)
    private String sapWoNumber;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "excel_file_ref", length = 200)
    private String excelFileRef;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "draft";
        if (this.manpowerDRatio == null) this.manpowerDRatio = new BigDecimal("0.50");
        if (this.manpowerNRatio == null) this.manpowerNRatio = new BigDecimal("0.50");
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
