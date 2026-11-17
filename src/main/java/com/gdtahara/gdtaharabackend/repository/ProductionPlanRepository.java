package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    Optional<ProductionPlan> findByMachineIdAndPlanDateAndProductId(Long machineId, LocalDate planDate, Long productId);

    List<ProductionPlan> findByPlanDateBetween(LocalDate from, LocalDate to);

    List<ProductionPlan> findByMachineIdAndPlanDateBetweenOrderByPlanDate(Long machineId, LocalDate from, LocalDate to);

    /** C-04: find today's plan for a machine by machine code (String like "RBL201") */
    List<ProductionPlan> findByMachine_MachineCodeAndPlanDate(String machineCode, LocalDate planDate);
}
