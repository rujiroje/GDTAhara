// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/NgTypeRepository.java
// (**แก้ไขไฟล์เดิม** เพิ่มเมธอด findByNgCode)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.NgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // เพิ่ม import

@Repository
public interface NgTypeRepository extends JpaRepository<NgType, Long> {
    Optional<NgType> findByNgCode(String ngCode);

    List<NgType> findByNgType(String ngType);
    List<NgType> findByNgTypeIgnoreCase(String ngType);

    List<NgType> findByMachineTypeAndNgType(String machineType, String ngType);
    List<NgType> findByMachineType(String machineType);
}