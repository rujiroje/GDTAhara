package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.dto.MachineRefillHistoryDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CmMeterService {

    private static final Logger log = LoggerFactory.getLogger(CmMeterService.class);

    private final MachineTankRepository tankRepo;
    private final TankMeterReadingRepository meterRepo;
    private final TankRefillRepository refillRepo;
    private final ShiftMaterialConsumptionRepository consumptionRepo;
    private final ShiftBoundaryConfigRepository shiftConfigRepo;
    private final MaterialStockTransactionRepository stockTxRepo;

    // ── Shift detection ───────────────────────────────────────────────────────

    public CurrentShiftDto getCurrentShift() {
        List<ShiftBoundaryConfig> configs = shiftConfigRepo.findAllByIsActiveTrue();
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        for (ShiftBoundaryConfig cfg : configs) {
            LocalTime start = cfg.getStartTime();
            LocalTime end = cfg.getEndTime();
            boolean crossesMidnight = start.isAfter(end);

            if (crossesMidnight) {
                // e.g. Shift C: 23:00–07:00
                if (now.compareTo(start) >= 0 || now.compareTo(end) < 0) {
                    LocalDate shiftDate = now.compareTo(start) >= 0 ? today : today.minusDays(1);
                    CurrentShiftDto dto = new CurrentShiftDto();
                    dto.setShiftCode(cfg.getShiftCode());
                    dto.setShiftDate(shiftDate);
                    dto.setStartTime(start.toString());
                    dto.setEndTime(end.toString());
                    return dto;
                }
            } else {
                if (now.compareTo(start) >= 0 && now.compareTo(end) < 0) {
                    CurrentShiftDto dto = new CurrentShiftDto();
                    dto.setShiftCode(cfg.getShiftCode());
                    dto.setShiftDate(today);
                    dto.setStartTime(start.toString());
                    dto.setEndTime(end.toString());
                    return dto;
                }
            }
        }
        // fallback — no matching shift (between shifts)
        CurrentShiftDto dto = new CurrentShiftDto();
        dto.setShiftDate(today);
        return dto;
    }

    // ── Tank queries ──────────────────────────────────────────────────────────

    public List<TankStatusDto> getTanksByMachine(String machineId) {
        CurrentShiftDto shift = getCurrentShift();
        return tankRepo.findByMachineIdAndIsActiveTrue(machineId)
                .stream()
                .map(t -> buildTankStatus(t, shift))
                .collect(Collectors.toList());
    }

    public TankStatusDto getTankStatus(String tankId) {
        MachineTank tank = tankRepo.findById(tankId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบถัง tankId=" + tankId));
        CurrentShiftDto shift = getCurrentShift();
        return buildTankStatus(tank, shift);
    }

    private TankStatusDto buildTankStatus(MachineTank tank, CurrentShiftDto shift) {
        TankStatusDto dto = new TankStatusDto();
        dto.setTankId(tank.getTankId());
        dto.setMachineId(tank.getMachineId());
        dto.setTankCode(tank.getTankCode());
        dto.setTankName(tank.getTankName());
        dto.setDefaultMaterialCode(tank.getDefaultMaterialCode());
        dto.setCapacityKg(tank.getCapacityKg());
        dto.setPhysicalPosition(tank.getPhysicalPosition());
        dto.setCurrentShiftCode(shift.getShiftCode());
        dto.setCurrentShiftDate(shift.getShiftDate());

        // latest reading overall
        List<TankMeterReading> latest = meterRepo.findLatestByTankId(tank.getTankId());
        if (!latest.isEmpty()) {
            TankMeterReading lr = latest.get(0);
            dto.setLatestMeterKg(lr.getMeterValueKg());
            dto.setLatestReadingAt(lr.getRecordedAt());
            dto.setLatestReadingType(lr.getReadingType());
        }

        if (shift.getShiftCode() != null && shift.getShiftDate() != null) {
            String sc = shift.getShiftCode();
            LocalDate sd = shift.getShiftDate();

            Optional<TankMeterReading> startReading = meterRepo
                    .findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtAsc(
                            tank.getTankId(), sd, sc, "SHIFT_START");
            dto.setShiftStartRecorded(startReading.isPresent());
            startReading.ifPresent(r -> dto.setShiftStartMeterKg(r.getMeterValueKg()));

            boolean endRecorded = meterRepo.existsByTankIdAndShiftDateAndShiftCodeAndReadingType(
                    tank.getTankId(), sd, sc, "SHIFT_END");
            dto.setShiftEndRecorded(endRecorded);

            BigDecimal totalRefills = refillRepo.sumRefillKg(tank.getTankId(), sd, sc);
            dto.setTotalRefillsKg(totalRefills);
            dto.setRefillCount(refillRepo.countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionFalse(
                    tank.getTankId(), sd, sc));
        }
        return dto;
    }

    // ── Meter reading ─────────────────────────────────────────────────────────

    public TankMeterReading recordMeterReading(String tankId, TankMeterEntryDto dto, String recordedBy) {
        MachineTank tank = tankRepo.findById(tankId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบถัง tankId=" + tankId));

        // ป้องกัน SHIFT_START ซ้ำในกะเดียวกัน
        if ("SHIFT_START".equals(dto.getReadingType()) &&
            meterRepo.existsByTankIdAndShiftDateAndShiftCodeAndReadingType(
                    tankId, dto.getShiftDate(), dto.getShiftCode(), "SHIFT_START")) {
            throw new IllegalStateException("บันทึก SHIFT_START ไว้แล้วในกะนี้ — ใช้ ADHOC สำหรับการอ่านเพิ่มเติม");
        }

        TankMeterReading reading = new TankMeterReading();
        reading.setTankId(tankId);
        reading.setMachineId(tank.getMachineId());
        reading.setReadingType(dto.getReadingType());
        reading.setMeterValueKg(dto.getMeterValueKg());
        reading.setShiftCode(dto.getShiftCode());
        reading.setShiftDate(dto.getShiftDate());
        reading.setRecordedBy(recordedBy);
        reading.setNote(dto.getNote());
        reading.setAutoGenerated(false);
        TankMeterReading saved = meterRepo.save(reading);

        log.info("บันทึก {} meter reading tankId={} value={} กะ={}/{} โดย {}",
                dto.getReadingType(), tankId, dto.getMeterValueKg(),
                dto.getShiftCode(), dto.getShiftDate(), recordedBy);

        updateConsumptionRecord(tankId, dto.getShiftDate(), dto.getShiftCode());
        return saved;
    }

    // ── Refill ────────────────────────────────────────────────────────────────

    public TankRefill recordRefill(String tankId, RefillRequestDto dto, String refillBy) {
        MachineTank tank = tankRepo.findById(tankId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบถัง tankId=" + tankId));

        if (dto.getQtyKg() == null || dto.getQtyKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("qtyKg ต้องมากกว่า 0");
        }

        // deduct from material stock lot when stockId is provided
        if (dto.getMaterialStockId() != null) {
            MaterialStockTransaction tx = stockTxRepo.findById(dto.getMaterialStockId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ไม่พบ lot materialStockId=" + dto.getMaterialStockId()));
            if (tx.getRemainingQty() == null || tx.getRemainingQty().compareTo(dto.getQtyKg()) < 0) {
                throw new IllegalStateException("วัตถุดิบใน lot ไม่เพียงพอ (คงเหลือ " +
                        (tx.getRemainingQty() == null ? 0 : tx.getRemainingQty()) + " kg)");
            }
            stockTxRepo.deductRemainingQty(dto.getMaterialStockId(), dto.getQtyKg());
        }

        TankRefill refill = new TankRefill();
        refill.setTankId(tankId);
        refill.setMachineId(tank.getMachineId());
        refill.setMaterialCode(dto.getMaterialCode());
        refill.setMaterialStockId(dto.getMaterialStockId());
        refill.setMaterialLotCode(dto.getMaterialLotCode());
        refill.setQtyKg(dto.getQtyKg());
        refill.setIsCorrection(false);
        refill.setRefillBy(refillBy);
        refill.setShiftCode(dto.getShiftCode());
        refill.setShiftDate(dto.getShiftDate());
        refill.setMaterialTypeOverride(Boolean.TRUE.equals(dto.getMaterialTypeOverride()));
        refill.setNote(dto.getNote());
        refill.setPhotoIds(dto.getPhotoIds());
        TankRefill saved = refillRepo.save(refill);

        log.info("บันทึก refill tankId={} material={} qty={}kg กะ={}/{} โดย {}",
                tankId, dto.getMaterialCode(), dto.getQtyKg(),
                dto.getShiftCode(), dto.getShiftDate(), refillBy);

        updateConsumptionRecord(tankId, dto.getShiftDate(), dto.getShiftCode());
        return saved;
    }

    // ── Correction ────────────────────────────────────────────────────────────

    public TankRefill recordCorrection(Long originalRefillId, CorrectionRequestDto dto, String correctedBy) {
        TankRefill original = refillRepo.findById(originalRefillId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ refill id=" + originalRefillId));

        if (Boolean.TRUE.equals(original.getIsCorrection())) {
            throw new IllegalStateException("ไม่สามารถแก้ไข correction ซ้อนกันได้");
        }

        // create a negative correction record (difference from original)
        BigDecimal diff = dto.getCorrectedQtyKg().subtract(original.getQtyKg());

        TankRefill correction = new TankRefill();
        correction.setTankId(original.getTankId());
        correction.setMachineId(original.getMachineId());
        correction.setMaterialCode(original.getMaterialCode());
        correction.setMaterialStockId(original.getMaterialStockId());
        correction.setMaterialLotCode(original.getMaterialLotCode());
        correction.setQtyKg(dto.getCorrectedQtyKg());
        correction.setIsCorrection(true);
        correction.setCorrectsRefillId(originalRefillId);
        correction.setRefillBy(correctedBy);
        correction.setShiftCode(original.getShiftCode());
        correction.setShiftDate(original.getShiftDate());
        correction.setNote(dto.getNote());
        TankRefill saved = refillRepo.save(correction);

        // adjust stock lot if linked
        if (original.getMaterialStockId() != null && diff.compareTo(BigDecimal.ZERO) != 0) {
            // positive diff = more used → deduct more; negative diff = less used → restore
            stockTxRepo.deductRemainingQty(original.getMaterialStockId(), diff);
        }

        log.info("บันทึก correction สำหรับ refillId={} qty={}→{} โดย {}",
                originalRefillId, original.getQtyKg(), dto.getCorrectedQtyKg(), correctedBy);

        updateConsumptionRecord(original.getTankId(), original.getShiftDate(), original.getShiftCode());
        return saved;
    }

    // ── Shift report ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ShiftReportDto getShiftReport(String tankId, LocalDate shiftDate, String shiftCode) {
        MachineTank tank = tankRepo.findById(tankId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบถัง tankId=" + tankId));

        Optional<TankMeterReading> startReading = meterRepo
                .findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtAsc(
                        tankId, shiftDate, shiftCode, "SHIFT_START");
        Optional<TankMeterReading> endReading = meterRepo
                .findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtDesc(
                        tankId, shiftDate, shiftCode, "SHIFT_END");

        BigDecimal startKg = startReading.map(TankMeterReading::getMeterValueKg).orElse(null);
        BigDecimal endKg = endReading.map(TankMeterReading::getMeterValueKg).orElse(null);
        BigDecimal totalRefills = refillRepo.sumRefillKg(tankId, shiftDate, shiftCode);
        int refillCount = refillRepo.countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionFalse(tankId, shiftDate, shiftCode);
        int correctionCount = refillRepo.countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionTrue(tankId, shiftDate, shiftCode);

        BigDecimal computedUsed = null;
        if (startKg != null && endKg != null) {
            // used = startMeter + totalRefills - endMeter
            computedUsed = startKg.add(totalRefills).subtract(endKg);
        }

        List<RefillHistoryItemDto> refills = refillRepo
                .findByTankIdAndShiftDateAndShiftCodeOrderByRefillAtDesc(tankId, shiftDate, shiftCode)
                .stream()
                .map(this::toRefillHistoryItem)
                .collect(Collectors.toList());

        Optional<ShiftMaterialConsumption> consumption = consumptionRepo
                .findByTankIdAndShiftDateAndShiftCode(tankId, shiftDate, shiftCode);

        ShiftReportDto report = new ShiftReportDto();
        report.setTankId(tankId);
        report.setTankCode(tank.getTankCode());
        report.setTankName(tank.getTankName());
        report.setMachineId(tank.getMachineId());
        report.setShiftCode(shiftCode);
        report.setShiftDate(shiftDate);
        report.setStartingMeterKg(startKg);
        report.setTotalRefillsKg(totalRefills);
        report.setEndingMeterKg(endKg);
        report.setComputedUsedKg(computedUsed);
        report.setRefillCount(refillCount);
        report.setCorrectionCount(correctionCount);
        report.setIsFinal(consumption.map(ShiftMaterialConsumption::getIsFinal).orElse(false));
        report.setRefills(refills);
        return report;
    }

    // ── Available lots ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AvailableLotDto> getAvailableLots(String materialCode) {
        return stockTxRepo.findAvailableLots(materialCode)
                .stream()
                .map(tx -> {
                    AvailableLotDto d = new AvailableLotDto();
                    d.setStockId(tx.getId());
                    d.setMaterialCode(tx.getMaterial().getMaterialCode());
                    d.setMaterialName(tx.getMaterial().getMaterialName());
                    d.setLotNumber(tx.getLotNumber());
                    d.setQuantity(tx.getQuantity());
                    d.setRemainingQty(tx.getRemainingQty());
                    d.setTimestamp(tx.getTimestamp());
                    return d;
                })
                .collect(Collectors.toList());
    }

    // ── Internal: update/create ShiftMaterialConsumption ─────────────────────

    private void updateConsumptionRecord(String tankId, LocalDate shiftDate, String shiftCode) {
        if (shiftDate == null || shiftCode == null) return;

        MachineTank tank = tankRepo.findById(tankId).orElse(null);

        Optional<TankMeterReading> startReading = meterRepo
                .findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtAsc(
                        tankId, shiftDate, shiftCode, "SHIFT_START");
        Optional<TankMeterReading> endReading = meterRepo
                .findFirstByTankIdAndShiftDateAndShiftCodeAndReadingTypeOrderByRecordedAtDesc(
                        tankId, shiftDate, shiftCode, "SHIFT_END");

        BigDecimal startKg = startReading.map(TankMeterReading::getMeterValueKg).orElse(null);
        BigDecimal endKg = endReading.map(TankMeterReading::getMeterValueKg).orElse(null);
        BigDecimal totalRefills = refillRepo.sumRefillKg(tankId, shiftDate, shiftCode);
        int refillCount = refillRepo.countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionFalse(tankId, shiftDate, shiftCode);
        int correctionCount = refillRepo.countByTankIdAndShiftDateAndShiftCodeAndIsCorrectionTrue(tankId, shiftDate, shiftCode);

        BigDecimal computedUsed = null;
        if (startKg != null && endKg != null) {
            computedUsed = startKg.add(totalRefills).subtract(endKg);
        }

        ShiftMaterialConsumption record = consumptionRepo
                .findByTankIdAndShiftDateAndShiftCode(tankId, shiftDate, shiftCode)
                .orElseGet(ShiftMaterialConsumption::new);

        record.setTankId(tankId);
        record.setMachineId(tank != null ? tank.getMachineId() : null);
        record.setMaterialCode(tank != null ? tank.getDefaultMaterialCode() : "TBD");
        record.setShiftCode(shiftCode);
        record.setShiftDate(shiftDate);
        record.setStartingMeterKg(startKg);
        record.setTotalRefillsKg(totalRefills);
        record.setEndingMeterKg(endKg);
        record.setComputedUsedKg(computedUsed);
        record.setRefillCount(refillCount);
        record.setCorrectionCount(correctionCount);
        // isFinal only set explicitly — don't reset it here

        consumptionRepo.save(record);
    }

    // ── Machines list ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<java.util.Map<String, String>> getMachines() {
        return tankRepo.findByIsActiveTrue().stream()
                .map(MachineTank::getMachineId)
                .distinct()
                .sorted()
                .map(id -> java.util.Map.of("machineId", id, "machineName", id))
                .collect(Collectors.toList());
    }

    // ── Refill history for a machine ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MachineRefillHistoryDto> getRefillHistoryForMachine(String machineId, int limit) {
        List<MachineTank> tanks = tankRepo.findByMachineIdAndIsActiveTrue(machineId);
        java.util.Map<String, String> tankNameMap = tanks.stream()
                .collect(Collectors.toMap(MachineTank::getTankId, MachineTank::getTankName));

        List<TankRefill> allRefills = new java.util.ArrayList<>();
        for (MachineTank mt : tanks) {
            allRefills.addAll(refillRepo.findByTankIdOrderByRefillAtDesc(mt.getTankId()));
        }
        allRefills.sort((a, b) -> b.getRefillAt().compareTo(a.getRefillAt()));

        return allRefills.stream().limit(limit).map(r -> {
            MachineRefillHistoryDto dto = new MachineRefillHistoryDto();
            dto.setRecordedAt(r.getRefillAt());
            dto.setTankId(r.getTankId());
            dto.setTankName(tankNameMap.getOrDefault(r.getTankId(), r.getTankId()));
            dto.setMaterial(r.getMaterialCode() != null ? r.getMaterialCode() : "");
            dto.setQuantity(r.getQtyKg());
            dto.setUnit("kg");
            dto.setShiftCode(r.getShiftCode());
            dto.setEntryType(Boolean.TRUE.equals(r.getIsCorrection()) ? "CORRECTION" : "REFILL");
            return dto;
        }).collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private RefillHistoryItemDto toRefillHistoryItem(TankRefill r) {
        RefillHistoryItemDto d = new RefillHistoryItemDto();
        d.setId(r.getId());
        d.setMaterialCode(r.getMaterialCode());
        d.setMaterialLotCode(r.getMaterialLotCode());
        d.setQtyKg(r.getQtyKg());
        d.setIsCorrection(r.getIsCorrection());
        d.setCorrectsRefillId(r.getCorrectsRefillId());
        d.setRefillBy(r.getRefillBy());
        d.setRefillAt(r.getRefillAt());
        d.setNote(r.getNote());
        return d;
    }
}
