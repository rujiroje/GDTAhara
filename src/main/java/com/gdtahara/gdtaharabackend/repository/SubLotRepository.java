package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SubLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubLotRepository extends JpaRepository<SubLot, Long> {

    @Query("SELECT sl FROM SubLot sl " +
           "JOIN FETCH sl.productionReport " +
           "JOIN FETCH sl.confirmedBy " +
           "WHERE sl.productionReport.id = :reportId " +
           "ORDER BY sl.confirmedAt")
    List<SubLot> findByProductionReportIdOrderByConfirmedAt(@Param("reportId") Long reportId);

    @Query("SELECT sl FROM SubLot sl " +
           "JOIN FETCH sl.productionReport " +
           "JOIN FETCH sl.confirmedBy " +
           "WHERE sl.subLotNumber = :num")
    Optional<SubLot> findBySubLotNumber(@Param("num") String num);

    long countByProductionReportId(Long reportId);

    List<SubLot> findByProductionReportIdAndPalletNumber(Long reportId, String palletNumber);

    @Query("SELECT sl FROM SubLot sl " +
           "JOIN FETCH sl.productionReport " +
           "JOIN FETCH sl.confirmedBy " +
           "WHERE sl.pallet.id = :palletId " +
           "ORDER BY sl.confirmedAt")
    List<SubLot> findByPallet_IdOrderByConfirmedAt(@Param("palletId") Long palletId);

    boolean existsBySubLotNumber(String subLotNumber);

    @Query("SELECT sl FROM SubLot sl " +
           "JOIN FETCH sl.productionReport " +
           "JOIN FETCH sl.confirmedBy " +
           "WHERE sl.id = :id")
    Optional<SubLot> findByIdWithDetails(@Param("id") Long id);
}
