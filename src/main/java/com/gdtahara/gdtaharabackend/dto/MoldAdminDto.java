package com.gdtahara.gdtaharabackend.dto;

public record MoldAdminDto(
        Long id,
        String moldCode,
        String moldName,
        String moldNameEn,
        String machineType,
        Long productId,
        String productCode,
        String moldType,
        Integer standardCavityCount,
        String standardCavityNo,
        Integer workCavityCount,
        String workCavityNo,
        Boolean isActive,
        String description
) {}
