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
        this.templateRepository  = templateRepository;
        this.machineRepository   = machineRepository;
        this.productRepository   = productRepository;
        this.planRepository      = planRepository;
        this.reportRepository    = reportRepository;
        this.importLogRepository = importLogRepository;
        this.userRepository      = userRepository;
        this.setupJobService     = setupJobService;
    }

    // ── Internal types ────────────────────────────────────────────────────────

    private record SheetConfig(
            XSSFSheet sheet,
            String    factoryCode,
            int machineColIdx,
            int productColIdx,
            int dateHeaderRowIdx,
            int dataStartRowIdx,
            Set<String> skipKeywords) {}

    private static class WoPlan {
        String machineCode, productCode, woNumber;
        LocalDate startDate, endDate;
        int totalQty;
        final TreeMap<LocalDate, Integer> dailyQty = new TreeMap<>();

        WoPlan(String mc, String pc, String wo, LocalDate s, LocalDate e, int t) {
            machineCode = mc;
            productCode = pc;
            woNumber    = wo;
            startDate   = s;
            endDate     = e;
            totalQty    = t;
        }
    }

    private static class WoCounts {
        int wosCreated, wosUpdated, plansCreated, plansSkipped, setupJobsCreated;
        final List<String> warnings = new ArrayList<>();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public ImportResult importPlanWorkbook(MultipartFile file,
                                           String factoryCodeOverride,
                                           String username) {
        return importPlanWorkbook(file, factoryCodeOverride, username, false);
    }

    public ImportResult importPlanWorkbook(MultipartFile file,
                                           String factoryCodeOverride,
                                           String username,
                                           boolean dryRun) {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "upload.xlsx";
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            SheetConfig config = detectConfig(wb, filename, factoryCodeOverride);

            // 1. Parse rows → WoPlan list
            List<WoPlan> plans = parseIntoWoPlans(config, filename);

            // 2. Determine YYMM from first date found in header
            String yymm = extractYYMM(config);

            // 3. Assign WO numbers to plans that don't have one from the file
            assignWoNumbers(plans, yymm);

            // 4. Apply policy (or preview for dryRun)
            WoCounts c;
            if (dryRun) {
                c = new WoCounts();
                c.wosCreated = plans.size();
            } else {
                User user = userRepository.findByUsername(username).orElse(null);
                c = applyWoPolicy(plans, user, filename);
            }

            if (!dryRun) {
                writeLog(filename, config.factoryCode(), c, username);
            }

            return buildResult(filename, config, c, dryRun);
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
                tpl.getMachineColIndex() - 1,
                tpl.getProductColIndex() - 1,
                tpl.getDateHeaderRow()  - 1,
                tpl.getDataStartRow()   - 1,
                keywords);
    }

    private SheetConfig detectByStructure(XSSFWorkbook wb, String factoryCode) {
        XSSFSheet bestSheet = null;
        int bestDateRow   = -1;
        int maxDateCells  = 0;

        for (int si = 0; si < wb.getNumberOfSheets(); si++) {
            XSSFSheet sheet = wb.getSheetAt(si);
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

    // ── WO-centric parser ─────────────────────────────────────────────────────

    /**
     * Parse the sheet into a list of WoPlan objects.
     * Each consecutive run of non-zero quantity days for the same machine+product becomes one WoPlan.
     */
    List<WoPlan> parseIntoWoPlans(SheetConfig cfg, String filename) {
        List<WoPlan> out = new ArrayList<>();
        XSSFSheet sheet = cfg.sheet();

        // Build dateByCol: use first occurrence of each date (deduplicate right-block duplicate dates)
        Map<LocalDate, Integer> firstColByDate = new LinkedHashMap<>();
        Row hdr = sheet.getRow(cfg.dateHeaderRowIdx());
        if (hdr != null) {
            for (Cell cell : hdr) {
                LocalDate d = parseDateCell(cell);
                if (d != null) firstColByDate.putIfAbsent(d, cell.getColumnIndex());
            }
        }
        if (firstColByDate.isEmpty()) {
            logger.warn("No date columns found in {}", filename);
            return out;
        }

        // Invert: colIdx → date
        Map<Integer, LocalDate> dateByCol = new LinkedHashMap<>();
        firstColByDate.forEach((date, col) -> dateByCol.put(col, date));

        // Sorted list of dates
        List<LocalDate> orderedDates = firstColByDate.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        boolean combinedCol = cfg.machineColIdx() == cfg.productColIdx();
        String currentMachine = null;

        for (int ri = cfg.dataStartRowIdx(); ri <= sheet.getLastRowNum(); ri++) {
            Row row = sheet.getRow(ri);
            if (row == null) continue;

            String mc, prod;
            if (combinedCol) {
                String cellValue = getStringValue(row.getCell(cfg.machineColIdx()));
                if (cellValue != null && !cellValue.isBlank()) {
                    Matcher split = MACHINE_PRODUCT_PATTERN.matcher(cellValue.trim());
                    if (split.find()) {
                        currentMachine = split.group(1);
                        prod = split.group(2);
                    } else {
                        prod = null;
                    }
                } else {
                    prod = null;
                }
                if (prod == null || cfg.skipKeywords().stream()
                        .anyMatch(kw -> kw.equalsIgnoreCase(prod))) {
                    continue;
                }
                if (currentMachine == null) continue;
                mc = currentMachine;

                // For TAHARA-style combined columns col B has "MACHINE PRODUCT" on EVERY
                // sub-row (Plan/Actual/Diff).  The sub-row type sits in col C (machineColIdx+1).
                // Skip any sub-row whose adjacent cell matches a skip keyword.
                String subRowType = getStringValue(row.getCell(cfg.machineColIdx() + 1));
                if (subRowType != null && !subRowType.isBlank()
                        && cfg.skipKeywords().stream()
                                .anyMatch(kw -> kw.equalsIgnoreCase(subRowType.trim()))) {
                    continue;
                }
            } else {
                String mcRaw = getStringValue(row.getCell(cfg.machineColIdx()));
                if (mcRaw != null && !mcRaw.isBlank()) currentMachine = mcRaw.trim();
                if (currentMachine == null) continue;
                mc = currentMachine;

                prod = getStringValue(row.getCell(cfg.productColIdx()));
                if (prod == null || prod.isBlank()) continue;
                if (cfg.skipKeywords().stream().anyMatch(kw -> kw.equalsIgnoreCase(prod))) continue;
            }

            // Read WO number from col M (POI index 12) — null if blank
            String woNumFromFile = getStringValue(row.getCell(12));
            if (woNumFromFile != null && woNumFromFile.isBlank()) woNumFromFile = null;

            // Build daily qty map for this row
            Map<LocalDate, Integer> rowQty = new LinkedHashMap<>();
            for (Map.Entry<Integer, LocalDate> entry : dateByCol.entrySet()) {
                Integer qty = getIntValue(row.getCell(entry.getKey()));
                if (qty != null && qty > 0) {
                    rowQty.put(entry.getValue(), qty);
                }
            }

            // Find consecutive runs and emit one WoPlan per run
            LocalDate runStart = null;
            LocalDate runEnd   = null;
            int runTotal = 0;
            TreeMap<LocalDate, Integer> runDailyQty = new TreeMap<>();

            final String finalMc   = mc;
            final String finalProd = prod;
            final String finalWo   = woNumFromFile;

            for (LocalDate date : orderedDates) {
                Integer qty = rowQty.get(date);
                if (qty != null && qty > 0) {
                    if (runStart == null) runStart = date;
                    runEnd = date;
                    runTotal += qty;
                    runDailyQty.put(date, qty);
                } else {
                    if (runStart != null) {
                        // Flush the current run
                        WoPlan wp = new WoPlan(finalMc, finalProd, finalWo, runStart, runEnd, runTotal);
                        wp.dailyQty.putAll(runDailyQty);
                        out.add(wp);
                        runStart = null;
                        runEnd   = null;
                        runTotal = 0;
                        runDailyQty = new TreeMap<>();
                    }
                }
            }
            // Flush last run
            if (runStart != null) {
                WoPlan wp = new WoPlan(finalMc, finalProd, finalWo, runStart, runEnd, runTotal);
                wp.dailyQty.putAll(runDailyQty);
                out.add(wp);
            }
        }

        logger.info("Parsed {} WO plans from sheet '{}'", out.size(), sheet.getSheetName());
        return out;
    }

    /**
     * Scan date header row for first real date cell; extract year+month → "YYmm" format.
     * E.g. 2026-06 → "2606". Fallback to current month.
     */
    String extractYYMM(SheetConfig cfg) {
        Row hdr = cfg.sheet().getRow(cfg.dateHeaderRowIdx());
        if (hdr != null) {
            for (Cell cell : hdr) {
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getDateCellValue()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return String.format("%02d%02d", d.getYear() % 100, d.getMonthValue());
                }
            }
        }
        LocalDate now = LocalDate.now();
        return String.format("%02d%02d", now.getYear() % 100, now.getMonthValue());
    }

    /**
     * Assign WO numbers to plans that do not have one from the file.
     * Format: {MACHINE_CODE}_{YYMM}_{NN} where NN is zero-padded 2-digit sequence.
     */
    void assignWoNumbers(List<WoPlan> plans, String yymm) {
        // Group by machine code
        Map<String, List<WoPlan>> byMachine = new LinkedHashMap<>();
        for (WoPlan p : plans) {
            byMachine.computeIfAbsent(p.machineCode, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<WoPlan>> entry : byMachine.entrySet()) {
            String machine = entry.getKey();
            List<WoPlan> machinePlans = entry.getValue();

            // Plans without WO number from file
            List<WoPlan> toAssign = machinePlans.stream()
                    .filter(p -> p.woNumber == null)
                    .sorted(Comparator.comparing(p -> p.startDate))
                    .collect(Collectors.toList());

            if (toAssign.isEmpty()) continue;

            // Load existing order numbers from DB for this machine+yymm
            String pattern = machine + "_" + yymm + "_%";
            List<String> existing = reportRepository.findOrderNumbersLike(pattern);

            // Find max existing sequence number
            int maxNN = 0;
            String prefix = machine + "_" + yymm + "_";
            for (String orderNum : existing) {
                if (orderNum != null && orderNum.startsWith(prefix)) {
                    String suffix = orderNum.substring(prefix.length());
                    try {
                        int nn = Integer.parseInt(suffix);
                        if (nn > maxNN) maxNN = nn;
                    } catch (NumberFormatException ignored) {
                        // Not a numeric suffix — skip
                    }
                }
            }

            // Assign sequential numbers starting from maxNN + 1
            int next = maxNN + 1;
            for (WoPlan p : toAssign) {
                p.woNumber = String.format("%s_%s_%02d", machine, yymm, next++);
            }
        }
    }

    /**
     * Apply WO upsert policy: create or update WOs and daily plans.
     */
    WoCounts applyWoPolicy(List<WoPlan> plans, User user, String filename) {
        WoCounts c = new WoCounts();
        TreeSet<LocalDate> affectedDates = new TreeSet<>();

        for (WoPlan plan : plans) {
            // Resolve machine
            Optional<Machine> machineOpt = machineRepository.findByMachineCode(plan.machineCode);
            if (machineOpt.isEmpty()) {
                c.warnings.add("Machine not found: " + plan.machineCode);
                continue;
            }
            Machine machine = machineOpt.get();

            // Resolve product
            Optional<Product> productOpt = productRepository.findByProductCode(plan.productCode);
            if (productOpt.isEmpty()) {
                c.warnings.add(plan.machineCode + ": product code '" + plan.productCode + "' not found in DB");
                continue;
            }
            Product product = productOpt.get();

            // Upsert WO (key: machineId + productId + startDate)
            Optional<ProductionReport> existingWo = reportRepository
                    .findByMachineIdAndProductIdAndStartDate(machine.getId(), product.getId(), plan.startDate);

            ProductionReport wo;
            if (existingWo.isPresent()) {
                wo = existingWo.get();
                wo.setTargetQty(plan.totalQty);
                wo.setEndDate(plan.endDate);
                reportRepository.save(wo);
                c.wosUpdated++;
            } else {
                wo = new ProductionReport();
                wo.setOrderNumber(plan.woNumber);
                wo.setMachine(machine);
                wo.setProduct(product);
                wo.setStartDate(plan.startDate);
                wo.setEndDate(plan.endDate);
                wo.setTargetQty(plan.totalQty);
                wo.setStatus("PLANNED");
                wo.setPc(user);
                // Generate parentLotNumber to satisfy unique constraint
                String lot = machine.getMachineCode()
                        + "-" + plan.startDate.toString().replace("-", "")
                        + "-" + plan.woNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                if (lot.length() > 95) lot = lot.substring(0, 95);
                wo.setParentLotNumber(lot);
                reportRepository.save(wo);
                c.wosCreated++;
            }

            // Upsert daily plans
            String sourceWo = "WO:" + plan.woNumber;
            if (sourceWo.length() > 20) sourceWo = sourceWo.substring(0, 20);

            for (Map.Entry<LocalDate, Integer> entry : plan.dailyQty.entrySet()) {
                LocalDate date = entry.getKey();
                int qty = entry.getValue();
                if (qty <= 0) continue;

                Optional<ProductionPlan> existingPlan = planRepository
                        .findByMachineIdAndPlanDateAndProductId(machine.getId(), date, product.getId());

                if (existingPlan.isPresent()) {
                    String existingSource = existingPlan.get().getSource();
                    if (existingSource != null && existingSource.equalsIgnoreCase("excel")) {
                        c.plansSkipped++;
                        continue;
                    }
                    existingPlan.get().setTargetQty(qty);
                    existingPlan.get().setSource(sourceWo);
                    planRepository.save(existingPlan.get());
                    c.plansCreated++;
                } else {
                    ProductionPlan p = new ProductionPlan();
                    p.setMachine(machine);
                    p.setProduct(product);
                    p.setPlanDate(date);
                    p.setTargetQty(qty);
                    p.setSource(sourceWo);
                    p.setStatus("draft");
                    p.setManpowerDRatio(new BigDecimal("0.50"));
                    p.setManpowerNRatio(new BigDecimal("0.50"));
                    p.setCreatedBy(user);
                    p.setExcelFileRef(filename);
                    planRepository.save(p);
                    c.plansCreated++;
                }
                affectedDates.add(date);
            }
        }

        // Trigger setup job scan for affected date range
        if (!affectedDates.isEmpty()) {
            try {
                List<MachineSetupJob> jobs = setupJobService.scanAndCreateSetupJobs(
                        affectedDates.first().minusDays(1),
                        affectedDates.last().plusDays(1));
                c.setupJobsCreated = jobs.size();
            } catch (Exception e) {
                logger.warn("Setup job scan failed: {}", e.getMessage());
                c.warnings.add("Setup job scan failed: " + e.getMessage());
            }
        }

        return c;
    }

    // ── Import log ────────────────────────────────────────────────────────────

    private void writeLog(String filename, String factoryCode, WoCounts c, String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return;
            ImportLog log = new ImportLog();
            log.setFilename(filename);
            log.setFactoryCode(factoryCode);
            log.setImportedBy(user);
            log.setRowsAdded(c.wosCreated);
            log.setRowsUpdated(c.wosUpdated);
            log.setRowsSkippedPast(0);
            log.setRowsSkippedStarted(0);
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

    private ImportResult buildResult(String filename, SheetConfig config, WoCounts c, boolean dryRun) {
        String status = dryRun ? "DRY_RUN" : "SUCCESS";
        String message = dryRun
                ? String.format("DRY RUN: %d WOs would be created", c.wosCreated)
                : String.format("+%d WOs  ~%d updated  %d plans  %d setup jobs",
                        c.wosCreated, c.wosUpdated, c.plansCreated, c.setupJobsCreated);
        return new ImportResult(
                filename,
                config.factoryCode(),
                config.sheet().getSheetName(),
                c.wosCreated,   // rowsAdded = wosCreated (backward compat)
                c.wosUpdated,   // rowsUpdated = wosUpdated
                0,              // rowsSkippedPast
                0,              // rowsSkippedStarted
                c.wosCreated,
                c.wosUpdated,
                c.plansCreated,
                c.setupJobsCreated,
                status,
                message,
                List.copyOf(c.warnings)
        );
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
        // Resolve formula cells to their cached result type before branching.
        CellType effective = (cell.getCellType() == CellType.FORMULA)
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (effective == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            double v = cell.getNumericCellValue();
            if (v > 40000 && v < 55000) {
                return LocalDate.ofEpochDay((long) v - 25569);
            }
        }
        if (effective == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            try {
                int day = Integer.parseInt(s);
                if (day >= 1 && day <= 31) return LocalDate.now().withDayOfMonth(day);
            } catch (NumberFormatException ignored) { /* not a day number */ }
        }
        return null;
    }

    String getStringValue(Cell cell) {
        if (cell == null) return null;
        CellType effective = (cell.getCellType() == CellType.FORMULA)
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (effective) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        // Resolve formula cells to their cached numeric/string result.
        CellType effective = (cell.getCellType() == CellType.FORMULA)
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (effective == CellType.ERROR || effective == CellType.BLANK) return null;
        if (effective == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            return (v > 0) ? (int) v : null;
        }
        if (effective == CellType.STRING) {
            try {
                int v = Integer.parseInt(cell.getStringCellValue().trim());
                return v > 0 ? v : null;
            } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}
