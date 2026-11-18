package com.gdtahara.gdtaharabackend.dto;

import java.time.LocalDateTime;

public record SetupTimeLogRequest(
        Long activityCodeId,
        LocalDateTime startTime,
        String description
) {}
