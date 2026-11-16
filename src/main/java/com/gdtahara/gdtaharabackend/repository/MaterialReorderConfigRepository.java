package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MaterialReorderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialReorderConfigRepository extends JpaRepository<MaterialReorderConfig, String> {

    List<MaterialReorderConfig> findByIsActiveTrue();
}
