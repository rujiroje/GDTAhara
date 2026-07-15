package com.gdtahara.gdtaharabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdtahara.gdtaharabackend.dto.BomImportResultDto;
import com.gdtahara.gdtaharabackend.model.*;
import com.gdtahara.gdtaharabackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BomImportService {

    private final BillOfMaterialsRepository bomRepository;
    private final BomItemRepository bomItemRepository;
    private final BomImportLogRepository importLogRepository;
    private final MaterialRepository materialRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------
    // Inner record to hold one parsed Excel row
    // -------------------------------------------------------
    private record BomRowData(
            String materialGroup,
            String fgCode,
            int itemNumber,
            int altNo,
            String rmCode,
            String rmName,
            String plantCode,
            BigDecimal quantityPer,
            String unit,
            BigDecimal lossPercent,
            int bomStatus,
            String materialType,
            boolean isScrap
    ) {}

    // -------------------------------------------------------
    // Main import method
    // -------------------------------------------------------
    @Transactional
    public BomImportResultDto importFromExcel(MultipartFile file, String username) {
        long startMs = System.currentTimeMillis();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        int rowsAdded = 0, rowsUpdated = 0, rowsSkipped = 0, rowsError = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            String fileType = detectFileType(workbook);
            Sheet sheet = getDataSheet(workbook, fileType);

            // Parse all rows into groups keyed by "fgCode|altNo|plantCode"
            Map<String, List<BomRowData>> groups = new LinkedHashMap<>();
            parseAllRows(sheet, fileType, groups, errors);
            rowsError = errors.size();

            LocalDate today = LocalDate.now();

            for (Map.Entry<String, List<BomRowData>> entry : groups.entrySet()) {
                List<BomRowData> rows = entry.getValue();
                if (rows.isEmpty()) continue;

                BomRowData first = rows.get(0);

                // Close any existing active BOM for this FG/alt/plant
                Optional<BillOfMaterials> existingOpt = bomRepository
                        .findFirstByFgCodeAndAlternativeNumberAndPlantCodeAndEffectiveToIsNull(
                                first.fgCode(), first.altNo(), first.plantCode());

                if (existingOpt.isPresent()) {
                    BillOfMaterials old = existingOpt.get();
                    old.setEffectiveTo(today.minusDays(1));
                    old.setStatus("SUPERSEDED");
                    bomRepository.save(old);
                    rowsUpdated++;
                }

                // Auto-create Product for fgCode if not registered yet
                autoCreateProduct(first.fgCode(), first.materialGroup(),
                        "FG".equals(fileType) ? "FERT" : "HALB", first.plantCode(), first.altNo());

                // Create new BOM header
                BillOfMaterials bom = new BillOfMaterials();
                bom.setFgCode(first.fgCode());
                bom.setAlternativeNumber(first.altNo());
                bom.setPlantCode(first.plantCode());
                bom.setMaterialGroup(first.materialGroup());
                bom.setSapMaterialType("FG".equals(fileType) ? "FERT" : "HALB");
                bom.setEffectiveFrom(today);
                bom.setStatus("ACTIVE");
                bom.setImportedFromFile(file.getOriginalFilename());
                bom.setImportedBy(user);
                bom = bomRepository.save(bom);

                // Create BOM line items
                for (BomRowData row : rows) {
                    autoCreateMaterial(row.rmCode(), row.rmName(), row.materialType(), row.plantCode());

                    BomItem item = new BomItem();
                    item.setBom(bom);
                    item.setItemNumber(row.itemNumber());
                    item.setRmCode(row.rmCode());
                    item.setRmName(row.rmName());
                    item.setQuantityPer(row.quantityPer().abs());
                    item.setUnit(row.unit());
                    item.setLossPercent(row.lossPercent());
                    item.setMaterialType(row.materialType());
                    item.setIsScrap(row.isScrap());
                    bomItemRepository.save(item);
                    rowsAdded++;
                }
            }

            // Persist import audit log
            saveImportLog(file.getOriginalFilename(), fileType, user,
                    rowsAdded, rowsUpdated, rowsSkipped, rowsError, errors,
                    (int) (System.currentTimeMillis() - startMs));

            return new BomImportResultDto(fileType, rowsAdded, rowsUpdated, rowsSkipped, rowsError, errors);

        } catch (Exception e) {
            log.error("BOM import failed", e);
            throw new RuntimeException("BOM import failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // Detect FG vs SEMI from sheet name
    // -------------------------------------------------------
    private String detectFileType(Workbook workbook) {
        String sheetName = workbook.getSheetAt(0).getSheetName().toLowerCase();
        if (sheetName.contains("halb")) return "SEMI";
        if (sheetName.contains("fert") || sheetName.contains("new")) return "FG";
        // Fallback: column count on header row (row index 1)
        Sheet sheet = workbook.getSheetAt(0);
        Row header = sheet.getRow(1);
        int cols = header != null ? header.getLastCellNum() : 0;
        return cols >= 24 ? "FG" : "SEMI";
    }

    private Sheet getDataSheet(Workbook workbook, String fileType) {
        if ("SEMI".equals(fileType)) {
            Sheet s = workbook.getSheet("bomfile-5100-HALB");
            return s != null ? s : workbook.getSheetAt(0);
        }
        Sheet s = workbook.getSheet("fert new");
        return s != null ? s : workbook.getSheetAt(0);
    }

    // -------------------------------------------------------
    // Parse all data rows (skip header rows 1-2 and separators)
    // -------------------------------------------------------
    private void parseAllRows(Sheet sheet, String fileType,
                               Map<String, List<BomRowData>> groups, List<String> errors) {
        // Row 0 = title, Row 1 = headers, data starts at Row 2 (0-indexed)
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String firstCell = getCellString(row, 0).trim();
            // Skip separator lines and truly empty rows
            if (firstCell.startsWith("---") || firstCell.startsWith("===")) continue;
            if (getCellString(row, 2).trim().isEmpty()) continue;

            try {
                BomRowData data = parseRow(row, fileType);
                if (data == null) continue;
                String key = data.fgCode() + "|" + data.altNo() + "|" + data.plantCode();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
            } catch (Exception e) {
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
                log.warn("Skipping row {}: {}", i + 1, e.getMessage());
            }
        }
    }

    // -------------------------------------------------------
    // Parse a single data row
    // Column indices (0-based) per plan section 14.1 (FG file):
    //   0=materialGroup, 2=fgCode, 3=itemNumber, 5=altNo,
    //   6=rmCode, 7=rmName, 8=plant, 10=qty, 11=unit, 12=loss%,
    //   14=bomStatus, 16=scrapFlag, 17=matType
    // SEMI file uses same positions (no significant shift for core columns)
    // -------------------------------------------------------
    private BomRowData parseRow(Row row, String fileType) {
        String fgCode = getCellString(row, 2).trim();
        if (fgCode.isEmpty()) return null;

        String rmCode = getCellString(row, 6).trim();
        if (rmCode.isEmpty()) return null;

        String materialGroup = getCellString(row, 0).trim();
        int itemNumber        = getCellInt(row, 3);
        int altNo             = getCellInt(row, 5);
        if (altNo <= 0) altNo = 1;
        String rmName         = getCellString(row, 7).trim();
        String plantCode      = getCellString(row, 8).trim();
        if (plantCode.isEmpty()) plantCode = "5100";

        BigDecimal qty        = getCellDecimal(row, 10);
        String unit           = getCellString(row, 11).trim();
        if (unit.isEmpty()) unit = "KG";

        BigDecimal lossPercent = getCellDecimal(row, 12);
        int bomStatus          = getCellInt(row, 14);
        if (bomStatus <= 0) bomStatus = 1;

        String scrapFlag  = getCellString(row, 16).trim();
        String matType    = getCellString(row, 17).trim().toUpperCase();
        if (matType.isEmpty()) matType = "ROH";

        boolean isScrap = "SCAP".equals(matType)
                || "X".equalsIgnoreCase(scrapFlag)
                || (qty != null && qty.compareTo(BigDecimal.ZERO) < 0);

        if (qty == null) qty = BigDecimal.ZERO;

        return new BomRowData(materialGroup, fgCode, itemNumber, altNo,
                rmCode, rmName, plantCode, qty, unit, lossPercent,
                bomStatus, matType, isScrap);
    }

    // -------------------------------------------------------
    // Auto-create Material record if rmCode is not in DB yet
    // -------------------------------------------------------
    private void autoCreateMaterial(String rmCode, String rmName, String matType, String plantCode) {
        if (rmCode == null || rmCode.isBlank()) return;
        boolean exists = materialRepository.findByMaterialCode(rmCode).isPresent();
        if (!exists) {
            Material m = new Material();
            m.setMaterialCode(rmCode);
            m.setMaterialName(rmName != null ? rmName : rmCode);
            m.setMaterialType(matType);
            m.setPlantCode(plantCode);
            m.setUnit("KG");
            materialRepository.save(m);
            log.info("Auto-created material: {}", rmCode);
        }
    }

    // -------------------------------------------------------
    // Auto-create Product record if fgCode is not in DB yet
    // productName ใช้ fgCode เป็นค่าเริ่มต้น (Admin แก้ได้ทีหลัง)
    // -------------------------------------------------------
    private void autoCreateProduct(String fgCode, String materialGroup,
                                   String sapMaterialType, String plantCode, int altNo) {
        if (fgCode == null || fgCode.isBlank()) return;
        if (productRepository.findByProductCode(fgCode).isPresent()) return;

        Product p = new Product();
        p.setProductCode(fgCode);
        p.setProductName(fgCode);   // placeholder — ให้ admin กรอกชื่อจริงในภายหลัง
        p.setMaterialGroup(materialGroup);
        p.setSapMaterialType(sapMaterialType);
        p.setPlantCode(plantCode);
        p.setAlternativeNumber(altNo);
        productRepository.save(p);
        log.info("Auto-created product: {}", fgCode);
    }

    // -------------------------------------------------------
    // Sync: สร้าง Product สำหรับ fgCode ที่มีใน BOM แต่ยังไม่มีใน products
    // ใช้ metadata จาก BOM ล่าสุดของแต่ละ fgCode
    // -------------------------------------------------------
    @Transactional
    public Map<String, Integer> syncProductsFromAllBoms() {
        List<String> fgCodes = bomRepository.findDistinctFgCodes();
        int created = 0, skipped = 0;
        for (String fgCode : fgCodes) {
            if (productRepository.findByProductCode(fgCode).isPresent()) {
                skipped++;
                continue;
            }
            bomRepository.findFirstByFgCodeOrderByEffectiveFromDesc(fgCode).ifPresent(bom -> {
                autoCreateProduct(fgCode,
                        bom.getMaterialGroup(),
                        bom.getSapMaterialType(),
                        bom.getPlantCode(),
                        bom.getAlternativeNumber() != null ? bom.getAlternativeNumber() : 1);
            });
            created++;
        }
        log.info("syncProductsFromAllBoms: created={}, skipped={}", created, skipped);
        return Map.of("created", created, "skipped", skipped, "total", fgCodes.size());
    }

    // -------------------------------------------------------
    // Save import audit log
    // -------------------------------------------------------
    private void saveImportLog(String filename, String fileType, User user,
                                int added, int updated, int skipped, int error,
                                List<String> errors, int durationMs) {
        BomImportLog logEntry = new BomImportLog();
        logEntry.setFilename(filename != null ? filename : "unknown");
        logEntry.setFileType(fileType);
        logEntry.setImportedBy(user);
        logEntry.setRowsAdded(added);
        logEntry.setRowsUpdated(updated);
        logEntry.setRowsSkipped(skipped);
        logEntry.setRowsError(error);
        logEntry.setDurationMs(durationMs);
        if (!errors.isEmpty()) {
            try {
                logEntry.setErrorsJson(objectMapper.writeValueAsString(errors));
            } catch (Exception ignored) {}
        }
        importLogRepository.save(logEntry);
    }

    // -------------------------------------------------------
    // Cell helpers
    // -------------------------------------------------------
    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private int getCellInt(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(getCellString(row, col).trim()); } catch (Exception e) { return 0; }
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = getCellString(row, col).trim().replace(",", "");
        if (s.isEmpty()) return null;
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }

    // -------------------------------------------------------
    // Query: last 20 import logs (for Admin UI)
    // -------------------------------------------------------
    public List<BomImportLog> getRecentImportLogs() {
        return importLogRepository.findTop20ByOrderByImportedAtDesc();
    }
}
