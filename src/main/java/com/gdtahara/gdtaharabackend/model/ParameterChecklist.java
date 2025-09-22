// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/ParameterChecklist.java
// (**สร้างไฟล์ใหม่** ใน package model)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "parameter_checklists")
public class ParameterChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_type", nullable = false)
    private String machineType; // e.g., "RBL"

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "standard_value")
    private String standardValue;

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    private String unit;
}