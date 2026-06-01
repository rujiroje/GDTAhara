// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/ProductionReport.java
// (ฉบับแก้ไข)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "production_reports")
public class ProductionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // **[ใหม่]** เพิ่ม Order Number
    @Column(name = "order_number")
    private String orderNumber;

    // **[แก้ไข]** เปลี่ยนจาก productionDate เป็น startDate และ endDate
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "target_qty")
    private Integer targetQty;

    @ManyToOne
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "pc_id", nullable = false)
    private User pc;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id", nullable = true)
    private ProductionPlan productionPlan;

    @Column(name = "parent_lot_number", nullable = true, unique = true, length = 100)
    private String parentLotNumber;

    @Column(name = "shift", nullable = true, length = 20)
    private String shift;

    @Column(name = "actual_qty", nullable = true)
    private Integer actualQty;

    @Column(name = "diff_qty", insertable = false, updatable = false)
    private Integer diffQty;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}