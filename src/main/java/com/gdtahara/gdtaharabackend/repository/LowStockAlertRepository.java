package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.LowStockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    List<LowStockAlert> findByResolvedAtIsNullOrderByAlertDateDesc();

    Optional<LowStockAlert> findByMaterialCodeAndAlertDate(String materialCode, LocalDate alertDate);

    @Query("SELECT a FROM LowStockAlert a WHERE a.resolvedAt IS NULL AND a.alertDate = :date")
    List<LowStockAlert> findActiveAlertsForDate(@org.springframework.data.repository.query.Param("date") LocalDate date);
}
