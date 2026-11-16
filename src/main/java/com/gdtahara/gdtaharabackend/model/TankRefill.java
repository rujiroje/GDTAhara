package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tank_refill")
public class TankRefill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tankId;
    private String machineId;
    private String materialCode;
    private Long materialStockId; // FK to material_stock_transactions, nullable
    private String materialLotCode;
    private BigDecimal qtyKg;
    private Boolean isCorrection = false;
    private Long correctsRefillId; // nullable
    private String refillBy;
    private LocalDateTime refillAt;
    private String shiftCode;
    private LocalDate shiftDate;
    private Boolean materialTypeOverride = false;
    private String note;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String photoIds;

    @PrePersist
    protected void onCreate() {
        if (this.refillAt == null) this.refillAt = LocalDateTime.now();
    }
}
