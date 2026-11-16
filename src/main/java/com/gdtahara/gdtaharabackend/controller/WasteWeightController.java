package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.waste.*;
import com.gdtahara.gdtaharabackend.service.WasteWeightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cm/waste")
@PreAuthorize("hasAnyRole('CM Operator', 'DataAdmin')")
public class WasteWeightController {

    private final WasteWeightService wasteService;

    public WasteWeightController(WasteWeightService wasteService) {
        this.wasteService = wasteService;
    }

    @GetMapping("/waste-types")
    public ResponseEntity<?> getWasteTypes() {
        return ResponseEntity.ok(wasteService.getActiveWasteTypes());
    }

    @PostMapping
    public ResponseEntity<WasteRecordDto> createRecord(@Valid @RequestBody WasteEntryDto dto,
                                                        Principal principal) {
        return ResponseEntity.ok(wasteService.createRecord(dto, principal.getName()));
    }

    @GetMapping("/current-shift")
    public ResponseEntity<List<WasteRecordDto>> getCurrentShiftRecords() {
        return ResponseEntity.ok(wasteService.getCurrentShiftRecords());
    }

    @GetMapping("/my-records")
    public ResponseEntity<List<WasteRecordDto>> getMyCurrentShiftRecords(Principal principal) {
        return ResponseEntity.ok(wasteService.getMyCurrentShiftRecords(principal.getName()));
    }

    @PatchMapping("/{id}/self-edit")
    public ResponseEntity<WasteRecordDto> selfEdit(@PathVariable Long id,
                                                    @Valid @RequestBody SelfEditDto dto,
                                                    Principal principal) {
        return ResponseEntity.ok(wasteService.selfEdit(id, dto, principal.getName()));
    }

    @PostMapping("/{id}/edit-request")
    public ResponseEntity<EditRequestStatusDto> createEditRequest(@PathVariable Long id,
                                                                   @Valid @RequestBody EditRequestDto dto,
                                                                   Principal principal) {
        return ResponseEntity.ok(wasteService.createEditRequest(id, dto, principal.getName()));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<EditRequestStatusDto>> getMyRequests(Principal principal) {
        return ResponseEntity.ok(wasteService.getMyRequests(principal.getName()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WasteEditLogDto>> getRecordHistory(@PathVariable Long id) {
        return ResponseEntity.ok(wasteService.getRecordHistory(id));
    }
}
