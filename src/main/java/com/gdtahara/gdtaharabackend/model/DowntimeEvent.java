// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/DowntimeEvent.java
// (**สร้างไฟล์ใหม่** ใน package model)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "downtime_events")
public class DowntimeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "report_id", nullable = false)
    private ProductionReport report;

    @ManyToOne @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String reason;

    @Column(name = "solution", columnDefinition = "NVARCHAR(MAX)")
    private String solution;

    // PLANNED | UNPLANNED | EXTERNAL
    @Column(name = "category", length = 50)
    private String category;

    // PM, MOLD_CHANGE, SETUP, BREAKDOWN, MATERIAL_SHORT, NO_ORDER, etc.
    @Column(name = "downtime_type", length = 100)
    private String downtimeType;

    @Transient
    private LocalDateTime timestamp;
}