package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MoldMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoldMasterRepository extends JpaRepository<MoldMaster, Long> {
    Optional<MoldMaster> findByMoldCode(String moldCode);
    List<MoldMaster> findByIsActiveTrueOrderByMoldCodeAsc();
    List<MoldMaster> findByMachineTypeAndIsActiveTrue(String machineType);
    List<MoldMaster> findByProductIdAndIsActiveTrueOrderByMoldCodeAsc(Long productId);
}
