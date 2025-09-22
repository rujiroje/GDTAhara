// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/NgLogController.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.service.NgLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ng-logs")
@PreAuthorize("hasAnyRole('Shift Leader', 'DataAdmin')")
public class NgLogController {

    @Autowired
    private NgLogService ngLogService;

    @PutMapping("/{id}")
    public ResponseEntity<NgLog> updateNgLog(@PathVariable Long id, @RequestBody NgLogRequestDto request) {
        NgLog updatedLog = ngLogService.updateNgLog(id, request);
        return ResponseEntity.ok(updatedLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNgLog(@PathVariable Long id) {
        ngLogService.deleteNgLog(id);
        return ResponseEntity.noContent().build();
    }
}