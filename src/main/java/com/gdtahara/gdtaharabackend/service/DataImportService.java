// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/DataImportService.java
// (วางทับไฟล์เดิม)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Material;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataImportService {

    private static final Logger logger = LoggerFactory.getLogger(DataImportService.class);

    @Autowired private MachineRepository machineRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private MaterialRepository materialRepository;


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
                
                logger.debug("Machine data - Code: '{}', Name: '{}'", machineCode, machineName);
                
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
                    // Removed setMachineType since it's not in the database schema
                    machinesToSave.add(machine);
                    processedCodes.add(machineCode);
                    logger.debug("Added machine to save list: Code='{}', Name='{}'", machineCode, machineName);
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
    
 // **[ใหม่]** เพิ่มเมธอดสำหรับนำเข้าข้อมูลวัตถุดิบ
    @Transactional
    public void importMaterials(MultipartFile file) throws IOException, CsvValidationException {
        List<Material> materialsToSave = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // Skip Header
            while ((line = reader.readNext()) != null) {
                if (line.length < 4) continue;
                String materialCode = line[0].trim();
                if (!materialCode.isEmpty() && !processedCodes.contains(materialCode) && materialRepository.findByMaterialCode(materialCode).isEmpty()) {
                    Material material = new Material();
                    material.setMaterialCode(materialCode);
                    material.setMaterialName(line[1].trim());
                    material.setMaterialType(line[2].trim());
                    material.setUnit(line[3].trim());
                    materialsToSave.add(material);
                    processedCodes.add(materialCode);
                }
            }
        }
        if (!materialsToSave.isEmpty()) materialRepository.saveAll(materialsToSave);
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
