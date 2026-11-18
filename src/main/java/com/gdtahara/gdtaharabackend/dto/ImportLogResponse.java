package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.ImportLog;

import java.time.LocalDateTime;

public record ImportLogResponse(
        Long id,
        String filename,
        String factoryCode,
        Long importedByUserId,
        String importedByUsername,
        LocalDateTime importedAt,
        Integer rowsAdded,
        Integer rowsSkippedPast,
        Integer rowsSkippedStarted,
        Integer rowsUpdated,
        String errorsJson
) {
    public static ImportLogResponse from(ImportLog l) {
        return new ImportLogResponse(
                l.getId(),
                l.getFilename(),
                l.getFactoryCode(),
                l.getImportedBy() != null ? l.getImportedBy().getId() : null,
                l.getImportedBy() != null ? l.getImportedBy().getUsername() : null,
                l.getImportedAt(),
                l.getRowsAdded(),
                l.getRowsSkippedPast(),
                l.getRowsSkippedStarted(),
                l.getRowsUpdated(),
                l.getErrorsJson()
        );
    }
}
