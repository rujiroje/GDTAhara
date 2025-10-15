// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/MachineRepository.java
// (**แก้ไขไฟล์เดิม** เพิ่มเมธอด findByMachineCode)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // เพิ่ม import

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    Optional<Machine> findByMachineCode(String machineCode); // เพิ่มเมธอดนี้
}