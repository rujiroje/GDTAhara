package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.WasteWeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface WasteWeightRecordRepository extends JpaRepository<WasteWeightRecord, Long> {

    List<WasteWeightRecord> findAllByShiftDateAndShiftCodeAndIsActiveTrueOrderByRecordedAtDesc(
            LocalDate shiftDate, String shiftCode);

    List<WasteWeightRecord> findAllByMachineIdAndShiftDateAndIsActiveTrueOrderByRecordedAtDesc(
            String machineId, LocalDate shiftDate);

    @Query("SELECT w FROM WasteWeightRecord w WHERE w.isActive = true " +
           "AND w.recordedBy = :username AND w.shiftDate = :shiftDate AND w.shiftCode = :shiftCode " +
           "ORDER BY w.recordedAt DESC")
    List<WasteWeightRecord> findByRecorderAndShift(
            @Param("username") String username,
            @Param("shiftDate") LocalDate shiftDate,
            @Param("shiftCode") String shiftCode);
}
