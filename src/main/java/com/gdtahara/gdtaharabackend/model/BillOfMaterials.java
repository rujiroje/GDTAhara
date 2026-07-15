package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bill_of_materials")
public class BillOfMaterials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fg_code", nullable = false, length = 50)
    private String fgCode;

    @Column(name = "alternative_number", nullable = false)
    private Integer alternativeNumber = 1;

    @Column(name = "plant_code", nullable = false, length = 20)
    private String plantCode = "5100";

    @Column(name = "material_group", length = 50)
    private String materialGroup;

    @Column(name = "sap_material_type", length = 10)
    private String sapMaterialType;

    @Column(name = "base_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal baseQuantity = BigDecimal.ONE;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit = "PCS";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "imported_from_file", length = 200)
    private String importedFromFile;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    private User importedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bom", fetch = FetchType.LAZY)
    private List<BomItem> items;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.importedAt = now;
        this.createdAt = now;
    }
}
