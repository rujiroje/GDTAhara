package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.TankRefill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TankRefillRepository extends JpaRepository<TankRefill, Long> {

    List<TankRefill> findByTankIdAndShiftDateAndShiftCodeOrderByRefillAtDesc(
            String tankId, LocalDate shiftDate, String shiftCode);

    // Sum effective refill qty: corrections replace originals.
    // Include: correction rows + original rows that have NOT been corrected.
    // Exclude: original rows whose id appears in correctsRefillId of a correction row.
    @Query("SELECT COALESCE(SUM(r.qtyKg), 0) FROM TankRefill r " +
           "WHERE r.tankId = :tankId AND r.shiftDate = :shiftDate AND r.shiftCode = :shiftCode " +
           "AND (r.isCorrection = true OR " +
           "(r.isCorrection = false AND r.id NOT IN " +
           "(SELECT c.correctsRefillId FROM TankRefill c " +
           "WHERE c.correctsRefillId IS NOT NULL " +
           "AND c.tankId = :tankId AND c.shiftDate = :shiftDate AND c.shiftCode = :shiftCode)))")
    BigDecimal sumRefillKg(@Param("tankId") String tankId,
                           @Param("shiftDate") LocalDate shiftDate,
                           @Param("shiftCode") String shiftCode);

    List<TankRefill> findByTankIdOrderByRefillAtDesc(String tankId);

    int countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionFalse(
            String tankId, LocalDate shiftDate, String shiftCode);

    int countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionTrue(
            String tankId, LocalDate shiftDate, String shiftCode);
}
