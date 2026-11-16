package com.gdtahara.gdtaharabackend.dto.waste;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EditRequestStatusDto(
    Long id,
    Long recordId,
    String requesterId,
    LocalDateTime requestedAt,
    String requestedWasteTypeCode,
    BigDecimal requestedWeightKg,
    String requestedNote,
    String reason,
    String status,
    String docId,
    LocalDateTime docRespondedAt,
    String docNote
) {}
