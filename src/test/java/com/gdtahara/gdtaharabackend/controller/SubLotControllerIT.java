package com.gdtahara.gdtaharabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.config.TestMvcSecurityConfig;
import com.gdtahara.gdtaharabackend.dto.ConfirmBoxRequest;
import com.gdtahara.gdtaharabackend.exception.GlobalExceptionHandler;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.service.SubLotService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SubLotController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@Import({TestMvcSecurityConfig.class, GlobalExceptionHandler.class})
class SubLotControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SubLotService subLotService;

    private SubLot stubSubLot(Long id, String number) {
        ProductionReport report = new ProductionReport();
        report.setId(1L);
        User user = new User();
        user.setId(10L);
        user.setUsername("op01");
        SubLot s = new SubLot();
        s.setId(id);
        s.setSubLotNumber(number);
        s.setBoxQuantity(100);
        s.setStatus("draft");
        s.setConfirmedAt(LocalDateTime.now());
        s.setProductionReport(report);
        s.setConfirmedBy(user);
        return s;
    }

    // ── 401 ──────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void confirmBox_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/sub-lots/confirm-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 400 validation ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Operator"}, username = "op01")
    void confirmBox_missingReportId_returns400() throws Exception {
        String body = "{\"boxQuantity\":100}";
        mockMvc.perform(post("/api/sub-lots/confirm-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── 201 confirm-box ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Operator"}, username = "op01")
    void confirmBox_validRequest_returns201() throws Exception {
        SubLot stub = stubSubLot(1L, "PL-MC01-20260601-D-B0001");
        when(subLotService.confirmBox(eq(1L), eq(100), any(), any(), eq("op01"))).thenReturn(stub);

        ConfirmBoxRequest req = new ConfirmBoxRequest();
        req.setProductionReportId(1L);
        req.setBoxQuantity(100);

        mockMvc.perform(post("/api/sub-lots/confirm-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subLotNumber").value("PL-MC01-20260601-D-B0001"));
    }

    // ── 409 FINALIZED report ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Operator"}, username = "op01")
    void confirmBox_finalizedReport_returns409() throws Exception {
        when(subLotService.confirmBox(anyLong(), anyInt(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Cannot add boxes to a finalized report"));

        ConfirmBoxRequest req = new ConfirmBoxRequest();
        req.setProductionReportId(1L);
        req.setBoxQuantity(100);

        mockMvc.perform(post("/api/sub-lots/confirm-box")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── GET /report/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Shift Leader"})
    void getByReport_returns200_withList() throws Exception {
        when(subLotService.getSubLotsForReport(1L)).thenReturn(
                List.of(stubSubLot(1L, "PL-MC01-20260601-D-B0001"),
                        stubSubLot(2L, "PL-MC01-20260601-D-B0002")));

        mockMvc.perform(get("/api/sub-lots/report/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subLotNumber").value("PL-MC01-20260601-D-B0001"))
                .andExpect(jsonPath("$[1].subLotNumber").value("PL-MC01-20260601-D-B0002"));
    }

    // ── GET /report/{id}/count ────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"})
    void countByReport_returns200() throws Exception {
        when(subLotService.countBoxesForReport(1L)).thenReturn(2L);
        mockMvc.perform(get("/api/sub-lots/report/1/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    // ── 404 unknown sub-lot number ────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"})
    void byNumber_unknownLot_returns404() throws Exception {
        when(subLotService.findBySubLotNumber("UNKNOWN")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/sub-lots/by-number/UNKNOWN"))
                .andExpect(status().isNotFound());
    }
}
