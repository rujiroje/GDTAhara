package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bom_item")
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BillOfMaterials bom;

    @Column(name = "item_number", nullable = false)
    private Integer itemNumber;

    @Column(name = "rm_code", nullable = false, length = 50)
    private String rmCode;

    @Column(name = "rm_name", length = 200)
    private String rmName;

    @Column(name = "quantity_per", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityPer;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "loss_percent", precision = 5, scale = 2)
    private BigDecimal lossPercent;

    @Column(name = "material_type", nullable = false, length = 10)
    private String materialType;

    @Column(name = "is_scrap", nullable = false)
    private Boolean isScrap = false;

    @Column(name = "bom_status", nullable = false)
    private Integer bomStatus = 1;

    @Column(name = "notes", length = 500)
    private String notes;
}
