package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import com.gdtahara.gdtaharabackend.model.SetupChecklistTemplate;
import com.gdtahara.gdtaharabackend.model.SetupJobStepResult;
import com.gdtahara.gdtaharabackend.repository.MachineSetupJobRepository;
import com.gdtahara.gdtaharabackend.repository.SetupChecklistTemplateRepository;
import com.gdtahara.gdtaharabackend.repository.SetupJobStepResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/setup-checklist")
@PreAuthorize("isAuthenticated()")
public class SetupChecklistController {

    private static final String UPLOAD_DIR = "uploads/setup-photos";

    @Autowired private SetupChecklistTemplateRepository templateRepo;
    @Autowired private SetupJobStepResultRepository stepResultRepo;
    @Autowired private MachineSetupJobRepository jobRepo;

    // ── Templates ─────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<List<SetupChecklistTemplate>> getTemplates(
            @RequestParam(required = false) String machineType,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        if (includeInactive) {
            return ResponseEntity.ok(templateRepo.findAllByOrderByStepOrderAsc());
        }
        if (machineType != null && !machineType.isBlank()) {
            return ResponseEntity.ok(templateRepo.findActiveByMachineType(machineType));
        }
        return ResponseEntity.ok(templateRepo.findByActiveTrueOrderByStepOrderAsc());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'DataAdmin')")
    public ResponseEntity<SetupChecklistTemplate> createTemplate(
            @RequestBody SetupChecklistTemplate template) {
        template.setId(null);
        return ResponseEntity.ok(templateRepo.save(template));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DataAdmin')")
    public ResponseEntity<SetupChecklistTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody SetupChecklistTemplate body) {
        SetupChecklistTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        t.setStepOrder(body.getStepOrder());
        t.setLabel(body.getLabel());
        t.setMachineType(body.getMachineType());
        t.setRequired(body.isRequired());
        t.setActive(body.isActive());
        return ResponseEntity.ok(templateRepo.save(t));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DataAdmin')")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long id) {
        SetupChecklistTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        t.setActive(false);
        templateRepo.save(t);
        return ResponseEntity.noContent().build();
    }

    // ── Step Results ──────────────────────────────────────────────────────────

    @GetMapping("/jobs/{jobId}/steps")
    public ResponseEntity<List<SetupJobStepResult>> getJobSteps(@PathVariable Long jobId) {
        return ResponseEntity.ok(stepResultRepo.findBySetupJobIdOrderByTemplateStepOrderAsc(jobId));
    }

    @PostMapping("/jobs/{jobId}/steps")
    public ResponseEntity<List<SetupJobStepResult>> saveSteps(
            @PathVariable Long jobId,
            @RequestBody List<StepResultRequest> requests) {
        MachineSetupJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Setup job not found: " + jobId));

        List<SetupJobStepResult> saved = new ArrayList<>();
        for (StepResultRequest req : requests) {
            SetupJobStepResult result = stepResultRepo
                    .findBySetupJobIdAndTemplateId(jobId, req.templateId())
                    .orElseGet(() -> {
                        SetupJobStepResult r = new SetupJobStepResult();
                        r.setSetupJob(job);
                        r.setTemplate(templateRepo.findById(req.templateId())
                                .orElseThrow(() -> new RuntimeException("Template not found: " + req.templateId())));
                        return r;
                    });
            result.setDone(req.done());
            result.setNotes(req.notes());
            if (req.done() && result.getCompletedAt() == null) {
                result.setCompletedAt(LocalDateTime.now());
            } else if (!req.done()) {
                result.setCompletedAt(null);
            }
            saved.add(stepResultRepo.save(result));
        }
        return ResponseEntity.ok(saved);
    }

    // ── Photo Upload ──────────────────────────────────────────────────────────

    @PostMapping("/jobs/{jobId}/steps/{templateId}/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long jobId,
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        // prevent path traversal in extension
        ext = ext.replaceAll("[^a-zA-Z0-9.]", "");
        String filename = "job" + jobId + "_tpl" + templateId + "_" + System.currentTimeMillis() + ext;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        MachineSetupJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        SetupJobStepResult result = stepResultRepo
                .findBySetupJobIdAndTemplateId(jobId, templateId)
                .orElseGet(() -> {
                    SetupJobStepResult r = new SetupJobStepResult();
                    r.setSetupJob(job);
                    r.setTemplate(templateRepo.findById(templateId)
                            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId)));
                    return r;
                });
        result.setPhotoFilename(filename);
        stepResultRepo.save(result);

        return ResponseEntity.ok(Map.of("filename", filename));
    }

    @GetMapping("/photos/{filename}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> servePhoto(@PathVariable String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = Paths.get(UPLOAD_DIR).resolve(filename);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        String lc = filename.toLowerCase();
        String contentType = lc.endsWith(".png") ? "image/png"
                : lc.endsWith(".gif") ? "image/gif"
                : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(Files.readAllBytes(file));
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    record StepResultRequest(Long templateId, boolean done, String notes) {}
}
