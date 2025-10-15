// =================================================================
// File: repository/LabelStockRepository.java
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.LabelStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LabelStockRepository extends JpaRepository<LabelStock, Long> {
    // เพิ่ม: Method สำหรับค้นหาสต็อกป้ายตาม Product ID
    Optional<LabelStock> findByProductId(Long productId);
}
