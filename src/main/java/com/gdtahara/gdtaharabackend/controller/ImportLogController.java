package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.ImportLogResponse;
import com.gdtahara.gdtaharabackend.model.ImportLog;
import com.gdtahara.gdtaharabackend.repository.ImportLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/import-logs")
@PreAuthorize("hasAnyRole('Production Control','DataAdmin','Management')")
@Tag(name = "Import Log", description = "Excel import history (read-only)")
public class ImportLogController {

    private static final Logger logger = LoggerFactory.getLogger(ImportLogController.class);

    private final ImportLogRepository importLogRepository;

    public ImportLogController(ImportLogRepository importLogRepository) {
        this.importLogRepository = importLogRepository;
    }

    /**
     * List import logs, optionally filtered by factoryCode.
     * Accepts both "/api/import-logs" and "/api/import-logs/" (frontend uses no trailing slash).
     * @Transactional ensures importedBy (LAZY) is loaded within the session.
     */
    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public ResponseEntity<List<ImportLogResponse>> list(
            @RequestParam(required = false) String factoryCode) {
        logger.debug("GET import-logs factoryCode={}", factoryCode);
        List<ImportLog> logs = factoryCode != null && !factoryCode.isBlank()
                ? importLogRepository.findByFactoryCodeOrderByImportedAtDesc(factoryCode)
                : importLogRepository.findTop20ByOrderByImportedAtDesc();
        return ResponseEntity.ok(logs.stream().map(ImportLogResponse::from).toList());
    }

    @GetMapping("/recent")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ImportLogResponse>> recent() {
        return ResponseEntity.ok(importLogRepository.findTop20ByOrderByImportedAtDesc().stream()
                .map(ImportLogResponse::from).toList());
    }
}
