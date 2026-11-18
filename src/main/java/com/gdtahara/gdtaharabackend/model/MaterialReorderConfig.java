package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "material_reorder_config")
public class MaterialReorderConfig {

    @Id
    @Column(name = "material_code")
    private String materialCode;

    @Column(name = "reorder_point_kg", nullable = false)
    private BigDecimal reorderPointKg;

    @Column(name = "safety_stock_kg")
    private BigDecimal safetyStockKg;

    @Column(name = "reorder_qty_kg")
    private BigDecimal reorderQtyKg;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
