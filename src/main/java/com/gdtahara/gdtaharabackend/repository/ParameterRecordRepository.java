package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterRecordRepository extends JpaRepository<ParameterRecord, Long> {
    
    /**
     * 🔥 OPTIMIZED: ดึงเฉพาะ fields ที่จำเป็นแทน SELECT *
     * Performance: ลดการถึงข้อมูลจาก 60+ columns เป็น 10 columns
     */
    @Query("""
        SELECT pr.id, pr.reportId, pr.recordTime, pr.createdAt,
               pr.extruderMainScrewRpm, pr.tempMainFb, pr.cycleTimeSec,
               pr.technicianId
        FROM ParameterRecord pr 
        WHERE pr.reportId = :reportId 
        ORDER BY pr.createdAt DESC
        """)
    List<Object[]> findOptimizedByReportId(@Param("reportId") Long reportId);
    
    /**
     * 🔥 OPTIMIZED: Pagination สำหรับข้อมูลขนาดใหญ่
     */
    @Query("""
        SELECT pr.id, pr.reportId, pr.recordTime, pr.createdAt,
               pr.extruderMainScrewRpm, pr.tempMainFb, pr.cycleTimeSec,
               pr.technicianId
        FROM ParameterRecord pr 
        WHERE pr.reportId = :reportId 
        ORDER BY pr.createdAt DESC
        """)
    Page<Object[]> findOptimizedByReportIdWithPagination(@Param("reportId") Long reportId, Pageable pageable);

    /**
     * ✅ เก็บ original method สำหรับใช้ใน cases ที่ต้องการข้อมูลครบ
     */
    List<ParameterRecord> findByReportIdOrderByCreatedAtDesc(Long reportId);

    /**
     * นับจำนวน Parameter Records ตาม Report ID
     */
    long countByReportId(Long reportId);

    /**
     * ค้นหา Parameter Records ตาม Report ID พร้อม Pagination
     */
    Page<ParameterRecord> findByReportIdOrderByCreatedAtDesc(Long reportId, Pageable pageable);
    
    /**
     * 🔥 OPTIMIZED: ดึงข้อมูลล่าสุด 10 records สำหรับ dashboard
     */
    @Query(value = """
        SELECT TOP 10 pr.id, pr.report_id, pr.record_time, pr.created_at,
               pr.extruder_main_screw_rpm, pr.temp_main_fb, pr.cycle_time_sec,
               pr.technician_id
        FROM parameter_records pr 
        WHERE pr.report_id = :reportId 
        ORDER BY pr.created_at DESC
        """, nativeQuery = true)
    List<Object[]> findLatest10ByReportId(@Param("reportId") Long reportId);
}