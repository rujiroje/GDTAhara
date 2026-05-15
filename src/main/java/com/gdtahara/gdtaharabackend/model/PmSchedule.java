package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pm_schedules")
public class PmSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "last_done_date")
    private LocalDate lastDoneDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    // UPCOMING | DUE | OVERDUE | DONE
    @Column(nullable = false, length = 20)
    private String status = "UPCOMING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        recalculateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateStatus();
    }

    // คำนวณ status จาก next_due_date อัตโนมัติ
    public void recalculateStatus() {
        if ("DONE".equals(this.status)) return;
        LocalDate today = LocalDate.now();
        if (nextDueDate == null) return;
        if (nextDueDate.isBefore(today)) {
            this.status = "OVERDUE";
        } else if (!nextDueDate.isAfter(today.plusDays(7))) {
            this.status = "DUE";
        } else {
            this.status = "UPCOMING";
        }
    }
}
