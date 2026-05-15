package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MachineStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineStatusLogRepository extends JpaRepository<MachineStatusLog, Long> {

    // หา active status ปัจจุบันของเครื่อง (end_time IS NULL)
    Optional<MachineStatusLog> findByMachineIdAndEndTimeIsNull(Long machineId);

    // ดึง timeline ของเครื่องในช่วงเวลา
    @Query("SELECT l FROM MachineStatusLog l WHERE l.machine.id = :machineId " +
           "AND l.startTime >= :from AND l.startTime <= :to ORDER BY l.startTime ASC")
    List<MachineStatusLog> findByMachineAndTimeRange(@Param("machineId") Long machineId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    // ดึง log ล่าสุดของแต่ละเครื่อง (สำหรับ dashboard)
    @Query("SELECT l FROM MachineStatusLog l WHERE l.machine.id = :machineId " +
           "ORDER BY l.startTime DESC")
    List<MachineStatusLog> findLatestByMachine(@Param("machineId") Long machineId,
                                               org.springframework.data.domain.Pageable pageable);

    // คำนวณรวม duration ตาม status ในช่วงเวลา (สำหรับ OEE)
    // ใช้ native SQL เพราะ Hibernate 6 HQL ไม่รองรับ DATEDIFF string-literal temporal unit
    @Query(value = "SELECT l.status, " +
           "SUM(DATEDIFF(MINUTE, l.start_time, CASE WHEN l.end_time IS NULL THEN :now ELSE l.end_time END)) " +
           "FROM machine_status_logs l " +
           "WHERE l.machine_id = :machineId " +
           "AND l.start_time >= :from AND l.start_time <= :to " +
           "GROUP BY l.status",
           nativeQuery = true)
    List<Object[]> sumDurationByStatus(@Param("machineId") Long machineId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("now") LocalDateTime now);
}
