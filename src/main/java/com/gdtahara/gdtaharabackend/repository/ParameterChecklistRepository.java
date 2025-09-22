// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/ParameterChecklistRepository.java
// (**สร้างไฟล์ใหม่** ใน package repository)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ParameterChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParameterChecklistRepository extends JpaRepository<ParameterChecklist, Long> {
    List<ParameterChecklist> findByMachineType(String machineType);
}