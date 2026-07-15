package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.MachineSetupJob;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SetupJobResponse(
        Long id,
        Long machineId,
        String machineName,
        String machineCode,
        String machineType,
        Long fromProductId,
        String fromProductCode,
        Long toProductId,
        String toProductCode,
        Long productionPlanId,
        LocalDate planDate,
        LocalTime requiredBefore,
        Long assignedToUserId,
        String assignedToUsername,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer durationMin,
        Boolean moldChanged,
        String moldCodeFrom,
        String moldCodeTo,
        Boolean tempAdjusted,
        Boolean cycleAdjusted,
        Boolean blowPinAligned,
        Boolean fpiPassed,
        String skipReason,
        String notes,
        Long completedByUserId,
        String completedByUsername
) {
    public static SetupJobResponse from(MachineSetupJob j) {
        return new SetupJobResponse(
                j.getId(),
                j.getMachine() != null ? j.getMachine().getId() : null,
                j.getMachine() != null ? j.getMachine().getMachineName() : null,
                j.getMachine() != null ? j.getMachine().getMachineCode() : null,
                j.getMachine() != null ? j.getMachine().getMachineType() : null,
                j.getFromProduct() != null ? j.getFromProduct().getId() : null,
                j.getFromProduct() != null ? j.getFromProduct().getProductCode() : null,
                j.getToProduct() != null ? j.getToProduct().getId() : null,
                j.getToProduct() != null ? j.getToProduct().getProductCode() : null,
                j.getProductionPlan() != null ? j.getProductionPlan().getId() : null,
                j.getPlanDate(),
                j.getRequiredBefore(),
                j.getAssignedTo() != null ? j.getAssignedTo().getId() : null,
                j.getAssignedTo() != null ? j.getAssignedTo().getUsername() : null,
                j.getStatus(),
                j.getStartedAt(),
                j.getCompletedAt(),
                j.getDurationMin(),
                j.getMoldChanged(),
                j.getMoldCodeFrom(),
                j.getMoldCodeTo(),
                j.getTempAdjusted(),
                j.getCycleAdjusted(),
                j.getBlowPinAligned(),
                j.getFpiPassed(),
                j.getSkipReason(),
                j.getNotes(),
                j.getCompletedBy() != null ? j.getCompletedBy().getId() : null,
                j.getCompletedBy() != null ? j.getCompletedBy().getUsername() : null
        );
    }
}
