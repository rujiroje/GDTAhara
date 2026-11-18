package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "waste_weight_record")
public class WasteWeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(name = "wo_number")
    private String woNumber;

    @Column(name = "waste_type_code", nullable = false)
    private String wasteTypeCode;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    @Column(name = "note")
    private String note;

    @Column(name = "photo_ids", columnDefinition = "NVARCHAR(MAX)")
    private String photoIds;

    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "shift_code", nullable = false)
    private String shiftCode;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "last_edited_by")
    private String lastEditedBy;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    @Column(name = "edit_count", nullable = false)
    private Integer editCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) this.recordedAt = LocalDateTime.now();
    }
}
