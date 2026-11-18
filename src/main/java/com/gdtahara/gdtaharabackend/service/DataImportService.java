// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/DataImportService.java
// (วางทับไฟล์เดิม)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Material;
import com.gdtahara.gdtaharabackend.model.MoldMaster;
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataImportService {

    private static final Logger logger = LoggerFactory.getLogger(DataImportService.class);

    @Autowired private MachineRepository machineRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MoldMasterRepository moldMasterRepository;


    @Transactional
    public void importMachines(MultipartFile file) throws IOException, CsvValidationException {
        logger.info("Starting machine import from file: {}", file.getOriginalFilename());
        List<Machine> machinesToSave = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>(); // **[ใหม่]** ใช้ Set เพื่อตรวจสอบข้อมูลซ้ำในไฟล์

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // ข้าม Header
            int lineNumber = 1; // Track line number for debugging
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                logger.debug("Processing line {}: {}", lineNumber, String.join(",", line));
                
                if (line.length < 2) {
                    logger.warn("Line {} has insufficient columns: {}", lineNumber, line.length);
                    continue;
                }
                
                String machineCode = line[0].trim();
                String machineName = line.length > 1 ? line[1].trim() : "";
                String machineType = line.length > 2 ? line[2].trim() : "";

                logger.debug("Machine data - Code: '{}', Name: '{}', Type: '{}'", machineCode, machineName, machineType);

                // **[แก้ไข]** ตรวจสอบว่าเคยเจอ Code นี้ในไฟล์หรือยัง และมีใน DB หรือยัง
                if (!machineCode.isEmpty() && !processedCodes.contains(machineCode)) {
                    // Check if already exists in database
                    boolean existsInDb = machineRepository.findByMachineCode(machineCode).isPresent();
                    if (existsInDb) {
                        logger.info("Machine code '{}' already exists in database, skipping", machineCode);
                        continue;
                    }

                    Machine machine = new Machine();
                    machine.setMachineCode(machineCode);
                    machine.setMachineName(machineName);
                    if (!machineType.isEmpty()) {
                        machine.setMachineType(machineType.toUpperCase());
                    }
                    machinesToSave.add(machine);
                    processedCodes.add(machineCode);
                    logger.debug("Added machine to save list: Code='{}', Name='{}', Type='{}'", machineCode, machineName, machineType);
                } else {
                    logger.warn("Skipping machine - Code: '{}' (empty={}, duplicate={})", 
                               machineCode, machineCode.isEmpty(), processedCodes.contains(machineCode));
                }
            }
            logger.info("Processed {} lines, found {} machines to save", lineNumber-1, machinesToSave.size());
        }
        
        if (!machinesToSave.isEmpty()) {
            logger.info("Saving {} machines to database", machinesToSave.size());
            machineRepository.saveAll(machinesToSave);
            logger.info("Successfully saved {} machines", machinesToSave.size());
        } else {
            logger.warn("No new machines to save");
        }
    }

    @Transactional
    public void importNgTypes(MultipartFile file) throws IOException, CsvValidationException {
        List<NgType> ngTypesToSave = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>(); // **[ใหม่]**

         try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // ข้าม Header
            while ((line = reader.readNext()) != null) {
                if (line.length < 3) continue;
                String ngCode = line[0].trim();
                // **[แก้ไข]**
                if (!ngCode.isEmpty() && !processedCodes.contains(ngCode) && ngTypeRepository.findByNgCode(ngCode).isEmpty()) {
                    NgType ngType = new NgType();
                    ngType.setNgCode(ngCode);
                    ngType.setNgDescriptionTh(line[1].trim());
                    ngType.setNgType(line[2].trim());
                    ngTypesToSave.add(ngType);
                    processedCodes.add(ngCode);
                }
            }
        }
        if (!ngTypesToSave.isEmpty()) ngTypeRepository.saveAll(ngTypesToSave);
    }

    @Transactional
    public void importProducts(MultipartFile file) throws IOException, CsvValidationException {
        List<Product> productsToSave = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>(); // **[ใหม่]**

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // ข้าม Header
            while ((line = reader.readNext()) != null) {
                if (line.length < 21) continue; 
                
                // **[แก้ไข]** ใช้คอลัมน์ที่ 0 เป็น Product Code
                String productCode = line[0].trim(); 

                if (!productCode.isEmpty() && !processedCodes.contains(productCode) && productRepository.findByProductCode(productCode).isEmpty()) {
                    Product product = new Product();
                    product.setProductCode(productCode);
                    product.setProductName(line[1].trim()); // คอลัมน์ที่ 1 คือ Product Name

                    // **[แก้ไข]** อัปเดตลำดับคอลัมน์ให้ตรงกับไฟล์ CSV ที่ให้มา
                    product.setStdWeight(safeParseBigDecimal(line[3]));     // TotalWeight
                    product.setBWeight(safeParseBigDecimal(line[5]));       // BWeight
                    product.setBWeightVar(line[6].trim());                  // BWeightVar
                    product.setCycleTime(safeParseBigDecimal(line[7]));     // Cycle time
                    product.setCavity(safeParseInteger(line[8]));           // Cavity
                    product.setPdiMain(safeParseBigDecimal(line[9]));       // PDI Main
                    product.setPdiVirgin(safeParseBigDecimal(line[10]));    // PDI Virgin
                    product.setPdiEvoh(safeParseBigDecimal(line[11]));      // PDI EVOH
                    product.setPdiAdm(safeParseBigDecimal(line[12]));       // PDI ADM
                    product.setQtyPerBox(safeParseInteger(line[18]));       // QtyperBox
                    product.setQtyPerBag(safeParseInteger(line[19]));       // QtyperBag
                    product.setQtyPerPallet(safeParseInteger(line[20]));    // QtyperPallet
                    
                    productsToSave.add(product);
                    processedCodes.add(productCode);
                }
            }
        }
        if (!productsToSave.isEmpty()) productRepository.saveAll(productsToSave);
    }
    
    @Transactional
    public void importMaterials(MultipartFile file) throws IOException, CsvValidationException {
        List<Material> toSave = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // skip header
            while ((line = reader.readNext()) != null) {
                if (line.length < 4) continue;
                String materialCode = line[0].trim();
                if (materialCode.isEmpty() || processedCodes.contains(materialCode)) continue;
                processedCodes.add(materialCode);
                // UPSERT: update existing row if code already exists (fixes corrupted Thai names)
                Material material = materialRepository.findByMaterialCode(materialCode)
                        .orElse(new Material());
                material.setMaterialCode(materialCode);
                material.setMaterialName(line[1].trim());
                material.setMaterialType(line[2].trim());
                material.setUnit(line[3].trim());
                toSave.add(material);
            }
        }
        if (!toSave.isEmpty()) materialRepository.saveAll(toSave);
    }
    
    // ── Mold Import ──────────────────────────────────────────────────────────────

    /**
     * CSV columns (row 1 = header, skipped):
     * moldCode, moldName, moldNameEn, machineType, productCode, moldType,
     * standardCavityCount, standardCavityNo, workCavityCount, workCavityNo,
     * isActive, description
     *
     * Returns map with keys: imported, updated, errors, message
     */
    @Transactional
    public Map<String, Object> importMolds(MultipartFile file) throws IOException, CsvValidationException {
        Map<String, Long> productIdByCode = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductCode, Product::getId, (a, b) -> a));

        int created = 0, updated = 0;
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readNext(); // skip header
            String[] line;
            int row = 1;
            while ((line = reader.readNext()) != null) {
                row++;
                String moldCode = col(line, 0);
                if (moldCode.isEmpty()) continue;
                try {
                    String moldName      = col(line, 1);
                    String moldNameEn    = col(line, 2);
                    String machineType   = col(line, 3);
                    String productCode   = col(line, 4);
                    String moldType      = col(line, 5);
                    Integer stdCount     = safeParseInteger(col(line, 6));
                    String stdNo         = col(line, 7);
                    Integer workCount    = safeParseInteger(col(line, 8));
                    String workNo        = col(line, 9);
                    boolean isActive     = parseBool(col(line, 10));
                    String description   = col(line, 11);

                    Long productId = null;
                    if (!productCode.isEmpty()) {
                        productId = productIdByCode.get(productCode);
                        if (productId == null) {
                            errors.add("Row " + row + ": productCode '" + productCode + "' ไม่พบ — product_id จะเป็น null");
                        }
                    }

                    MoldMaster m = moldMasterRepository.findByMoldCode(moldCode)
                            .orElse(new MoldMaster());
                    boolean isNew = m.getId() == null;

                    m.setMoldCode(moldCode);
                    m.setMoldName(!moldName.isEmpty() ? moldName : (isNew ? moldCode : m.getMoldName()));
                    m.setMoldNameEn(!moldNameEn.isEmpty() ? moldNameEn : null);
                    m.setMachineType(!machineType.isEmpty() ? machineType : m.getMachineType());
                    m.setProductId(productId);
                    m.setMoldType(!moldType.isEmpty() ? moldType : null);
                    m.setStandardCavityCount(stdCount);
                    m.setStandardCavityNo(!stdNo.isEmpty() ? stdNo : null);
                    m.setWorkCavityCount(workCount);
                    m.setWorkCavityNo(!workNo.isEmpty() ? workNo : null);
                    m.setIsActive(isActive);
                    m.setDescription(!description.isEmpty() ? description : null);

                    // Sync legacy cavityCount/cavityNumbers (used in TC mold dropdown)
                    m.setCavityCount(workCount != null ? workCount :
                                     stdCount  != null ? stdCount  :
                                     (m.getCavityCount() != null ? m.getCavityCount() : 1));
                    String legacyNo = !workNo.isEmpty() ? workNo : (!stdNo.isEmpty() ? stdNo : null);
                    if (legacyNo != null) m.setCavityNumbers(legacyNo);
                    else if (isNew) m.setCavityNumbers("1");

                    if (isNew) m.setCreatedBy("import");

                    moldMasterRepository.save(m);
                    if (isNew) created++; else updated++;

                } catch (Exception e) {
                    errors.add("Row " + row + " (" + moldCode + "): " + e.getMessage());
                    logger.warn("Mold import error at row {}: {}", row, e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", created);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("message", String.format("นำเข้า %d รายการใหม่, อัปเดต %d รายการ%s",
                created, updated,
                errors.isEmpty() ? "" : String.format(", พบ %d ข้อผิดพลาด (ดูรายละเอียดใน errors)", errors.size())));
        return result;
    }

    private String col(String[] row, int idx) {
        return (idx < row.length && row[idx] != null) ? row[idx].trim() : "";
    }

    private boolean parseBool(String v) {
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v);
    }

    private BigDecimal safeParseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("-")) return null;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Integer safeParseInteger(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("-")) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return null; }
    }
}
