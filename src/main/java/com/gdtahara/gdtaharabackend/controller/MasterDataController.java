// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/MasterDataController.java
// (ฉบับแก้ไข เพิ่ม API สำหรับ Materials)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.model.Material;
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.service.MasterDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/master-data")
@PreAuthorize("isAuthenticated()")
public class MasterDataController {

    @Autowired
    private MasterDataService masterDataService;

    @GetMapping("/ng-types")
    public ResponseEntity<List<NgType>> getAllNgTypes() {
        return ResponseEntity.ok(masterDataService.getAllNgTypes());
    }
    
    @GetMapping("/materials")
    public ResponseEntity<List<Material>> getAllMaterials() {
        return ResponseEntity.ok(masterDataService.getAllMaterials());
    }
}