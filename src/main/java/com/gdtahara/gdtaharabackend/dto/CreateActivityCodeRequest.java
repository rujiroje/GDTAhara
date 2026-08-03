package com.gdtahara.gdtaharabackend.dto;

public record CreateActivityCodeRequest(
        String code,
        String descriptionTh,
        String descriptionEn,
        String category,
        String colorHex,
        Integer displayOrder
) {}
