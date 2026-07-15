package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.MaterialRequirementDto;
import com.gdtahara.gdtaharabackend.dto.VarianceDto;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.repository.MaterialStockTransactionRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.ScrapWeightLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BomVarianceService {

    private final ProductionReportRepository reportRepository;
    private final MaterialRequirementService requirementService;
    private final MaterialStockTransactionRepository stockTxRepository;
    private final ScrapWeightLogRepository scrapLogRepository;

    // ----------------------------------------------------------
    // วิเคราะห์ Variance สำหรับ 1 Production Report
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<VarianceDto> analyzeForReport(Long reportId) {
        ProductionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบ Report ID: " + reportId));

        if (report.getProductionPlan() == null) {
            log.warn("Report {} ไม่มี ProductionPlan เชื่อมต่อ — ไม่สามารถวิเคราะห์ BOM Variance ได้", reportId);
            return Collections.emptyList();
        }

        Long planId = report.getProductionPlan().getId();
        String fgCode = report.getProduct().getProductCode();

        // Planned requirement (RM ทั้งหมดใน BOM × target qty)
        List<MaterialRequirementDto> planned = requirementService.calculateForPlan(planId);
        if (planned.isEmpty()) return Collections.emptyList();

        // Actual stock-out ของ report นี้ → Map<materialCode, actualQty>
        Map<String, BigDecimal> actual = loadActualUsage(reportId);

        return planned.stream()
                .map(p -> buildVariance(p, actual, reportId, planId, fgCode))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // วิเคราะห์ Scrap Variance สำหรับ 1 Report
    // เปรียบเทียบ: BOM scrap items vs scrap_weight_logs จริง
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<VarianceDto> analyzeScrap(Long reportId) {
        ProductionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบ Report ID: " + reportId));

        if (report.getProductionPlan() == null) return Collections.emptyList();

        Long planId = report.getProductionPlan().getId();
        String fgCode = report.getProduct().getProductCode();

        // Planned scrap items เท่านั้น
        List<MaterialRequirementDto> scrapItems = requirementService.calculateForPlan(planId)
                .stream().filter(p -> Boolean.TRUE.equals(p.getIsScrap()))
                .collect(Collectors.toList());

        if (scrapItems.isEmpty()) return Collections.emptyList();

        // Actual scrap จาก scrap_weight_logs (รวมทั้งหมดของ report)
        BigDecimal totalActualScrap = scrapLogRepository.sumWeightByReportId(reportId);
        if (totalActualScrap == null) totalActualScrap = BigDecimal.ZERO;

        // กระจาย actual scrap สัดส่วน planned เท่ากัน (simple approach)
        BigDecimal totalPlannedScrap = scrapItems.stream()
                .map(MaterialRequirementDto::getTotalRequired)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal finalActual = totalActualScrap;
        final BigDecimal finalPlanned = totalPlannedScrap;

        return scrapItems.stream()
                .map(item -> {
                    // สัดส่วน actual ตาม weight ของแต่ละ scrap item
                    BigDecimal ratio = finalPlanned.compareTo(BigDecimal.ZERO) > 0
                            ? item.getTotalRequired().divide(finalPlanned, 8, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal itemActual = finalActual.multiply(ratio).setScale(4, RoundingMode.HALF_UP);

                    return buildVariance(item, Map.of(item.getRmCode(), itemActual),
                            reportId, planId, fgCode);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // Aggregate variance ในช่วงวันที่ (grouped by rm_code)
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<VarianceDto> aggregateByDateRange(LocalDate from, LocalDate to) {
        List<ProductionReport> reports = reportRepository.findByStartDateBetween(from, to);
        Map<String, VarianceDto> aggregated = new LinkedHashMap<>();

        for (ProductionReport report : reports) {
            if (report.getProductionPlan() == null) continue;
            List<VarianceDto> variances = analyzeForReport(report.getId());
            for (VarianceDto v : variances) {
                String key = v.getRmCode() + "|" + v.getUnit();
                aggregated.merge(key, v, (existing, newItem) -> {
                    existing.setPlannedQty(existing.getPlannedQty().add(newItem.getPlannedQty()));
                    existing.setActualQty(existing.getActualQty().add(newItem.getActualQty()));
                    existing.setVariance(existing.getVariance().add(newItem.getVariance()));
                    // คำนวณ variancePercent และ status ใหม่
                    recalcStatus(existing);
                    return existing;
                });
            }
        }
        return new ArrayList<>(aggregated.values());
    }

    // ----------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------

    private Map<String, BigDecimal> loadActualUsage(Long reportId) {
        List<Object[]> rows = stockTxRepository.sumActualOutByReportId(reportId);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            String code = (String) row[0];
            BigDecimal qty = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            map.put(code, qty);
        }
        return map;
    }

    private VarianceDto buildVariance(MaterialRequirementDto planned,
                                      Map<String, BigDecimal> actualMap,
                                      Long reportId, Long planId, String fgCode) {
        BigDecimal plannedQty = planned.getTotalRequired();
        BigDecimal actualQty  = actualMap.getOrDefault(planned.getRmCode(), BigDecimal.ZERO);
        BigDecimal variance   = actualQty.subtract(plannedQty).setScale(4, RoundingMode.HALF_UP);

        BigDecimal variancePct = BigDecimal.ZERO;
        if (plannedQty.compareTo(BigDecimal.ZERO) != 0) {
            variancePct = variance.divide(plannedQty, 4, RoundingMode.HALF_UP)
                                  .multiply(BigDecimal.valueOf(100))
                                  .setScale(2, RoundingMode.HALF_UP);
        }

        return VarianceDto.builder()
                .reportId(reportId)
                .planId(planId)
                .fgCode(fgCode)
                .rmCode(planned.getRmCode())
                .rmName(planned.getRmName())
                .materialType(planned.getMaterialType())
                .unit(planned.getUnit())
                .isScrap(planned.getIsScrap())
                .plannedQty(plannedQty)
                .actualQty(actualQty)
                .variance(variance)
                .variancePercent(variancePct)
                .status(resolveStatus(variancePct))
                .build();
    }

    private String resolveStatus(BigDecimal variancePct) {
        BigDecimal abs = variancePct.abs();
        if (abs.compareTo(BigDecimal.valueOf(5)) <= 0)  return "GREEN";
        if (abs.compareTo(BigDecimal.valueOf(10)) <= 0) return "YELLOW";
        return "RED";
    }

    private void recalcStatus(VarianceDto v) {
        if (v.getPlannedQty().compareTo(BigDecimal.ZERO) == 0) {
            v.setVariancePercent(BigDecimal.ZERO);
            v.setStatus("GREEN");
            return;
        }
        BigDecimal pct = v.getVariance()
                .divide(v.getPlannedQty(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        v.setVariancePercent(pct);
        v.setStatus(resolveStatus(pct));
    }
}
