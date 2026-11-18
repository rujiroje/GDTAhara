package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.LowStockAlert;
import com.gdtahara.gdtaharabackend.model.MaterialReorderConfig;
import com.gdtahara.gdtaharabackend.repository.LowStockAlertRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialReorderConfigRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CmStockService {

    private final EntityManager em;
    private final MaterialRepository materialRepo;
    private final MaterialReorderConfigRepository reorderConfigRepo;
    private final LowStockAlertRepository alertRepo;

    // ── Overview ──────────────────────────────────────────────────────────────

    public StockOverviewDto getOverview() {
        // Query v_material_balance for all materials
        List<Object[]> balanceRows = em.createNativeQuery(
                "SELECT material_code, total_in_kg, total_out_kg, total_correction_kg, " +
                "balance_kg, lot_count, last_movement_at FROM v_material_balance ORDER BY material_code"
        ).getResultList();

        // Build reorder config lookup
        Map<String, MaterialReorderConfig> configMap = reorderConfigRepo.findByIsActiveTrue().stream()
                .collect(Collectors.toMap(MaterialReorderConfig::getMaterialCode, c -> c));

        // Build active alerts lookup
        Set<String> alertedCodes = alertRepo.findByResolvedAtIsNullOrderByAlertDateDesc().stream()
                .map(LowStockAlert::getMaterialCode)
                .collect(Collectors.toSet());

        // Build material name lookup
        Map<String, String> nameMap = new HashMap<>();
        materialRepo.findAll().forEach(m -> nameMap.put(m.getMaterialCode(), m.getMaterialName()));

        List<StockOverviewItemDto> items = new ArrayList<>();
        BigDecimal totalBalance = BigDecimal.ZERO;
        int totalLots = 0;

        for (Object[] row : balanceRows) {
            StockOverviewItemDto item = new StockOverviewItemDto();
            item.setMaterialCode((String) row[0]);
            item.setTotalInKg(toBd(row[1]));
            item.setTotalOutKg(toBd(row[2]));
            item.setBalanceKg(toBd(row[4]));
            item.setLotCount(toInt(row[5]));
            item.setLastMovementAt(toDateTime(row[6]));
            item.setMaterialName(nameMap.getOrDefault(item.getMaterialCode(), item.getMaterialCode()));

            MaterialReorderConfig cfg = configMap.get(item.getMaterialCode());
            if (cfg != null) {
                item.setReorderPointKg(cfg.getReorderPointKg());
                item.setSafetyStockKg(cfg.getSafetyStockKg());
                item.setStockStatus(calcStatus(item.getBalanceKg(), cfg));
            } else {
                item.setStockStatus("UNKNOWN");
            }
            item.setHasActiveAlert(alertedCodes.contains(item.getMaterialCode()));

            if (item.getBalanceKg() != null) totalBalance = totalBalance.add(item.getBalanceKg());
            if (item.getLotCount() != null) totalLots += item.getLotCount();
            items.add(item);
        }

        StockOverviewDto dto = new StockOverviewDto();
        dto.setMaterials(items);
        dto.setTotalBalanceKg(totalBalance);
        dto.setTotalActiveLots(totalLots);
        dto.setAlertCount(alertedCodes.size());
        return dto;
    }

    // ── Material Detail ───────────────────────────────────────────────────────

    public MaterialDetailDto getMaterialDetail(String materialCode) {
        // Per-lot balances from v_material_lot_balance
        List<Object[]> lotRows = em.createNativeQuery(
                "SELECT lot_ref, balance_kg, received_at, last_activity_at " +
                "FROM v_material_lot_balance WHERE material_code = :mc ORDER BY received_at ASC"
        ).setParameter("mc", materialCode).getResultList();

        // Overall balance
        List<Object[]> balRows = em.createNativeQuery(
                "SELECT balance_kg, total_in_kg, total_out_kg FROM v_material_balance WHERE material_code = :mc"
        ).setParameter("mc", materialCode).getResultList();

        BigDecimal balance = balRows.isEmpty() ? BigDecimal.ZERO : toBd(balRows.get(0)[0]);
        String materialName = materialRepo.findByMaterialCode(materialCode)
                .map(m -> m.getMaterialName()).orElse(materialCode);

        // 7-day average daily usage
        BigDecimal weeklyOut = getOutKgSince(materialCode, LocalDate.now().minusDays(7));
        BigDecimal dailyAvg = weeklyOut.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
        BigDecimal estDays = dailyAvg.compareTo(BigDecimal.ZERO) > 0
                ? balance.divide(dailyAvg, 1, RoundingMode.HALF_UP)
                : null;

        MaterialReorderConfig cfg = reorderConfigRepo.findById(materialCode).orElse(null);

        List<LotBalanceDto> lots = new ArrayList<>();
        for (Object[] row : lotRows) {
            LotBalanceDto lot = new LotBalanceDto();
            lot.setLotRef((String) row[0]);
            lot.setBalanceKg(toBd(row[1]));
            lot.setReceivedAt(toDateTime(row[2]));
            lot.setLastActivityAt(toDateTime(row[3]));
            lots.add(lot);
        }

        MaterialDetailDto dto = new MaterialDetailDto();
        dto.setMaterialCode(materialCode);
        dto.setMaterialName(materialName);
        dto.setBalanceKg(balance);
        dto.setReorderPointKg(cfg != null ? cfg.getReorderPointKg() : null);
        dto.setStockStatus(cfg != null ? calcStatus(balance, cfg) : "UNKNOWN");
        dto.setLots(lots);
        dto.setDailyAvgUsageKg(dailyAvg);
        dto.setEstDaysRemaining(estDays);
        return dto;
    }

    // ── Stock Card ────────────────────────────────────────────────────────────

    public StockCardResponseDto getStockCard(String materialCode, LocalDate from, LocalDate to,
                                              String machineId, String lotRef) {
        StringBuilder sql = new StringBuilder(
                "SELECT movement_type, material_code, lot_ref, qty_kg, negative_qty, " +
                "movement_at, actor, machine_id, tank_id, note, is_correction, source_id, source_table " +
                "FROM v_stock_movement WHERE material_code = :mc");
        if (from != null) sql.append(" AND CAST(movement_at AS DATE) >= :fromDate");
        if (to != null)   sql.append(" AND CAST(movement_at AS DATE) <= :toDate");
        if (machineId != null && !machineId.isBlank()) sql.append(" AND machine_id = :machineId");
        if (lotRef != null && !lotRef.isBlank())       sql.append(" AND lot_ref = :lotRef");
        sql.append(" ORDER BY movement_at ASC");

        var query = em.createNativeQuery(sql.toString()).setParameter("mc", materialCode);
        if (from != null) query.setParameter("fromDate", from.toString());
        if (to != null)   query.setParameter("toDate", to.toString());
        if (machineId != null && !machineId.isBlank()) query.setParameter("machineId", machineId);
        if (lotRef != null && !lotRef.isBlank())       query.setParameter("lotRef", lotRef);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        // Compute opening balance = total balance before 'from' date
        BigDecimal openingBalance = from != null ? getBalanceBefore(materialCode, from) : BigDecimal.ZERO;

        String materialName = materialRepo.findByMaterialCode(materialCode)
                .map(m -> m.getMaterialName()).orElse(materialCode);

        List<StockCardItemDto> movements = new ArrayList<>();
        BigDecimal running = openingBalance;
        BigDecimal totalIn = BigDecimal.ZERO, totalOut = BigDecimal.ZERO, totalCorr = BigDecimal.ZERO;

        for (Object[] row : rows) {
            StockCardItemDto item = new StockCardItemDto();
            item.setMovementType((String) row[0]);
            item.setMaterialCode((String) row[1]);
            item.setLotRef((String) row[2]);
            item.setQtyKg(toBd(row[3]));
            item.setNegativeQty(toBd(row[4]));
            item.setMovementAt(toDateTime(row[5]));
            item.setActor((String) row[6]);
            item.setMachineId((String) row[7]);
            item.setTankId((String) row[8]);
            item.setNote((String) row[9]);
            Object isCorr = row[10];
            item.setIsCorrection(isCorr instanceof Boolean ? (Boolean) isCorr
                    : (isCorr instanceof Number && ((Number) isCorr).intValue() == 1));
            item.setSourceId(toLong(row[11]));
            item.setSourceTable((String) row[12]);

            switch (item.getMovementType()) {
                case "IN":
                    running = running.add(nvl(item.getQtyKg()));
                    totalIn = totalIn.add(nvl(item.getQtyKg()));
                    break;
                case "OUT":
                    running = running.subtract(nvl(item.getQtyKg()));
                    totalOut = totalOut.add(nvl(item.getQtyKg()));
                    break;
                case "CORRECTION":
                    // negative_qty is negative; add it back (reduce OUT)
                    running = running.subtract(nvl(item.getNegativeQty()));
                    totalCorr = totalCorr.add(nvl(item.getNegativeQty()));
                    break;
                default:
                    break;
            }
            item.setRunningBalance(running);
            movements.add(item);
        }

        StockCardResponseDto dto = new StockCardResponseDto();
        dto.setMaterialCode(materialCode);
        dto.setMaterialName(materialName);
        dto.setMovements(movements);
        dto.setTotalInKg(totalIn);
        dto.setTotalOutKg(totalOut);
        dto.setTotalCorrectionKg(totalCorr);
        dto.setOpeningBalance(openingBalance);
        dto.setClosingBalance(running);
        return dto;
    }

    // ── Multi-material Summary ────────────────────────────────────────────────

    public List<MultiMaterialSummaryItemDto> getMultiMaterialSummary(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<Object[]> balanceRows = em.createNativeQuery(
                "SELECT material_code, balance_kg FROM v_material_balance ORDER BY material_code"
        ).getResultList();

        Map<String, MaterialReorderConfig> configMap = reorderConfigRepo.findByIsActiveTrue().stream()
                .collect(Collectors.toMap(MaterialReorderConfig::getMaterialCode, c -> c));
        Map<String, String> nameMap = new HashMap<>();
        materialRepo.findAll().forEach(m -> nameMap.put(m.getMaterialCode(), m.getMaterialName()));

        List<MultiMaterialSummaryItemDto> result = new ArrayList<>();
        for (Object[] row : balanceRows) {
            String mc = (String) row[0];
            BigDecimal balance = toBd(row[1]);

            BigDecimal used = getOutKgSince(mc, since);
            BigDecimal received = getInKgSince(mc, since);
            BigDecimal change = received.subtract(used);
            String trend = change.compareTo(BigDecimal.ZERO) > 0 ? "UP"
                         : change.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE";

            MaterialReorderConfig cfg = configMap.get(mc);
            MultiMaterialSummaryItemDto item = new MultiMaterialSummaryItemDto();
            item.setMaterialCode(mc);
            item.setMaterialName(nameMap.getOrDefault(mc, mc));
            item.setBalanceKg(balance);
            item.setUsedKg(used);
            item.setReceivedKg(received);
            item.setChangeKg(change);
            item.setTrend(trend);
            item.setStockStatus(cfg != null ? calcStatus(balance, cfg) : "UNKNOWN");
            result.add(item);
        }
        return result;
    }

    // ── Low-stock Alerts ──────────────────────────────────────────────────────

    public List<LowStockAlertDto> getActiveLowStockAlerts() {
        return alertRepo.findByResolvedAtIsNullOrderByAlertDateDesc().stream()
                .map(this::toAlertDto)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal getOutKgSince(String materialCode, LocalDate since) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT COALESCE(SUM(qty_kg), 0) FROM v_stock_movement " +
                "WHERE material_code = :mc AND movement_type = 'OUT' AND CAST(movement_at AS DATE) >= :since"
        ).setParameter("mc", materialCode).setParameter("since", since.toString()).getResultList();
        return rows.isEmpty() ? BigDecimal.ZERO : nvl(rows.get(0)[0]);
    }

    private BigDecimal getInKgSince(String materialCode, LocalDate since) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT COALESCE(SUM(qty_kg), 0) FROM v_stock_movement " +
                "WHERE material_code = :mc AND movement_type = 'IN' AND CAST(movement_at AS DATE) >= :since"
        ).setParameter("mc", materialCode).setParameter("since", since.toString()).getResultList();
        return rows.isEmpty() ? BigDecimal.ZERO : nvl(rows.get(0)[0]);
    }

    private BigDecimal getBalanceBefore(String materialCode, LocalDate before) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT COALESCE(SUM(CASE " +
                "  WHEN movement_type = 'IN' THEN qty_kg " +
                "  WHEN movement_type = 'OUT' THEN -qty_kg " +
                "  WHEN movement_type = 'CORRECTION' THEN -negative_qty " +
                "  ELSE 0 END), 0) " +
                "FROM v_stock_movement " +
                "WHERE material_code = :mc AND CAST(movement_at AS DATE) < :before"
        ).setParameter("mc", materialCode).setParameter("before", before.toString()).getResultList();
        return rows.isEmpty() ? BigDecimal.ZERO : nvl(rows.get(0)[0]);
    }

    private String calcStatus(BigDecimal balance, MaterialReorderConfig cfg) {
        if (balance == null || cfg == null) return "UNKNOWN";
        if (cfg.getSafetyStockKg() != null && balance.compareTo(cfg.getSafetyStockKg()) <= 0) return "CRITICAL";
        if (balance.compareTo(cfg.getReorderPointKg()) <= 0) return "LOW";
        return "NORMAL";
    }

    private LowStockAlertDto toAlertDto(LowStockAlert a) {
        LowStockAlertDto d = new LowStockAlertDto();
        d.setId(a.getId());
        d.setMaterialCode(a.getMaterialCode());
        d.setBalanceAtAlert(a.getBalanceAtAlert());
        d.setReorderPoint(a.getReorderPoint());
        d.setEstDaysRemaining(a.getEstDaysRemaining());
        d.setAlertDate(a.getAlertDate());
        d.setTelegramSent(a.getTelegramSent());
        d.setTelegramSentAt(a.getTelegramSentAt());
        d.setResolvedAt(a.getResolvedAt());
        return d;
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        return new BigDecimal(v.toString());
    }

    private BigDecimal nvl(Object v) {
        BigDecimal bd = toBd(v);
        return bd == null ? BigDecimal.ZERO : bd;
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    private LocalDateTime toDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime();
        return null;
    }
}
