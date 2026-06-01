package com.gdtahara.gdtaharabackend.dto;

import java.util.List;

public record ImportResult(
        String filename,
        String factoryCode,
        String sheetName,
        int rowsAdded,
        int rowsUpdated,
        int rowsSkippedPast,
        int rowsSkippedStarted,
        String status,
        String message,
        List<String> warnings
) {}
