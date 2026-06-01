package com.gdtahara.gdtaharabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.config.TestMvcSecurityConfig;
import com.gdtahara.gdtaharabackend.dto.SkipSetupRequest;
import com.gdtahara.gdtaharabackend.exception.GlobalExceptionHandler;
import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import com.gdtahara.gdtaharabackend.service.MachineSetupJobService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = MachineSetupJobController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@Import({TestMvcSecurityConfig.class, GlobalExceptionHandler.class})
class MachineSetupJobControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MachineSetupJobService machineSetupJobService;

    private MachineSetupJob stubJob(Long id, String status) {
        MachineSetupJob j = new MachineSetupJob();
        j.setId(id);
        j.setStatus(status);
        j.setPlanDate(LocalDate.of(2026, 6, 1));
        j.setMoldChanged(false);
        j.setTempAdjusted(false);
        j.setCycleAdjusted(false);
        j.setBlowPinAligned(false);
        j.setFpiPassed(false);
        return j;
    }

    // ── 401 ──────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void scan_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/setup-jobs/scan?from=2026-06-01&to=2026-06-30"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Technician cannot scan ────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Technician"})
    void scan_technicianRole_returns403() throws Exception {
        mockMvc.perform(post("/api/setup-jobs/scan?from=2026-06-01&to=2026-06-30"))
                .andExpect(status().isForbidden());
    }

    // ── 200 scan with Production Control ─────────────────────────────

    @Test
    @WithMockUser(roles = {"Production Control"}, username = "pc01")
    void scan_productionControl_returns200() throws Exception {
        when(machineSetupJobService.scanAndCreateSetupJobs(any(), any()))
                .thenReturn(List.of(stubJob(1L, "PENDING")));

        mockMvc.perform(post("/api/setup-jobs/scan?from=2026-06-01&to=2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ── 400 skip with blank reason ────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Technician"}, username = "tech01")
    void skip_blankReason_returns400() throws Exception {
        SkipSetupRequest req = new SkipSetupRequest();
        req.setSkipReason(""); // blank — @NotBlank should reject

        mockMvc.perform(put("/api/setup-jobs/1/skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── 404 plan has no active job ────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"})
    void getByPlan_notFound_returns404() throws Exception {
        when(machineSetupJobService.getJobForPlan(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/setup-jobs/plan/99"))
                .andExpect(status().isNotFound());
    }

    // ── 409 skip a COMPLETED job ──────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Technician"}, username = "tech01")
    void skip_completedJob_returns409() throws Exception {
        when(machineSetupJobService.skipSetup(eq(1L), any(), any()))
                .thenThrow(new IllegalStateException("Cannot skip a COMPLETED setup job: 1"));

        SkipSetupRequest req = new SkipSetupRequest();
        req.setSkipReason("Product cancelled");

        mockMvc.perform(put("/api/setup-jobs/1/skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ── Technician can start a job ────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Technician"}, username = "tech01")
    void startSetup_technicianRole_returns200() throws Exception {
        when(machineSetupJobService.startSetup(eq(1L), eq("tech01")))
                .thenReturn(stubJob(1L, "IN_PROGRESS"));

        mockMvc.perform(put("/api/setup-jobs/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
