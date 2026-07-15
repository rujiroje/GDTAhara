package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.MaterialRequirementDto;
import com.gdtahara.gdtaharabackend.model.BillOfMaterials;
import com.gdtahara.gdtaharabackend.model.BomItem;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.repository.BillOfMaterialsRepository;
import com.gdtahara.gdtaharabackend.repository.BomItemRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialStockTransactionRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionPlanRepository;
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
public class MaterialRequirementService {

    private final ProductionPlanRepository planRepository;
    private final BillOfMaterialsRepository bomRepository;
    private final BomItemRepository bomItemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockTransactionRepository stockTxRepository;

    // ----------------------------------------------------------
    // คำนวณความต้องการวัตถุดิบสำหรับ Plan ID เดียว
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MaterialRequirementDto> calculateForPlan(Long planId) {
        ProductionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบ Plan ID: " + planId));
        List<MaterialRequirementDto> result = calculateFromPlan(plan);
        enrichWithStock(result);
        return result;
    }

    // ----------------------------------------------------------
    // คำนวณรวมทุก Plan ในช่วงวันที่ (grouped by rm_code)
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MaterialRequirementDto> calculateForDateRange(LocalDate from, LocalDate to) {
        List<ProductionPlan> plans = planRepository.findByPlanDateBetween(from, to);
        if (plans.isEmpty()) return Collections.emptyList();

        Map<String, MaterialRequirementDto> aggregated = new LinkedHashMap<>();

        for (ProductionPlan plan : plans) {
            List<MaterialRequirementDto> items = calculateFromPlan(plan);
            for (MaterialRequirementDto item : items) {
                String key = item.getRmCode() + "|" + item.getUnit();
                aggregated.merge(key, item, (existing, newItem) -> {
                    existing.setPlannedQuantity(existing.getPlannedQuantity().add(newItem.getPlannedQuantity()));
                    existing.setPlannedLossQuantity(existing.getPlannedLossQuantity().add(newItem.getPlannedLossQuantity()));
                    existing.setTotalRequired(existing.getTotalRequired().add(newItem.getTotalRequired()));
                    return existing;
                });
            }
        }
        List<MaterialRequirementDto> result = new ArrayList<>(aggregated.values());
        enrichWithStock(result);
        return result;
    }

    // ----------------------------------------------------------
    // คำนวณสำหรับ Machine + วันที่ที่กำหนด
    // ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<MaterialRequirementDto> calculateForMachineDate(Long machineId, LocalDate date) {
        List<ProductionPlan> plans = planRepository
                .findByMachineIdAndPlanDateBetweenOrderByPlanDate(machineId, date, date);
        List<MaterialRequirementDto> result = plans.stream()
                .flatMap(plan -> calculateFromPlan(plan).stream())
                .collect(Collectors.toList());
        enrichWithStock(result);
        return result;
    }

    // ----------------------------------------------------------
    // Core logic: คำนวณจาก 1 ProductionPlan
    // ----------------------------------------------------------
    private List<MaterialRequirementDto> calculateFromPlan(ProductionPlan plan) {
        String fgCode   = plan.getProduct().getProductCode();
        LocalDate date  = plan.getPlanDate();
        BigDecimal target = BigDecimal.valueOf(plan.getTargetQty() != null ? plan.getTargetQty() : 0);

        Optional<BillOfMaterials> bomOpt = bomRepository.findActiveByFgCodeAndDate(fgCode, date);
        if (bomOpt.isEmpty()) {
            log.warn("ไม่พบ BOM สำหรับ fgCode={} date={}", fgCode, date);
            return Collections.emptyList();
        }

        BillOfMaterials bom = bomOpt.get();
        List<BomItem> items = bomItemRepository.findByBomIdOrderByItemNumber(bom.getId());

        return items.stream()
                .map(item -> toDto(item, bom, plan, target))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // เติม currentStock + shortfall หลังคำนวณ
    // ใช้ findAll() + normalize ใน Java เพื่อป้องกัน trailing-space หรือ case mismatch
    // ที่เกิดจาก Excel import (แทน findByMaterialCode ที่ใช้ SQL WHERE ตรงๆ)
    // ----------------------------------------------------------
    private void enrichWithStock(List<MaterialRequirementDto> items) {
        if (items.isEmpty()) return;

        // โหลด materials ทั้งหมด → map: normalized(materialCode) → materialId
        Map<String, Long> normalizedCodeToId = new HashMap<>();
        materialRepository.findAll().forEach(mat -> {
            if (mat.getMaterialCode() != null) {
                String key = mat.getMaterialCode().trim().toUpperCase(Locale.ROOT);
                normalizedCodeToId.put(key, mat.getId());
            }
        });

        log.info("enrichWithStock: loaded {} materials; looking up {} distinct rmCodes",
                normalizedCodeToId.size(),
                items.stream().map(MaterialRequirementDto::getRmCode).filter(Objects::nonNull).distinct().count());

        // รวม materialId ที่พบ → ดึง stock balance
        Set<Long> matchedIds = new HashSet<>();
        for (MaterialRequirementDto item : items) {
            if (item.getRmCode() == null) continue;
            Long id = normalizedCodeToId.get(item.getRmCode().trim().toUpperCase(Locale.ROOT));
            if (id != null) matchedIds.add(id);
        }

        // material ID → stock balance (IN - OUT)
        Map<Long, BigDecimal> idToStock = new HashMap<>();
        for (Long matId : matchedIds) {
            BigDecimal balance = stockTxRepository.getStockBalanceByMaterialId(matId);
            idToStock.put(matId, balance != null ? balance : BigDecimal.ZERO);
        }

        // เติมค่าลงใน dto แต่ละรายการ
        for (MaterialRequirementDto item : items) {
            if (item.getRmCode() == null) {
                item.setCurrentStock(null);
                item.setShortfall(null);
                continue;
            }
            Long matId = normalizedCodeToId.get(item.getRmCode().trim().toUpperCase(Locale.ROOT));
            if (matId == null) {
                // rmCode ไม่ได้ลงทะเบียนใน materials table → แสดง null
                item.setCurrentStock(null);
                item.setShortfall(null);
            } else {
                BigDecimal stock = idToStock.getOrDefault(matId, BigDecimal.ZERO);
                item.setCurrentStock(stock.setScale(3, RoundingMode.HALF_UP));
                BigDecimal shortfall = item.getTotalRequired().subtract(stock);
                item.setShortfall(shortfall.compareTo(BigDecimal.ZERO) > 0
                        ? shortfall.setScale(3, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
            }
        }
    }

    // ----------------------------------------------------------
    // สูตรคำนวณ:
    //   plannedQty     = target × quantityPer
    //   plannedLossQty = plannedQty × lossPercent / 100
    //   totalRequired  = plannedQty + plannedLossQty
    // ----------------------------------------------------------
    private MaterialRequirementDto toDto(BomItem item, BillOfMaterials bom,
                                         ProductionPlan plan, BigDecimal target) {
        BigDecimal qtyPer = item.getQuantityPer() != null ? item.getQuantityPer() : BigDecimal.ZERO;
        BigDecimal loss   = item.getLossPercent()  != null ? item.getLossPercent()  : BigDecimal.ZERO;

        BigDecimal planned     = target.multiply(qtyPer).setScale(4, RoundingMode.HALF_UP);
        BigDecimal plannedLoss = planned.multiply(loss)
                                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal total       = planned.add(plannedLoss).setScale(4, RoundingMode.HALF_UP);

        String machineName = plan.getMachine() != null ? plan.getMachine().getMachineName() : null;

        return MaterialRequirementDto.builder()
                .planId(plan.getId())
                .planDate(plan.getPlanDate())
                .machineName(machineName)
                .fgCode(plan.getProduct().getProductCode())
                .targetQty(plan.getTargetQty())
                .bomId(bom.getId())
                .bomItemId(item.getId())
                .rmCode(item.getRmCode())
                .rmName(item.getRmName())
                .materialType(item.getMaterialType())
                .unit(item.getUnit())
                .quantityPer(qtyPer)
                .lossPercent(loss)
                .isScrap(item.getIsScrap())
                .plannedQuantity(planned)
                .plannedLossQuantity(plannedLoss)
                .totalRequired(total)
                .build();
    }
}
