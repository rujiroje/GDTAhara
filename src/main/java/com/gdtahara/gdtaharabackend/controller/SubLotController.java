package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.ConfirmBoxRequest;
import com.gdtahara.gdtaharabackend.dto.MarkLabeledRequest;
import com.gdtahara.gdtaharabackend.dto.SubLotResponse;
import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.service.SubLotService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sub-lots")
@PreAuthorize("hasAnyRole('Operator','Shift Leader','DataAdmin')")
@Tag(name = "Sub-Lot", description = "Sub-lot / box confirmation and labeling")
public class SubLotController {

    private static final Logger logger = LoggerFactory.getLogger(SubLotController.class);

    private final SubLotService subLotService;

    public SubLotController(SubLotService subLotService) {
        this.subLotService = subLotService;
    }

    @PostMapping("/confirm-box")
    public ResponseEntity<SubLotResponse> confirmBox(
            @Valid @RequestBody ConfirmBoxRequest req, Principal principal) {
        logger.info("Confirm box for report {} by {}", req.getProductionReportId(), principal.getName());
        SubLot subLot = subLotService.confirmBox(
                req.getProductionReportId(), req.getBoxQuantity(),
                req.getWeightKg(), req.getPalletNumber(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubLotResponse.from(subLot));
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<List<SubLotResponse>> byReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(subLotService.getSubLotsForReport(reportId).stream()
                .map(SubLotResponse::from).toList());
    }

    @GetMapping("/report/{reportId}/count")
    public ResponseEntity<Map<String, Long>> countByReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(Map.of("count", subLotService.countBoxesForReport(reportId)));
    }

    @GetMapping("/by-number/{subLotNumber}")
    public ResponseEntity<SubLotResponse> byNumber(@PathVariable String subLotNumber) {
        SubLot subLot = subLotService.findBySubLotNumber(subLotNumber)
                .orElseThrow(() -> new EntityNotFoundException("SubLot not found: " + subLotNumber));
        return ResponseEntity.ok(SubLotResponse.from(subLot));
    }

    @PutMapping("/{id}/labeled")
    public ResponseEntity<SubLotResponse> markLabeled(
            @PathVariable Long id, @RequestBody MarkLabeledRequest req, Principal principal) {
        SubLot subLot = subLotService.markAsLabeled(id, req.getZplRef(), principal.getName());
        return ResponseEntity.ok(SubLotResponse.from(subLot));
    }
}
