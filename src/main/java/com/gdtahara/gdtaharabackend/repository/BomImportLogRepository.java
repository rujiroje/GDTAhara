package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.BomImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BomImportLogRepository extends JpaRepository<BomImportLog, Long> {

    List<BomImportLog> findTop20ByOrderByImportedAtDesc();

    List<BomImportLog> findByFileTypeOrderByImportedAtDesc(String fileType);
}
