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

    /** Single query — balance summary for ALL materials that have any transaction */
    @Query("SELECT t.material.id, SUM(CASE WHEN t.transactionType = 'IN' THEN t.quantity ELSE -t.quantity END) " +
           "FROM MaterialStockTransaction t GROUP BY t.material.id")
    List<Object[]> findBalancesForAllMaterials();

    /** History for ONE material with associations pre-fetched (avoids N+1 on user + productionReport) */
    @Query("SELECT t FROM MaterialStockTransaction t " +
           "LEFT JOIN FETCH t.user " +
           "LEFT JOIN FETCH t.productionReport pr " +
           "LEFT JOIN FETCH pr.product " +
           "LEFT JOIN FETCH pr.machine " +
           "WHERE t.material.id = :materialId " +
           "ORDER BY t.timestamp DESC")
    List<MaterialStockTransaction> findByMaterialIdWithDetailsOrderByTimestampDesc(@Param("materialId") Long materialId);

    // **[ใหม่]** เพิ่มเมธอดสำหรับค้นหา Lot Number ที่ไม่ซ้ำกัน
    @Query("SELECT DISTINCT m.lotNumber FROM MaterialStockTransaction m WHERE m.material.id = :materialId AND m.transactionType = :type")
    List<String> findDistinctLotNumbersByMaterialIdAndTransactionType(@Param("materialId") Long materialId, @Param("type") String transactionType);

    @Query("""
        SELECT m.material.materialCode, SUM(m.quantity)
        FROM MaterialStockTransaction m
        WHERE m.productionReport.id = :reportId AND m.transactionType = 'OUT'
        GROUP BY m.material.materialCode
        """)
    List<Object[]> sumActualOutByReportId(@Param("reportId") Long reportId);

    List<MaterialStockTransaction> findByProductionReportIdAndTransactionTypeAndTimestampBetween(
            Long productionReportId, String transactionType, java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<MaterialStockTransaction> findByProductionReportIdAndTimestampBetween(
            Long productionReportId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}