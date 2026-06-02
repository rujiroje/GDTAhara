package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.exception.PlanParseException;
import com.gdtahara.gdtaharabackend.dto.ImportResult;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionPlanImportServiceTest {

    @Mock PlanSheetTemplateRepository  templateRepository;
    @Mock MachineRepository            machineRepository;
    @Mock ProductRepository            productRepository;
    @Mock ProductionPlanRepository     planRepository;
    @Mock ProductionReportRepository   reportRepository;
    @Mock ImportLogRepository          importLogRepository;
    @Mock UserRepository               userRepository;
    @Mock MachineSetupJobService       setupJobService;

    @InjectMocks ProductionPlanImportService service;

    private Machine  mockMachine;
    private Product  mockProduct;
    private User     mockUser;

    @BeforeEach
    void setUp() {
        mockMachine = new Machine();
        mockMachine.setId(1L);
        mockMachine.setMachineCode("RBL101");
        mockMachine.setMachineName("RBL101");

        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setProductCode("PBCGAHBRB");
        mockProduct.setProductName("Bottle 500ml");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("pc01");

        lenient().when(machineRepository.findByMachineCode("RBL101")).thenReturn(Optional.of(mockMachine));
        lenient().when(productRepository.findByProductCode("PBCGAHBRB")).thenReturn(Optional.of(mockProduct));
        lenient().when(userRepository.findByUsername("pc01")).thenReturn(Optional.of(mockUser));
        lenient().when(templateRepository.findByFactoryCode(any())).thenReturn(Optional.empty());
        lenient().when(planRepository.findByMachineIdAndPlanDateAndProductId(any(), any(), any()))
                 .thenReturn(Optional.empty());
        lenient().when(reportRepository.existsForMachineOnDate(any(), any())).thenReturn(false);
        lenient().when(importLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Workbook builders ─────────────────────────────────────────────────────

    /**
     * Builds a workbook with SEPARATE machine/product columns (original L3 format).
     *  row 1 – 10 date cells at cols 2..11  (> 7 → L3 detects this row)
     *  row 2+ – machine (col 0), product (col 1), qty per date col
     */
    private MockMultipartFile buildWorkbook(String filename, LocalDate[] dates, int[] quantities)
            throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");

            Row dateRow = sheet.createRow(1);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));

            for (int i = 0; i < 10; i++) {
                LocalDate d = (i < dates.length) ? dates[i] : LocalDate.now().plusDays(i + 1);
                Cell cell = dateRow.createCell(2 + i);
                cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                cell.setCellStyle(dateStyle);
            }

            Row dataRow = sheet.createRow(2);
            dataRow.createCell(0).setCellValue("RBL101");
            dataRow.createCell(1).setCellValue("PBCGAHBRB");
            for (int i = 0; i < 10; i++) {
                int qty = (i < quantities.length) ? quantities[i] : 0;
                if (qty > 0) dataRow.createCell(2 + i).setCellValue(qty);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }

    /**
     * Builds a workbook that mimics the real TAHARA layout:
     *  row 5 (0-based) – date header at columns 13..22 (≥10 dates)
     *  row 6 (0-based) – skip row (sub-header), col 1 = "Plan" (skip keyword)
     *  row 7 (0-based) – data rows, col 1 = "RBL101   PBCGAHBRB" (combined), qty cols 13+
     *
     *  Matches template: machineColIdx=1, productColIdx=1 (both col B=0-based),
     *  dateHeaderRow=6 (1-based=row 5 0-based), dataStartRow=8 (1-based=row 7 0-based).
     */
    private MockMultipartFile buildTaharaWorkbook(String filename, LocalDate[] dates, int[] quantities)
            throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Plan");

            // Row 5 (0-based) = date header row
            Row dateRow = sheet.createRow(5);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
            for (int i = 0; i < 10; i++) {
                LocalDate d = (i < dates.length) ? dates[i] : LocalDate.now().plusDays(i + 1);
                Cell cell = dateRow.createCell(13 + i);
                cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                cell.setCellStyle(dateStyle);
            }

            // Row 6 (0-based) = sub-header / skip row
            Row subHeader = sheet.createRow(6);
            subHeader.createCell(2).setCellValue("Plan"); // col C = skip keyword

            // Row 7 (0-based) = first data row — combined machine+product in col B (1)
            Row dataRow = sheet.createRow(7);
            dataRow.createCell(1).setCellValue("RBL101     PBCGAHBRB");
            dataRow.createCell(2).setCellValue("Bottle 500ml"); // col C = product name (ignored)
            for (int i = 0; i < 10; i++) {
                int qty = (i < quantities.length) ? quantities[i] : 0;
                if (qty > 0) dataRow.createCell(13 + i).setCellValue(qty);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }

    // ── Regex unit tests ──────────────────────────────────────────────────────

    @Test
    void machineProductPattern_extractsCodes() {
        assertPattern("RBL101     PBCGAHBRB",       "RBL101",  "PBCGAHBRB");
        assertPattern("RBL102     EBKPMY0350A (2)", "RBL102",  "EBKPMY0350A");
        assertPattern("RBL114   TBRZS0250",         "RBL114",  "TBRZS0250");
        assertPattern("RIL205   XXPRODUCT",         "RIL205",  "XXPRODUCT");
    }

    private void assertPattern(String input, String expectedMachine, String expectedProduct) {
        Matcher m = ProductionPlanImportService.MACHINE_PRODUCT_PATTERN.matcher(input.trim());
        assertThat(m.find())
                .as("Pattern should match: %s", input)
                .isTrue();
        assertThat(m.group(1)).as("machine from [%s]", input).isEqualTo(expectedMachine);
        assertThat(m.group(2)).as("product from [%s]", input).isEqualTo(expectedProduct);
    }

    @Test
    void machineProductPattern_noMatchForSkipKeywords() {
        // "RBL101" alone (no product) should not match
        assertThat(ProductionPlanImportService.MACHINE_PRODUCT_PATTERN
                .matcher("RBL101").find()).isFalse();
        // Non-machine text
        assertThat(ProductionPlanImportService.MACHINE_PRODUCT_PATTERN
                .matcher("Remark").find()).isFalse();
        assertThat(ProductionPlanImportService.MACHINE_PRODUCT_PATTERN
                .matcher("Actual").find()).isFalse();
    }

    // ── Original L3 (separate columns) tests ─────────────────────────────────

    @Test
    void import_futureDates_upsert() throws Exception {
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);
        LocalDate d3 = LocalDate.now().plusDays(3);
        int[] qtys = {2400, 2400, 2400, 0, 0, 0, 0, 0, 0, 0};

        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{d1, d2, d3}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsAdded()).isEqualTo(3);
        assertThat(result.rowsSkippedPast()).isZero();
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(planRepository, times(3)).save(any(ProductionPlan.class));
        verify(setupJobService).scanAndCreateSetupJobs(eq(d1), eq(d3));
    }

    @Test
    void import_pastDates_skipped() throws Exception {
        LocalDate d1 = LocalDate.now().minusDays(3);
        LocalDate d2 = LocalDate.now().minusDays(2);
        LocalDate d3 = LocalDate.now().minusDays(1);
        int[] qtys = {2400, 2400, 2400, 0, 0, 0, 0, 0, 0, 0};

        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{d1, d2, d3}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsSkippedPast()).isEqualTo(3);
        assertThat(result.rowsAdded()).isZero();
        verify(planRepository, never()).save(any());
        verify(setupJobService, never()).scanAndCreateSetupJobs(any(), any());
    }

    @Test
    void import_todayWithReport_skipped() throws Exception {
        LocalDate today = LocalDate.now();
        int[] qtys = {2400, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        when(reportRepository.existsForMachineOnDate(1L, today)).thenReturn(true);

        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{today}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsSkippedStarted()).isEqualTo(1);
        assertThat(result.rowsAdded()).isZero();
        verify(planRepository, never()).save(any());
    }

    @Test
    void import_existingPlan_updates() throws Exception {
        LocalDate future = LocalDate.now().plusDays(5);
        int[] qtys = {3000, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        ProductionPlan existing = new ProductionPlan();
        existing.setId(99L);
        existing.setTargetQty(2400);
        when(planRepository.findByMachineIdAndPlanDateAndProductId(1L, future, 1L))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{future}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsUpdated()).isEqualTo(1);
        assertThat(result.rowsAdded()).isZero();
        assertThat(existing.getTargetQty()).isEqualTo(3000);
    }

    @Test
    void import_unknownMachine_warning() throws Exception {
        when(machineRepository.findByMachineCode("RBL101")).thenReturn(Optional.empty());

        LocalDate future = LocalDate.now().plusDays(1);
        int[] qtys = {2400, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{future}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsAdded()).isZero();
        assertThat(result.warnings()).anyMatch(w -> w.contains("RBL101"));
    }

    @Test
    void import_unknownProduct_warning() throws Exception {
        when(productRepository.findByProductCode("PBCGAHBRB")).thenReturn(Optional.empty());

        LocalDate future = LocalDate.now().plusDays(1);
        int[] qtys = {2400, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = buildWorkbook("upload.xlsx", new LocalDate[]{future}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.rowsAdded()).isZero();
        // Warning must contain the product code and not throw an exception
        assertThat(result.warnings()).anyMatch(w -> w.contains("PBCGAHBRB"));
    }

    @Test
    void badStructure_throws() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Bad");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            MockMultipartFile file = new MockMultipartFile("file", "garbage.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());

            assertThatThrownBy(() -> service.importPlanWorkbook(file, null, "pc01"))
                    .isInstanceOf(PlanParseException.class)
                    .hasMessageContaining("detect");
        }
    }

    // ── L1/L2 detection tests ─────────────────────────────────────────────────

    @Test
    void detection_filenameHeuristic_underscore() throws Exception {
        // Original underscore format: "Plan_TAHARA_June26_Rev00.xlsx"
        PlanSheetTemplate tpl = buildTemplate("TAHARA", null, 1, 2, 2, 3, "Remark|Actual|Diff");
        when(templateRepository.findByFactoryCode("TAHARA")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(1);
        MockMultipartFile file = buildWorkbook("Plan_TAHARA_June26_Rev00.xlsx",
                new LocalDate[]{future}, new int[]{2400, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.factoryCode()).isEqualTo("TAHARA");
    }

    @Test
    void detection_filenameHeuristic_spaces_and_apostrophe() throws Exception {
        // Lenient format: "Plan TAHARA June'26 Rev00.xlsx"
        PlanSheetTemplate tpl = buildTemplate("TAHARA", null, 1, 2, 2, 3, "Remark|Actual|Diff");
        when(templateRepository.findByFactoryCode("TAHARA")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(1);
        MockMultipartFile file = buildWorkbook("Plan TAHARA June'26 Rev00.xlsx",
                new LocalDate[]{future}, new int[]{2400, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.factoryCode()).isEqualTo("TAHARA");
    }

    @Test
    void import_factoryCodeOverride_usedForL1() throws Exception {
        PlanSheetTemplate tpl = buildTemplate("ASB", null, 1, 2, 2, 3, null);
        when(templateRepository.findByFactoryCode("ASB")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(1);
        MockMultipartFile file = buildWorkbook("someRandomName.xlsx",
                new LocalDate[]{future}, new int[]{1500, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, "ASB", "pc01");

        assertThat(result.factoryCode()).isEqualTo("ASB");
    }

    // ── TAHARA combined-column format tests ───────────────────────────────────

    /**
     * Tests the full flow with a TAHARA-format workbook via L1 (factory code override)
     * and a template with machineColIndex=productColIndex=2 (both column B).
     */
    @Test
    void import_tahara_combinedColumn_futureDates_added() throws Exception {
        // Template: col B (1-based=2) for both machine+product, date header row 6, data start row 8
        PlanSheetTemplate tpl = buildTemplate("TAHARA", "Plan", 2, 2, 6, 8, "Remark|Actual|Plan|Diff");
        when(templateRepository.findByFactoryCode("TAHARA")).thenReturn(Optional.of(tpl));

        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);
        MockMultipartFile file = buildTaharaWorkbook(
                "Plan_TAHARA_June26_Rev00.xlsx",
                new LocalDate[]{d1, d2},
                new int[]{2400, 2400, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, "TAHARA", "pc01");

        assertThat(result.rowsAdded()).isEqualTo(2);
        assertThat(result.rowsSkippedPast()).isZero();
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.factoryCode()).isEqualTo("TAHARA");
        verify(planRepository, times(2)).save(any(ProductionPlan.class));
    }

    /**
     * Combined-column row where extracted product code is unknown in DB → warning, not throw.
     */
    @Test
    void import_tahara_unknownProduct_warning_notThrow() throws Exception {
        when(productRepository.findByProductCode("PBCGAHBRB")).thenReturn(Optional.empty());

        PlanSheetTemplate tpl = buildTemplate("TAHARA", "Plan", 2, 2, 6, 8, "Remark|Actual|Plan|Diff");
        when(templateRepository.findByFactoryCode("TAHARA")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(3);
        MockMultipartFile file = buildTaharaWorkbook(
                "Plan_TAHARA_June26_Rev00.xlsx",
                new LocalDate[]{future},
                new int[]{2400, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, "TAHARA", "pc01");

        assertThat(result.rowsAdded()).isZero();
        assertThat(result.warnings()).anyMatch(w -> w.contains("PBCGAHBRB"));
    }

    /**
     * L3 auto-detects combined column from a TAHARA-format workbook (no template, no override).
     */
    @Test
    void import_tahara_l3_detects_combined_column() throws Exception {
        // No template → pure L3 detection (templateRepository already returns empty in @BeforeEach)
        LocalDate future = LocalDate.now().plusDays(5);
        MockMultipartFile file = buildTaharaWorkbook(
                "unknown_upload.xlsx",
                new LocalDate[]{future},
                new int[]{1800, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        // L3 should detect the combined column and parse the single data row
        assertThat(result.rowsAdded()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private PlanSheetTemplate buildTemplate(String factoryCode, String sheetHint,
                                            int machineCol, int productCol,
                                            int dateHeaderRow, int dataStartRow,
                                            String skipKeywords) {
        PlanSheetTemplate tpl = new PlanSheetTemplate();
        tpl.setFactoryCode(factoryCode);
        tpl.setSheetNameHint(sheetHint);
        tpl.setMachineColIndex(machineCol);
        tpl.setProductColIndex(productCol);
        tpl.setDateHeaderRow(dateHeaderRow);
        tpl.setDataStartRow(dataStartRow);
        tpl.setSkipRowKeywords(skipKeywords);
        return tpl;
    }
}
