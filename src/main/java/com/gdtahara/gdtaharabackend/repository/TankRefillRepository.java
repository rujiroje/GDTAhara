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

    @Query("SELECT COALESCE(SUM(r.qtyKg), 0) FROM TankRefill r " +
           "WHERE r.tankId = :tankId AND r.shiftDate = :shiftDate AND r.shiftCode = :shiftCode " +
           "AND r.isCorrection = false")
    BigDecimal sumRefillKg(@Param("tankId") String tankId,
                           @Param("shiftDate") LocalDate shiftDate,
                           @Param("shiftCode") String shiftCode);

    List<TankRefill> findByTankIdOrderByRefillAtDesc(String tankId);

    int countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionFalse(
            String tankId, LocalDate shiftDate, String shiftCode);

    int countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionTrue(
            String tankId, LocalDate shiftDate, String shiftCode);
}
