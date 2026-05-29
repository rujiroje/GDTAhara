// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/CmOperatorService.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.StockOutRequestDto;
import com.gdtahara.gdtaharabackend.model.Material;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.MaterialRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialStockTransactionRepository;
import com.gdtahara.gdtaharabackend.repository.ProductionReportRepository;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialUsageLogRepository;
import com.gdtahara.gdtaharabackend.model.MaterialUsageLog;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Transactional
public class CmOperatorService {

    @Autowired private MaterialStockTransactionRepository transactionRepository;
    @Autowired private ProductionReportRepository reportRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MaterialUsageLogRepository materialUsageLogRepository;
    @Autowired private AuditLogService auditLogService;

    public MaterialStockTransaction recordStockOut(StockOutRequestDto request, String username) {
        // 1. ตรวจสอบข้อมูล
        ProductionReport report = reportRepository.findById(request.getProductionReportId())
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบใบสั่งผลิต"));
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบวัตถุดิบ"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบผู้ใช้งาน"));

        // 2. ตรวจสอบสต็อกคงเหลือ
        BigDecimal currentStock = transactionRepository.getStockBalanceByMaterialId(request.getMaterialId());
        if (currentStock == null || currentStock.compareTo(request.getQuantity()) < 0) {
            throw new IllegalStateException("วัตถุดิบ '" + material.getMaterialName() + "' มีสต็อกไม่เพียงพอ (คงเหลือ: " + (currentStock != null ? currentStock : "0") + ")");
        }

        // 3. สร้าง Transaction สำหรับ Stock-Out
        MaterialStockTransaction transaction = new MaterialStockTransaction();
        transaction.setProductionReport(report);
        transaction.setMaterial(material);
        transaction.setLotNumber(request.getLotNumber());
        transaction.setQuantity(request.getQuantity());
        transaction.setTransactionType("OUT"); // กำหนดประเภทเป็น OUT
        transaction.setUser(user);
        
        MaterialStockTransaction savedTransaction = transactionRepository.save(transaction);

        // 4. บันทึกข้อมูลลงใน MaterialUsageLog ด้วย
        MaterialUsageLog materialUsageLog = new MaterialUsageLog();
        materialUsageLog.setReport(report);
        materialUsageLog.setMaterialCode(material.getMaterialCode()); // ใช้ MaterialCode จาก Material entity
        materialUsageLog.setLotNumber(request.getLotNumber());
        materialUsageLog.setQuantityKg(request.getQuantity()); // ใช้ Quantity จาก request
        materialUsageLog.setTechnician(user);
        // ไม่ต้อง setTimestamp เพราะ @PrePersist จะทำให้
        materialUsageLogRepository.save(materialUsageLog);

        auditLogService.log("STOCK_OUT", "MaterialStockTransaction", savedTransaction.getId(), null,
                Map.of("id", savedTransaction.getId(),
                        "materialId", String.valueOf(request.getMaterialId()),
                        "materialName", material.getMaterialName(),
                        "quantity", String.valueOf(request.getQuantity()),
                        "lotNumber", String.valueOf(request.getLotNumber()),
                        "reportId", String.valueOf(request.getProductionReportId())));

        return savedTransaction;
    }
}