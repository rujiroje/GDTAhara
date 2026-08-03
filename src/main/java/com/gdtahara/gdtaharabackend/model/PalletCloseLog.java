package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pallet_close_log")
public class PalletCloseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id", nullable = false)
    private Pallet pallet;

    @Column(nullable = false, length = 30)
    private String action;  // CREATED BOX_ADDED BOX_REMOVED CLOSED PRINTED REOPENED REARRANGED_FROM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_lot_id")
    private SubLot subLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_pallet_id")
    private Pallet refPallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) performedAt = LocalDateTime.now();
    }
}
