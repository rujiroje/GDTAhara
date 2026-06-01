package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.PlanSheetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanSheetTemplateRepository extends JpaRepository<PlanSheetTemplate, Long> {

    Optional<PlanSheetTemplate> findByFactoryCode(String factoryCode);
}
