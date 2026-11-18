package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SetupChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SetupChecklistTemplateRepository extends JpaRepository<SetupChecklistTemplate, Long> {

    List<SetupChecklistTemplate> findByActiveTrueOrderByStepOrderAsc();

    List<SetupChecklistTemplate> findAllByOrderByStepOrderAsc();

    // Returns templates where machineType matches OR machineType is null (applies to all)
    @Query("SELECT t FROM SetupChecklistTemplate t WHERE t.active = true AND (t.machineType IS NULL OR t.machineType = :machineType) ORDER BY t.stepOrder ASC")
    List<SetupChecklistTemplate> findActiveByMachineType(@Param("machineType") String machineType);
}
