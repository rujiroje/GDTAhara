package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shift_material_consumption",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tank_id", "shift_date", "shift_code"}))
public class ShiftMaterialConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tank_id")
    private String tankId;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "material_code")
    private String materialCode;

    @Column(name = "shift_code")
    private String shiftCode;

    @Column(name = "shift_date")
    private LocalDate shiftDate;

    private BigDecimal startingMeterKg;
    private BigDecimal totalRefillsKg;
    private BigDecimal endingMeterKg;
    private BigDecimal computedUsedKg;
    private Integer refillCount = 0;
    private Integer correctionCount = 0;
    private Boolean isFinal = false;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @Column(name = "computed_by")
    private String computedBy;

    @PrePersist
    protected void onCreate() {
        if (this.computedAt == null) this.computedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.computedAt = LocalDateTime.now();
    }
}
