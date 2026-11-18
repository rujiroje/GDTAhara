package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mold_master")
public class MoldMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String moldCode;

    @Column(nullable = false, length = 200)
    private String moldName;

    @Column(length = 200)
    private String moldNameEn;

    @Column(nullable = false)
    private Integer cavityCount = 1;

    /** Comma-separated cavity numbers, e.g. "1,2,3,4" */
    @Column(nullable = false, length = 200)
    private String cavityNumbers;

    @Column(length = 50)
    private String machineType;

    /** JSON array of compatible product codes, e.g. ["NBRZS0500","NBRZS1000"] */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String compatibleProducts;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, length = 50)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
