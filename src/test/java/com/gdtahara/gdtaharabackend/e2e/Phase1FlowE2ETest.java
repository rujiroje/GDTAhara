package com.gdtahara.gdtaharabackend.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.SubLotRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E integration test — full Phase 1 flow on H2 in-memory DB.
 *
 * No service/repository mocks: every layer (controller → service → repository → H2)
 * runs for real. Schema is built from JPA entities (Flyway disabled in "test" profile).
 *
 * Flow tested:
 *   (a) PC creates production plan  → 201
 *   (b) GET plans for date          → contains the new plan
 *   (c) Shift-split                 → dayTarget + nightTarget == targetQty
 *   (d) Seed ProductionReport via repo (parent-lot anchor for sub-lot numbers)
 *   (e) Operator confirms 2 boxes   → 201 × 2, sub-lot numbers end B0001 / B0002
 *   (f) Count boxes                 → {"count": 2}
 *   (g) Barcode PNG                 → 200, image/png, non-empty body
 *   (h) ZPL label preview           → 200, body starts with ^XA
 *
 * @Transactional rolls back every change after the test, except AuditLog entries
 * (they are committed by AuditLogService's REQUIRES_NEW inner transaction — harmless).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase1FlowE2ETest {

    // ── injected infrastructure ──────────────────────────────────────────────

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;

    @Autowired MachineRepository          machineRepo;
    @Autowired ProductRepository          productRepo;
    @Autowired UserRepository             userRepo;
    @Autowired ProductionReportRepository reportRepo;
    @Autowired SubLotRepository           subLotRepo;

    // ── per-test master-data (seeded in @BeforeEach) ─────────────────────────

    private Machine machine;
    private Product product;
    private User    pcUser;

    @BeforeEach
    void seedMasterData() {
        Machine m = new Machine();
        m.setMachineCode("RBL101");
        m.setMachineName("RBL-101");
        machine = machineRepo.save(m);

        Product p = new Product();
        p.setProductCode("P001");
        p.setProductName("Test Product");
        p.setQtyPerBox(600);
        product = productRepo.save(p);

        // pc01 is the authenticated PC user for plan creation
        pcUser = seedUser("pc01", "Production Control");

        // op01 is used by .with(user("op01")) — the service looks it up by username
        seedUser("op01", "Operator");
    }

    private User seedUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("{noop}test");   // value irrelevant — tests use @WithMockUser / user()
        u.setRole(role);
        return userRepo.save(u);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main E2E scenario
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "pc01", roles = {"Production Control"})
    void phase1_fullFlow_planToLabel() throws Exception {

        final LocalDate planDate = LocalDate.of(2026, 6, 2);

        // ── (a) PC creates a production plan ─────────────────────────────────
        String createBody = """
                {
                  "planDate"  : "%s",
                  "machineId" : %d,
                  "productId" : %d,
                  "targetQty" : 1200,
                  "source"    : "manual"
                }
                """.formatted(planDate, machine.getId(), product.getId());

        String planJson = mockMvc.perform(post("/api/production-plans/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.targetQty").value(1200))
                .andExpect(jsonPath("$.planDate").value(planDate.toString()))
                .andReturn().getResponse().getContentAsString();

        long planId = objectMapper.readTree(planJson).get("id").asLong();
        assertThat(planId).as("created plan id must be positive").isPositive();

        // ── (b) GET /date/{date} must contain the new plan ───────────────────
        mockMvc.perform(get("/api/production-plans/date/{date}", planDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId))
                .andExpect(jsonPath("$[0].targetQty").value(1200));

        // ── (c) Shift-split: dayTarget + nightTarget == 1200 ─────────────────
        String splitJson = mockMvc.perform(
                        get("/api/production-plans/{id}/shift-split", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayTarget").isNumber())
                .andExpect(jsonPath("$.nightTarget").isNumber())
                .andReturn().getResponse().getContentAsString();

        JsonNode split = objectMapper.readTree(splitJson);
        assertThat(split.get("dayTarget").asInt() + split.get("nightTarget").asInt())
                .as("dayTarget + nightTarget must equal targetQty (1200)")
                .isEqualTo(1200);

        // ── (d) Seed ProductionReport via repository ──────────────────────────
        //   (skip_creating via HTTP to avoid ORDER_OF endpoint setup; seed directly)
        ProductionReport report = new ProductionReport();
        report.setMachine(machine);
        report.setProduct(product);
        report.setPc(pcUser);
        report.setStartDate(planDate);
        report.setEndDate(planDate);
        report.setStatus("IN_PROGRESS");
        report.setTargetQty(1200);
        report.setOrderNumber("ORD-E2E-001");
        report.setParentLotNumber("PL-RBL101-20260602-D");
        report.setShift("D");
        // saveAndFlush: ensures the row is visible to the service within the same
        // Hibernate session before the first confirm-box request executes.
        report = reportRepo.saveAndFlush(report);

        final long reportId = report.getId();
        assertThat(reportId).as("seeded report id must be positive").isPositive();

        // ── (e) Operator confirms 2 boxes ─────────────────────────────────────
        //   Sub-lot number format (from SubLotService): {parentLotNumber}-B{seq:04d}
        for (int seq = 1; seq <= 2; seq++) {
            String boxBody = """
                    {
                      "productionReportId" : %d,
                      "boxQuantity"        : 600,
                      "weightKg"           : 12.5,
                      "palletNumber"       : "PAL-001"
                    }
                    """.formatted(reportId);

            String boxJson = mockMvc.perform(post("/api/sub-lots/confirm-box")
                            .with(user("op01").roles("Operator"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(boxBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subLotNumber").isString())
                    .andExpect(jsonPath("$.boxQuantity").value(600))
                    .andReturn().getResponse().getContentAsString();

            String subLotNumber = objectMapper.readTree(boxJson).get("subLotNumber").asText();
            assertThat(subLotNumber)
                    .as("sub-lot number for box %d must end with B%04d", seq, seq)
                    .endsWith(String.format("B%04d", seq));
        }

        // ── (f) Count → 2 ────────────────────────────────────────────────────
        mockMvc.perform(get("/api/sub-lots/report/{reportId}/count", reportId)
                        .with(user("op01").roles("Operator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // ── (g) Barcode PNG ───────────────────────────────────────────────────
        //   Retrieve sub-lot id via repository (same transaction → auto-flush finds it)
        long subLotId = subLotRepo
                .findByProductionReportIdOrderByConfirmedAt(reportId)
                .get(0).getId();

        byte[] png = mockMvc.perform(get("/api/sub-lots/{id}/barcode", subLotId)
                        .with(user("op01").roles("Operator")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(png).as("PNG barcode body must be non-empty").isNotEmpty();

        // ── (h) ZPL label preview ─────────────────────────────────────────────
        String zpl = mockMvc.perform(get("/api/labels/sub-lots/{id}/zpl", subLotId)
                        .with(user("op01").roles("Operator")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(zpl).as("ZPL output must begin with ^XA").startsWith("^XA");
    }
}
