package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.LowStockAlert;
import com.gdtahara.gdtaharabackend.model.MaterialReorderConfig;
import com.gdtahara.gdtaharabackend.repository.LowStockAlertRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialReorderConfigRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockAlertScheduler {

    private final EntityManager em;
    private final MaterialReorderConfigRepository reorderConfigRepo;
    private final LowStockAlertRepository alertRepo;

    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void checkLowStock() {
        log.info("Low-stock check started");

        Map<String, MaterialReorderConfig> configMap = reorderConfigRepo.findByIsActiveTrue().stream()
                .collect(Collectors.toMap(MaterialReorderConfig::getMaterialCode, c -> c));
        if (configMap.isEmpty()) {
            log.debug("No active reorder configs — skipping");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT material_code, balance_kg FROM v_material_balance WHERE material_code IN (:codes)"
        ).setParameter("codes", new ArrayList<>(configMap.keySet())).getResultList();

        Map<String, BigDecimal> balanceMap = new HashMap<>();
        for (Object[] r : rows) {
            if (r[1] != null) balanceMap.put((String) r[0], new BigDecimal(r[1].toString()));
        }

        Map<String, LowStockAlert> activeAlerts = alertRepo.findByResolvedAtIsNullOrderByAlertDateDesc().stream()
                .collect(Collectors.toMap(LowStockAlert::getMaterialCode, a -> a, (a, b) -> a));

        for (Map.Entry<String, MaterialReorderConfig> entry : configMap.entrySet()) {
            String mc = entry.getKey();
            MaterialReorderConfig cfg = entry.getValue();
            BigDecimal balance = balanceMap.getOrDefault(mc, BigDecimal.ZERO);
            boolean isLow = balance.compareTo(cfg.getReorderPointKg()) <= 0;

            if (isLow && !activeAlerts.containsKey(mc)) {
                LowStockAlert alert = new LowStockAlert();
                alert.setMaterialCode(mc);
                alert.setBalanceAtAlert(balance);
                alert.setReorderPoint(cfg.getReorderPointKg());
                alert.setAlertDate(LocalDate.now());
                alert.setTelegramSent(false);
                alertRepo.save(alert);
                log.warn("Low stock alert created: {} balance={} kg (reorder point={} kg)",
                        mc, balance, cfg.getReorderPointKg());
                // TODO: Telegram notification
            } else if (!isLow && activeAlerts.containsKey(mc)) {
                LowStockAlert alert = activeAlerts.get(mc);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepo.save(alert);
                log.info("Low stock alert resolved: {} balance={} kg", mc, balance);
            }
        }
        log.info("Low-stock check finished — checked {} materials", configMap.size());
    }
}
