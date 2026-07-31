package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.DailyBlowReportDto;
import com.gdtahara.gdtaharabackend.service.DailyBlowReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final DailyBlowReportService dailyBlowReportService;

    public ReportController(DailyBlowReportService dailyBlowReportService) {
        this.dailyBlowReportService = dailyBlowReportService;
    }

    @GetMapping("/production")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin','Management','Document')")
    public ResponseEntity<?> getReports() {
        logger.info("Fetching reports...");
        return ResponseEntity.ok(List.of("Report A", "Report B", "Report C"));
    }

    /**
     * Daily Blow Report — ใบรายงานประจำวัน BLOW (RBL MACHINE)
     * GET /api/reports/blow-daily/{reportId}?date=YYYY-MM-DD
     */
    @GetMapping("/blow-daily/{reportId}")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin','Management','Document')")
    public ResponseEntity<DailyBlowReportDto> getBlowDailyReport(
            @PathVariable Long reportId,
            @RequestParam String date) {

        logger.info("Blow daily report: reportId={}, date={}", reportId, date);
        LocalDate localDate = LocalDate.parse(date);
        DailyBlowReportDto dto = dailyBlowReportService.buildReport(reportId, localDate);
        return ResponseEntity.ok(dto);
    }
}
