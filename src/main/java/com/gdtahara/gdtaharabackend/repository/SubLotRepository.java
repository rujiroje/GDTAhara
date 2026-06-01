package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SubLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubLotRepository extends JpaRepository<SubLot, Long> {

    List<SubLot> findByProductionReportIdOrderByConfirmedAt(Long reportId);

    Optional<SubLot> findBySubLotNumber(String subLotNumber);

    long countByProductionReportId(Long reportId);

    List<SubLot> findByProductionReportIdAndPalletNumber(Long reportId, String palletNumber);
}
