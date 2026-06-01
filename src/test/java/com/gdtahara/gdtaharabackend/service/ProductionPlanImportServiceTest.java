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

        // Common stubs (lenient so not every test needs to declare them)
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

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds an in-memory .xlsx that Layer-3 detection can pick up:
     *  row 0 – empty header
     *  row 1 – 10 date cells at cols 2..11  (>= 7 → Layer 3 detects this row)
     *  row 2+ – machine code (col 0), product code (col 1), qty per date (col 2..11)
     */
    private MockMultipartFile buildWorkbook(String filename, LocalDate[] dates, int[] quantities)
            throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");

            // Row 1 – date header (10 dates so count > 7)
            Row dateRow = sheet.createRow(1);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));

            // Fill all 10 columns with dates (first 'dates.length' real, rest with tomorrow)
            for (int i = 0; i < 10; i++) {
                LocalDate d = (i < dates.length) ? dates[i] : LocalDate.now().plusDays(i + 1);
                Cell cell = dateRow.createCell(2 + i);
                cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                cell.setCellStyle(dateStyle);
            }

            // Data row 2 – one machine/product row with quantities
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

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void import_futureDates_upsert() throws Exception {
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);
        LocalDate d3 = LocalDate.now().plusDays(3);
        int[] qtys = {2400, 2400, 2400, 0, 0, 0, 0, 0, 0, 0};

        MockMultipartFile file = buildWorkbook("upload.xlsx",
                new LocalDate[]{d1, d2, d3}, qtys);

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

        MockMultipartFile file = buildWorkbook("upload.xlsx",
                new LocalDate[]{d1, d2, d3}, qtys);

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
    void detection_filenameHeuristic() throws Exception {
        // Filename matches "Plan_TAHARA_June26_Rev00.xlsx" → factory_code = TAHARA
        PlanSheetTemplate tpl = new PlanSheetTemplate();
        tpl.setFactoryCode("TAHARA");
        tpl.setSheetNameHint(null);       // use first sheet
        tpl.setMachineColIndex(1);        // 1-based → 0-based = 0
        tpl.setProductColIndex(2);        // 1-based → 0-based = 1
        tpl.setDateHeaderRow(2);          // 1-based → 0-based = 1
        tpl.setDataStartRow(3);           // 1-based → 0-based = 2
        tpl.setSkipRowKeywords("Remark|Actual|Diff");

        when(templateRepository.findByFactoryCode("TAHARA")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(1);
        int[] qtys = {2400, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = buildWorkbook("Plan_TAHARA_June26_Rev00.xlsx",
                new LocalDate[]{future}, qtys);

        ImportResult result = service.importPlanWorkbook(file, null, "pc01");

        assertThat(result.factoryCode()).isEqualTo("TAHARA");
    }

    @Test
    void badStructure_throws() throws Exception {
        // Workbook with < 7 date cells in first 7 rows and no filename/template match
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Bad");          // empty sheet
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
    void import_factoryCodeOverride_usedForL1() throws Exception {
        PlanSheetTemplate tpl = new PlanSheetTemplate();
        tpl.setFactoryCode("ASB");
        tpl.setSheetNameHint(null);
        tpl.setMachineColIndex(1);
        tpl.setProductColIndex(2);
        tpl.setDateHeaderRow(2);
        tpl.setDataStartRow(3);
        tpl.setSkipRowKeywords(null);
        when(templateRepository.findByFactoryCode("ASB")).thenReturn(Optional.of(tpl));

        LocalDate future = LocalDate.now().plusDays(1);
        int[] qtys = {1500, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = buildWorkbook("someRandomName.xlsx",
                new LocalDate[]{future}, qtys);

        ImportResult result = service.importPlanWorkbook(file, "ASB", "pc01");

        assertThat(result.factoryCode()).isEqualTo("ASB");
    }
}
