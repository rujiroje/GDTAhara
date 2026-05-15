package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.OeeResultDto;
import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.MachineStatusLogRepository;
import com.gdtahara.gdtaharabackend.repository.NgLogRepository;
import com.gdtahara.gdtaharabackend.repository.PackagingLogRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class OeeService {

    private final MachineStatusLogRepository statusLogRepo;
    private final MachineRepository machineRepo;
    private final ProductionReportRepository reportRepo;
    private final PackagingLogRepository packagingRepo;
    private final NgLogRepository ngLogRepo;
    private final RecipeRepository recipeRepo;

    public OeeService(MachineStatusLogRepository statusLogRepo,
                      MachineRepository machineRepo,
                      ProductionReportRepository reportRepo,
                      PackagingLogRepository packagingRepo,
                      NgLogRepository ngLogRepo,
                      RecipeRepository recipeRepo) {
        this.statusLogRepo = statusLogRepo;
        this.machineRepo = machineRepo;
        this.reportRepo = reportRepo;
        this.packagingRepo = packagingRepo;
        this.ngLogRepo = ngLogRepo;
        this.recipeRepo = recipeRepo;
    }

    @Transactional(readOnly = true)
    public OeeResultDto calculate(Long machineId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        return calculateRange(machineId, from, to);
    }

    @Transactional(readOnly = true)
    public OeeResultDto calculateRange(Long machineId, LocalDateTime from, LocalDateTime to) {
        Machine machine = machineRepo.findById(machineId)
                .orElseThrow(() -> new RuntimeException("Machine not found: " + machineId));

        OeeResultDto result = new OeeResultDto();
        result.setMachineId(machineId);
        result.setMachineName(machine.getMachineName());
        result.setFrom(from);
        result.setTo(to);

        // --- Availability ---
        // ดึง duration รวมของแต่ละ status
        List<Object[]> durations = statusLogRepo.sumDurationByStatus(machineId, from, to, LocalDateTime.now());
        long running = 0, idle = 0, plannedStop = 0, unplannedStop = 0;
        for (Object[] row : durations) {
            String status = (String) row[0];
            long minutes = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            switch (status) {
                case "RUNNING"        -> running       = minutes;
                case "IDLE"           -> idle          = minutes;
                case "PLANNED_STOP"   -> plannedStop   = minutes;
                case "UNPLANNED_STOP" -> unplannedStop = minutes;
            }
        }

        long plannedProduction = running + idle + unplannedStop; // ไม่รวม PLANNED_STOP
        result.setRunningMinutes(running);
        result.setIdleMinutes(idle);
        result.setPlannedStopMinutes(plannedStop);
        result.setUnplannedStopMinutes(unplannedStop);
        result.setPlannedProductionMinutes(plannedProduction);

        double availability = plannedProduction > 0 ? (double) running / plannedProduction : 0.0;
        result.setAvailability(round(availability * 100));

        // --- Quality ---
        // หา reports ที่ active ในช่วงนี้สำหรับเครื่องนี้
        var reports = reportRepo.findActiveOnDate(from.toLocalDate(), machine.getMachineName());
        long totalQty = 0, goodQty = 0, ngQty = 0;
        double idealCycleTimeSec = 0;

        for (var report : reports) {
            Long boxCount = packagingRepo.countPackagesByReportId(report.getId());
            int qtyPerBox = (report.getProduct() != null && report.getProduct().getQtyPerBox() != null)
                    ? report.getProduct().getQtyPerBox() : 1;
            long good = (boxCount != null ? boxCount : 0L) * qtyPerBox;

            Long ng = ngLogRepo.sumQuantityByReportId(report.getId());
            long ngCount = ng != null ? ng : 0L;

            goodQty += good;
            ngQty += ngCount;
            totalQty += good + ngCount;

            // ดึง ideal cycle time จาก recipe (ใช้ record แรกที่เจอ)
            if (idealCycleTimeSec == 0 && report.getProduct() != null) {
                recipeRepo.findByProductIdAndIsActiveTrue(report.getProduct().getId())
                        .stream().findFirst()
                        .ifPresent(r -> {
                            // ไม่สามารถ set ใน lambda โดยตรง — จะ set ข้างนอก loop
                        });
            }
        }

        // หา ideal cycle time ถ้ายังไม่ได้
        if (idealCycleTimeSec == 0 && !reports.isEmpty() && reports.get(0).getProduct() != null) {
            var recipes = recipeRepo.findByProductIdAndIsActiveTrue(reports.get(0).getProduct().getId());
            if (!recipes.isEmpty() && recipes.get(0).getTargetCycleTimeSec() != null) {
                idealCycleTimeSec = recipes.get(0).getTargetCycleTimeSec().doubleValue();
            }
        }

        result.setTotalQty(totalQty);
        result.setGoodQty(goodQty);
        result.setNgQty(ngQty);
        result.setIdealCycleTimeSec(idealCycleTimeSec);

        double quality = totalQty > 0 ? (double) goodQty / totalQty : 0.0;
        result.setQuality(round(quality * 100));

        // --- Performance ---
        double performance = 0.0;
        if (running > 0 && idealCycleTimeSec > 0) {
            double runningSeconds = running * 60.0;
            performance = (totalQty * idealCycleTimeSec) / runningSeconds;
        }
        result.setPerformance(round(Math.min(performance, 1.0) * 100)); // cap at 100%

        // --- OEE ---
        result.setOee(round(availability * Math.min(performance, 1.0) * quality * 100));

        return result;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
