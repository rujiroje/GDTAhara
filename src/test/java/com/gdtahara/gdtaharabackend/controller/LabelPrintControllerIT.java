package com.gdtahara.gdtaharabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.config.TestMvcSecurityConfig;
import com.gdtahara.gdtaharabackend.dto.PrintLabelRequest;
import com.gdtahara.gdtaharabackend.exception.GlobalExceptionHandler;
import com.gdtahara.gdtaharabackend.security.JwtUtil;
import com.gdtahara.gdtaharabackend.service.LabelPrintService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = LabelPrintController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@Import({TestMvcSecurityConfig.class, GlobalExceptionHandler.class})
class LabelPrintControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean LabelPrintService labelPrintService;

    // ── GET /zpl ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"Operator"})
    void zpl_returnsText() throws Exception {
        when(labelPrintService.previewSubLotLabel(1L)).thenReturn("^XA\n^FD TEST ^FS\n^XZ");

        mockMvc.perform(get("/api/labels/sub-lots/1/zpl"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("^XA")));
    }

    @Test
    @WithMockUser(roles = {"Operator"})
    void zpl_notFound_returns404() throws Exception {
        when(labelPrintService.previewSubLotLabel(99L))
                .thenThrow(new EntityNotFoundException("SubLot not found: 99"));

        mockMvc.perform(get("/api/labels/sub-lots/99/zpl"))
                .andExpect(status().isNotFound());
    }

    // ── POST /print ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"DataAdmin"}, username = "admin01")
    void print_ok_returns200() throws Exception {
        doNothing().when(labelPrintService)
                .printSubLotLabel(eq(1L), eq("192.168.1.100:9100"), any());

        PrintLabelRequest req = new PrintLabelRequest();
        req.setPrinterTarget("192.168.1.100:9100");

        mockMvc.perform(post("/api/labels/sub-lots/1/print")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("printed"))
                .andExpect(jsonPath("$.subLotId").value(1))
                .andExpect(jsonPath("$.target").value("192.168.1.100:9100"));

        verify(labelPrintService).printSubLotLabel(eq(1L), eq("192.168.1.100:9100"), any());
    }

    // ── security ──────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/labels/sub-lots/1/zpl"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"QA"})
    void wrongRole_returns403() throws Exception {
        mockMvc.perform(get("/api/labels/sub-lots/1/zpl"))
                .andExpect(status().isForbidden());
    }
}
