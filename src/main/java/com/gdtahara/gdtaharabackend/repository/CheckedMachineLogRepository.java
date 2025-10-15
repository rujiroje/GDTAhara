// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/CheckedMachineLogRepository.java
// (**สร้างไฟล์ใหม่** ใน package repository)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.CheckedMachineLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckedMachineLogRepository extends JpaRepository<CheckedMachineLog, Long> {}