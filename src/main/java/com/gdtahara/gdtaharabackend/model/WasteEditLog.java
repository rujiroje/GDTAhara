package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "waste_edit_log")
public class WasteEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "edit_type", nullable = false)
    private String editType; // SELF_EDIT | DOC_APPROVED

    @Column(name = "edited_by", nullable = false)
    private String editedBy;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    @Column(name = "old_machine_id")
    private String oldMachineId;

    @Column(name = "old_wo_number")
    private String oldWoNumber;

    @Column(name = "old_waste_type_code")
    private String oldWasteTypeCode;

    @Column(name = "old_weight_kg")
    private BigDecimal oldWeightKg;

    @Column(name = "old_note")
    private String oldNote;

    @Column(name = "new_machine_id")
    private String newMachineId;

    @Column(name = "new_wo_number")
    private String newWoNumber;

    @Column(name = "new_waste_type_code")
    private String newWasteTypeCode;

    @Column(name = "new_weight_kg")
    private BigDecimal newWeightKg;

    @Column(name = "new_note")
    private String newNote;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "edit_request_id")
    private Long editRequestId;

    @PrePersist
    protected void onCreate() {
        if (this.editedAt == null) this.editedAt = LocalDateTime.now();
    }
}
