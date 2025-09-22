// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/MaterialStockTransactionRepository.java
// (ฉบับแก้ไข เพิ่มเมธอดที่ขาดไป)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MaterialStockTransactionRepository extends JpaRepository<MaterialStockTransaction, Long> {

    // **[ใหม่]** เพิ่มเมธอดสำหรับคำนวณ Stock คงเหลือ
    @Query("SELECT SUM(CASE WHEN m.transactionType = 'IN' THEN m.quantity ELSE -m.quantity END) FROM MaterialStockTransaction m WHERE m.material.id = :materialId")
    BigDecimal getStockBalanceByMaterialId(@Param("materialId") Long materialId);

    // **[ใหม่]** เพิ่มเมธอดสำหรับดึงประวัติการทำรายการ
    List<MaterialStockTransaction> findByMaterialIdOrderByTimestampDesc(Long materialId);

    // **[ใหม่]** เพิ่มเมธอดสำหรับค้นหา Lot Number ที่ไม่ซ้ำกัน
    @Query("SELECT DISTINCT m.lotNumber FROM MaterialStockTransaction m WHERE m.material.id = :materialId AND m.transactionType = :type")
    List<String> findDistinctLotNumbersByMaterialIdAndTransactionType(@Param("materialId") Long materialId, @Param("type") String transactionType);
}