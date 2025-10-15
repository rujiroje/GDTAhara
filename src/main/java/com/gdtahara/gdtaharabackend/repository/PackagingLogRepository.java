// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/PackagingLogRepository.java
// (ฉบับสมบูรณ์ล่าสุด)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.PackagingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PackagingLogRepository extends JpaRepository<PackagingLog, Long> {

    // เมธอดสำหรับนับจำนวน (ใช้ใน Dashboard และการตรวจสอบสิทธิ์)
    long countByReportId(Long reportId);

    // เมธอดสำหรับหา Box No. ล่าสุด
    PackagingLog findTopByReportIdAndLotNumberOrderByBoxNoDesc(Long reportId, String lotNumber);

    // เมธอดสำหรับดึงข้อมูลตามช่วงเวลา (ใช้ใน Shift Leader Dashboard)
    List<PackagingLog> findByReportIdAndTimestampBetween(Long reportId, LocalDateTime start, LocalDateTime end);

    // Added for historical report
    List<PackagingLog> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<PackagingLog> findByReportIdInAndTimestampBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(pl) FROM PackagingLog pl WHERE pl.report.id IN :reportIds")
    Long countAllForReports(@Param("reportIds") List<Long> reportIds);

    @Query("SELECT l.report.id, COUNT(l) FROM PackagingLog l WHERE l.report.id IN :reportIds GROUP BY l.report.id")
    List<Object[]> countBoxesByReportIds(@Param("reportIds") List<Long> reportIds);
    
    // เปลี่ยนชื่อเมธอดนี้เพื่อไม่ให้ซ้ำกับ countByReportId
    @Query("SELECT COUNT(p) FROM PackagingLog p WHERE p.report.id = :reportId")
    Long countPackagesByReportId(@Param("reportId") Long reportId);
    
    // เพิ่มเมธอดสำหรับหาข้อมูลในช่วงเวลาโดยตรวจสอบ null
    @Query("SELECT pl FROM PackagingLog pl WHERE pl.report.id IN :reportIds AND pl.timestamp BETWEEN :startDate AND :endDate")
    List<PackagingLog> findByReportIdInAndTimestampBetweenSafe(
        @Param("reportIds") List<Long> reportIds, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
}