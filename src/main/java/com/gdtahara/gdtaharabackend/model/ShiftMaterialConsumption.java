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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
