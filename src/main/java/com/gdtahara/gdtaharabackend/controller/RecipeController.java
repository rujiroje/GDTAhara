package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.RecipeDto;
import com.gdtahara.gdtaharabackend.exception.ResourceNotFoundException;
import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.Recipe;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.repository.RecipeRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import com.gdtahara.gdtaharabackend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeRepository recipeRepo;
    private final ProductRepository productRepo;
    private final MachineRepository machineRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;

    public RecipeController(RecipeRepository recipeRepo, ProductRepository productRepo,
                            MachineRepository machineRepo, UserRepository userRepo,
                            AuditLogService auditLogService) {
        this.recipeRepo = recipeRepo;
        this.productRepo = productRepo;
        this.machineRepo = machineRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    // GET /api/recipes — ดึงทั้งหมด (active)
    @GetMapping
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','Management','QA','Technician','CM Operator')")
    public ResponseEntity<List<RecipeDto>> getAll() {
        return ResponseEntity.ok(recipeRepo.findByIsActiveTrue().stream()
                .map(this::toDto).collect(Collectors.toList()));
    }

    // GET /api/recipes/by-product/{productId} — กรองตาม product + machine
    @GetMapping("/by-product/{productId}")
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','CM Operator')")
    public ResponseEntity<List<RecipeDto>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) Long machineId) {
        List<Recipe> list = machineId != null
                ? recipeRepo.findActiveByProductAndMachine(productId, machineId)
                : recipeRepo.findByProductIdAndIsActiveTrue(productId);
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    // GET /api/recipes/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin','Management','QA','Technician','CM Operator')")
    public ResponseEntity<RecipeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(recipeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id))));
    }

    // POST /api/recipes — สร้าง recipe ใหม่
    @PostMapping
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin','Technician')")
    public ResponseEntity<RecipeDto> create(@RequestBody RecipeDto req, Principal principal) {
        Recipe recipe = new Recipe();
        mapFromDto(req, recipe, principal.getName());
        RecipeDto saved = toDto(recipeRepo.save(recipe));
        auditLogService.log("CREATE", "Recipe", saved.getId(), null,
                Map.of("id", saved.getId(), "code", String.valueOf(saved.getRecipeCode())));
        return ResponseEntity.ok(saved);
    }

    // PUT /api/recipes/{id} — แก้ไข
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin','Technician')")
    public ResponseEntity<RecipeDto> update(@PathVariable Long id,
                                            @RequestBody RecipeDto req,
                                            Principal principal) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id));
        mapFromDto(req, recipe, principal.getName());
        RecipeDto saved = toDto(recipeRepo.save(recipe));
        auditLogService.log("UPDATE", "Recipe", id, Map.of("id", id),
                Map.of("id", saved.getId(), "code", String.valueOf(saved.getRecipeCode())));
        return ResponseEntity.ok(saved);
    }

    // DELETE /api/recipes/{id} — soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Production Control','DataAdmin','Technician')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Principal principal) {
        Recipe recipe = recipeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id));
        recipe.setIsActive(false);
        recipeRepo.save(recipe);
        auditLogService.log("DEACTIVATE", "Recipe", id, Map.of("id", id), null);
        return ResponseEntity.noContent().build();
    }

    private void mapFromDto(RecipeDto dto, Recipe recipe, String username) {
        recipe.setRecipeCode(dto.getRecipeCode());
        recipe.setRecipeName(dto.getRecipeName());
        recipe.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0");
        recipe.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        recipe.setTargetCycleTimeSec(dto.getTargetCycleTimeSec());
        recipe.setTargetTempZone1(dto.getTargetTempZone1());
        recipe.setTargetTempZone2(dto.getTargetTempZone2());
        recipe.setTargetTempHead(dto.getTargetTempHead());
        recipe.setTargetBlowPressure(dto.getTargetBlowPressure());
        recipe.setNotes(dto.getNotes());

        if (dto.getProductId() != null)
            recipe.setProduct(productRepo.findById(dto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
        if (dto.getMachineId() != null)
            recipe.setMachine(machineRepo.findById(dto.getMachineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine not found")));
        if (recipe.getCreatedBy() == null)
            userRepo.findByUsername(username).ifPresent(recipe::setCreatedBy);
    }

    private RecipeDto toDto(Recipe r) {
        RecipeDto dto = new RecipeDto();
        dto.setId(r.getId());
        dto.setRecipeCode(r.getRecipeCode());
        dto.setRecipeName(r.getRecipeName());
        dto.setVersion(r.getVersion());
        dto.setIsActive(r.getIsActive());
        dto.setTargetCycleTimeSec(r.getTargetCycleTimeSec());
        dto.setTargetTempZone1(r.getTargetTempZone1());
        dto.setTargetTempZone2(r.getTargetTempZone2());
        dto.setTargetTempHead(r.getTargetTempHead());
        dto.setTargetBlowPressure(r.getTargetBlowPressure());
        dto.setNotes(r.getNotes());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        if (r.getProduct() != null) { dto.setProductId(r.getProduct().getId()); dto.setProductName(r.getProduct().getProductName()); }
        if (r.getMachine() != null) { dto.setMachineId(r.getMachine().getId()); dto.setMachineName(r.getMachine().getMachineName()); }
        return dto;
    }
}
