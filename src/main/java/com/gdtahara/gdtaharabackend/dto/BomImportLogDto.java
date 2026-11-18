package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.BomImportLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BomImportLogDto {

    private Long id;
    private String filename;
    private String fileType;
    private String factoryCode;
    private String importedBy;
    private LocalDateTime importedAt;
    private Integer rowsAdded;
    private Integer rowsUpdated;
    private Integer rowsSkipped;
    private Integer rowsError;
    private String errorsJson;
    private Integer durationMs;

    public static BomImportLogDto from(BomImportLog log) {
        return BomImportLogDto.builder()
                .id(log.getId())
                .filename(log.getFilename())
                .fileType(log.getFileType())
                .factoryCode(log.getFactoryCode())
                .importedBy(log.getImportedBy() != null ? log.getImportedBy().getUsername() : null)
                .importedAt(log.getImportedAt())
                .rowsAdded(log.getRowsAdded())
                .rowsUpdated(log.getRowsUpdated())
                .rowsSkipped(log.getRowsSkipped())
                .rowsError(log.getRowsError())
                .errorsJson(log.getErrorsJson())
                .durationMs(log.getDurationMs())
                .build();
    }
}
