// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/ProductionReportRepository.java
// (ฉบับแก้ไข - ลบ imports ที่มีปัญหา)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.dto.ProductionReportListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionReportRepository extends JpaRepository<ProductionReport, Long> {

    // Basic finders
    List<ProductionReport> findByStatus(String status);
    // รองรับค้นหาหลายสถานะ (เช่น IN_PROGRESS และ ACTIVE สำหรับงานที่กำลังดำเนินการ)
    List<ProductionReport> findByStatusIn(java.util.Collection<String> statuses);
    List<ProductionReport> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String status, LocalDate startDate, LocalDate endDate);
    List<ProductionReport> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate, LocalDate startDate);
    List<ProductionReport> findByStartDate(LocalDate startDate);
    
    // Machine-specific filters
    @Query("SELECT pr FROM ProductionReport pr " +
           "LEFT JOIN FETCH pr.machine m " +
           "LEFT JOIN FETCH pr.product p " +
           "WHERE pr.startDate = :startDate " +
           "AND pr.machine.machineName = :machineId")
    List<ProductionReport> findByStartDateAndMachineId(@Param("startDate") LocalDate startDate, @Param("machineId") String machineId);

    @Query("SELECT pr FROM ProductionReport pr " +
        "LEFT JOIN FETCH pr.machine m " +
        "LEFT JOIN FETCH pr.product p " +
        "WHERE (:machineName IS NULL OR m.machineName = :machineName) " +
        "AND (pr.startDate IS NULL OR pr.startDate <= :targetDate) " +
        "AND (pr.endDate IS NULL OR pr.endDate >= :targetDate)")
    List<ProductionReport> findActiveOnDate(@Param("targetDate") LocalDate targetDate,
                          @Param("machineName") String machineName);

    // Fast, case-insensitive active reports query with left fetch joins to avoid N+1 and delays
    // Returns reports considered "active" if status is in active set (case-insensitive),
    // or if today is within [startDate, endDate] and status is not a terminal status.
    @Query("SELECT DISTINCT pr FROM ProductionReport pr " +
        "LEFT JOIN FETCH pr.machine m " +
        "LEFT JOIN FETCH pr.product p " +
        "WHERE (TRIM(UPPER(pr.status)) IN :activeStatuses) " +
        "   OR ((pr.startDate IS NULL OR pr.startDate <= :today) " +
        "       AND (pr.endDate IS NULL OR pr.endDate >= :today) " +
        "       AND (pr.status IS NULL OR TRIM(UPPER(pr.status)) NOT IN :terminalStatuses)) " +
        "ORDER BY pr.createdAt DESC")
    List<ProductionReport> findActiveReportsFast(@Param("activeStatuses") java.util.Collection<String> activeStatuses,
                            @Param("terminalStatuses") java.util.Collection<String> terminalStatuses,
                            @Param("today") LocalDate today);

    // Overlapping reports for machine schedule validation
    @Query("SELECT pr FROM ProductionReport pr WHERE pr.machine.id = :machineId AND pr.status = 'In Progress' AND pr.startDate <= :endDate AND pr.endDate >= :startDate")
    List<ProductionReport> findOverlappingReports(@Param("machineId") Long machineId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    // Paginated lightweight list (DTO projection)
    @Query("SELECT new com.gdtahara.gdtaharabackend.dto.ProductionReportListDto(" +
        "pr.id, pr.orderNumber, pr.startDate, pr.endDate, pr.status, m.machineName, p.productName, pr.createdAt) " +
        "FROM ProductionReport pr " +
        "JOIN pr.machine m " +
        "JOIN pr.product p " +
        "ORDER BY pr.createdAt DESC")
    Page<ProductionReportListDto> findAllList(Pageable pageable);

    // Native latest 5 for dashboard (explicit columns only)
    @Query(value = "SELECT TOP 5 pr.id, pr.order_number, pr.start_date, pr.end_date, pr.status, m.machine_name AS machine_name, p.product_name AS product_name, pr.created_at " +
        "FROM production_reports pr " +
        "JOIN machines m ON pr.machine_id = m.id " +
        "JOIN products p ON pr.product_id = p.id " +
        "ORDER BY pr.created_at DESC", nativeQuery = true)
    List<Object[]> findLatest5Raw();

    // Native latest 10
    @Query(value = "SELECT TOP 10 pr.id, pr.order_number, pr.start_date, pr.end_date, pr.status, m.machine_name AS machine_name, p.product_name AS product_name, pr.created_at " +
        "FROM production_reports pr " +
        "JOIN machines m ON pr.machine_id = m.id " +
        "JOIN products p ON pr.product_id = p.id " +
        "ORDER BY pr.created_at DESC", nativeQuery = true)
    List<Object[]> findLatest10Raw();

    // Status count
    long countByStatus(String status);

    // Find by order number
    @Query("SELECT pr FROM ProductionReport pr WHERE pr.orderNumber = :orderNumber")
    Optional<ProductionReport> findByOrderNumber(@Param("orderNumber") String orderNumber);

    // Find historical reports by date range with optional machine and product filters
    @Query("SELECT DISTINCT pr FROM ProductionReport pr " +
        "LEFT JOIN FETCH pr.machine m " +
        "LEFT JOIN FETCH pr.product p " +
        "WHERE pr.startDate >= :startDate " +
        "AND pr.startDate <= :endDate " +
        "AND (:machineId IS NULL OR pr.machine.id = :machineId) " +
        "AND (:productId IS NULL OR pr.product.id = :productId) " +
        "ORDER BY pr.startDate DESC")
    List<ProductionReport> findHistoricalReports(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("machineId") Long machineId,
                                                @Param("productId") Long productId);

    // Find distinct production dates between date range
    @Query("SELECT DISTINCT pr.startDate FROM ProductionReport pr " +
        "WHERE pr.startDate >= :startDate " +
        "AND pr.startDate <= :endDate " +
        "AND pr.startDate IS NOT NULL " +
        "ORDER BY pr.startDate DESC")
    List<LocalDate> findDistinctDatesBetween(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // Filtered by machineName (as String) and/or productId
    @Query("SELECT DISTINCT pr.startDate FROM ProductionReport pr " +
        "LEFT JOIN pr.machine m " +
        "LEFT JOIN pr.product p " +
        "WHERE pr.startDate >= :startDate " +
        "AND pr.startDate <= :endDate " +
        "AND pr.startDate IS NOT NULL " +
        "AND (:machineName IS NULL OR m.machineName = :machineName) " +
        "AND (:productId IS NULL OR p.id = :productId) " +
        "ORDER BY pr.startDate DESC")
    List<LocalDate> findDistinctDatesBetweenFiltered(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("machineName") String machineName,
                                                    @Param("productId") Long productId);

    // Filtered by order number only
    @Query("SELECT DISTINCT pr.startDate FROM ProductionReport pr " +
        "WHERE pr.startDate >= :startDate " +
        "AND pr.startDate <= :endDate " +
        "AND pr.startDate IS NOT NULL " +
        "AND pr.orderNumber = :orderNumber " +
        "ORDER BY pr.startDate DESC")
    List<LocalDate> findDistinctDatesBetweenWithOrder(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("orderNumber") String orderNumber);

    // Overlapping reports within a date range with optional filters (machineName, productId, orderNumber)
    @Query("SELECT pr FROM ProductionReport pr " +
        "LEFT JOIN pr.machine m " +
        "LEFT JOIN pr.product p " +
        "WHERE (pr.startDate IS NULL OR pr.startDate <= :endDate) " +
        "AND (pr.endDate IS NULL OR pr.endDate >= :startDate) " +
        "AND (:machineName IS NULL OR m.machineName = :machineName) " +
        "AND (:productId IS NULL OR p.id = :productId) " +
        "AND (:orderNumber IS NULL OR pr.orderNumber = :orderNumber)")
    List<ProductionReport> findOverlappingReportsWithFilters(@Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate,
                                                            @Param("machineName") String machineName,
                                                            @Param("productId") Long productId,
                                                            @Param("orderNumber") String orderNumber);
}