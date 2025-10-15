// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/MaterialRepository.java
// (ฉบับแก้ไข เพิ่มเมธอด findByMaterialCode)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    // **[ใหม่]** เพิ่มเมธอดสำหรับค้นหาวัตถุดิบด้วยรหัส
    Optional<Material> findByMaterialCode(String materialCode);
}