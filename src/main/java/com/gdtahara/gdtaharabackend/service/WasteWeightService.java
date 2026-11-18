package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.waste.*;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WasteWeightService {

    private final WasteWeightRecordRepository recordRepo;
    private final WasteEditLogRepository logRepo;
    private final WasteEditRequestRepository requestRepo;
    private final WasteTypeMasterRepository typeRepo;
    private final CmMeterService cmMeterService;

    public WasteWeightService(WasteWeightRecordRepository recordRepo,
                               WasteEditLogRepository logRepo,
                               WasteEditRequestRepository requestRepo,
                               WasteTypeMasterRepository typeRepo,
                               CmMeterService cmMeterService) {
        this.recordRepo = recordRepo;
        this.logRepo = logRepo;
        this.requestRepo = requestRepo;
        this.typeRepo = typeRepo;
        this.cmMeterService = cmMeterService;
    }

    // ── Waste type master ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WasteTypeMaster> getActiveWasteTypes() {
        return typeRepo.findAllByIsActiveTrueOrderBySortOrderAsc();
    }

    // ── Create record ─────────────────────────────────────────────────────────

    public WasteRecordDto createRecord(WasteEntryDto dto, String username) {
        var shift = cmMeterService.getCurrentShift();

        WasteWeightRecord rec = new WasteWeightRecord();
        rec.setMachineId(dto.machineId());
        rec.setWoNumber(dto.woNumber());
        rec.setWasteTypeCode(dto.wasteTypeCode());
        rec.setWeightKg(dto.weightKg());
        rec.setNote(dto.note());
        rec.setPhotoIds(dto.photoIds());
        rec.setRecordedBy(username);
        rec.setShiftCode(shift.getShiftCode());
        rec.setShiftDate(shift.getShiftDate());

        recordRepo.save(rec);
        return toDto(rec);
    }

    // ── Shift records ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WasteRecordDto> getCurrentShiftRecords() {
        var shift = cmMeterService.getCurrentShift();
        return recordRepo.findAllByShiftDateAndShiftCodeAndIsActiveTrueOrderByRecordedAtDesc(
                shift.getShiftDate(), shift.getShiftCode())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WasteRecordDto> getMyCurrentShiftRecords(String username) {
        var shift = cmMeterService.getCurrentShift();
        return recordRepo.findByRecorderAndShift(username, shift.getShiftDate(), shift.getShiftCode())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Self-edit (same shift, own record) ────────────────────────────────────

    public WasteRecordDto selfEdit(Long recordId, SelfEditDto dto, String username) {
        WasteWeightRecord rec = findActive(recordId);
        var shift = cmMeterService.getCurrentShift();

        if (!rec.getRecordedBy().equals(username))
            throw new IllegalStateException("สามารถแก้ไขได้เฉพาะรายการของตนเองเท่านั้น");
        if (!rec.getShiftDate().equals(shift.getShiftDate()) || !rec.getShiftCode().equals(shift.getShiftCode()))
            throw new IllegalStateException("ไม่อยู่ใน shift เดียวกัน ต้องส่งคำขอแก้ไขผ่านหัวหน้างาน");

        WasteEditLog log = buildLog(rec, "SELF_EDIT", username, dto.reason(), null);

        rec.setMachineId(dto.machineId());
        rec.setWoNumber(dto.woNumber());
        rec.setWasteTypeCode(dto.wasteTypeCode());
        rec.setWeightKg(dto.weightKg());
        rec.setNote(dto.note());
        rec.setLastEditedBy(username);
        rec.setLastEditedAt(LocalDateTime.now());
        rec.setVersion(rec.getVersion() + 1);
        rec.setEditCount(rec.getEditCount() + 1);

        logRepo.save(log);
        recordRepo.save(rec);
        return toDto(rec);
    }

    // ── Edit request (cross-shift, within 24 h) ───────────────────────────────

    public EditRequestStatusDto createEditRequest(Long recordId, EditRequestDto dto, String username) {
        WasteWeightRecord rec = findActive(recordId);

        if (rec.getRecordedAt().isBefore(LocalDateTime.now().minusHours(24)))
            throw new IllegalStateException("เกิน 24 ชั่วโมงแล้ว ไม่สามารถส่งคำขอแก้ไขได้");

        // only one PENDING request per record at a time
        requestRepo.findFirstByRecordIdAndStatusOrderByRequestedAtDesc(recordId, "PENDING")
                .ifPresent(r -> { throw new IllegalStateException("มีคำขอแก้ไขค้างอยู่แล้ว ต้องรอการอนุมัติก่อน"); });

        WasteEditRequest req = new WasteEditRequest();
        req.setRecordId(recordId);
        req.setRequesterId(username);
        req.setRequestedMachineId(dto.requestedMachineId());
        req.setRequestedWoNumber(dto.requestedWoNumber());
        req.setRequestedWasteTypeCode(dto.requestedWasteTypeCode());
        req.setRequestedWeightKg(dto.requestedWeightKg());
        req.setRequestedNote(dto.requestedNote());
        req.setReason(dto.reason());
        req.setRequestPhotoIds(dto.requestPhotoIds());
        // TODO: send Telegram notification to Document role users

        requestRepo.save(req);
        return toRequestDto(req);
    }

    // ── Doc: list pending requests ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EditRequestStatusDto> getPendingRequests() {
        return requestRepo.findAllByStatusOrderByRequestedAtDesc("PENDING")
                .stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EditRequestStatusDto> getMyRequests(String username) {
        return requestRepo.findAllByRequesterIdOrderByRequestedAtDesc(username)
                .stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }

    // ── Doc: approve / reject ─────────────────────────────────────────────────

    public EditRequestStatusDto respondToEditRequest(Long requestId, DocResponseDto dto, String docUsername) {
        WasteEditRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบคำขอแก้ไข id=" + requestId));

        if (!"PENDING".equals(req.getStatus()))
            throw new IllegalStateException("คำขอนี้ถูกดำเนินการไปแล้ว");

        req.setStatus(dto.decision());
        req.setDocId(docUsername);
        req.setDocRespondedAt(LocalDateTime.now());
        req.setDocNote(dto.docNote());

        if ("APPROVED".equals(dto.decision())) {
            WasteWeightRecord rec = findActive(req.getRecordId());
            WasteEditLog log = buildLog(rec, "DOC_APPROVED", docUsername, req.getReason(), req.getId());

            if (req.getRequestedMachineId() != null) rec.setMachineId(req.getRequestedMachineId());
            if (req.getRequestedWoNumber() != null) rec.setWoNumber(req.getRequestedWoNumber());
            if (req.getRequestedWasteTypeCode() != null) rec.setWasteTypeCode(req.getRequestedWasteTypeCode());
            if (req.getRequestedWeightKg() != null) rec.setWeightKg(req.getRequestedWeightKg());
            if (req.getRequestedNote() != null) rec.setNote(req.getRequestedNote());
            rec.setLastEditedBy(docUsername);
            rec.setLastEditedAt(LocalDateTime.now());
            rec.setVersion(rec.getVersion() + 1);
            rec.setEditCount(rec.getEditCount() + 1);

            logRepo.save(log);
            recordRepo.save(rec);
        }

        requestRepo.save(req);
        return toRequestDto(req);
    }

    // ── History ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WasteEditLogDto> getRecordHistory(Long recordId) {
        return logRepo.findAllByRecordIdOrderByEditedAtDesc(recordId)
                .stream()
                .map(this::toLogDto)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WasteWeightRecord findActive(Long id) {
        WasteWeightRecord rec = recordRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบรายการ id=" + id));
        if (!Boolean.TRUE.equals(rec.getIsActive()))
            throw new EntityNotFoundException("รายการนี้ถูกยกเลิกแล้ว");
        return rec;
    }

    private WasteEditLog buildLog(WasteWeightRecord rec, String editType, String editedBy,
                                   String reason, Long requestId) {
        WasteEditLog log = new WasteEditLog();
        log.setRecordId(rec.getId());
        log.setEditType(editType);
        log.setEditedBy(editedBy);
        log.setReason(reason);
        log.setEditRequestId(requestId);
        log.setOldMachineId(rec.getMachineId());
        log.setOldWoNumber(rec.getWoNumber());
        log.setOldWasteTypeCode(rec.getWasteTypeCode());
        log.setOldWeightKg(rec.getWeightKg());
        log.setOldNote(rec.getNote());
        return log;
    }

    private WasteRecordDto toDto(WasteWeightRecord rec) {
        String pendingStatus = requestRepo
                .findFirstByRecordIdAndStatusOrderByRequestedAtDesc(rec.getId(), "PENDING")
                .map(WasteEditRequest::getStatus)
                .orElse(null);
        return new WasteRecordDto(
                rec.getId(), rec.getMachineId(), rec.getWoNumber(), rec.getWasteTypeCode(),
                rec.getWeightKg(), rec.getNote(), rec.getPhotoIds(),
                rec.getRecordedBy(), rec.getRecordedAt(),
                rec.getShiftCode(), rec.getShiftDate(),
                rec.getVersion(), rec.getLastEditedBy(), rec.getLastEditedAt(),
                rec.getEditCount(), pendingStatus);
    }

    private EditRequestStatusDto toRequestDto(WasteEditRequest r) {
        return new EditRequestStatusDto(
                r.getId(), r.getRecordId(), r.getRequesterId(), r.getRequestedAt(),
                r.getRequestedWasteTypeCode(), r.getRequestedWeightKg(), r.getRequestedNote(),
                r.getReason(), r.getStatus(), r.getDocId(), r.getDocRespondedAt(), r.getDocNote());
    }

    private WasteEditLogDto toLogDto(WasteEditLog l) {
        return new WasteEditLogDto(
                l.getId(), l.getEditType(), l.getEditedBy(), l.getEditedAt(),
                l.getOldWasteTypeCode(), l.getOldWeightKg(), l.getOldNote(),
                l.getNewWasteTypeCode(), l.getNewWeightKg(), l.getNewNote(),
                l.getReason());
    }
}
