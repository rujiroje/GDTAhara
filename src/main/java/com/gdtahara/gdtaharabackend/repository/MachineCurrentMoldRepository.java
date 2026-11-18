package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MachineCurrentMold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineCurrentMoldRepository extends JpaRepository<MachineCurrentMold, Long> {

    /** Returns the currently installed mold for a machine by machine DB id. */
    Optional<MachineCurrentMold> findByMachineId(Long machineId);

    /**
     * Convenience: lookup by machine name string (joins through machines table).
     * Used by OperatorService which has machineName, not machine.id.
     */
    @Query("SELECT mcm FROM MachineCurrentMold mcm " +
           "JOIN Machine m ON m.id = mcm.machineId " +
           "WHERE m.machineName = :machineName")
    Optional<MachineCurrentMold> findByMachineName(@Param("machineName") String machineName);
}
