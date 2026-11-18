package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
