// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/DowntimeEventRepository.java
// (ฉบับเต็มและแก้ไขแล้ว)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.gdtahara.gdtaharabackend.dto.DowntimeEventSummaryDto;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {

    // **[แก้ไข]** ใช้ Report_Id
	List<DowntimeEvent> findByReportId(Long reportId);

	List<DowntimeEvent> findByReportIdAndStartTimeBetween(Long reportId, LocalDateTime start, LocalDateTime end);

    // Added for historical report
    List<DowntimeEvent> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<DowntimeEvent> findByReportIdInAndStartTimeBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);

    @Query("SELECT new com.gdtahara.gdtaharabackend.dto.DowntimeEventSummaryDto(" +
           "CAST(FUNCTION('FORMATDATETIME', de.startTime, 'HH:mm') AS string), " +
           "CAST(FUNCTION('FORMATDATETIME', de.endTime, 'HH:mm') AS string), " +
           "CAST(FUNCTION('CONCAT', TIMESTAMPDIFF(MINUTE, de.startTime, de.endTime), ' นาที') AS string), " +
           "de.reason, " +
           "de.technician.username) " +
           "FROM DowntimeEvent de WHERE de.report.id IN :reportIds")
    List<DowntimeEventSummaryDto> findDowntimeSummaryByReportIds(@Param("reportIds") List<Long> reportIds);
}