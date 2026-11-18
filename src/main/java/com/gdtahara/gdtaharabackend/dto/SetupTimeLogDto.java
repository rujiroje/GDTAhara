package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.SetupTimeLog;
import java.time.LocalDateTime;

public record SetupTimeLogDto(
        Long id,
        Long setupJobId,
        Long activityCodeId,
        String activityCode,
        String activityDescTh,
        String colorHex,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMin,
        Integer sequenceNo,
        String description,
        String createdBy,
        LocalDateTime createdAt
) {
    public static SetupTimeLogDto from(SetupTimeLog l) {
        return new SetupTimeLogDto(
                l.getId(),
                l.getSetupJob().getId(),
                l.getActivityCode().getId(),
                l.getActivityCode().getCode(),
                l.getActivityCode().getDescriptionTh(),
                l.getActivityCode().getColorHex(),
                l.getStartTime(),
                l.getEndTime(),
                l.getDurationMin(),
                l.getSequenceNo(),
                l.getDescription(),
                l.getCreatedBy() != null ? l.getCreatedBy().getUsername() : null,
                l.getCreatedAt()
        );
    }
}
