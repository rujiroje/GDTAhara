package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.BomImportLogDto;
import com.gdtahara.gdtaharabackend.dto.BomImportResultDto;
import com.gdtahara.gdtaharabackend.dto.BomItemDto;
import com.gdtahara.gdtaharabackend.dto.BomViewDto;
import com.gdtahara.gdtaharabackend.dto.MaterialRequirementDto;
import com.gdtahara.gdtaharabackend.model.BillOfMaterials;
import com.gdtahara.gdtaharabackend.repository.BillOfMaterialsRepository;
import com.gdtahara.gdtaharabackend.repository.BomItemRepository;
import com.gdtahara.gdtaharabackend.dto.VarianceDto;
import com.gdtahara.gdtaharabackend.service.BomImportService;
import com.gdtahara.gdtaharabackend.service.BomVarianceService;
import com.gdtahara.gdtaharabackend.service.MaterialRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom")
@RequiredArgsConstructor
public class BomController {

    private final BomImportService bomImportService;
    private final MaterialRequirementService requirementService;
    private final BomVarianceService varianceService;
    private final BillOfMaterialsRepository bomRepository;
    private final BomItemRepository bomItemRepository;

    // ----------------------------------------------------------
    // POST /api/bom/import — Upload BOM Excel file
    // ----------------------------------------------------------
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','DataAdmin','Production Control')")
    public ResponseEntity<?> importBom(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("กรุณาเลือกไฟล์");
        }
        try {
            BomImportResultDto result = bomImportService.importFromExcel(file, principal.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("นำเข้าล้มเหลว: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // GET /api/bom/{fgCode} — Get active BOM for a FG code
    // ----------------------------------------------------------
    @GetMapping("/{fgCode}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getActiveBom(@PathVariable String fgCode) {
        return bomRepository.findActiveByFgCodeAndDate(fgCode, LocalDate.now())
                .map(bom -> {
                    List<BomItemDto> items = bomItemRepository
                            .findByBomIdOrderByItemNumber(bom.getId())
                            .stream().map(BomItemDto::from).collect(Collectors.toList());
                    return ResponseEntity.ok(BomViewDto.from(bom, items));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ----------------------------------------------------------
    // GET /api/bom/{fgCode}/history — All BOM versions for FG
    // ----------------------------------------------------------
    @GetMapping("/{fgCode}/history")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','DataAdmin','Production Control')")
    public ResponseEntity<List<BomViewDto>> getBomHistory(@PathVariable String fgCode) {
        List<BomViewDto> history = bomRepository
                .findByFgCodeOrderByEffectiveFromDesc(fgCode)
                .stream()
                .map(bom -> {
                    List<BomItemDto> items = bomItemRepository
                            .findByBomIdOrderByItemNumber(bom.getId())
                            .stream().map(BomItemDto::from).collect(Collectors.toList());
                    return BomViewDto.from(bom, items);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    // ----------------------------------------------------------
    // GET /api/bom/plan/{planId}/requirement
    // GET /api/bom/plan/date?from=2026-07-01&to=2026-07-07
    // ----------------------------------------------------------
    @GetMapping("/plan/{planId}/requirement")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','CM Operator','Shift Leader','Document')")
    public ResponseEntity<?> getRequirementByPlan(@PathVariable Long planId) {
        try {
            return ResponseEntity.ok(requirementService.calculateForPlan(planId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/plan/date")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','CM Operator','Shift Leader','Document')")
    public ResponseEntity<List<MaterialRequirementDto>> getRequirementByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(requirementService.calculateForDateRange(from, to));
    }

    @GetMapping("/plan/machine/{machineId}")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','CM Operator','Shift Leader','Document')")
    public ResponseEntity<List<MaterialRequirementDto>> getRequirementByMachine(
            @PathVariable Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(requirementService.calculateForMachineDate(machineId, date));
    }

    // ----------------------------------------------------------
    // GET /api/bom/report/{reportId}/variance
    // GET /api/bom/report/{reportId}/variance/scrap
    // GET /api/bom/report/variance/range?from=...&to=...
    // ----------------------------------------------------------
    @GetMapping("/report/{reportId}/variance")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','Management','QA')")
    public ResponseEntity<?> getVariance(@PathVariable Long reportId) {
        try {
            return ResponseEntity.ok(varianceService.analyzeForReport(reportId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/report/{reportId}/variance/scrap")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','Management','QA')")
    public ResponseEntity<?> getScrapVariance(@PathVariable Long reportId) {
        try {
            return ResponseEntity.ok(varianceService.analyzeScrap(reportId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/report/variance/range")
    @PreAuthorize("hasAnyRole('ADMIN','Production Control','Management','QA')")
    public ResponseEntity<List<VarianceDto>> getVarianceByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(varianceService.aggregateByDateRange(from, to));
    }

    // ----------------------------------------------------------
    // POST /api/bom/sync-products
    // สร้าง Product record สำหรับทุก fgCode ใน bill_of_materials ที่ยังไม่มีใน products
    // ----------------------------------------------------------
    @PostMapping("/sync-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'DataAdmin', 'Production Control')")
    public ResponseEntity<Map<String, Integer>> syncProducts() {
        return ResponseEntity.ok(bomImportService.syncProductsFromAllBoms());
    }

    // ----------------------------------------------------------
    // GET /api/bom/import-log — Recent import history
    // ----------------------------------------------------------
    @GetMapping("/import-log")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','DataAdmin','Production Control')")
    public ResponseEntity<List<BomImportLogDto>> getImportLog() {
        return ResponseEntity.ok(
                bomImportService.getRecentImportLogs().stream()
                        .map(BomImportLogDto::from)
                        .collect(Collectors.toList())
        );
    }
}
