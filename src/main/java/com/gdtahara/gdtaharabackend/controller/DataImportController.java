// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/DataImportController.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@PreAuthorize("hasRole('DataAdmin') or hasRole('ADMIN')")
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/machines")
    public ResponseEntity<?> uploadMachines(@RequestParam("file") MultipartFile file) {
        try { dataImportService.importMachines(file); return ResponseEntity.ok("นำเข้าข้อมูลเครื่องจักรสำเร็จ"); } 
        catch (Exception e) { return ResponseEntity.badRequest().body("เกิดข้อผิดพลาด: " + e.getMessage()); }
    }

    @PostMapping("/ng-types")
    public ResponseEntity<?> uploadNgTypes(@RequestParam("file") MultipartFile file) {
         try { dataImportService.importNgTypes(file); return ResponseEntity.ok("นำเข้าข้อมูลประเภทของเสียสำเร็จ"); } 
         catch (Exception e) { return ResponseEntity.badRequest().body("เกิดข้อผิดพลาด: " + e.getMessage()); }
    }

    @PostMapping("/products")
    public ResponseEntity<?> uploadProducts(@RequestParam("file") MultipartFile file) {
         try { dataImportService.importProducts(file); return ResponseEntity.ok("นำเข้าข้อมูลผลิตภัณฑ์สำเร็จ"); } 
         catch (Exception e) { return ResponseEntity.badRequest().body("เกิดข้อผิดพลาด: " + e.getMessage()); }
    }

    @PostMapping("/materials")
    public ResponseEntity<?> uploadMaterials(@RequestParam("file") MultipartFile file) {
         try { dataImportService.importMaterials(file); return ResponseEntity.ok("นำเข้าข้อมูลวัตถุดิบสำเร็จ"); } 
         catch (Exception e) { return ResponseEntity.badRequest().body("เกิดข้อผิดพลาด: " + e.getMessage()); }
    }
}