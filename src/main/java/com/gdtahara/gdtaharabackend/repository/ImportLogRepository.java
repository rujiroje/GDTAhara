package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    List<ImportLog> findByFactoryCodeOrderByImportedAtDesc(String factoryCode);

    List<ImportLog> findTop20ByOrderByImportedAtDesc();
}
