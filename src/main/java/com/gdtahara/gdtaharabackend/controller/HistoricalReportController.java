package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.HistoricalReportSummaryDto; // Add this import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoricalReportController {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalReportController.class);

    @GetMapping("/api/pc/reports/historical")
    public HistoricalReportSummaryDto getHistoricalReports(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        logger.info("Fetching historical reports from {} to {}", startDate, endDate);
        // ...existing code...
        return new HistoricalReportSummaryDto(); // Replace with actual logic
    }
}