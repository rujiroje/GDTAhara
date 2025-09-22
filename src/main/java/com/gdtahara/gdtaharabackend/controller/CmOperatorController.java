// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/CmOperatorController.java
// (ฉบับแก้ไขสมบูรณ์)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.StockOutRequestDto;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.service.CmOperatorService;
import com.gdtahara.gdtaharabackend.service.MaterialStockService; // **[ใหม่]** Import
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // **[แก้ไข]** Import ให้ครบ

import java.security.Principal;
import java.util.List; // **[ใหม่]** Import

@RestController
@RequestMapping("/api/cm-operator")
@PreAuthorize("hasAnyRole('CM Operator', 'DataAdmin')")
public class CmOperatorController {

    private final CmOperatorService cmOperatorService;
    private final MaterialStockService materialStockService; // **[ใหม่]** เพิ่ม Service

    public CmOperatorController(CmOperatorService cmOperatorService, MaterialStockService materialStockService) {
        this.cmOperatorService = cmOperatorService;
        this.materialStockService = materialStockService; // **[ใหม่]** เพิ่มใน Constructor
    }

    @PostMapping("/stock-out")
    public ResponseEntity<?> recordStockOut(@RequestBody StockOutRequestDto request, Principal principal) {
        try {
            MaterialStockTransaction transaction = cmOperatorService.recordStockOut(request, principal.getName());
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // **[ใหม่]** เพิ่ม Endpoint สำหรับดึง Lot Number
    @GetMapping("/materials/{materialId}/lot-numbers")
    public ResponseEntity<List<String>> getAvailableLots(@PathVariable Long materialId) {
        return ResponseEntity.ok(materialStockService.getAvailableLotNumbers(materialId));
    }
}