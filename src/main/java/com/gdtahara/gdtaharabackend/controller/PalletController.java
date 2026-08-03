package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.*;
import com.gdtahara.gdtaharabackend.service.PalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/operator/pallets")
@RequiredArgsConstructor
public class PalletController {

    private final PalletService palletService;

    @PostMapping
    public ResponseEntity<PalletDto> create(
            @RequestBody CreatePalletRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.createOpen(req, user.getUsername()));
    }

    @PostMapping("/{id}/scan")
    public ResponseEntity<PalletDto> scanBox(
            @PathVariable Long id,
            @RequestBody ScanBoxRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.scanBox(id, req, user.getUsername()));
    }

    @DeleteMapping("/{id}/boxes/{subLotId}")
    public ResponseEntity<PalletDto> removeBox(
            @PathVariable Long id,
            @PathVariable Long subLotId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.removeBox(id, subLotId, user.getUsername()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PalletDto> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.close(id, user.getUsername()));
    }

    @PostMapping("/{id}/print")
    public ResponseEntity<PalletDto> print(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.markPrinted(id, user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PalletDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(palletService.getById(id));
    }

    @GetMapping("/open")
    public ResponseEntity<List<PalletDto>> listOpen(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.listOpen(user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<PalletDto>> listByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long productId) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(palletService.listByDate(effectiveDate, productId));
    }

    @PostMapping("/{id}/rearrange")
    public ResponseEntity<PalletDto> rearrange(
            @PathVariable Long id,
            @RequestBody RearrangePalletRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(palletService.rearrangePallet(id, req, user.getUsername()));
    }

    @GetMapping("/{id}/label")
    public ResponseEntity<PalletLabelDto> getLabel(@PathVariable Long id) {
        return ResponseEntity.ok(palletService.getLabelData(id));
    }
}
