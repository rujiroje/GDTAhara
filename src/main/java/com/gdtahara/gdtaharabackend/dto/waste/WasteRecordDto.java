package com.gdtahara.gdtaharabackend.dto.waste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WasteRecordDto(
    Long id,
    String machineId,
    String woNumber,
    String wasteTypeCode,
    BigDecimal weightKg,
    String note,
    String photoIds,
    String recordedBy,
    LocalDateTime recordedAt,
    String shiftCode,
    LocalDate shiftDate,
    Integer version,
    String lastEditedBy,
    LocalDateTime lastEditedAt,
    Integer editCount,
    String pendingRequestStatus
) {}
