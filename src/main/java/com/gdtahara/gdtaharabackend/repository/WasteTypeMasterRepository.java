package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.WasteTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WasteTypeMasterRepository extends JpaRepository<WasteTypeMaster, String> {
    List<WasteTypeMaster> findAllByIsActiveTrueOrderBySortOrderAsc();
}
