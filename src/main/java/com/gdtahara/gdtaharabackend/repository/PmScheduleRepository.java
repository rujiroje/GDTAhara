package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.PmSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PmScheduleRepository extends JpaRepository<PmSchedule, Long> {

    List<PmSchedule> findByIsActiveTrueOrderByNextDueDateAsc();

    List<PmSchedule> findByMachineIdAndIsActiveTrueOrderByNextDueDateAsc(Long machineId);

    // หา task ที่ DUE หรือ OVERDUE (สำหรับ alert)
    @Query("SELECT p FROM PmSchedule p WHERE p.isActive = true " +
           "AND p.status IN ('DUE', 'OVERDUE') ORDER BY p.nextDueDate ASC")
    List<PmSchedule> findUrgent();

    // หา task ที่ครบกำหนดภายใน N วัน
    @Query("SELECT p FROM PmSchedule p WHERE p.isActive = true " +
           "AND p.nextDueDate <= :cutoff ORDER BY p.nextDueDate ASC")
    List<PmSchedule> findDueWithin(@Param("cutoff") LocalDate cutoff);
}
