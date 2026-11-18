package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.WasteEditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WasteEditLogRepository extends JpaRepository<WasteEditLog, Long> {
    List<WasteEditLog> findAllByRecordIdOrderByEditedAtDesc(Long recordId);
}
