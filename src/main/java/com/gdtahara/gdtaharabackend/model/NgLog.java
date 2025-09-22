// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/NgLog.java
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ng_logs")
public class NgLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "report_id", nullable = false)
    private ProductionReport report;

    @ManyToOne
    @JoinColumn(name = "ng_type_id", nullable = false)
    private NgType ngType;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String source;
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}