package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "checked_machine_logs")
public class CheckedMachineLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "report_id", nullable = false)
    private ProductionReport report;

    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false)
    private ParameterChecklist checklistItem;

    @ManyToOne
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(name = "actual_value")
    private BigDecimal actualValue;

    @Column(name = "time_record", nullable = false)
    private LocalTime timeRecord;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}