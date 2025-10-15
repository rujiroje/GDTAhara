// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/DowntimeEventRepository.java
// (ฉบับเต็มและแก้ไขแล้ว)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {

    // **[แก้ไข]** ใช้ Report_Id
	List<DowntimeEvent> findByReportId(Long reportId);

	List<DowntimeEvent> findByReportIdAndStartTimeBetween(Long reportId, LocalDateTime start, LocalDateTime end);

    // Added for historical report
    List<DowntimeEvent> findByReportIdIn(List<Long> reportIds);

    // Added for daily summary report
    List<DowntimeEvent> findByReportIdInAndStartTimeBetween(List<Long> reportIds, LocalDateTime start, LocalDateTime end);
}