package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ShiftMaterialConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftMaterialConsumptionRepository extends JpaRepository<ShiftMaterialConsumption, Long> {

    Optional<ShiftMaterialConsumption> findByTankIdAndShiftDateAndShiftCode(
            String tankId, LocalDate shiftDate, String shiftCode);

    List<ShiftMaterialConsumption> findByTankIdOrderByShiftDateDescShiftCodeAsc(String tankId);

    List<ShiftMaterialConsumption> findByMachineIdAndShiftDateOrderByTankIdAsc(
            String machineId, LocalDate shiftDate);
}
