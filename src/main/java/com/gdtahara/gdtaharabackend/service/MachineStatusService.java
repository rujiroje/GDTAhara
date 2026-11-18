package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.MachineStatusLogDto;
import com.gdtahara.gdtaharabackend.dto.MachineStatusUpdateRequest;
import com.gdtahara.gdtaharabackend.exception.ResourceNotFoundException;
import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.MachineStatusLog;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.MachineStatusLogRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MachineStatusService {

    private final MachineStatusLogRepository statusLogRepo;
    private final MachineRepository machineRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;

    public MachineStatusService(MachineStatusLogRepository statusLogRepo,
                                MachineRepository machineRepo,
                                UserRepository userRepo,
                                AuditLogService auditLogService) {
        this.statusLogRepo = statusLogRepo;
        this.machineRepo = machineRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    // Operator กดเปลี่ยนสถานะเครื่อง — ปิด record เดิม + เปิด record ใหม่
    @Transactional
    public MachineStatusLogDto updateStatus(MachineStatusUpdateRequest req, String username) {
        Machine machine = machineRepo.findById(req.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + req.getMachineId()));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        LocalDateTime now = LocalDateTime.now();

        // ปิด active record เดิม (ถ้ามี)
        Optional<MachineStatusLog> active = statusLogRepo.findByMachineIdAndEndTimeIsNull(machine.getId());
        active.ifPresent(log -> {
            log.setEndTime(now);
            statusLogRepo.save(log);
        });

        // เปิด record ใหม่
        MachineStatusLog newLog = new MachineStatusLog();
        newLog.setMachine(machine);
        newLog.setStatus(req.getStatus());
        newLog.setStartTime(now);
        newLog.setReason(req.getReason());
        newLog.setSource("MANUAL");
        newLog.setRecordedBy(user);
        statusLogRepo.save(newLog);

        auditLogService.log("UPDATE", "MachineStatus", machine.getId(),
                Map.of("machineId", machine.getId(), "previousStatus", active.map(MachineStatusLog::getStatus).orElse("NONE")),
                Map.of("machineId", machine.getId(), "newStatus", req.getStatus(), "reason", String.valueOf(req.getReason())));

        return toDto(newLog);
    }

    // ดึงสถานะปัจจุบันของเครื่อง
    @Transactional(readOnly = true)
    public MachineStatusLogDto getCurrentStatus(Long machineId) {
        return statusLogRepo.findByMachineIdAndEndTimeIsNull(machineId)
                .map(this::toDto)
                .orElse(null);
    }

    // ดึง timeline ย้อนหลัง
    @Transactional(readOnly = true)
    public List<MachineStatusLogDto> getTimeline(Long machineId, LocalDateTime from, LocalDateTime to) {
        return statusLogRepo.findByMachineAndTimeRange(machineId, from, to)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ดึง log ล่าสุด N รายการ
    @Transactional(readOnly = true)
    public List<MachineStatusLogDto> getRecent(Long machineId, int limit) {
        return statusLogRepo.findLatestByMachine(machineId, PageRequest.of(0, limit))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private MachineStatusLogDto toDto(MachineStatusLog log) {
        MachineStatusLogDto dto = new MachineStatusLogDto();
        dto.setId(log.getId());
        dto.setMachineId(log.getMachine().getId());
        dto.setMachineName(log.getMachine().getMachineName());
        dto.setStatus(log.getStatus());
        dto.setStartTime(log.getStartTime());
        dto.setEndTime(log.getEndTime());
        dto.setReason(log.getReason());
        dto.setSource(log.getSource());

        if (log.getStartTime() != null) {
            LocalDateTime end = log.getEndTime() != null ? log.getEndTime() : LocalDateTime.now();
            dto.setDurationMinutes(ChronoUnit.MINUTES.between(log.getStartTime(), end));
        }
        return dto;
    }
}
