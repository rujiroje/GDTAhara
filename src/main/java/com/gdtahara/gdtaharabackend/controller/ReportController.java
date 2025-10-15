package com.gdtahara.gdtaharabackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List; // Added import for List
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/reports") // เปลี่ยนจาก "/api/pc" เป็น "/api/reports"
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @GetMapping("/production") // เปลี่ยนจาก "/reports" เป็น "/production"
    public ResponseEntity<?> getReports() {
        logger.info("Fetching reports...");
        return ResponseEntity.ok(List.of("Report A", "Report B", "Report C"));
    }
}
