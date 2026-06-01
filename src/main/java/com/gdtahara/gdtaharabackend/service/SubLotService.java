package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.SubLotRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubLotService {

    private static final Logger logger = LoggerFactory.getLogger(SubLotService.class);

    private final SubLotRepository subLotRepository;
    private final ProductionReportRepository productionReportRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SubLotService(SubLotRepository subLotRepository,
                         ProductionReportRepository productionReportRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.subLotRepository = subLotRepository;
        this.productionReportRepository = productionReportRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SubLot confirmBox(Long productionReportId, Integer boxQuantity,
                             BigDecimal weightKg, String palletNumber, String username) {
        logger.info("Confirming box for report {} by user {}", productionReportId, username);

        ProductionReport report = productionReportRepository.findById(productionReportId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Production report not found: " + productionReportId));

        if ("FINALIZED".equalsIgnoreCase(report.getStatus())) {
            throw new IllegalStateException("Cannot add boxes to a finalized report");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        long seq = subLotRepository.countByProductionReportId(productionReportId) + 1;
        String base = report.getParentLotNumber() != null
                ? report.getParentLotNumber()
                : "RPT-" + productionReportId;
        String subLotNumber = String.format("%s-B%04d", base, seq);
        logger.info("Generated sub-lot number: {}", subLotNumber);

        SubLot subLot = new SubLot();
        subLot.setProductionReport(report);
        subLot.setSubLotNumber(subLotNumber);
        subLot.setPalletNumber(palletNumber);
        subLot.setBoxQuantity(boxQuantity);
        subLot.setWeightKg(weightKg);
        subLot.setConfirmedAt(LocalDateTime.now());
        subLot.setConfirmedBy(user);
        subLot.setStatus("draft");

        SubLot saved = subLotRepository.save(subLot);
        auditLogService.log("CREATE", "SubLot", saved.getId(), null,
                Map.of("reportId", String.valueOf(productionReportId),
                        "subLotNumber", subLotNumber,
                        "boxQuantity", String.valueOf(boxQuantity),
                        "createdBy", username));

        logger.info("Confirmed box {} for report {}", subLotNumber, productionReportId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SubLot> getSubLotsForReport(Long productionReportId) {
        return subLotRepository.findByProductionReportIdOrderByConfirmedAt(productionReportId);
    }

    @Transactional(readOnly = true)
    public long countBoxesForReport(Long productionReportId) {
        return subLotRepository.countByProductionReportId(productionReportId);
    }

    @Transactional(readOnly = true)
    public SubLot getById(Long id) {
        return subLotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubLot not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<SubLot> findBySubLotNumber(String subLotNumber) {
        return subLotRepository.findBySubLotNumber(subLotNumber);
    }

    @Transactional
    public SubLot markAsLabeled(Long subLotId, String zplRef, String username) {
        logger.info("Marking sub-lot {} as labeled by user {}", subLotId, username);

        SubLot subLot = subLotRepository.findById(subLotId)
                .orElseThrow(() -> new EntityNotFoundException("SubLot not found: " + subLotId));

        String previousStatus = subLot.getStatus();
        subLot.setStatus("labeled");
        subLot.setZplLabelPrintedRef(zplRef);

        SubLot saved = subLotRepository.save(subLot);

        Map<String, Object> after = new HashMap<>();
        after.put("status", "labeled");
        after.put("zplRef", zplRef != null ? zplRef : "");
        after.put("updatedBy", username);

        auditLogService.log("UPDATE", "SubLot", subLotId,
                Map.of("status", previousStatus != null ? previousStatus : ""),
                after);

        logger.info("Sub-lot {} marked as labeled", subLotId);
        return saved;
    }
}
