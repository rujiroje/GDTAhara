package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SetupTimeLogServiceImpl implements SetupTimeLogService {

    private final SetupActivityCodeRepository codeRepo;
    private final SetupTimeLogRepository logRepo;
    private final MachineSetupJobRepository jobRepo;
    private final UserRepository userRepo;

    // ── Activity Codes ────────────────────────────────────────────────────────

    @Override
    public List<SetupActivityCodeDto> getActiveCodes() {
        return codeRepo.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream().map(SetupActivityCodeDto::from).toList();
    }

    @Override
    @Transactional
    public SetupActivityCodeDto createCode(CreateActivityCodeRequest req, String username) {
        if (req.code() == null || req.code().isBlank())
            throw new IllegalArgumentException("Code ห้ามว่าง");
        if (req.descriptionTh() == null || req.descriptionTh().isBlank())
            throw new IllegalArgumentException("กรุณาระบุชื่อภาษาไทย");

        SetupActivityCode c = new SetupActivityCode();
        c.setCode(req.code().trim().toUpperCase());
        c.setDescriptionTh(req.descriptionTh().trim());
        c.setDescriptionEn(req.descriptionEn());
        c.setCategory(req.category());
        c.setColorHex(req.colorHex() != null ? req.colorHex() : "#607D8B");
        c.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 99);
        c.setIsActive(true);
        userRepo.findByUsername(username).ifPresent(c::setCreatedBy);
        return SetupActivityCodeDto.from(codeRepo.save(c));
    }

    @Override
    @Transactional
    public SetupActivityCodeDto toggleCodeActive(Long codeId, boolean active) {
        SetupActivityCode c = codeRepo.findById(codeId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Activity Code: " + codeId));
        c.setIsActive(active);
        return SetupActivityCodeDto.from(codeRepo.save(c));
    }

    // ── Time Log entries ──────────────────────────────────────────────────────

    @Override
    public List<SetupTimeLogDto> getTimeLogs(Long jobId) {
        return logRepo.findByJobIdWithDetails(jobId)
                .stream().map(SetupTimeLogDto::from).toList();
    }

    @Override
    @Transactional
    public SetupTimeLogDto startLog(Long jobId, SetupTimeLogRequest req, String username) {
        if (req.activityCodeId() == null)
            throw new IllegalArgumentException("กรุณาเลือก Activity Code");

        MachineSetupJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Setup Job: " + jobId));
        SetupActivityCode code = codeRepo.findById(req.activityCodeId())
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Activity Code: " + req.activityCodeId()));

        SetupTimeLog log = new SetupTimeLog();
        log.setSetupJob(job);
        log.setActivityCode(code);
        log.setStartTime(req.startTime() != null ? req.startTime() : LocalDateTime.now());
        log.setDescription(req.description());
        log.setSequenceNo(logRepo.maxSequenceNoByJobId(jobId) + 1);
        userRepo.findByUsername(username).ifPresent(log::setCreatedBy);

        return SetupTimeLogDto.from(logRepo.save(log));
    }

    @Override
    @Transactional
    public SetupTimeLogDto endLog(Long jobId, Long logId, String username) {
        SetupTimeLog log = logRepo.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Time Log: " + logId));
        if (!log.getSetupJob().getId().equals(jobId))
            throw new IllegalArgumentException("Log นี้ไม่ใช่ของ Job นี้");
        if (log.getEndTime() != null)
            throw new IllegalStateException("Log นี้จบแล้ว");

        LocalDateTime now = LocalDateTime.now();
        log.setEndTime(now);
        long minutes = ChronoUnit.MINUTES.between(log.getStartTime(), now);
        log.setDurationMin((int) Math.max(1, minutes));
        log.setUpdatedAt(now);
        userRepo.findByUsername(username).ifPresent(log::setUpdatedBy);

        return SetupTimeLogDto.from(logRepo.save(log));
    }

    @Override
    @Transactional
    public void deleteLog(Long jobId, Long logId, String username) {
        SetupTimeLog log = logRepo.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Time Log: " + logId));
        if (!log.getSetupJob().getId().equals(jobId))
            throw new IllegalArgumentException("Log นี้ไม่ใช่ของ Job นี้");
        logRepo.delete(log);
    }

    @Override
    public boolean hasRunningLogs(Long jobId) {
        return logRepo.existsRunningByJobId(jobId);
    }

    @Override
    @Transactional
    public List<SetupTimeLogDto> endAllRunningLogs(Long jobId, String username) {
        List<SetupTimeLog> running = logRepo.findRunningByJobId(jobId);
        LocalDateTime now = LocalDateTime.now();
        for (SetupTimeLog log : running) {
            log.setEndTime(now);
            long minutes = ChronoUnit.MINUTES.between(log.getStartTime(), now);
            log.setDurationMin((int) Math.max(1, minutes));
            log.setUpdatedAt(now);
            userRepo.findByUsername(username).ifPresent(log::setUpdatedBy);
            logRepo.save(log);
        }
        return getTimeLogs(jobId);
    }
}
