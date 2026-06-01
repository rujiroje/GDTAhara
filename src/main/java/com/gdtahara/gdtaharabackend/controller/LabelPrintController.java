package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.PrintLabelRequest;
import com.gdtahara.gdtaharabackend.service.LabelPrintService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/labels")
@PreAuthorize("hasAnyRole('Operator','Shift Leader','Production Control','DataAdmin')")
@Tag(name = "Label Print", description = "ZPL preview and label print for sub-lots")
public class LabelPrintController {

    private final LabelPrintService labelPrintService;

    public LabelPrintController(LabelPrintService labelPrintService) {
        this.labelPrintService = labelPrintService;
    }

    @GetMapping(value = "/sub-lots/{id}/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getZpl(@PathVariable Long id) {
        return ResponseEntity.ok(labelPrintService.previewSubLotLabel(id));
    }

    @PostMapping("/sub-lots/{id}/print")
    public ResponseEntity<Map<String, Object>> print(
            @PathVariable Long id,
            @Valid @RequestBody PrintLabelRequest req,
            Principal principal) {
        labelPrintService.printSubLotLabel(id, req.getPrinterTarget(), principal.getName());
        return ResponseEntity.ok(Map.of(
                "status", "printed",
                "subLotId", id,
                "target", req.getPrinterTarget()));
    }
}
