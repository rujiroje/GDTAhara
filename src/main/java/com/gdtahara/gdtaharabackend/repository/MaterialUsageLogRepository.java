package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MaterialUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.gdtahara.gdtaharabackend.dto.MaterialUsageLogDto;

@Repository
public interface MaterialUsageLogRepository extends JpaRepository<MaterialUsageLog, Long> {
    
    List<MaterialUsageLog> findByReportId(Long reportId);

    List<MaterialUsageLog> findByReportIdAndTimestampBetween(Long reportId, LocalDateTime start, LocalDateTime end);

    // Added for historical report
    List<MaterialUsageLog> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<MaterialUsageLog> findByReportIdInAndTimestampBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);

    @Query("SELECT new com.gdtahara.gdtaharabackend.dto.MaterialUsageLogDto(" +
           "mul.timestamp, mul.materialCode, mul.lotNumber, mul.quantityKg, mul.technician.username) " +
           "FROM MaterialUsageLog mul WHERE mul.report.id IN :reportIds")
    List<MaterialUsageLogDto> findMaterialUsageSummaryByReportIds(@Param("reportIds") List<Long> reportIds);
}