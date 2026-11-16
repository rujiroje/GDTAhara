package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ShiftBoundaryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftBoundaryConfigRepository extends JpaRepository<ShiftBoundaryConfig, Long> {

    Optional<ShiftBoundaryConfig> findByShiftCodeAndIsActiveTrue(String shiftCode);
    List<ShiftBoundaryConfig> findAllByIsActiveTrue();
    Optional<ShiftBoundaryConfig> findByShiftCode(String shiftCode);
}
