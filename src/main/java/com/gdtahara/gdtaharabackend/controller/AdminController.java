package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.CreateUserRequest;
import com.gdtahara.gdtaharabackend.dto.MoldAdminDto;
import com.gdtahara.gdtaharabackend.dto.ResetPasswordRequest;
import com.gdtahara.gdtaharabackend.dto.UserDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.MoldMasterRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.service.AuditLogService;
import com.gdtahara.gdtaharabackend.service.MasterDataService;
import com.gdtahara.gdtaharabackend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('DataAdmin') or hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private MasterDataService masterDataService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private MoldMasterRepository moldMasterRepository;
    @Autowired private ProductRepository productRepository;

    // --- User Management ---
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() { return ResponseEntity.ok(userService.findAllUsers()); }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest userRequest, Principal principal) {
        User created = userService.createUser(userRequest);
        auditLogService.log("CREATE", "User", created.getId(), null,
                Map.of("id", created.getId(), "username", created.getUsername(), "role", created.getRole()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDto userDetails, Principal principal) {
        User updated = userService.updateUser(id, userDetails);
        auditLogService.log("UPDATE", "User", id, Map.of("id", id),
                Map.of("id", updated.getId(), "username", updated.getUsername(), "role", updated.getRole()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "User", id, Map.of("id", id), null);
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request, Principal principal) {
        userService.resetPassword(id, request.getNewPassword());
        auditLogService.log("RESET_PASSWORD", "User", id, null, null);
        return ResponseEntity.ok().build();
    }

    // --- Product Management ---
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() { return ResponseEntity.ok(masterDataService.getAllProducts()); }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product, Principal principal) {
        Product saved = masterDataService.saveProduct(product);
        auditLogService.log("CREATE", "Product", saved.getId(), null,
                Map.of("id", saved.getId(), "name", String.valueOf(saved.getProductName())));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails, Principal principal) {
        productDetails.setId(id);
        Product saved = masterDataService.saveProduct(productDetails);
        auditLogService.log("UPDATE", "Product", id, Map.of("id", id),
                Map.of("id", saved.getId(), "name", String.valueOf(saved.getProductName())));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "Product", id, Map.of("id", id), null);
        masterDataService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // --- Machine Management ---
    @GetMapping("/machines")
    public ResponseEntity<List<Machine>> getAllMachines() { return ResponseEntity.ok(masterDataService.getAllMachines()); }

    @PostMapping("/machines")
    public ResponseEntity<Machine> createMachine(@RequestBody Machine machine, Principal principal) {
        Machine saved = masterDataService.saveMachine(machine);
        auditLogService.log("CREATE", "Machine", saved.getId(), null,
                Map.of("id", saved.getId(), "name", String.valueOf(saved.getMachineName())));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/machines/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody Machine machineDetails, Principal principal) {
        machineDetails.setId(id);
        Machine saved = masterDataService.saveMachine(machineDetails);
        auditLogService.log("UPDATE", "Machine", id, Map.of("id", id),
                Map.of("id", saved.getId(), "name", String.valueOf(saved.getMachineName())));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/machines/{id}")
    public ResponseEntity<?> deleteMachine(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "Machine", id, Map.of("id", id), null);
        masterDataService.deleteMachine(id);
        return ResponseEntity.ok().build();
    }

    // --- NG Type Management ---
    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getAllNgTypes() { return ResponseEntity.ok(masterDataService.getAllNgTypes()); }

    @PostMapping("/ng-types")
    public ResponseEntity<NgType> createNgType(@RequestBody NgType ngType, Principal principal) {
        NgType saved = masterDataService.saveNgType(ngType);
        auditLogService.log("CREATE", "NgType", saved.getId(), null, Map.of("id", saved.getId()));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/ng-types/{id}")
    public ResponseEntity<NgType> updateNgType(@PathVariable Long id, @RequestBody NgType ngTypeDetails, Principal principal) {
        ngTypeDetails.setId(id);
        NgType saved = masterDataService.saveNgType(ngTypeDetails);
        auditLogService.log("UPDATE", "NgType", id, Map.of("id", id), Map.of("id", saved.getId()));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/ng-types/{id}")
    public ResponseEntity<?> deleteNgType(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "NgType", id, Map.of("id", id), null);
        masterDataService.deleteNgType(id);
        return ResponseEntity.ok().build();
    }

    // --- Parameter Checklist Management ---
    @GetMapping("/parameter-checklists")
    public ResponseEntity<List<ParameterChecklist>> getAllParameterChecklists() { return ResponseEntity.ok(masterDataService.getAllParameterChecklists()); }

    @PostMapping("/parameter-checklists")
    public ResponseEntity<ParameterChecklist> createParameterChecklist(@RequestBody ParameterChecklist item, Principal principal) {
        ParameterChecklist saved = masterDataService.saveParameterChecklist(item);
        auditLogService.log("CREATE", "ParameterChecklist", saved.getId(), null, Map.of("id", saved.getId()));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/parameter-checklists/{id}")
    public ResponseEntity<ParameterChecklist> updateParameterChecklist(@PathVariable Long id, @RequestBody ParameterChecklist itemDetails, Principal principal) {
        itemDetails.setId(id);
        ParameterChecklist saved = masterDataService.saveParameterChecklist(itemDetails);
        auditLogService.log("UPDATE", "ParameterChecklist", id, Map.of("id", id), Map.of("id", saved.getId()));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/parameter-checklists/{id}")
    public ResponseEntity<?> deleteParameterChecklist(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "ParameterChecklist", id, Map.of("id", id), null);
        masterDataService.deleteParameterChecklist(id);
        return ResponseEntity.ok().build();
    }

    // --- Material Management ---
    @GetMapping("/materials")
    public ResponseEntity<List<Material>> getAllMaterials() { return ResponseEntity.ok(masterDataService.getAllMaterials()); }

    @PostMapping("/materials")
    public ResponseEntity<Material> createMaterial(@RequestBody Material material, Principal principal) {
        Material saved = masterDataService.saveMaterial(material);
        auditLogService.log("CREATE", "Material", saved.getId(), null,
                Map.of("id", saved.getId(), "code", String.valueOf(saved.getMaterialCode())));
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<Material> updateMaterial(@PathVariable Long id, @RequestBody Material materialDetails, Principal principal) {
        materialDetails.setId(id);
        Material saved = masterDataService.saveMaterial(materialDetails);
        auditLogService.log("UPDATE", "Material", id, Map.of("id", id),
                Map.of("id", saved.getId(), "code", String.valueOf(saved.getMaterialCode())));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id, Principal principal) {
        auditLogService.log("DELETE", "Material", id, Map.of("id", id), null);
        masterDataService.deleteMaterial(id);
        return ResponseEntity.ok().build();
    }

    // --- Available Roles (for FE dropdown) ---
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAvailableRoles() {
        List<String> roles = List.of(
            "DataAdmin", "ADMIN", "Production Control", "Shift Leader",
            "Operator", "Technician", "QA", "CM Operator", "Management", "Document"
        );
        return ResponseEntity.ok(roles);
    }

    // --- Mold Master Management ---

    @GetMapping("/molds")
    public ResponseEntity<List<MoldAdminDto>> getAllMolds() {
        Map<Long, String> codeById = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getProductCode));
        List<MoldAdminDto> dtos = moldMasterRepository.findAll().stream()
                .sorted(Comparator.comparing(MoldMaster::getMoldCode))
                .map(m -> toMoldDto(m, codeById))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/molds")
    public ResponseEntity<MoldAdminDto> createMold(@RequestBody MoldAdminDto dto, Principal principal) {
        MoldMaster m = fromMoldDto(dto);
        m.setCreatedBy(principal.getName());
        MoldMaster saved = moldMasterRepository.save(m);
        auditLogService.log("CREATE", "MoldMaster", saved.getId(), null, Map.of("code", saved.getMoldCode()));
        return ResponseEntity.ok(toMoldDto(saved, buildProductCodeMap()));
    }

    @PutMapping("/molds/{id}")
    public ResponseEntity<MoldAdminDto> updateMold(@PathVariable Long id,
                                                    @RequestBody MoldAdminDto dto,
                                                    Principal principal) {
        MoldMaster m = moldMasterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mold not found: " + id));
        applyMoldDto(m, dto);
        m.setUpdatedBy(principal.getName());
        MoldMaster saved = moldMasterRepository.save(m);
        auditLogService.log("UPDATE", "MoldMaster", id, null, Map.of("code", saved.getMoldCode()));
        return ResponseEntity.ok(toMoldDto(saved, buildProductCodeMap()));
    }

    @DeleteMapping("/molds/{id}")
    public ResponseEntity<?> deactivateMold(@PathVariable Long id, Principal principal) {
        MoldMaster m = moldMasterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mold not found: " + id));
        m.setIsActive(false);
        m.setUpdatedBy(principal.getName());
        moldMasterRepository.save(m);
        auditLogService.log("DELETE", "MoldMaster", id, null, Map.of("code", m.getMoldCode()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/molds/template")
    public ResponseEntity<byte[]> getMoldTemplate() {
        String csv = "moldCode,moldName,moldNameEn,machineType,productCode,moldType," +
                     "standardCavityCount,standardCavityNo,workCavityCount,workCavityNo,isActive,description\n" +
                     "M001,ชื่อ Mold ภาษาไทย,Mold Name EN,BLOW,PROD-CODE,BLOW,8," +
                     "\"1-1,1-2,2-1,2-2,3-1,3-2,4-1,4-2\",8," +
                     "\"1-1,1-2,2-1,2-2,3-1,3-2,4-1,4-2\",true,หมายเหตุเพิ่มเติม\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mold_master_template.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/molds/export")
    public ResponseEntity<byte[]> exportMolds() {
        Map<Long, String> codeById = buildProductCodeMap();
        List<MoldMaster> molds = moldMasterRepository.findAll().stream()
                .sorted(Comparator.comparing(MoldMaster::getMoldCode)).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("moldCode,moldName,moldNameEn,machineType,productCode,moldType," +
                  "standardCavityCount,standardCavityNo,workCavityCount,workCavityNo,isActive,description\n");
        for (MoldMaster m : molds) {
            sb.append(csvQ(m.getMoldCode())).append(",")
              .append(csvQ(m.getMoldName())).append(",")
              .append(csvQ(m.getMoldNameEn())).append(",")
              .append(csvQ(m.getMachineType())).append(",")
              .append(csvQ(m.getProductId() != null ? codeById.get(m.getProductId()) : null)).append(",")
              .append(csvQ(m.getMoldType())).append(",")
              .append(m.getStandardCavityCount() != null ? m.getStandardCavityCount() : "").append(",")
              .append(csvQ(m.getStandardCavityNo())).append(",")
              .append(m.getWorkCavityCount() != null ? m.getWorkCavityCount() : "").append(",")
              .append(csvQ(m.getWorkCavityNo())).append(",")
              .append(Boolean.TRUE.equals(m.getIsActive())).append(",")
              .append(csvQ(m.getDescription())).append("\n");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mold_master.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    // ── Mold helpers ─────────────────────────────────────────────────────────────

    private Map<Long, String> buildProductCodeMap() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getProductCode));
    }

    private MoldAdminDto toMoldDto(MoldMaster m, Map<Long, String> codeById) {
        return new MoldAdminDto(m.getId(), m.getMoldCode(), m.getMoldName(), m.getMoldNameEn(),
                m.getMachineType(), m.getProductId(),
                m.getProductId() != null ? codeById.getOrDefault(m.getProductId(), null) : null,
                m.getMoldType(), m.getStandardCavityCount(), m.getStandardCavityNo(),
                m.getWorkCavityCount(), m.getWorkCavityNo(),
                m.getIsActive(), m.getDescription());
    }

    private MoldMaster fromMoldDto(MoldAdminDto dto) {
        MoldMaster m = new MoldMaster();
        m.setMoldCode(dto.moldCode());
        applyMoldDto(m, dto);
        return m;
    }

    private void applyMoldDto(MoldMaster m, MoldAdminDto dto) {
        if (dto.moldName() != null) m.setMoldName(dto.moldName());
        m.setMoldNameEn(dto.moldNameEn());
        m.setMachineType(dto.machineType());
        m.setProductId(dto.productId());
        m.setMoldType(dto.moldType());
        m.setStandardCavityCount(dto.standardCavityCount());
        m.setStandardCavityNo(dto.standardCavityNo());
        m.setWorkCavityCount(dto.workCavityCount());
        m.setWorkCavityNo(dto.workCavityNo());
        m.setIsActive(dto.isActive() != null ? dto.isActive() : true);
        m.setDescription(dto.description());
        // Sync legacy fields used by TC flow
        if (dto.workCavityCount() != null) m.setCavityCount(dto.workCavityCount());
        else if (dto.standardCavityCount() != null) m.setCavityCount(dto.standardCavityCount());
        if (dto.workCavityNo() != null) m.setCavityNumbers(dto.workCavityNo());
        else if (dto.standardCavityNo() != null) m.setCavityNumbers(dto.standardCavityNo());
    }

    /** Wrap CSV field value in quotes when it contains comma, quote, or newline. */
    private String csvQ(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
