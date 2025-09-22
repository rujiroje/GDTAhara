// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/AdminController.java
// (ฉบับแก้ไข เพิ่ม Parameter Checklist & Material Management กลับคืนมา)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.CreateUserRequest;
import com.gdtahara.gdtaharabackend.dto.ResetPasswordRequest;
import com.gdtahara.gdtaharabackend.dto.UserDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.service.MasterDataService;
import com.gdtahara.gdtaharabackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('DataAdmin') or hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private MasterDataService masterDataService;

    // --- User Management ---
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() { return ResponseEntity.ok(userService.findAllUsers()); }
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest userRequest) { userService.createUser(userRequest); return ResponseEntity.ok().build(); }
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDto userDetails) { userService.updateUser(id, userDetails); return ResponseEntity.ok().build(); }
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) { userService.deleteUser(id); return ResponseEntity.ok().build(); }
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) { userService.resetPassword(id, request.getNewPassword()); return ResponseEntity.ok().build(); }

    // --- Product Management ---
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() { return ResponseEntity.ok(masterDataService.getAllProducts()); }
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) { return ResponseEntity.ok(masterDataService.saveProduct(product)); }
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) { productDetails.setId(id); return ResponseEntity.ok(masterDataService.saveProduct(productDetails)); }
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) { masterDataService.deleteProduct(id); return ResponseEntity.ok().build(); }

    // --- Machine Management ---
    @GetMapping("/machines")
    public ResponseEntity<List<Machine>> getAllMachines() { return ResponseEntity.ok(masterDataService.getAllMachines()); }
    @PostMapping("/machines")
    public ResponseEntity<Machine> createMachine(@RequestBody Machine machine) { return ResponseEntity.ok(masterDataService.saveMachine(machine)); }
    @PutMapping("/machines/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody Machine machineDetails) { machineDetails.setId(id); return ResponseEntity.ok(masterDataService.saveMachine(machineDetails)); }
    @DeleteMapping("/machines/{id}")
    public ResponseEntity<?> deleteMachine(@PathVariable Long id) { masterDataService.deleteMachine(id); return ResponseEntity.ok().build(); }

    // --- NG Type Management ---
    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getAllNgTypes() { return ResponseEntity.ok(masterDataService.getAllNgTypes()); }
    @PostMapping("/ng-types")
    public ResponseEntity<NgType> createNgType(@RequestBody NgType ngType) { return ResponseEntity.ok(masterDataService.saveNgType(ngType)); }
    @PutMapping("/ng-types/{id}")
    public ResponseEntity<NgType> updateNgType(@PathVariable Long id, @RequestBody NgType ngTypeDetails) { ngTypeDetails.setId(id); return ResponseEntity.ok(masterDataService.saveNgType(ngTypeDetails)); }
    @DeleteMapping("/ng-types/{id}")
    public ResponseEntity<?> deleteNgType(@PathVariable Long id) { masterDataService.deleteNgType(id); return ResponseEntity.ok().build(); }
    
    // **[ใหม่]** --- Parameter Checklist Management ---
    @GetMapping("/parameter-checklists")
    public ResponseEntity<List<ParameterChecklist>> getAllParameterChecklists() { return ResponseEntity.ok(masterDataService.getAllParameterChecklists()); }
    @PostMapping("/parameter-checklists")
    public ResponseEntity<ParameterChecklist> createParameterChecklist(@RequestBody ParameterChecklist item) { return ResponseEntity.ok(masterDataService.saveParameterChecklist(item)); }
    @PutMapping("/parameter-checklists/{id}")
    public ResponseEntity<ParameterChecklist> updateParameterChecklist(@PathVariable Long id, @RequestBody ParameterChecklist itemDetails) { itemDetails.setId(id); return ResponseEntity.ok(masterDataService.saveParameterChecklist(itemDetails)); }
    @DeleteMapping("/parameter-checklists/{id}")
    public ResponseEntity<?> deleteParameterChecklist(@PathVariable Long id) { masterDataService.deleteParameterChecklist(id); return ResponseEntity.ok().build(); }

    // **[ใหม่]** --- Material Management ---
    @GetMapping("/materials")
    public ResponseEntity<List<Material>> getAllMaterials() { return ResponseEntity.ok(masterDataService.getAllMaterials()); }
    @PostMapping("/materials")
    public ResponseEntity<Material> createMaterial(@RequestBody Material material) { return ResponseEntity.ok(masterDataService.saveMaterial(material)); }
    @PutMapping("/materials/{id}")
    public ResponseEntity<Material> updateMaterial(@PathVariable Long id, @RequestBody Material materialDetails) { materialDetails.setId(id); return ResponseEntity.ok(masterDataService.saveMaterial(materialDetails)); }
    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) { masterDataService.deleteMaterial(id); return ResponseEntity.ok().build(); }
}