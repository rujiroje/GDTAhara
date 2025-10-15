// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/ScrapWeightLogRepository.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ScrapWeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScrapWeightLogRepository extends JpaRepository<ScrapWeightLog, Long> {

    // Added for single report scrap weight logs
    List<ScrapWeightLog> findByReportId(Long reportId);

    // Added for historical report
    List<ScrapWeightLog> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<ScrapWeightLog> findByReportIdInAndTimestampBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(swl.weightKg) FROM ScrapWeightLog swl WHERE swl.report.id IN :reportIds")
    BigDecimal sumWeightForReports(@Param("reportIds") List<Long> reportIds);

    @Query("SELECT l.report.id, SUM(l.weightKg) FROM ScrapWeightLog l WHERE l.report.id IN :reportIds GROUP BY l.report.id")
    List<Object[]> sumScrapWeightByReportIds(@Param("reportIds") List<Long> reportIds);

    @Query("SELECT COALESCE(SUM(s.weightKg), 0) FROM ScrapWeightLog s WHERE s.report.id = :reportId")
    BigDecimal sumWeightByReportId(@Param("reportId") Long reportId);
}