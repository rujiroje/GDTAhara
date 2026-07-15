package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.BillOfMaterials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillOfMaterialsRepository extends JpaRepository<BillOfMaterials, Long> {

    // ดึง fgCode ที่ไม่ซ้ำทั้งหมด สำหรับ sync products
    @Query("SELECT DISTINCT b.fgCode FROM BillOfMaterials b WHERE b.fgCode IS NOT NULL")
    List<String> findDistinctFgCodes();

    // ดึง BOM ล่าสุดของ fgCode (สำหรับ metadata เวลา sync)
    Optional<BillOfMaterials> findFirstByFgCodeOrderByEffectiveFromDesc(String fgCode);

    @Query("""
        SELECT b FROM BillOfMaterials b
        WHERE b.fgCode = :fgCode
          AND b.status = 'ACTIVE'
          AND b.effectiveFrom <= :date
          AND (b.effectiveTo IS NULL OR b.effectiveTo >= :date)
        ORDER BY b.effectiveFrom DESC
        """)
    Optional<BillOfMaterials> findActiveByFgCodeAndDate(
            @Param("fgCode") String fgCode,
            @Param("date") LocalDate date);

    List<BillOfMaterials> findByFgCodeOrderByEffectiveFromDesc(String fgCode);

    List<BillOfMaterials> findByStatus(String status);

    boolean existsByFgCodeAndAlternativeNumberAndPlantCodeAndEffectiveFrom(
            String fgCode, Integer alternativeNumber, String plantCode, LocalDate effectiveFrom);

    Optional<BillOfMaterials> findFirstByFgCodeAndAlternativeNumberAndPlantCodeAndEffectiveToIsNull(
            String fgCode, Integer alternativeNumber, String plantCode);
}
