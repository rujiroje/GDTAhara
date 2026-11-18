package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductionPlanResponse(
        Long id,
        LocalDate planDate,
        Long machineId,
        String machineName,
        String machineCode,
        Long productId,
        String productName,
        String productCode,
        Integer targetQty,
        BigDecimal manpowerDRatio,
        BigDecimal manpowerNRatio,
        String sapWoNumber,
        String source,
        String excelFileRef,
        String status,
        Long createdByUserId,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductionPlanResponse from(ProductionPlan p) {
        return new ProductionPlanResponse(
                p.getId(),
                p.getPlanDate(),
                p.getMachine() != null ? p.getMachine().getId() : null,
                p.getMachine() != null ? p.getMachine().getMachineName() : null,
                p.getMachine() != null ? p.getMachine().getMachineCode() : null,
                p.getProduct() != null ? p.getProduct().getId() : null,
                p.getProduct() != null ? p.getProduct().getProductName() : null,
                p.getProduct() != null ? p.getProduct().getProductCode() : null,
                p.getTargetQty(),
                p.getManpowerDRatio(),
                p.getManpowerNRatio(),
                p.getSapWoNumber(),
                p.getSource(),
                p.getExcelFileRef(),
                p.getStatus(),
                p.getCreatedBy() != null ? p.getCreatedBy().getId() : null,
                p.getCreatedBy() != null ? p.getCreatedBy().getUsername() : null,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
