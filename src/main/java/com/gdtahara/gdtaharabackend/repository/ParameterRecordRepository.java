package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterRecordRepository extends JpaRepository<ParameterRecord, Long> {
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
     * ค้นหา record ตามคู่ (reportId, recordTime) เพื่อป้องกันการซ้ำซ้อน เช่น "Standard", "10:00" ฯลฯ
     */
    java.util.Optional<ParameterRecord> findTopByReportIdAndRecordTime(Long reportId, String recordTime);
    
    // Summary DTO queries removed to avoid classpath issues; use entity methods above.
}