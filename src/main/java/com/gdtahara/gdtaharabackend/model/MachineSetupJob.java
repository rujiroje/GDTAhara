package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "machine_setup_job")
public class MachineSetupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_product_id", nullable = true)
    private Product fromProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_product_id", nullable = false)
    private Product toProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id", nullable = false)
    private ProductionPlan productionPlan;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "required_before")
    private LocalTime requiredBefore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", nullable = true)
    private User assignedTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "mold_changed", nullable = false)
    private Boolean moldChanged;

    @Column(name = "mold_code_from", length = 50)
    private String moldCodeFrom;

    @Column(name = "mold_code_to", length = 50)
    private String moldCodeTo;

    @Column(name = "temp_adjusted", nullable = false)
    private Boolean tempAdjusted;

    @Column(name = "cycle_adjusted", nullable = false)
    private Boolean cycleAdjusted;

    @Column(name = "blow_pin_aligned", nullable = false)
    private Boolean blowPinAligned;

    @Column(name = "fpi_passed", nullable = false)
    private Boolean fpiPassed;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id", nullable = true)
    private User completedBy;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = "PENDING";
        if (this.moldChanged == null) this.moldChanged = false;
        if (this.tempAdjusted == null) this.tempAdjusted = false;
        if (this.cycleAdjusted == null) this.cycleAdjusted = false;
        if (this.blowPinAligned == null) this.blowPinAligned = false;
        if (this.fpiPassed == null) this.fpiPassed = false;
    }
}
