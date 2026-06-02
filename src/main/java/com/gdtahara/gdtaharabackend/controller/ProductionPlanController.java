package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.CreatePlanRequest;
import com.gdtahara.gdtaharabackend.dto.ImportResult;
import com.gdtahara.gdtaharabackend.dto.ProductionPlanResponse;
import com.gdtahara.gdtaharabackend.dto.ShiftSplitResponse;
import com.gdtahara.gdtaharabackend.dto.UpdatePlanRequest;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.service.ProductionPlanImportService;
import com.gdtahara.gdtaharabackend.service.ProductionPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/production-plans")
@PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
@Tag(name = "Production Plan", description = "Production plan CRUD and shift-split")
public class ProductionPlanController {

    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanController.class);

    private final ProductionPlanService       productionPlanService;
    private final ProductionPlanImportService importService;

    public ProductionPlanController(ProductionPlanService productionPlanService,
                                    ProductionPlanImportService importService) {
        this.productionPlanService = productionPlanService;
        this.importService         = importService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin')")
    public ResponseEntity<ImportResult> importPlans(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String factoryCode,
            @RequestParam(defaultValue = "false") boolean dryRun,
            Principal principal) {
        logger.info("Import plan file: {} by {} dryRun={}", file.getOriginalFilename(), principal.getName(), dryRun);
        return ResponseEntity.ok(importService.importPlanWorkbook(file, factoryCode, principal.getName(), dryRun));
    }

    @PostMapping("/")
    public ResponseEntity<ProductionPlanResponse> create(
            @Valid @RequestBody CreatePlanRequest req, Principal principal) {
        logger.info("Create plan: machine={} date={} by {}", req.getMachineId(), req.getPlanDate(), principal.getName());
        ProductionPlan plan = productionPlanService.createPlan(
                req.getPlanDate(), req.getMachineId(), req.getProductId(),
                req.getTargetQty(), req.getManpowerDRatio(), req.getManpowerNRatio(),
                req.getSapWoNumber(), req.getSource(), req.getExcelFileRef(),
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductionPlanResponse.from(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductionPlanResponse> update(
            @PathVariable Long id, @RequestBody UpdatePlanRequest req, Principal principal) {
        ProductionPlan plan = productionPlanService.updatePlan(
                id, req.getTargetQty(), req.getManpowerDRatio(), req.getManpowerNRatio(),
                req.getStatus(), principal.getName());
        return ResponseEntity.ok(ProductionPlanResponse.from(plan));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('Production Control','Shift Leader','Management','Operator','DataAdmin')")
    public ResponseEntity<List<ProductionPlanResponse>> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(productionPlanService.getPlansForDateAsDto(date));
    }

    @GetMapping("/machine/{machineId}")
    @PreAuthorize("hasAnyRole('Production Control','Shift Leader','Management','Operator','DataAdmin')")
    public ResponseEntity<List<ProductionPlanResponse>> byMachineRange(
            @PathVariable Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(productionPlanService.getPlansForMachineInRange(machineId, from, to)
                .stream().map(ProductionPlanResponse::from).toList());
    }

    @GetMapping("/month/{year}/{month}")
    @PreAuthorize("hasAnyRole('Production Control','Shift Leader','Management','Operator','DataAdmin')")
    public ResponseEntity<List<ProductionPlanResponse>> byMonth(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(productionPlanService.getPlansForMonth(year, month)
                .stream().map(ProductionPlanResponse::from).toList());
    }

    @GetMapping("/{id}/shift-split")
    @PreAuthorize("hasAnyRole('Production Control','Shift Leader','Management','Operator','DataAdmin')")
    public ResponseEntity<ShiftSplitResponse> shiftSplit(@PathVariable Long id) {
        ProductionPlan plan = productionPlanService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Production plan not found: " + id));
        ProductionPlanService.ShiftSplit split = productionPlanService.splitTargetByShift(plan);
        return ResponseEntity.ok(new ShiftSplitResponse(split.dayTarget(), split.nightTarget()));
    }
}
