package com.gdtahara.gdtaharabackend.dto.waste;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WasteEditLogDto(
    Long id,
    String editType,
    String editedBy,
    LocalDateTime editedAt,
    String oldWasteTypeCode,
    BigDecimal oldWeightKg,
    String oldNote,
    String newWasteTypeCode,
    BigDecimal newWeightKg,
    String newNote,
    String reason
) {}
