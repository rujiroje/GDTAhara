package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.PalletCloseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalletCloseLogRepository extends JpaRepository<PalletCloseLog, Long> {
    List<PalletCloseLog> findByPallet_IdOrderByPerformedAtDesc(Long palletId);
}
