package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "material_stock_transactions")
public class MaterialStockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private String transactionType; // "IN" or "OUT"
    private BigDecimal quantity;
    private String lotNumber;

    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne @JoinColumn(name = "production_report_id")
    private ProductionReport productionReport;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_item_id", nullable = true)
    private BomItem bomItem;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}