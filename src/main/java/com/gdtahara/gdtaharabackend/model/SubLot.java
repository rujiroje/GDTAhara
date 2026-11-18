package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sub_lot")
public class SubLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_report_id", nullable = false)
    private ProductionReport productionReport;

    @Column(name = "sub_lot_number", nullable = false, unique = true, length = 100)
    private String subLotNumber;

    @Column(name = "pallet_number", length = 50)
    private String palletNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private Pallet pallet;

    @Column(name = "box_quantity", nullable = false)
    private Integer boxQuantity;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_id", nullable = false)
    private User confirmedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "zpl_label_printed_ref", length = 100)
    private String zplLabelPrintedRef;

    @PrePersist
    protected void onCreate() {
        if (this.confirmedAt == null) this.confirmedAt = LocalDateTime.now();
        if (this.status == null) this.status = "draft";
    }
}
