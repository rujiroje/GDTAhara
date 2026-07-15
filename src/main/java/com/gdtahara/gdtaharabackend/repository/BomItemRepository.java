package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BomItemRepository extends JpaRepository<BomItem, Long> {

    List<BomItem> findByBomIdOrderByItemNumber(Long bomId);

    List<BomItem> findByRmCode(String rmCode);

    List<BomItem> findByBomIdAndIsScrap(Long bomId, Boolean isScrap);
}
