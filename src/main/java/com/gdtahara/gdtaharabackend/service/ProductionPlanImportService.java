package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.ImportResult;
import com.gdtahara.gdtaharabackend.exception.PlanParseException;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductionPlanImportService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanImportService.class);

    /**
     * Lenient filename heuristic — matches both underscore and space separators, handles
     * apostrophes in month names ("June'26"):
     *   "Plan_TAHARA_June26_Rev00.xlsx"  → group(1)=TAHARA
     *   "Plan TAHARA June'26 Rev00.xlsx" → group(1)=TAHARA
     */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("(?i)plan[ _]+([A-Za-z]+)[ _]+.*rev\\s*\\d+");

    /** Exact standalone machine code, e.g. "RBL101". */
    private static final Pattern MACHINE_CODE_PATTERN = Pattern.compile("^[A-Z]{2,4}\\d+$");

    /**
     * Combined "MACHINE_CODE   PRODUCT_CODE [variant]" in a single cell.
     * group(1) = machine code (RBL/RIL prefix + digits)
     * group(2) = product code (first non-whitespace token after machine code)
     * Variant suffixes like " (2)" are dropped automatically by \S+ stopping at whitespace.
     */
    static final Pattern MACHINE_PRODUCT_PATTERN =
            Pattern.compile("^([A-Z]{2,4}\\d+)\\s+(\\S+)");

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final PlanSheetTemplateRepository templateRepository;
    private final MachineRepository           machineRepository;
    private final ProductRepository            productRepository;
    private final ProductionPlanRepository     planRepository;
    private final ProductionReportRepository   reportRepository;
    private final ImportLogRepository          importLogRepository;
    private final UserRepository               userRepository;
    private final MachineSetupJobService       setupJobService;

    public ProductionPlanImportService(
            PlanSheetTemplateRepository templateRepository,
            MachineRepository machineRepository,
            ProductRepository productRepository,
            ProductionPlanRepository planRepository,
            ProductionReportRepository reportRepository,
            ImportLogRepository importLogRepository,
            UserRepository userRepository,
            MachineSetupJobService setupJobService) {
        this.templateRepository = templateRepository;
        this.machineRepository  = machineRepository;
        this.productRepository  = productRepository;
        this.planRepository     = planRepository;
        this.reportRepository   = reportRepository;
        this.importLogRepository = importLogRepository;
        this.userRepository     = userRepository;
        this.setupJobService    = setupJobService;
    }

    // ── Internal types ────────────────────────────────────────────────────────

    private record SheetConfig(
            XSSFSheet sheet,
            String    factoryCode,
            int machineColIdx,      // 0-based
            int productColIdx,      // 0-based; == machineColIdx when combined cell
            int dateHeaderRowIdx,   // 0-based
            int dataStartRowIdx,    // 0-based
            Set<String> skipKeywords) {}

    private record PlanRecord(
            String machineCode,
            String productCode,
            LocalDate planDate,
            int targetQty) {}

    private static class Counts {
        int added, updated, skippedPast, skippedStarted;
        final List<String> warnings = new ArrayList<>();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public ImportResult importPlanWorkbook(MultipartFile file,
                                           String factoryCodeOverride,
                                           String username) {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "upload.xlsx";
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            SheetConfig config  = detectConfig(wb, filename, factoryCodeOverride);
            List<PlanRecord> records = parseSheet(config, filename);
            Counts counts = applyPolicy(records, filename, username);
            writeLog(filename, config.factoryCode(), counts, username);
            return new ImportResult(filename, config.factoryCode(),
                    config.sheet().getSheetName(),
                    counts.added, counts.updated,
                    counts.skippedPast, counts.skippedStarted,
                    "SUCCESS",
                    String.format("+%d new  ~%d updated  %d past-skipped  %d started-skipped",
                            counts.added, counts.updated, counts.skippedPast, counts.skippedStarted),
                    List.copyOf(counts.warnings));
        } catch (PlanParseException e) {
            throw e;
        } catch (IOException e) {
            throw new PlanParseException("Cannot open workbook: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PlanParseException("Import failed: " + e.getMessage(), e);
        }
    }

    // ── 3-Layer detection ─────────────────────────────────────────────────────

    private SheetConfig detectConfig(XSSFWorkbook wb, String filename, String override) {

        // Layer 1 — explicit factory code from caller or DB
        String fc = (override != null && !override.isBlank()) ? override.toUpperCase() : null;
        if (fc != null) {
            Optional<PlanSheetTemplate> tpl = templateRepository.findByFactoryCode(fc);
            if (tpl.isPresent()) {
                logger.info("L1 detection: factory_code={}", fc);
                return fromTemplate(wb, tpl.get(), fc);
            }
        }

        // Layer 2 — filename heuristic (lenient pattern)
        Matcher m = FILENAME_PATTERN.matcher(filename);
        if (m.find()) {
            String fcFromFile = m.group(1).toUpperCase();
            Optional<PlanSheetTemplate> tpl = templateRepository.findByFactoryCode(fcFromFile);
            if (tpl.isPresent()) {
                logger.info("L2 detection: filename={} → factory_code={}", filename, fcFromFile);
                return fromTemplate(wb, tpl.get(), fcFromFile);
            }
            logger.info("L2: filename matched factory_code={} but no template; falling to L3", fcFromFile);
            return detectByStructure(wb, fcFromFile);
        }

        // Layer 3 — smart structure auto-detection
        logger.info("L3 detection: no hint available for {}", filename);
        return detectByStructure(wb, null);
    }

    private SheetConfig fromTemplate(XSSFWorkbook wb, PlanSheetTemplate tpl, String factoryCode) {
        XSSFSheet sheet = tpl.getSheetNameHint() != null
                ? wb.getSheet(tpl.getSheetNameHint()) : null;
        if (sheet == null) sheet = wb.getSheetAt(0);

        Set<String> keywords = Set.of();
        if (tpl.getSkipRowKeywords() != null && !tpl.getSkipRowKeywords().isBlank()) {
            keywords = Arrays.stream(tpl.getSkipRowKeywords().split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        return new SheetConfig(sheet, factoryCode,
                tpl.getMachineColIndex() - 1,   // 1-based → 0-based
                tpl.getProductColIndex() - 1,   // 1-based → 0-based (== machineCol when combined)
                tpl.getDateHeaderRow()  - 1,
                tpl.getDataStartRow()   - 1,
                keywords);
    }

    /**
     * L3: scan the first 10 rows of every sheet; pick the sheet+row combination that
     * has the most date cells (≥7 required).  Then auto-detect whether the machine
     * column uses the combined "MACHINE PRODUCT_CODE" format.
     */
    private SheetConfig detectByStructure(XSSFWorkbook wb, String factoryCode) {
        XSSFSheet bestSheet = null;
        int bestDateRow   = -1;
        int maxDateCells  = 0;

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            XSSFSheet sheet = wb.getSheetAt(si);
            // Scan first 10 rows (indices 0..9)
            for (int ri = 0; ri <= Math.min(9, sheet.getLastRowNum()); ri++) {
                Row row = sheet.getRow(ri);
                if (row == null) continue;
                int cnt = 0;
                for (Cell cell : row) if (isDateCell(cell)) cnt++;
                if (cnt > maxDateCells) {
                    maxDateCells = cnt;
                    bestSheet    = sheet;
                    bestDateRow  = ri;
                }
            }
        }

        if (bestSheet == null || maxDateCells < 7) {
            throw new PlanParseException(
                    "Could not detect plan sheet structure. " +
                    "Provide factory_code parameter or check file format.");
        }

        int machineColIdx = detectMachineCol(bestSheet, bestDateRow + 1);
        // Check if this column uses combined "MACHINE PRODUCT_CODE" cells
        boolean combined = isCombinedColumn(bestSheet, bestDateRow + 1, machineColIdx);
        int productColIdx = combined ? machineColIdx : machineColIdx + 1;

        logger.info("L3: sheet='{}' dateHeaderRow={} machineCol={} combined={} maxDateCells={}",
                bestSheet.getSheetName(), bestDateRow, machineColIdx, combined, maxDateCells);

        return new SheetConfig(bestSheet,
                factoryCode != null ? factoryCode : "UNKNOWN",
                machineColIdx, productColIdx,
                bestDateRow, bestDateRow + 1,
                Set.of("Remark", "Actual", "Plan", "Diff"));
    }

    /**
     * Returns the first column index (0-based) where ≥30 % of data rows contain a
     * value that looks like a machine code — either standalone ("RBL101") or combined
     * ("RBL101   PBCGAHBRB").
     */
    private int detectMachineCol(XSSFSheet sheet, int dataStartRow) {
        Map<Integer, Integer> hits  = new HashMap<>();
        int totalRows = 0;
        for (int ri = dataStartRow; ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null) continue;
            totalRows++;
            for (Cell cell : row) {
                String v = getStringValue(cell);
                if (v == null) continue;
                String trimmed = v.trim();
                if (MACHINE_CODE_PATTERN.matcher(trimmed).matches()
                        || MACHINE_PRODUCT_PATTERN.matcher(trimmed).find()) {
                    hits.merge(cell.getColumnIndex(), 1, Integer::sum);
                }
            }
        }
        if (totalRows == 0) return 0;
        final int rows = totalRows;
        return hits.entrySet().stream()
                .filter(e -> (double) e.getValue() / rows >= 0.30)
                .mapToInt(Map.Entry::getKey)
                .min()
                .orElse(0);
    }

    /**
     * Returns true if ≥25 % of non-blank cells in {@code colIdx} match the combined
     * "MACHINE_CODE   PRODUCT_CODE" pattern — meaning both codes share one cell.
     */
    private boolean isCombinedColumn(XSSFSheet sheet, int dataStartRow, int colIdx) {
        int combined = 0, total = 0;
        int limit = Math.min(sheet.getLastRowNum(), dataStartRow + 60);
        for (int ri = dataStartRow; ri <= limit; ri++) {
            Row row = sheet.getRow(ri);
            if (row == null) continue;
            String v = getStringValue(row.getCell(colIdx));
            if (v == null || v.isBlank()) continue;
            total++;
            if (MACHINE_PRODUCT_PATTERN.matcher(v.trim()).find()) combined++;
        }
        return total > 0 && (double) combined / total >= 0.25;
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    private List<PlanRecord> parseSheet(SheetConfig cfg, String filename) {
        List<PlanRecord> out = new ArrayList<>();
        XSSFSheet sheet = cfg.sheet();

        // Build date map: colIdx → LocalDate (from header row)
        Map<Integer, LocalDate> dateByCol = new LinkedHashMap<>();
        Row hdr = sheet.getRow(cfg.dateHeaderRowIdx());
        if (hdr != null) {
            for (Cell cell : hdr) {
                LocalDate d = parseDateCell(cell);
                if (d != null) dateByCol.put(cell.getColumnIndex(), d);
            }
        }
        if (dateByCol.isEmpty()) {
            logger.warn("No date columns found in {}", filename);
            return out;
        }

        boolean combinedCol = cfg.machineColIdx() == cfg.productColIdx();
        String currentMachine = null;

        for (int ri = cfg.dataStartRowIdx(); ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null) continue;

            String mc, prod;
            if (combinedCol) {
                // ── Combined cell: "MACHINE_CODE   PRODUCT_CODE [variant]" ──────
                String cellValue = getStringValue(row.getCell(cfg.machineColIdx()));
                if (cellValue != null && !cellValue.isBlank()) {
                    Matcher split = MACHINE_PRODUCT_PATTERN.matcher(cellValue.trim());
                    if (split.find()) {
                        currentMachine = split.group(1);
                        prod = split.group(2);
                    } else {
                        prod = null; // doesn't match → no product on this row
                    }
                } else {
                    prod = null;
                }
                // Skip rows whose extracted product is a sub-row-type keyword
                if (prod == null || cfg.skipKeywords().stream()
                        .anyMatch(kw -> kw.equalsIgnoreCase(prod))) {
                    continue;
                }
                if (currentMachine == null) continue;
                mc = currentMachine;
            } else {
                // ── Separate columns: machine in one cell, product in another ──
                String mcRaw = getStringValue(row.getCell(cfg.machineColIdx()));
                if (mcRaw != null && !mcRaw.isBlank()) currentMachine = mcRaw.trim();
                if (currentMachine == null) continue;
                mc = currentMachine;

                prod = getStringValue(row.getCell(cfg.productColIdx()));
                if (prod == null || prod.isBlank()) continue;
                if (cfg.skipKeywords().stream().anyMatch(kw -> kw.equalsIgnoreCase(prod))) continue;
            }

            // ── Emit a PlanRecord for every date column with qty > 0 ─────────
            for (Map.Entry<Integer, LocalDate> entry : dateByCol.entrySet()) {
                Integer qty = getIntValue(row.getCell(entry.getKey()));
                if (qty != null && qty > 0) {
                    out.add(new PlanRecord(mc, prod, entry.getValue(), qty));
                }
            }
        }
        logger.info("Parsed {} records from sheet '{}'", out.size(), sheet.getSheetName());
        return out;
    }

    // ── Import policy ─────────────────────────────────────────────────────────

    private Counts applyPolicy(List<PlanRecord> records, String filename, String username) {
        Counts c    = new Counts();
        LocalDate today = LocalDate.now();
        TreeSet<LocalDate> upsertedDates = new TreeSet<>();

        User user = userRepository.findByUsername(username).orElse(null);

        for (PlanRecord rec : records) {
            // Resolve machine by machine_name (the file uses the same value as machine_name)
            Optional<Machine> machineOpt = machineRepository.findByMachineCode(rec.machineCode());
            if (machineOpt.isEmpty()) {
                c.warnings.add("Machine not found: " + rec.machineCode());
                continue;
            }
            Machine machine = machineOpt.get();

            // Resolve product
            Optional<Product> productOpt = productRepository.findByProductCode(rec.productCode());
            if (productOpt.isEmpty()) {
                c.warnings.add(rec.machineCode() + ": product code '"
                        + rec.productCode() + "' not found in DB");
                continue;
            }
            Product product = productOpt.get();

            // ── Import policy ─────────────────────────────────────────────────
            if (rec.planDate().isBefore(today)) {
                c.skippedPast++;
                continue;
            }
            if (rec.planDate().isEqual(today)) {
                if (reportRepository.existsForMachineOnDate(machine.getId(), today)) {
                    c.skippedStarted++;
                    continue;
                }
            }

            // UPSERT
            Optional<ProductionPlan> existing = planRepository
                    .findByMachineIdAndPlanDateAndProductId(
                            machine.getId(), rec.planDate(), product.getId());

            if (existing.isPresent()) {
                ProductionPlan plan = existing.get();
                plan.setTargetQty(rec.targetQty());
                plan.setExcelFileRef(filename);
                planRepository.save(plan);
                c.updated++;
            } else {
                ProductionPlan plan = new ProductionPlan();
                plan.setMachine(machine);
                plan.setProduct(product);
                plan.setPlanDate(rec.planDate());
                plan.setTargetQty(rec.targetQty());
                plan.setSource("excel");
                plan.setExcelFileRef(filename);
                plan.setManpowerDRatio(new BigDecimal("0.50"));
                plan.setManpowerNRatio(new BigDecimal("0.50"));
                if (user != null) plan.setCreatedBy(user);
                planRepository.save(plan);
                c.added++;
            }
            upsertedDates.add(rec.planDate());
        }

        // Trigger setup job scan for affected date range
        if (!upsertedDates.isEmpty()) {
            LocalDate minDate = upsertedDates.first();
            LocalDate maxDate = upsertedDates.last();
            try {
                setupJobService.scanAndCreateSetupJobs(minDate, maxDate);
            } catch (Exception e) {
                logger.warn("Setup job scan failed: {}", e.getMessage());
                c.warnings.add("Setup job scan failed: " + e.getMessage());
            }
        }
        return c;
    }

    // ── Import log ────────────────────────────────────────────────────────────

    private void writeLog(String filename, String factoryCode, Counts c, String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return;
            ImportLog log = new ImportLog();
            log.setFilename(filename);
            log.setFactoryCode(factoryCode);
            log.setImportedBy(user);
            log.setRowsAdded(c.added);
            log.setRowsUpdated(c.updated);
            log.setRowsSkippedPast(c.skippedPast);
            log.setRowsSkippedStarted(c.skippedStarted);
            if (!c.warnings.isEmpty()) {
                log.setErrorsJson(c.warnings.stream()
                        .map(w -> "\"" + w.replace("\"", "\\\"") + "\"")
                        .collect(Collectors.joining(",", "[", "]")));
            }
            importLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to write import log: {}", e.getMessage());
        }
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private boolean isDateCell(Cell cell) {
        if (cell == null) return false;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) return true;
            double v = cell.getNumericCellValue();
            return v > 40000 && v < 55000;
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                int d = Integer.parseInt(cell.getStringCellValue().trim());
                return d >= 1 && d <= 31;
            } catch (NumberFormatException ignored) { /* not a day number */ }
        }
        return false;
    }

    private LocalDate parseDateCell(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            double v = cell.getNumericCellValue();
            if (v > 40000 && v < 55000) {
                return LocalDate.ofEpochDay((long) v - 25569);
            }
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            try {
                int day = Integer.parseInt(s);
                if (day >= 1 && day <= 31) return LocalDate.now().withDayOfMonth(day);
            } catch (NumberFormatException ignored) { /* not a day number */ }
        }
        return null;
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.ERROR) return null;
        if (cell.getCellType() == CellType.BLANK)  return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            return (v > 0) ? (int) v : null;
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                int v = Integer.parseInt(cell.getStringCellValue().trim());
                return v > 0 ? v : null;
            } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}
