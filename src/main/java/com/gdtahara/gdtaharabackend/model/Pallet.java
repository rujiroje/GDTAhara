package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pallet")
public class Pallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pallet_number", nullable = false, length = 50)
    private String palletNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "target_qty")
    private Integer targetQty;

    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty = 0;

    @Column(name = "box_count", nullable = false)
    private Integer boxCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";   // OPEN · CLOSED · PRINTED · CANCELLED

    @Column(name = "pallet_date", nullable = false)
    private LocalDate palletDate;

    @Column(name = "lot_date_min")
    private LocalDate lotDateMin;

    @Column(name = "lot_date_max")
    private LocalDate lotDateMax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private User closedBy;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "print_count", nullable = false)
    private Integer printCount = 0;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_pallet_id")
    private Pallet parentPallet;

    @Column(name = "rearrange_reason", length = 500)
    private String rearrangeReason;

    @Column(name = "revision_suffix", nullable = false, length = 10)
    private String revisionSuffix = "";

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "pallet", fetch = FetchType.LAZY)
    private List<SubLot> subLots = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (palletDate == null) palletDate = LocalDate.now();
        if (actualQty == null) actualQty = 0;
        if (boxCount == null) boxCount = 0;
        if (printCount == null) printCount = 0;
        if (revisionSuffix == null) revisionSuffix = "";
    }
}
