package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.DailyBlowReportDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyBlowReportService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductionReportRepository reportRepo;
    private final PackagingLogRepository packagingLogRepo;
    private final NgLogRepository ngLogRepo;
    private final MaterialStockTransactionRepository matTxRepo;
    private final ScrapWeightLogRepository scrapRepo;
    private final DowntimeEventRepository downtimeRepo;
    private final BillOfMaterialsRepository bomRepo;
    private final BomItemRepository bomItemRepo;

    public DailyBlowReportDto buildReport(Long reportId, LocalDate date) {

        ProductionReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay   = date.atTime(LocalTime.MAX);

        DailyBlowReportDto dto = new DailyBlowReportDto();

        // ── Header ──────────────────────────────────────────────────────────
        dto.setReportDate(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dto.setMachineName(report.getMachine() != null ? report.getMachine().getMachineName() : "");
        dto.setProductCode(report.getProduct() != null ? report.getProduct().getProductCode() : "");
        dto.setProductName(report.getProduct() != null ? report.getProduct().getProductName() : "");
        dto.setOrderNumber(report.getOrderNumber());
        dto.setParentLotNumber(report.getParentLotNumber());
        dto.setTargetQty(report.getTargetQty());
        dto.setShift(report.getShift());

        // ── Section 1: Productivity ──────────────────────────────────────────
        long boxesPacked = packagingLogRepo.countByReportIdAndTimestampBetween(reportId, startOfDay, endOfDay);
        dto.setTotalBoxesPacked(boxesPacked);

        List<NgLog> ngLogs = ngLogRepo.findByReportIdAndTimestampBetween(reportId, startOfDay, endOfDay);
        long totalNg = ngLogs.stream().mapToLong(n -> n.getQuantity() != null ? n.getQuantity() : 0).sum();
        dto.setTotalNgQty(totalNg);

        // ── Section 2: Packing Materials (UNBW BOM) ──────────────────────────
        dto.setPackingMaterials(buildPackingMaterials(report, date, boxesPacked));

        // ── Section 3: Raw Material Usage ────────────────────────────────────
        dto.setRawMaterials(buildRawMaterials(reportId, startOfDay, endOfDay));

        // ── Section 4: Labor ──────────────────────────────────────────────────
        dto.setActiveUsers(collectActiveUsers(reportId, startOfDay, endOfDay));

        // ── Section 5: NG Details + Scrap ────────────────────────────────────
        dto.setNgDetails(buildNgRows(ngLogs));
        List<ScrapWeightLog> scraps = scrapRepo.findByReportIdAndTimestampBetween(reportId, startOfDay, endOfDay);
        dto.setScrapDetails(buildScrapRows(scraps));
        dto.setScrapComparison(buildScrapComparison(report, date, boxesPacked, scraps));

        // ── Section 6: Downtime ───────────────────────────────────────────────
        List<DowntimeEvent> downtimes = downtimeRepo.findByReportIdAndStartTimeBetween(reportId, startOfDay, endOfDay);
        dto.setDowntimeEvents(buildDowntimeRows(downtimes));
        long totalDowntimeMin = downtimes.stream()
                .filter(d -> d.getStartTime() != null && d.getEndTime() != null)
                .mapToLong(d -> java.time.Duration.between(d.getStartTime(), d.getEndTime()).toMinutes())
                .sum();
        dto.setTotalDowntimeMinutes(totalDowntimeMin);

        return dto;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<DailyBlowReportDto.PackingMaterialRow> buildPackingMaterials(
            ProductionReport report, LocalDate date, long boxesPacked) {

        if (report.getProduct() == null) return Collections.emptyList();

        String fgCode = report.getProduct().getProductCode();
        Optional<BillOfMaterials> bomOpt = bomRepo.findActiveByFgCodeAndDate(fgCode, date);
        if (bomOpt.isEmpty()) return Collections.emptyList();

        List<BomItem> items = bomItemRepo.findByBomIdOrderByItemNumber(bomOpt.get().getId());

        return items.stream()
                .filter(i -> "UNBW".equalsIgnoreCase(i.getMaterialType()) && Boolean.FALSE.equals(i.getIsScrap()))
                .map(i -> {
                    BigDecimal qty = i.getQuantityPer() != null ? i.getQuantityPer() : BigDecimal.ZERO;
                    BigDecimal total = qty.multiply(BigDecimal.valueOf(boxesPacked));
                    return new DailyBlowReportDto.PackingMaterialRow(
                            i.getRmCode(), i.getRmName(), i.getUnit(), qty, boxesPacked, total);
                })
                .collect(Collectors.toList());
    }

    private List<DailyBlowReportDto.RawMaterialRow> buildRawMaterials(
            Long reportId, LocalDateTime start, LocalDateTime end) {

        List<MaterialStockTransaction> txs = matTxRepo
                .findByProductionReportIdAndTransactionTypeAndTimestampBetween(reportId, "OUT", start, end);

        // Group by materialCode + lotNumber to show each stock-out entry
        Map<String, DailyBlowReportDto.RawMaterialRow> map = new LinkedHashMap<>();
        for (MaterialStockTransaction tx : txs) {
            if (tx.getMaterial() == null) continue;
            // Exclude UNBW — those are packing materials shown in Section 2
            if ("UNBW".equalsIgnoreCase(tx.getMaterial().getMaterialType())) continue;

            String key = tx.getMaterial().getMaterialCode() + "|" + nullSafe(tx.getLotNumber());
            map.merge(key,
                    new DailyBlowReportDto.RawMaterialRow(
                            tx.getMaterial().getMaterialCode(),
                            tx.getMaterial().getMaterialName(),
                            tx.getMaterial().getMaterialType(),
                            tx.getMaterial().getUnit(),
                            tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO,
                            tx.getLotNumber()),
                    (existing, newRow) -> {
                        existing.setTotalUsed(existing.getTotalUsed().add(newRow.getTotalUsed()));
                        return existing;
                    });
        }
        return new ArrayList<>(map.values());
    }

    private List<String> collectActiveUsers(Long reportId, LocalDateTime start, LocalDateTime end) {
        Set<String> users = new LinkedHashSet<>();

        packagingLogRepo.findByReportIdAndTimestampBetween(reportId, start, end)
                .forEach(p -> addUser(users, p.getOperator()));

        ngLogRepo.findByReportIdAndTimestampBetween(reportId, start, end)
                .forEach(n -> addUser(users, n.getUser()));

        matTxRepo.findByProductionReportIdAndTimestampBetween(reportId, start, end)
                .forEach(m -> addUser(users, m.getUser()));

        scrapRepo.findByReportIdAndTimestampBetween(reportId, start, end)
                .forEach(s -> addUser(users, s.getTechnician()));

        downtimeRepo.findByReportIdAndStartTimeBetween(reportId, start, end)
                .forEach(d -> addUser(users, d.getTechnician()));

        return new ArrayList<>(users);
    }

    private List<DailyBlowReportDto.NgRow> buildNgRows(List<NgLog> logs) {
        return logs.stream().map(n -> new DailyBlowReportDto.NgRow(
                n.getNgType() != null ? n.getNgType().getNgType() : "",
                n.getNgType() != null ? firstNonBlank(
                        n.getNgType().getNgDescriptionTh(), n.getNgType().getNgCode(), "") : "",
                n.getSource(),
                n.getQuantity() != null ? n.getQuantity() : 0,
                n.getNote(),
                n.getTimestamp() != null ? n.getTimestamp().format(TS_FMT) : ""
        )).collect(Collectors.toList());
    }

    private List<DailyBlowReportDto.ScrapComparisonRow> buildScrapComparison(
            ProductionReport report, LocalDate date, long boxesPacked, List<ScrapWeightLog> scraps) {

        // ── BOM expected scrap — group isScrap=true items by materialType ──────
        Map<String, BigDecimal> expectedByType = new LinkedHashMap<>();
        Map<String, String> nameByType = new LinkedHashMap<>();

        if (report.getProduct() != null) {
            String fgCode = report.getProduct().getProductCode();
            bomRepo.findActiveByFgCodeAndDate(fgCode, date).ifPresent(bom -> {
                BigDecimal baseQty = (bom.getBaseQuantity() != null
                        && bom.getBaseQuantity().compareTo(BigDecimal.ZERO) > 0)
                        ? bom.getBaseQuantity() : BigDecimal.ONE;

                for (BomItem item : bomItemRepo.findByBomIdOrderByItemNumber(bom.getId())) {
                    if (!Boolean.TRUE.equals(item.getIsScrap())) continue;
                    String matType = item.getMaterialType() != null
                            ? item.getMaterialType().toUpperCase() : "OTHER";
                    BigDecimal qtyPer = item.getQuantityPer() != null
                            ? item.getQuantityPer() : BigDecimal.ZERO;
                    BigDecimal expected = boxesPacked > 0
                            ? qtyPer.multiply(BigDecimal.valueOf(boxesPacked))
                                    .divide(baseQty, 3, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    expectedByType.merge(matType, expected, BigDecimal::add);
                    nameByType.putIfAbsent(matType,
                            item.getRmName() != null ? item.getRmName() : matType);
                }
            });
        }

        // ── Actual scrap — sum by matType ────────────────────────────────────
        Map<String, BigDecimal> actualByType = new LinkedHashMap<>();
        for (ScrapWeightLog s : scraps) {
            String matType = s.getMatType() != null ? s.getMatType().toUpperCase() : "OTHER";
            BigDecimal weight = s.getWeightKg() != null ? s.getWeightKg() : BigDecimal.ZERO;
            actualByType.merge(matType, weight, BigDecimal::add);
        }

        // ── Merge — include types from either source ──────────────────────────
        Set<String> allTypes = new LinkedHashSet<>();
        allTypes.addAll(expectedByType.keySet());
        allTypes.addAll(actualByType.keySet());

        List<DailyBlowReportDto.ScrapComparisonRow> result = new ArrayList<>();
        for (String matType : allTypes) {
            BigDecimal expected = expectedByType.getOrDefault(matType, BigDecimal.ZERO);
            BigDecimal actual   = actualByType.getOrDefault(matType, BigDecimal.ZERO);
            BigDecimal variance = actual.subtract(expected);
            result.add(new DailyBlowReportDto.ScrapComparisonRow(
                    matType, nameByType.getOrDefault(matType, matType),
                    expected, actual, variance));
        }
        return result;
    }

    private List<DailyBlowReportDto.ScrapRow> buildScrapRows(List<ScrapWeightLog> logs) {
        return logs.stream().map(s -> new DailyBlowReportDto.ScrapRow(
                s.getScrapType(),
                s.getMatType(),
                s.getWeightKg(),
                s.getTechnician() != null ? s.getTechnician().getUsername() : "",
                s.getTimestamp() != null ? s.getTimestamp().format(TS_FMT) : ""
        )).collect(Collectors.toList());
    }

    private List<DailyBlowReportDto.DowntimeRow> buildDowntimeRows(List<DowntimeEvent> events) {
        return events.stream().map(d -> {
            long mins = (d.getStartTime() != null && d.getEndTime() != null)
                    ? java.time.Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() : 0;
            return new DailyBlowReportDto.DowntimeRow(
                    d.getCategory() != null ? d.getCategory() : "",
                    d.getDowntimeType() != null ? d.getDowntimeType() : "",
                    d.getReason(),
                    d.getSolution(),
                    d.getStartTime() != null ? d.getStartTime().format(TS_FMT) : "",
                    d.getEndTime() != null ? d.getEndTime().format(TS_FMT) : "",
                    mins);
        }).collect(Collectors.toList());
    }

    private void addUser(Set<String> set, User user) {
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            set.add(user.getUsername());
        }
    }

    private String nullSafe(String s) { return s != null ? s : ""; }

    private String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return "";
    }
}
