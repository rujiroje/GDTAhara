// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/ProductionReportRepository.java
// (ฉบับแก้ไข - ลบ imports ที่มีปัญหา)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ProductionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionReportRepository extends JpaRepository<ProductionReport, Long> {

    /**
     * 🔥 OPTIMIZED: เพิ่ม JOIN FETCH เพื่อป้องกัน N+1 queries
     */
    @Query("SELECT pr FROM ProductionReport pr " +
           "LEFT JOIN FETCH pr.machine " +
           "LEFT JOIN FETCH pr.product " +
           "WHERE pr.status = :status")
    List<ProductionReport> findByStatusWithJoins(@Param("status") String status);

    List<ProductionReport> findByStatus(String status);

    List<ProductionReport> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String status, LocalDate startDate, LocalDate endDate);

    // **[ใหม่]** เพิ่มเมธอดสำหรับค้นหา Report ที่มีช่วงเวลาทับซ้อนกับเครื่องจักรที่กำหนด
    @Query("SELECT pr FROM ProductionReport pr WHERE pr.machine.id = :machineId AND pr.status = 'In Progress' AND pr.startDate <= :endDate AND pr.endDate >= :startDate")
    List<ProductionReport> findOverlappingReports(
            @Param("machineId") Long machineId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<ProductionReport> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);
    
    // เพิ่ม methods ที่ใช้ใน ShiftLeaderService
    List<ProductionReport> findByStartDate(LocalDate startDate);
    
    /**
     * 🔥 OPTIMIZED: ดึงรายงานล่าสุด 5 รายการพร้อม JOIN FETCH
     */
    @Query("SELECT pr FROM ProductionReport pr " +
           "LEFT JOIN FETCH pr.machine " +
           "LEFT JOIN FETCH pr.product " +
           "ORDER BY pr.id DESC")
    List<ProductionReport> findTop5ByOrderByCreatedAtDescWithJoins();
    
    /**
     * ✅ เก็บ method เดิมไว้เพื่อ backward compatibility
     */
    @Query("SELECT pr FROM ProductionReport pr ORDER BY pr.id DESC")
    List<ProductionReport> findTop5ByOrderByCreatedAtDesc();

    /**
     * 🔥 PERFORMANCE OPTIMIZED: Fixed N+1 Query Problem
     * ดึงข้อมูล Production Reports ทั้งหมดพร้อม Machine และ Product ด้วย JOIN FETCH
     */
    @Query("SELECT pr FROM ProductionReport pr " +
           "LEFT JOIN FETCH pr.machine " +
           "LEFT JOIN FETCH pr.product " +
           "ORDER BY pr.id DESC")
    List<ProductionReport> findAllWithJoins();

    /**
     * หารายงานล่าสุด N รายการ
     */
    // แทนที่ findTop10ByOrderByCreatedAtDesc() ด้วย query ที่ปลอดภัย
    @Query("SELECT pr FROM ProductionReport pr " +
           "LEFT JOIN FETCH pr.machine " +
           "LEFT JOIN FETCH pr.product " +
           "ORDER BY pr.id DESC")
    List<ProductionReport> findLatest10ReportsWithJoins();
    
    @Query("SELECT pr FROM ProductionReport pr ORDER BY pr.id DESC")
    List<ProductionReport> findLatest10Reports();
    
    /**
     * นับจำนวนรายงานตามสถานะ
     */
    long countByStatus(String status);
    
    /**
     * ใช้ orderNumber แทน reportNumber (ถ้ามี field orderNumber ใน entity)
     */
    // ใช้ field ที่มีอยู่จริงในการค้นหา
    @Query("SELECT pr FROM ProductionReport pr WHERE pr.orderNumber = :orderNumber")
    Optional<ProductionReport> findByOrderNumber(@Param("orderNumber") String orderNumber);
}