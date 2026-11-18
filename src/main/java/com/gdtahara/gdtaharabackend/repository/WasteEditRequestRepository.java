package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.WasteEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WasteEditRequestRepository extends JpaRepository<WasteEditRequest, Long> {

    List<WasteEditRequest> findAllByStatusOrderByRequestedAtDesc(String status);

    Optional<WasteEditRequest> findFirstByRecordIdAndStatusOrderByRequestedAtDesc(Long recordId, String status);

    List<WasteEditRequest> findAllByRequesterIdOrderByRequestedAtDesc(String requesterId);
}
