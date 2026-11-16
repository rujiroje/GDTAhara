package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MachineTank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MachineTankRepository extends JpaRepository<MachineTank, String> {
    List<MachineTank> findByMachineIdAndIsActiveTrue(String machineId);
    List<MachineTank> findByIsActiveTrue();
}
