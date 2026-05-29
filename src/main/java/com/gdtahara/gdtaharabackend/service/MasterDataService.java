// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/MasterDataService.java
// (ฉบับสมบูรณ์ - เพิ่มฟังก์ชัน Material)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Material; // **[ใหม่]** Import Material
import com.gdtahara.gdtaharabackend.model.NgType;
import com.gdtahara.gdtaharabackend.model.ParameterChecklist;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.repository.MachineRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialRepository; // **[ใหม่]** Import MaterialRepository
import com.gdtahara.gdtaharabackend.repository.NgTypeRepository;
import com.gdtahara.gdtaharabackend.repository.ParameterChecklistRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MasterDataService {

    private static final Logger logger = LoggerFactory.getLogger(MasterDataService.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private MachineRepository machineRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private ParameterChecklistRepository parameterChecklistRepository;
    @Autowired private MaterialRepository materialRepository; // **[ใหม่]**

    // Product methods
    public List<Product> getAllProducts() { return productRepository.findAll(); }
    public Product saveProduct(Product product) { return productRepository.save(product); }
    public void deleteProduct(Long id) { productRepository.deleteById(id); }

    // Machine methods
    public List<Machine> getAllMachines() { return machineRepository.findAll(); }
    public Machine saveMachine(Machine machine) { return machineRepository.save(machine); }
    public void deleteMachine(Long id) { machineRepository.deleteById(id); }

    // NG Type methods
    public List<NgType> getAllNgTypes() { return ngTypeRepository.findAll(); }
    public NgType saveNgType(NgType ngType) { return ngTypeRepository.save(ngType); }
    public void deleteNgType(Long id) { ngTypeRepository.deleteById(id); }
    
    // Parameter Checklist methods
    public List<ParameterChecklist> getAllParameterChecklists() { return parameterChecklistRepository.findAll(); }
    public ParameterChecklist saveParameterChecklist(ParameterChecklist item) { return parameterChecklistRepository.save(item); }
    public void deleteParameterChecklist(Long id) { parameterChecklistRepository.deleteById(id); }

    // Material methods [ใหม่]
    public List<Material> getAllMaterials() {
        List<Material> materials = materialRepository.findAll();
        logger.debug("getAllMaterials() found {} materials", materials.size());
        return materials;
    }
    public Material saveMaterial(Material material) { return materialRepository.save(material); }
    public void deleteMaterial(Long id) { materialRepository.deleteById(id); }
    
}