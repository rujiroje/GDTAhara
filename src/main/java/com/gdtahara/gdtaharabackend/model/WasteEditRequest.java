package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "waste_edit_request")
public class WasteEditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "requester_id", nullable = false)
    private String requesterId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "requested_machine_id")
    private String requestedMachineId;

    @Column(name = "requested_wo_number", nullable = false)
    private String requestedWoNumber;

    @Column(name = "requested_waste_type_code")
    private String requestedWasteTypeCode;

    @Column(name = "requested_weight_kg")
    private BigDecimal requestedWeightKg;

    @Column(name = "requested_note")
    private String requestedNote;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "request_photo_ids", columnDefinition = "NVARCHAR(MAX)")
    private String requestPhotoIds;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING | APPROVED | REJECTED | CANCELLED

    @Column(name = "doc_id")
    private String docId;

    @Column(name = "doc_responded_at")
    private LocalDateTime docRespondedAt;

    @Column(name = "doc_note")
    private String docNote;

    @Column(name = "telegram_sent")
    private Boolean telegramSent = false;

    @Column(name = "telegram_sent_at")
    private LocalDateTime telegramSentAt;

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) this.requestedAt = LocalDateTime.now();
    }
}
