package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineSetupJobRepository extends JpaRepository<MachineSetupJob, Long> {

    List<MachineSetupJob> findByMachineIdAndPlanDate(Long machineId, LocalDate planDate);

    List<MachineSetupJob> findByAssignedToIdAndStatusIn(Long userId, List<String> statuses);

    Optional<MachineSetupJob> findFirstByProductionPlanIdAndStatusIn(Long planId, List<String> statuses);
}
