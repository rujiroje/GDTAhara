package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.TankMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TankMeterReadingRepository extends JpaRepository<TankMeterReading, Long> {

    List<TankMeterReading> findByTankIdAndShiftDateAndShiftCodeOrderByRecordedAtDesc(
            String tankId, LocalDate shiftDate, String shiftCode);

    Optional<TankMeterReading> findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtAsc(
            String tankId, LocalDate shiftDate, String shiftCode, String readingType);

    Optional<TankMeterReading> findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtDesc(
            String tankId, LocalDate shiftDate, String shiftCode, String readingType);

    @Query("SELECT r FROM TankMeterReading r WHERE r.tankId = :tankId ORDER BY r.recordedAt DESC")
    List<TankMeterReading> findLatestByTankId(@Param("tankId") String tankId);

    boolean existsByTankIdAndShiftDateAndShiftCodeAndReadingType(
            String tankId, LocalDate shiftDate, String shiftCode, String readingType);
}
