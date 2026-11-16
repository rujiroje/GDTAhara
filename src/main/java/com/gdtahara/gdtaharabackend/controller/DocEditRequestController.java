package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.waste.DocResponseDto;
import com.gdtahara.gdtaharabackend.dto.waste.EditRequestStatusDto;
import com.gdtahara.gdtaharabackend.service.WasteWeightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/doc")
@PreAuthorize("hasAnyRole('Document', 'DataAdmin')")
public class DocEditRequestController {

    private final WasteWeightService wasteService;

    public DocEditRequestController(WasteWeightService wasteService) {
        this.wasteService = wasteService;
    }

    @GetMapping("/edit-requests")
    public ResponseEntity<List<EditRequestStatusDto>> getPendingRequests() {
        return ResponseEntity.ok(wasteService.getPendingRequests());
    }

    @PatchMapping("/edit-requests/{requestId}/respond")
    public ResponseEntity<EditRequestStatusDto> respond(@PathVariable Long requestId,
                                                         @Valid @RequestBody DocResponseDto dto,
                                                         Principal principal) {
        return ResponseEntity.ok(wasteService.respondToEditRequest(requestId, dto, principal.getName()));
    }
}
