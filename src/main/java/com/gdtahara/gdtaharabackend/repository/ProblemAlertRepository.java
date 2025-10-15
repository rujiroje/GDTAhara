package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.ProblemAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProblemAlertRepository extends JpaRepository<ProblemAlert, Long> {
    // เพิ่มเมธอดนี้สำหรับค้นหาการแจ้งเตือนตามสถานะ
    List<ProblemAlert> findByStatusOrderByTimestampDesc(String status);
}