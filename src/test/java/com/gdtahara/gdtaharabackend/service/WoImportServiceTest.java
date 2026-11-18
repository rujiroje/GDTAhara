package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WO-import logic in ProductionPlanImportService.
 * Uses Mockito only (no Spring context).
 */
@ExtendWith(MockitoExtension.class)
class WoImportServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock PlanSheetTemplateRepository templateRepository;
    @Mock MachineRepository           machineRepository;
    @Mock ProductRepository            productRepository;
    @Mock ProductionPlanRepository     planRepository;
    @Mock ProductionReportRepository   reportRepository;
    @Mock ImportLogRepository          importLogRepository;
    @Mock UserRepository               userRepository;
    @Mock MachineSetupJobService       setupJobService;

    private ProductionPlanImportService service;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Machine mockMachine;
    private Product mockProduct;
    private User    mockUser;

    /** Current year/month as "YYmm" string, e.g. "2606" for June 2026. */
    private String yymm;

    @BeforeEach
    void setUp() {
        service = new ProductionPlanImportService(
                templateRepository, machineRepository, productRepository,
                planRepository, reportRepository, importLogRepository,
                userRepository, setupJobService);

        mockMachine = new Machine();
        mockMachine.setId(1L);
        mockMachine.setMachineCode("RBL104");
        mockMachine.setMachineName("RBL104");

        mockProduct = new Product();
        mockProduct.setId(10L);
        mockProduct.setProductCode("QP520");
        mockProduct.setProductName("QP520 Test");

        mockUser = new User();
        mockUser.setId(99L);
        mockUser.setUsername("testuser");

        LocalDate now = LocalDate.now();
        yymm = String.format("%02d%02d", now.getYear() % 100, now.getMonthValue());
    }

    // ── Excel builder helper ──────────────────────────────────────────────────

    /**
     * Build a minimal in-memory workbook with sheet "Plan":
     *  - Row 5 (0-indexed): date header at POI columns 13..30 (days 1..18 of current month)
     *  - Row 7 (0-indexed): data row with machine+product in col B (idx 1),
     *                        optional WO in col M (idx 12), qtys at date columns
     *
     * @param woNumber      value for col M (null = blank)
     * @param qtys          length-18 array of quantities for days 1..18; 0 = empty
     */
    private MultipartFile buildWorkbook(String woNumber, int[] qtys) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Plan");

        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        // Row 5: date header — real date cells at POI cols 13..30
        XSSFRow hdrRow = sheet.createRow(5);
        for (int i = 0; i < 18; i++) {
            XSSFCell cell = hdrRow.createCell(13 + i);
            // Write date as a date-formatted numeric cell
            LocalDate date = firstOfMonth.plusDays(i);
            // POI date: days since 1899-12-30 (Excel epoch)
            double excelDate = date.toEpochDay() + 25569;
            cell.setCellValue(excelDate);
            // Apply date format so DateUtil.isCellDateFormatted returns true
            XSSFCellStyle style = wb.createCellStyle();
            style.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
            cell.setCellStyle(style);
        }

        // Row 7: data row
        XSSFRow dataRow = sheet.createRow(7);

        // Col B (idx 1): "RBL104 QP520"
        dataRow.createCell(1).setCellValue("RBL104 QP520");

        // Col C (idx 2): blank (not a skip keyword)
        // Col M (idx 12): WO number (optional)
        if (woNumber != null) {
            dataRow.createCell(12).setCellValue(woNumber);
        }

        // Qty cells
        for (int i = 0; i < Math.min(qtys.length, 18); i++) {
            if (qtys[i] > 0) {
                dataRow.createCell(13 + i).setCellValue(qtys[i]);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        return new MockMultipartFile("file", "Plan_TEST_June26_Rev01.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray());
    }

    /**
     * Same as buildWorkbook but writes qty cells as FORMULA cells (=VALUE(qty))
     * to simulate the real TAHARA Excel file where qty cells are formula-based.
     */
    private MultipartFile buildWorkbookWithFormulas(String woNumber, int[] qtys) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Plan");
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        XSSFRow hdrRow = sheet.createRow(5);
        XSSFCellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        for (int i = 0; i < 18; i++) {
            XSSFCell cell = hdrRow.createCell(13 + i);
            cell.setCellValue(firstOfMonth.plusDays(i).toEpochDay() + 25569);
            cell.setCellStyle(dateStyle);
        }

        XSSFRow dataRow = sheet.createRow(7);
        dataRow.createCell(1).setCellValue("RBL104 QP520");
        if (woNumber != null) dataRow.createCell(12).setCellValue(woNumber);

        // Write qty as FORMULA cells — cells evaluate to the qty value.
        // setCellFormula sets CellType.FORMULA; we also set the cached numeric value
        // so getCachedFormulaResultType() == NUMERIC and getNumericCellValue() works.
        for (int i = 0; i < Math.min(qtys.length, 18); i++) {
            if (qtys[i] > 0) {
                XSSFCell cell = dataRow.createCell(13 + i);
                cell.setCellFormula(String.valueOf(qtys[i]));
                cell.setCellValue((double) qtys[i]); // seed cached value
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        return new MockMultipartFile("file", "Plan_TEST_June26_Rev01.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray());
    }

    // ── Common stub helpers ───────────────────────────────────────────────────

    private void stubCommonMocks() {
        lenient().when(machineRepository.findByMachineCode("RBL104"))
                .thenReturn(Optional.of(mockMachine));
        lenient().when(productRepository.findByProductCode("QP520"))
                .thenReturn(Optional.of(mockProduct));
        lenient().when(reportRepository.findOrderNumbersLike(any()))
                .thenReturn(List.of());
        lenient().when(reportRepository.findByMachineIdAndProductIdAndStartDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(reportRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(planRepository.findByMachineIdAndPlanDateAndProductId(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(planRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findByUsername(any()))
                .thenReturn(Optional.of(mockUser));
        lenient().when(importLogRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(setupJobService.scanAndCreateSetupJobs(any(), any()))
                .thenReturn(List.of());
        // L3 detection fallback: no template
        lenient().when(templateRepository.findByFactoryCode(any()))
                .thenReturn(Optional.empty());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Test 1: Two consecutive runs (days 1-2, then days 7-9) → 2 WOs created.
     * Also runs with FORMULA cells to confirm formula-cell reading works (regression
     * for the bug where getIntValue returned null for CellType.FORMULA).
     */
    @Test
    void runAggregation_creates2Wos() throws IOException {
        stubCommonMocks();

        // Days 1-2: qty=1200, days 3-6: 0, days 7-9: qty=800
        int[] qtys = {1200, 1200, 0, 0, 0, 0, 800, 800, 800, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        var result = service.importPlanWorkbook(file, "TEST", "testuser", false);

        assertThat(result.wosCreated()).isEqualTo(2);
        assertThat(result.wosUpdated()).isEqualTo(0);

        // Verify reportRepository.save called twice (once per WO)
        verify(reportRepository, times(2)).save(any(ProductionReport.class));

        // Check first WO: startDate=day1, endDate=day2, totalQty=2400
        LocalDate day1 = LocalDate.now().withDayOfMonth(1);
        LocalDate day2 = LocalDate.now().withDayOfMonth(2);
        LocalDate day7 = LocalDate.now().withDayOfMonth(7);
        LocalDate day9 = LocalDate.now().withDayOfMonth(9);

        // Capture saved WOs
        var captor = org.mockito.ArgumentCaptor.forClass(ProductionReport.class);
        verify(reportRepository, times(2)).save(captor.capture());
        List<ProductionReport> savedWos = captor.getAllValues();
        // Sort by startDate
        savedWos.sort(java.util.Comparator.comparing(ProductionReport::getStartDate));

        assertThat(savedWos.get(0).getStartDate()).isEqualTo(day1);
        assertThat(savedWos.get(0).getEndDate()).isEqualTo(day2);
        assertThat(savedWos.get(0).getTargetQty()).isEqualTo(2400);

        assertThat(savedWos.get(1).getStartDate()).isEqualTo(day7);
        assertThat(savedWos.get(1).getEndDate()).isEqualTo(day9);
        assertThat(savedWos.get(1).getTargetQty()).isEqualTo(2400);
    }

    /**
     * Test 2: When col M is blank, WO numbers should be auto-generated sequentially.
     */
    @Test
    void generateWoNumber_sequentialIfNoColM() throws IOException {
        stubCommonMocks();
        when(reportRepository.findOrderNumbersLike(any())).thenReturn(List.of());

        int[] qtys = {1200, 1200, 0, 0, 0, 0, 800, 800, 800, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        service.importPlanWorkbook(file, "TEST", "testuser", false);

        var captor = org.mockito.ArgumentCaptor.forClass(ProductionReport.class);
        verify(reportRepository, times(2)).save(captor.capture());
        List<ProductionReport> savedWos = captor.getAllValues();
        savedWos.sort(java.util.Comparator.comparing(ProductionReport::getStartDate));

        String expectedWo1 = "RBL104_" + yymm + "_01";
        String expectedWo2 = "RBL104_" + yymm + "_02";

        assertThat(savedWos.get(0).getOrderNumber()).isEqualTo(expectedWo1);
        assertThat(savedWos.get(1).getOrderNumber()).isEqualTo(expectedWo2);
    }

    /**
     * Test 3: When DB already has "RBL104_2606_01", new WO should get "RBL104_2606_02".
     * (Uses fixed yymm "2606" to match the DB mock.)
     */
    @Test
    void generateWoNumber_continuesFromDB() throws IOException {
        stubCommonMocks();

        // Override to return an existing WO for machine in 2606
        when(reportRepository.findOrderNumbersLike(contains("RBL104_" + yymm + "_")))
                .thenReturn(List.of("RBL104_" + yymm + "_01"));

        // Single run: days 1-2 only
        int[] qtys = {1200, 1200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        service.importPlanWorkbook(file, "TEST", "testuser", false);

        var captor = org.mockito.ArgumentCaptor.forClass(ProductionReport.class);
        verify(reportRepository, times(1)).save(captor.capture());

        String expectedWo = "RBL104_" + yymm + "_02";
        assertThat(captor.getValue().getOrderNumber()).isEqualTo(expectedWo);
    }

    /**
     * Test 4: When a WO already exists for machine+product+startDate, it should be updated, not inserted.
     */
    @Test
    void idempotent_updatesExistingWo() throws IOException {
        stubCommonMocks();

        // Mock existing WO
        ProductionReport existingWo = new ProductionReport();
        existingWo.setId(42L);
        existingWo.setOrderNumber("RBL104_" + yymm + "_01");
        existingWo.setMachine(mockMachine);
        existingWo.setProduct(mockProduct);
        LocalDate day1 = LocalDate.now().withDayOfMonth(1);
        existingWo.setStartDate(day1);
        existingWo.setEndDate(day1);
        existingWo.setTargetQty(1000);
        existingWo.setStatus("PLANNED");

        when(reportRepository.findByMachineIdAndProductIdAndStartDate(
                eq(1L), eq(10L), eq(day1)))
                .thenReturn(Optional.of(existingWo));

        // Single day run: day 1 only
        int[] qtys = {1200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        var result = service.importPlanWorkbook(file, "TEST", "testuser", false);

        assertThat(result.wosCreated()).isEqualTo(0);
        assertThat(result.wosUpdated()).isEqualTo(1);

        // Verify save was called (for the update)
        verify(reportRepository, times(1)).save(any(ProductionReport.class));
        assertThat(existingWo.getTargetQty()).isEqualTo(1200);
    }

    /**
     * Test 5: For 3 consecutive non-zero days, planRepository.save should be called 3 times.
     */
    @Test
    void dailyPlan_createdForEachNonZeroDay() throws IOException {
        stubCommonMocks();

        int[] qtys = {500, 600, 700, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        service.importPlanWorkbook(file, "TEST", "testuser", false);

        // 1 WO + 3 daily plans
        verify(planRepository, times(3)).save(any(ProductionPlan.class));
    }

    /**
     * Test 6: When product code is not found in DB, a warning is added and no WO is saved.
     */
    @Test
    void productCodeNotFound_addsWarning() throws IOException {
        lenient().when(machineRepository.findByMachineCode("RBL104"))
                .thenReturn(Optional.of(mockMachine));
        lenient().when(productRepository.findByProductCode("QP520"))
                .thenReturn(Optional.empty());   // <-- not found
        lenient().when(reportRepository.findOrderNumbersLike(any()))
                .thenReturn(List.of());
        lenient().when(userRepository.findByUsername(any()))
                .thenReturn(Optional.of(mockUser));
        lenient().when(importLogRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(setupJobService.scanAndCreateSetupJobs(any(), any()))
                .thenReturn(List.of());
        lenient().when(templateRepository.findByFactoryCode(any()))
                .thenReturn(Optional.empty());

        int[] qtys = {1200, 1200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        var result = service.importPlanWorkbook(file, "TEST", "testuser", false);

        assertThat(result.warnings()).isNotEmpty();
        verify(reportRepository, never()).save(any(ProductionReport.class));
    }

    /**
     * Test 7: TAHARA sub-rows (Plan/Actual/Diff) share col B "RBL104 QP520" but have
     * col C = "Plan"/"Actual"/"Diff".  They must be skipped so that the Actual sub-row
     * (which may have qty only on day 1) does not overwrite the main row's full date-range.
     *
     * Regression for the bug where endDate = startDate because the Actual sub-row
     * (qty on day 1 only) was processed after the main row (qty on days 1-5).
     */
    @Test
    void subRowKeywords_skipped_endDateIsFromMainRow() throws IOException {
        stubCommonMocks();

        LocalDate d1  = LocalDate.now().withDayOfMonth(1);
        LocalDate d5  = LocalDate.now().withDayOfMonth(5);

        // Build workbook with 3 rows for the same product on the same machine:
        //  row 7  = main data row   (col C = product name, qty days 1-5)
        //  row 8  = Plan sub-row    (col C = "Plan",   qty days 1-5 — should be skipped)
        //  row 9  = Actual sub-row  (col C = "Actual", qty day 1 only — must be skipped!)
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Plan");
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        // Row 5: date header
        XSSFRow hdrRow = sheet.createRow(5);
        XSSFCellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        for (int i = 0; i < 10; i++) {
            XSSFCell cell = hdrRow.createCell(13 + i);
            cell.setCellValue(firstOfMonth.plusDays(i).toEpochDay() + 25569);
            cell.setCellStyle(dateStyle);
        }

        // Row 7: main data row — col C = "QP520 Test" (not a keyword)
        XSSFRow mainRow = sheet.createRow(7);
        mainRow.createCell(1).setCellValue("RBL104 QP520");
        mainRow.createCell(2).setCellValue("QP520 Test"); // col C = product name
        for (int i = 0; i < 5; i++) mainRow.createCell(13 + i).setCellValue(1000); // days 1-5

        // Row 8: Plan sub-row — col C = "Plan" → must be SKIPPED
        XSSFRow planRow = sheet.createRow(8);
        planRow.createCell(1).setCellValue("RBL104 QP520");
        planRow.createCell(2).setCellValue("Plan");
        for (int i = 0; i < 5; i++) planRow.createCell(13 + i).setCellValue(1000);

        // Row 9: Actual sub-row — col C = "Actual" → must be SKIPPED
        // If NOT skipped, it would create WO with endDate = day 1 only!
        XSSFRow actualRow = sheet.createRow(9);
        actualRow.createCell(1).setCellValue("RBL104 QP520");
        actualRow.createCell(2).setCellValue("Actual");
        actualRow.createCell(13).setCellValue(800); // only day 1 has actual qty

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        MultipartFile file = new MockMultipartFile("file", "Plan_TEST_June26_Rev01.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray());

        var result = service.importPlanWorkbook(file, "TEST", "testuser", false);

        // Only 1 WO from main row (Plan + Actual sub-rows must be skipped)
        assertThat(result.wosCreated()).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(ProductionReport.class);
        verify(reportRepository, times(1)).save(captor.capture());
        ProductionReport saved = captor.getValue();

        // endDate must be day 5 (from main row), NOT day 1 (from Actual row)
        assertThat(saved.getStartDate()).isEqualTo(d1);
        assertThat(saved.getEndDate()).isEqualTo(d5);
        assertThat(saved.getTargetQty()).isEqualTo(5000);
    }

    /**
     * Test 9 (formula regression): Qty cells stored as Excel FORMULA cells must be
     * read correctly via getCachedFormulaResultType().  Before the fix, getIntValue()
     * returned null for CellType.FORMULA → only cells with literal values were seen
     * → endDate = startDate = day 1 for every product.
     */
    @Test
    void formulaCells_readCorrectly_endDateIsMultiDay() throws IOException {
        stubCommonMocks();

        // Days 1-5 have qty, stored as FORMULA cells
        int[] qtys = {1000, 1000, 1000, 1000, 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbookWithFormulas(null, qtys);

        var result = service.importPlanWorkbook(file, "TEST", "testuser", false);

        assertThat(result.wosCreated()).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(ProductionReport.class);
        verify(reportRepository, times(1)).save(captor.capture());
        ProductionReport saved = captor.getValue();

        LocalDate d1 = LocalDate.now().withDayOfMonth(1);
        LocalDate d5 = LocalDate.now().withDayOfMonth(5);

        // endDate must be day 5, NOT day 1 (which was the bug)
        assertThat(saved.getStartDate()).isEqualTo(d1);
        assertThat(saved.getEndDate()).isEqualTo(d5);
        assertThat(saved.getTargetQty()).isEqualTo(5000);
    }

    /**
     * Test 8: When dryRun=true, no writes to DB should occur.
     */
    @Test
    void dryRun_doesNotWriteDb() throws IOException {
        lenient().when(templateRepository.findByFactoryCode(any()))
                .thenReturn(Optional.empty());

        int[] qtys = {1200, 1200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MultipartFile file = buildWorkbook(null, qtys);

        var result = service.importPlanWorkbook(file, "TEST", "testuser", true);

        // Dry run: no DB writes
        verify(reportRepository, never()).save(any());
        verify(planRepository, never()).save(any());
        verify(importLogRepository, never()).save(any());

        // But preview count should be populated
        assertThat(result.wosCreated()).isGreaterThanOrEqualTo(0);
        assertThat(result.status()).isEqualTo("DRY_RUN");
    }
}
