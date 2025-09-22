// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/NgLogRepository.java
// (ฉบับสมบูรณ์ล่าสุด)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.NgLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.gdtahara.gdtaharabackend.dto.NgTypeSummaryDto;

@Repository
public interface NgLogRepository extends JpaRepository<NgLog, Long> {

    List<NgLog> findByReportId(Long reportId);

    // เมธอดสำหรับนับจำนวน (ใช้ในการตรวจสอบสิทธิ์)
    long countByReportId(Long reportId);

    // เมธอดสำหรับรวมจำนวน NG (ใช้ใน Dashboard)
    @Query("SELECT COALESCE(SUM(n.quantity), 0) FROM NgLog n WHERE n.report.id = :reportId")
    Long sumQuantityByReportId(@Param("reportId") Long reportId);

    // เมธอดสำหรับดึงข้อมูลตามช่วงเวลา (ใช้ใน Shift Leader Dashboard)
    List<NgLog> findByReportIdAndTimestampBetween(Long reportId, LocalDateTime start, LocalDateTime end);
    List<NgLog> findByReportIdAndSource(Long reportId, String source);

    // Added for historical report
    List<NgLog> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<NgLog> findByReportIdInAndTimestampBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(nl.quantity) FROM NgLog nl WHERE nl.report.id IN :reportIds")
    Long sumQuantityForReports(@Param("reportIds") List<Long> reportIds);

    @Query("SELECT new com.gdtahara.gdtaharabackend.dto.NgTypeSummaryDto(" +
           "nl.ngType.ngDescriptionTh, SUM(nl.quantity)) " +
           "FROM NgLog nl WHERE nl.report.id IN :reportIds " +
           "GROUP BY nl.ngType.ngDescriptionTh")
    List<NgTypeSummaryDto> findNgSummaryByReportIds(@Param("reportIds") List<Long> reportIds);

    // เพิ่มเมธอดสำหรับการ Query ข้อมูลแบบมีประสิทธิภาพ
    @Query("SELECT l.report.id, SUM(l.quantity) FROM NgLog l WHERE l.report.id IN :reportIds GROUP BY l.report.id")
    List<Object[]> countNgQuantityByReportIds(@Param("reportIds") List<Long> reportIds);
}