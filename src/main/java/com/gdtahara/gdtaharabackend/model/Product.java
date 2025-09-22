// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/Product.java
// (**อัปเดตให้มี Field ครบถ้วน**)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "std_weight")
    private BigDecimal stdWeight;

    @Column(name = "cycle_time")
    private BigDecimal cycleTime;

    private Integer cavity;
    private Integer qtyPerBox;
    
    @Column(name = "b_weight")
    private BigDecimal bWeight;

    @Column(name = "b_weight_var")
    private String bWeightVar;

    @Column(name = "pdi_main")
    private BigDecimal pdiMain;

    @Column(name = "pdi_virgin")
    private BigDecimal pdiVirgin;

    @Column(name = "pdi_evoh")
    private BigDecimal pdiEvoh;

    @Column(name = "pdi_adm")
    private BigDecimal pdiAdm;

    private Integer qtyPerBag;
    private Integer qtyPerPallet;
}