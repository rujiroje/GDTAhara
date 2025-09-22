package com.gdtahara.gdtaharabackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pc/dashboard-summary")
public class DashboardSummaryController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSummaryController.class);

    @GetMapping
    public Map<String, Object> getDashboardSummary() {
        List<Map<String, Object>> dashboardData = List.of(
            Map.of(
                "reportId", 1,
                "machineName", "Machine A",
                "productName", "Product A",
                "targetQty", 1000,
                "currentGoodQty", 800,
                "currentNgQty", 50,
                "scrapWeight", 10.5,
                "downtimeMinutes", 120
            ),
            Map.of(
                "reportId", 2,
                "machineName", "Machine B",
                "productName", "Product B",
                "targetQty", 2000,
                "currentGoodQty", 1500,
                "currentNgQty", 100,
                "scrapWeight", 20.0,
                "downtimeMinutes", 90
            )
        );

        logger.info("Dashboard data type: {}", dashboardData.getClass().getName()); // Log the dashboardData type
        logger.info("Dashboard data content: {}", dashboardData); // Log the dashboardData content

        if (dashboardData == null || dashboardData.isEmpty()) {
            logger.warn("Dashboard data is null or empty");
        } else {
            logger.info("Dashboard data retrieved successfully");
        }

        // Ensure dashboardData is always returned as an array
        return Map.of("status", "success", "dashboardData", List.copyOf(dashboardData));
    }
}

