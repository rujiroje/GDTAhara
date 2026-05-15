package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_code", unique = true, nullable = false, length = 50)
    private String recipeCode;

    @Column(name = "recipe_name", nullable = false, length = 200)
    private String recipeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // NULL = ใช้ได้กับทุกเครื่อง
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(nullable = false, length = 20)
    private String version = "1.0";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "target_cycle_time_sec", precision = 8, scale = 2)
    private BigDecimal targetCycleTimeSec;

    @Column(name = "target_temp_zone1", precision = 6, scale = 2)
    private BigDecimal targetTempZone1;

    @Column(name = "target_temp_zone2", precision = 6, scale = 2)
    private BigDecimal targetTempZone2;

    @Column(name = "target_temp_head", precision = 6, scale = 2)
    private BigDecimal targetTempHead;

    @Column(name = "target_blow_pressure", precision = 6, scale = 2)
    private BigDecimal targetBlowPressure;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
