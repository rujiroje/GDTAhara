package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.SubLot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubLotResponse(
        Long id,
        Long productionReportId,
        String subLotNumber,
        String palletNumber,
        Integer boxQuantity,
        BigDecimal weightKg,
        LocalDateTime confirmedAt,
        Long confirmedByUserId,
        String confirmedByUsername,
        String status,
        String zplLabelPrintedRef
) {
    public static SubLotResponse from(SubLot s) {
        return new SubLotResponse(
                s.getId(),
                s.getProductionReport() != null ? s.getProductionReport().getId() : null,
                s.getSubLotNumber(),
                s.getPalletNumber(),
                s.getBoxQuantity(),
                s.getWeightKg(),
                s.getConfirmedAt(),
                s.getConfirmedBy() != null ? s.getConfirmedBy().getId() : null,
                s.getConfirmedBy() != null ? s.getConfirmedBy().getUsername() : null,
                s.getStatus(),
                s.getZplLabelPrintedRef()
        );
    }
}
