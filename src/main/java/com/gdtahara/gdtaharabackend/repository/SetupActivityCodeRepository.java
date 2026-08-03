package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SetupActivityCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SetupActivityCodeRepository extends JpaRepository<SetupActivityCode, Long> {
    List<SetupActivityCode> findByIsActiveTrueOrderByDisplayOrderAsc();
}
