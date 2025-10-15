package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ProductionReportRepository productionReportRepository;

    /**
     * ดึงรายงานการผลิตทั้งหมด
     */
    public List<ProductionReport> getAllReports() {
        try {
            logger.info("Getting all production reports");
            return productionReportRepository.findAll();
        } catch (Exception e) {
            logger.error("Error getting all reports: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get reports: " + e.getMessage(), e);
        }
    }

    /**
     * ดึงรายงานการผลิตตาม ID
     */
    public ProductionReport getReportById(Long id) {
        try {
            logger.info("Getting production report with id: {}", id);
            return productionReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + id));
        } catch (Exception e) {
            logger.error("Error getting report by id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get report: " + e.getMessage(), e);
        }
    }

    /**
     * นับจำนวนรายงานทั้งหมด
     */
    public long getTotalReportsCount() {
        try {
            return productionReportRepository.count();
        } catch (Exception e) {
            logger.error("Error counting reports: {}", e.getMessage(), e);
            return 0;
        }
    }
}