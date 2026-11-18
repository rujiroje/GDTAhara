package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ImportLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {

    @EntityGraph(attributePaths = {"importedBy"})
    List<ImportLog> findByFactoryCodeOrderByImportedAtDesc(String factoryCode);

    @EntityGraph(attributePaths = {"importedBy"})
    List<ImportLog> findTop20ByOrderByImportedAtDesc();
}
