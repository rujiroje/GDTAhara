package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.CmBomItemDto;
import com.gdtahara.gdtaharabackend.dto.StockOutRequestDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class CmOperatorService {

    /** Material types that are auto-deducted from BOM×actual — no stock-IN required */
    private static final Set<String> AUTO_DEDUCT_TYPES = Set.of("HIBE", "VERP", "UNBW");

    private final MaterialStockTransactionRepository transactionRepository;
    private final ProductionReportRepository reportRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final MaterialUsageLogRepository materialUsageLogRepository;
    private final AuditLogService auditLogService;
    private final BillOfMaterialsRepository bomRepository;
    private final BomItemRepository bomItemRepository;
    private final MaterialStockService materialStockService;

    public MaterialStockTransaction recordStockOut(StockOutRequestDto request, String username) {
        ProductionReport report = reportRepository.findById(request.getProductionReportId())
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบใบสั่งผลิต"));
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบวัตถุดิบ"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบผู้ใช้งาน"));

        // HIBE / VERP are consumables with no stock-IN — skip balance check
        if (!request.isAutoDeduct()) {
            BigDecimal currentStock = transactionRepository.getStockBalanceByMaterialId(request.getMaterialId());
            if (currentStock == null || currentStock.compareTo(request.getQuantity()) < 0) {
                throw new IllegalStateException("วัตถุดิบ '" + material.getMaterialName()
                        + "' มีสต็อกไม่เพียงพอ (คงเหลือ: " + (currentStock != null ? currentStock : "0") + ")");
            }
        }

        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setProductionReport(report);
        transaction.setMaterial(material);
        transaction.setLotNumber(request.getLotNumber());
        transaction.setQuantity(request.getQuantity());
        transaction.setTransactionType("OUT");
        transaction.setUser(user);

        MaterialStockTransaction saved = transactionRepository.save(transaction);

        MaterialUsageLog usageLog = new MaterialUsageLog();
        usageLog.setReport(report);
        usageLog.setMaterialCode(material.getMaterialCode());
        usageLog.setLotNumber(request.getLotNumber());
        usageLog.setQuantityKg(request.getQuantity());
        usageLog.setTechnician(user);
        materialUsageLogRepository.save(usageLog);

        auditLogService.log("STOCK_OUT", "MaterialStockTransaction", saved.getId(), null,
                Map.of("id", saved.getId(),
                        "materialId", String.valueOf(request.getMaterialId()),
                        "materialName", material.getMaterialName(),
                        "quantity", String.valueOf(request.getQuantity()),
                        "lotNumber", String.valueOf(request.getLotNumber()),
                        "reportId", String.valueOf(request.getProductionReportId())));

        return saved;
    }

    /** Batch stock-out — records multiple materials in one transaction. */
    public List<MaterialStockTransaction> batchRecordStockOut(List<StockOutRequestDto> requests, String username) {
        List<MaterialStockTransaction> results = new ArrayList<>();
        for (StockOutRequestDto req : requests) {
            results.add(recordStockOut(req, username));
        }
        return results;
    }

    /**
     * Returns BOM raw-material items (non-UNBW, non-scrap) for the given report,
     * enriched with materialId (looked up via rmCode) and available lot numbers.
     */
    @Transactional(readOnly = true)
    public List<CmBomItemDto> getBomItemsForReport(Long reportId) {
        ProductionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบใบสั่งผลิต"));

        if (report.getProduct() == null) return Collections.emptyList();

        String fgCode = report.getProduct().getProductCode();
        LocalDate refDate = report.getStartDate() != null ? report.getStartDate() : LocalDate.now();

        Optional<BillOfMaterials> bomOpt = bomRepository.findActiveByFgCodeAndDate(fgCode, refDate);
        if (bomOpt.isEmpty()) return Collections.emptyList();

        BillOfMaterials bom = bomOpt.get();
        BigDecimal baseQty = (bom.getBaseQuantity() != null && bom.getBaseQuantity().compareTo(BigDecimal.ZERO) > 0)
                ? bom.getBaseQuantity() : BigDecimal.ONE;

        BigDecimal target = report.getTargetQty() != null
                ? BigDecimal.valueOf(report.getTargetQty()) : BigDecimal.ZERO;

        List<CmBomItemDto> result = new ArrayList<>();
        for (BomItem item : bomItemRepository.findByBomIdOrderByItemNumber(bom.getId())) {
            if (Boolean.TRUE.equals(item.getIsScrap())) continue;

            BigDecimal qtyPer    = item.getQuantityPer() != null ? item.getQuantityPer() : BigDecimal.ZERO;
            BigDecimal suggested = target.compareTo(BigDecimal.ZERO) > 0
                    ? qtyPer.multiply(target).divide(baseQty, 3, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            boolean autoDeduct = item.getMaterialType() != null
                    && AUTO_DEDUCT_TYPES.contains(item.getMaterialType().toUpperCase());

            Optional<Material> matOpt = materialRepository.findByMaterialCode(item.getRmCode());
            Long     materialId       = matOpt.map(Material::getId).orElse(null);
            String   materialName     = matOpt.map(Material::getMaterialName).orElse(item.getRmName());
            // Auto-deduct types don't need lot numbers — skip lot lookup
            List<String> lots = (!autoDeduct && materialId != null)
                    ? materialStockService.getAvailableLotNumbers(materialId)
                    : Collections.emptyList();

            result.add(new CmBomItemDto(
                    item.getItemNumber(), item.getRmCode(), materialName,
                    item.getMaterialType(), item.getUnit(),
                    qtyPer, suggested, materialId, lots, materialId != null, autoDeduct));
        }
        return result;
    }
}
