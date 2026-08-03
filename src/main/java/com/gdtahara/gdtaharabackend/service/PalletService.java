package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PalletService {

    private final PalletRepository palletRepository;
    private final PalletCloseLogRepository closeLogRepository;
    private final SubLotRepository subLotRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter LOT_DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    // ─── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public PalletDto createOpen(CreatePalletRequest req, String username) {
        User user = findUser(username);
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.getProductId()));

        LocalDate palletDate = req.getPalletDate() != null ? req.getPalletDate() : LocalDate.now();

        Pallet pallet = new Pallet();
        pallet.setPalletNumber(req.getPalletNumber().trim());
        pallet.setProduct(product);
        pallet.setProductCode(product.getProductCode());
        pallet.setTargetQty(product.getQtyPerPallet());
        pallet.setPalletDate(palletDate);
        pallet.setStatus("OPEN");
        pallet.setActualQty(0);
        pallet.setBoxCount(0);
        pallet.setPrintCount(0);
        pallet.setRevisionSuffix("");
        pallet.setCreatedBy(user);
        pallet.setNotes(req.getNotes());
        pallet = palletRepository.save(pallet);

        log(pallet, "CREATED", null, null, user, null);
        return toDto(pallet, Collections.emptyList());
    }

    // ─── Scan box ────────────────────────────────────────────────────────────

    @Transactional
    public PalletDto scanBox(Long palletId, ScanBoxRequest req, String username) {
        User user = findUser(username);
        Pallet pallet = findPallet(palletId);

        if (!"OPEN".equals(pallet.getStatus())) {
            throw new IllegalArgumentException("Pallet #" + pallet.getPalletNumber() + " is not OPEN (status=" + pallet.getStatus() + ")");
        }

        String subLotNum = req.getSubLotNumber().trim();
        SubLot subLot = subLotRepository.findBySubLotNumber(subLotNum)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบ Sub-lot: " + subLotNum));

        if ("packed".equalsIgnoreCase(subLot.getStatus()) || subLot.getPallet() != null) {
            throw new IllegalArgumentException("กล่องนี้อยู่ใน Pallet #" + subLot.getPallet().getPalletNumber() + " แล้ว");
        }

        ProductionReport pr = subLot.getProductionReport();
        if (pr == null || pr.getProduct() == null ||
                !pr.getProduct().getId().equals(pallet.getProduct().getId())) {
            throw new IllegalArgumentException("Product ของกล่องไม่ตรงกับ Pallet นี้ (" + pallet.getProductCode() + ")");
        }

        subLot.setPallet(pallet);
        subLot.setPalletNumber(pallet.getPalletNumber());
        subLot.setStatus("packed");
        subLotRepository.save(subLot);

        pallet.setActualQty(pallet.getActualQty() + (subLot.getBoxQuantity() != null ? subLot.getBoxQuantity() : 0));
        pallet.setBoxCount(pallet.getBoxCount() + 1);

        // update lot date range
        LocalDate lotDate = pr.getStartDate();
        if (lotDate != null) {
            if (pallet.getLotDateMin() == null || lotDate.isBefore(pallet.getLotDateMin())) pallet.setLotDateMin(lotDate);
            if (pallet.getLotDateMax() == null || lotDate.isAfter(pallet.getLotDateMax())) pallet.setLotDateMax(lotDate);
        }
        pallet = palletRepository.save(pallet);
        log(pallet, "BOX_ADDED", subLot, null, user, null);

        return toDto(pallet, loadBoxes(pallet.getId()));
    }

    // ─── Remove box ──────────────────────────────────────────────────────────

    @Transactional
    public PalletDto removeBox(Long palletId, Long subLotId, String username) {
        User user = findUser(username);
        Pallet pallet = findPallet(palletId);

        if (!"OPEN".equals(pallet.getStatus())) {
            throw new IllegalArgumentException("ไม่สามารถลบกล่องจาก Pallet ที่ปิดแล้ว");
        }

        SubLot subLot = subLotRepository.findById(subLotId)
                .orElseThrow(() -> new EntityNotFoundException("Sub-lot not found: " + subLotId));

        if (subLot.getPallet() == null || !subLot.getPallet().getId().equals(palletId)) {
            throw new IllegalArgumentException("กล่องนี้ไม่ได้อยู่ใน Pallet นี้");
        }

        subLot.setPallet(null);
        subLot.setPalletNumber(null);
        subLot.setStatus("draft");
        subLotRepository.save(subLot);

        pallet.setActualQty(Math.max(0, pallet.getActualQty() - (subLot.getBoxQuantity() != null ? subLot.getBoxQuantity() : 0)));
        pallet.setBoxCount(Math.max(0, pallet.getBoxCount() - 1));
        pallet = palletRepository.save(pallet);
        log(pallet, "BOX_REMOVED", subLot, null, user, null);

        return toDto(pallet, loadBoxes(pallet.getId()));
    }

    // ─── Close ───────────────────────────────────────────────────────────────

    @Transactional
    public PalletDto close(Long palletId, String username) {
        User user = findUser(username);
        Pallet pallet = findPallet(palletId);

        if (!"OPEN".equals(pallet.getStatus())) {
            throw new IllegalArgumentException("Pallet ไม่ได้อยู่ในสถานะ OPEN");
        }
        if (pallet.getBoxCount() == 0) {
            throw new IllegalArgumentException("ไม่สามารถปิด Pallet ที่ไม่มีกล่องใดๆ");
        }

        pallet.setStatus("CLOSED");
        pallet.setClosedAt(LocalDateTime.now());
        pallet.setClosedBy(user);
        pallet = palletRepository.save(pallet);
        log(pallet, "CLOSED", null, null, user, null);

        return toDto(pallet, loadBoxes(pallet.getId()));
    }

    // ─── Mark printed ────────────────────────────────────────────────────────

    @Transactional
    public PalletDto markPrinted(Long palletId, String username) {
        User user = findUser(username);
        Pallet pallet = findPallet(palletId);

        if ("OPEN".equals(pallet.getStatus())) {
            throw new IllegalArgumentException("ต้องปิด Pallet ก่อนพิมพ์ label");
        }
        pallet.setStatus("PRINTED");
        pallet.setPrintedAt(LocalDateTime.now());
        pallet.setPrintCount(pallet.getPrintCount() + 1);
        pallet = palletRepository.save(pallet);
        log(pallet, "PRINTED", null, null, user, null);

        return toDto(pallet, loadBoxes(pallet.getId()));
    }

    // ─── Get ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PalletDto getById(Long palletId) {
        Pallet pallet = findPallet(palletId);
        return toDto(pallet, loadBoxes(palletId));
    }

    @Transactional(readOnly = true)
    public List<PalletDto> listOpen(String username) {
        return palletRepository.findAllOpen().stream()
                .map(p -> toDto(p, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PalletDto> listByDate(LocalDate date, Long productId) {
        List<Pallet> list = productId != null
                ? palletRepository.findByProductAndDate(productId, date)
                : palletRepository.findByPalletDate(date);
        return list.stream().map(p -> toDto(p, Collections.emptyList())).collect(Collectors.toList());
    }

    // ─── Label data ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PalletLabelDto getLabelData(Long palletId) {
        Pallet pallet = findPallet(palletId);
        List<SubLot> boxes = loadBoxEntities(palletId);

        // Group by YYMMDD lot date → list of sub-lot numbers (abbreviated for display)
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        boxes.stream()
                .sorted(Comparator.comparing(
                        sl -> sl.getProductionReport().getStartDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(sl -> {
                    LocalDate d = sl.getProductionReport().getStartDate();
                    String key = d != null ? d.format(LOT_DATE_FMT) : "??????";
                    // Extract box number portion for matrix display
                    // New format: EBAJMY0130A-260731-001 → "001"
                    // Old format: 260731-B0001 → "0001"
                    String num = sl.getSubLotNumber();
                    int lastDash = num.lastIndexOf('-');
                    String displayNum;
                    if (lastDash >= 0) {
                        String seg = num.substring(lastDash + 1);
                        displayNum = (seg.startsWith("B") || seg.startsWith("b")) ? seg.substring(1) : seg;
                    } else {
                        displayNum = num;
                    }
                    matrix.computeIfAbsent(key, k -> new ArrayList<>()).add(displayNum);
                });

        // Determine piece count robustly:
        // 1. Sum boxQuantity from the actual sub-lots.
        //    If each sub-lot stores piece count (e.g. 480), sum > boxCount → correct.
        // 2. If sum ≤ boxCount, boxQuantity was stored as 1 (old confirmBox path).
        //    Fall back to product.qtyPerBox × boxCount — but only when qtyPerBox > 1.
        // 3. Last resort: use pallet.actualQty (already accumulated).
        int boxCountSafe = pallet.getBoxCount() != null ? pallet.getBoxCount() : 0;
        int qtyFromBoxes = boxes.stream()
                .mapToInt(sl -> sl.getBoxQuantity() != null ? sl.getBoxQuantity() : 0)
                .sum();
        Integer qtyPerBox = pallet.getProduct() != null ? pallet.getProduct().getQtyPerBox() : null;
        int totalQty;
        if (qtyFromBoxes > boxCountSafe) {
            // boxQuantity holds piece count correctly
            totalQty = qtyFromBoxes;
        } else if (qtyPerBox != null && qtyPerBox > 1) {
            // boxQuantity was 0 or 1 (wrong); derive from product master
            totalQty = boxCountSafe * qtyPerBox;
        } else {
            totalQty = pallet.getActualQty() != null ? pallet.getActualQty() : 0;
        }

        return new PalletLabelDto(
                pallet.getPalletNumber(),
                pallet.getProductCode(),
                pallet.getProduct().getProductName(),
                pallet.getLotDateMin(),
                pallet.getLotDateMax(),
                totalQty,
                pallet.getBoxCount(),
                pallet.getClosedAt() != null ? pallet.getClosedAt().toLocalDate() : LocalDate.now(),
                matrix
        );
    }

    // ─── Rearrange ───────────────────────────────────────────────────

    @Transactional
    public PalletDto rearrangePallet(Long sourcePalletId, RearrangePalletRequest req, String username) {
        if (req.newPalletNumber() == null || req.newPalletNumber().isBlank())
            throw new IllegalArgumentException("กรุณาระบุเลข Pallet ใหม่");

        User user = findUser(username);
        Pallet source = findPallet(sourcePalletId);

        if ("OPEN".equals(source.getStatus()))
            throw new IllegalArgumentException("ไม่สามารถประกอบใหม่จาก Pallet ที่ยังเปิดอยู่");
        if ("REARRANGED".equals(source.getStatus()))
            throw new IllegalArgumentException("Pallet นี้ถูกประกอบใหม่ไปแล้ว");

        Pallet newPallet = new Pallet();
        newPallet.setPalletNumber(req.newPalletNumber().trim());
        newPallet.setProduct(source.getProduct());
        newPallet.setProductCode(source.getProductCode());
        newPallet.setTargetQty(source.getTargetQty());
        newPallet.setPalletDate(LocalDate.now());
        newPallet.setStatus("OPEN");
        newPallet.setActualQty(0);
        newPallet.setBoxCount(0);
        newPallet.setPrintCount(0);
        newPallet.setRevisionSuffix("");
        newPallet.setCreatedBy(user);
        newPallet.setParentPallet(source);
        newPallet.setRearrangeReason(req.reason());
        newPallet = palletRepository.save(newPallet);

        // move all boxes from source to new pallet
        List<SubLot> boxes = loadBoxEntities(sourcePalletId);
        int totalQty = 0;
        for (SubLot sl : boxes) {
            sl.setPallet(newPallet);
            sl.setPalletNumber(newPallet.getPalletNumber());
            subLotRepository.save(sl);
            totalQty += sl.getBoxQuantity() != null ? sl.getBoxQuantity() : 0;
        }
        newPallet.setBoxCount(boxes.size());
        newPallet.setActualQty(totalQty);
        newPallet.setLotDateMin(source.getLotDateMin());
        newPallet.setLotDateMax(source.getLotDateMax());
        newPallet = palletRepository.save(newPallet);

        source.setStatus("REARRANGED");
        palletRepository.save(source);

        log(newPallet, "REARRANGED_FROM", null, source, user, req.reason());
        return toDto(newPallet, loadBoxes(newPallet.getId()));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Pallet findPallet(Long id) {
        return palletRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Pallet not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private void log(Pallet pallet, String action, SubLot subLot, Pallet refPallet, User user, String reason) {
        PalletCloseLog entry = new PalletCloseLog();
        entry.setPallet(pallet);
        entry.setAction(action);
        entry.setSubLot(subLot);
        entry.setRefPallet(refPallet);
        entry.setPerformedBy(user);
        entry.setReason(reason);
        closeLogRepository.save(entry);
    }

    private List<SubLot> loadBoxEntities(Long palletId) {
        return subLotRepository.findByPallet_IdOrderByConfirmedAt(palletId);
    }

    private List<PalletBoxDto> loadBoxes(Long palletId) {
        return loadBoxEntities(palletId).stream().map(sl -> {
            LocalDate lotDate = sl.getProductionReport() != null ? sl.getProductionReport().getStartDate() : null;
            String displayLotDate = lotDate != null ? lotDate.format(LOT_DATE_FMT) : "??????";
            return new PalletBoxDto(
                    sl.getId(),
                    sl.getSubLotNumber(),
                    displayLotDate,
                    lotDate,
                    sl.getBoxQuantity(),
                    sl.getWeightKg()
            );
        }).collect(Collectors.toList());
    }

    private PalletDto toDto(Pallet p, List<PalletBoxDto> boxes) {
        PalletDto dto = new PalletDto();
        dto.setId(p.getId());
        dto.setPalletNumber(p.getPalletNumber());
        dto.setProductCode(p.getProductCode());
        dto.setProductName(p.getProduct() != null ? p.getProduct().getProductName() : null);
        dto.setActualQty(p.getActualQty());
        dto.setTargetQty(p.getTargetQty());
        dto.setBoxCount(p.getBoxCount());
        dto.setStatus(p.getStatus());
        dto.setPalletDate(p.getPalletDate());
        dto.setLotDateMin(p.getLotDateMin());
        dto.setLotDateMax(p.getLotDateMax());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setCreatedByName(p.getCreatedBy() != null ? p.getCreatedBy().getUsername() : null);
        dto.setClosedAt(p.getClosedAt());
        dto.setClosedByName(p.getClosedBy() != null ? p.getClosedBy().getUsername() : null);
        dto.setPrintedAt(p.getPrintedAt());
        dto.setPrintCount(p.getPrintCount());
        dto.setNotes(p.getNotes());
        dto.setRevisionSuffix(p.getRevisionSuffix());
        dto.setRearrangeReason(p.getRearrangeReason());
        if (p.getParentPallet() != null) {
            dto.setParentPalletId(p.getParentPallet().getId());
            dto.setParentPalletNumber(p.getParentPallet().getPalletNumber());
        }
        dto.setBoxes(boxes);
        return dto;
    }
}
