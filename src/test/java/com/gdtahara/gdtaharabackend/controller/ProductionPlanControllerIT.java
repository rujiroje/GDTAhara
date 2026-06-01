package com.gdtahara.gdtaharabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.config.TestMvcSecurityConfig;
import com.gdtahara.gdtaharabackend.dto.CreatePlanRequest;
import com.gdtahara.gdtaharabackend.dto.ProductionPlanResponse;
import com.gdtahara.gdtaharabackend.dto.UpdatePlanRequest;
import com.gdtahara.gdtaharabackend.exception.GlobalExceptionHandler;
import com.gdtahara.gdtaharabackend.model.ProductionPlan;
import com.gdtahara.gdtaharabackend.service.ProductionPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductionPlanController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@Import({TestMvcSecurityConfig.class, GlobalExceptionHandler.class})
class ProductionPlanControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ProductionPlanService productionPlanService;

    // ── 401 / 403 ────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void getByDate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/production-plans/date/2026-06-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"Operator"})
    void createPlan_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/production-plans/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── 400 validation ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"}, username = "admin01")
    void createPlan_missingPlanDate_returns400() throws Exception {
        String body = "{\"machineId\":1,\"productId\":1,\"targetQty\":500,\"source\":\"manual\"}";
        mockMvc.perform(post("/api/production-plans/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── 201 happy path ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"}, username = "admin01")
    void createPlan_validRequest_returns201() throws Exception {
        ProductionPlan stub = new ProductionPlan();
        stub.setId(1L);
        stub.setPlanDate(LocalDate.of(2026, 6, 1));
        stub.setTargetQty(500);
        stub.setSource("manual");
        stub.setStatus("draft");
        stub.setManpowerDRatio(new BigDecimal("0.50"));
        stub.setManpowerNRatio(new BigDecimal("0.50"));

        when(productionPlanService.createPlan(any(), anyLong(), anyLong(), anyInt(),
                any(), any(), any(), any(), any(), any())).thenReturn(stub);

        CreatePlanRequest req = new CreatePlanRequest();
        req.setPlanDate(LocalDate.of(2026, 6, 1));
        req.setMachineId(1L);
        req.setProductId(1L);
        req.setTargetQty(500);
        req.setSource("manual");

        mockMvc.perform(post("/api/production-plans/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ── 409 duplicate ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Production Control"}, username = "pc01")
    void createPlan_duplicateKey_returns409() throws Exception {
        when(productionPlanService.createPlan(any(), anyLong(), anyLong(), anyInt(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("A plan already exists for machine 1 on 2026-06-01 for product 1"));

        CreatePlanRequest req = new CreatePlanRequest();
        req.setPlanDate(LocalDate.of(2026, 6, 1));
        req.setMachineId(1L);
        req.setProductId(1L);
        req.setTargetQty(500);
        req.setSource("manual");

        mockMvc.perform(post("/api/production-plans/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── GET /date/{date} open to more roles ──────────────────────────

    @Test
    @WithMockUser(roles = {"Operator"})
    void getByDate_operatorRole_returns200() throws Exception {
        when(productionPlanService.getPlansForDate(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/production-plans/date/2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /{id}/shift-split ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"})
    void shiftSplit_missingPlan_returns404() throws Exception {
        when(productionPlanService.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/production-plans/99/shift-split"))
                .andExpect(status().isNotFound());
    }
}
