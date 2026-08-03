package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.SetupActivityCode;

public record SetupActivityCodeDto(
        Long id,
        String code,
        String descriptionTh,
        String descriptionEn,
        String category,
        String colorHex,
        Boolean isActive,
        Integer displayOrder
) {
    public static SetupActivityCodeDto from(SetupActivityCode c) {
        return new SetupActivityCodeDto(
                c.getId(), c.getCode(), c.getDescriptionTh(), c.getDescriptionEn(),
                c.getCategory(), c.getColorHex(), c.getIsActive(), c.getDisplayOrder()
        );
    }
}
