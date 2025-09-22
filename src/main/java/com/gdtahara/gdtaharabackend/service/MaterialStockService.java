// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/MaterialStockService.java
// (ฉบับแก้ไขสมบูรณ์)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.MaterialStockTransactionDto;
import com.gdtahara.gdtaharabackend.dto.StockCardDto;
import com.gdtahara.gdtaharabackend.model.Material;
import com.gdtahara.gdtaharabackend.model.MaterialStockTransaction;
import com.gdtahara.gdtaharabackend.repository.MaterialRepository;
import com.gdtahara.gdtaharabackend.repository.MaterialStockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaterialStockService {

    private final MaterialRepository materialRepository;
    private final MaterialStockTransactionRepository transactionRepository;

    public MaterialStockService(MaterialRepository materialRepository, MaterialStockTransactionRepository transactionRepository) {
        this.materialRepository = materialRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<StockCardDto> getAllStockCards() {
        return materialRepository.findAll().stream()
                .map(this::createStockCardForMaterial)
                .collect(Collectors.toList());
    }

    private StockCardDto createStockCardForMaterial(Material material) {
        BigDecimal balance = transactionRepository.getStockBalanceByMaterialId(material.getId());
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        List<MaterialStockTransaction> transactions = transactionRepository.findByMaterialIdOrderByTimestampDesc(material.getId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<MaterialStockTransactionDto> transactionDtos = transactions.stream().map(t -> new MaterialStockTransactionDto(
                t.getId(),
                t.getTimestamp().format(formatter),
                t.getTransactionType(),
                t.getQuantity(),
                t.getLotNumber(),
                t.getProductionReport() != null ?
                        String.format("Order %s, %s",
                                t.getProductionReport().getOrderNumber(),
                                t.getProductionReport().getMachine().getMachineName())
                        : "-",
                t.getUser().getUsername()
        )).collect(Collectors.toList());

        return new StockCardDto(
                material.getId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                balance,
                transactionDtos
        );
    }
    
    @Transactional(readOnly = true)
    public List<String> getAvailableLotNumbers(Long materialId) {
        // Logic นี้จะดึง Lot Number ทั้งหมดที่มี Transaction "IN"
        // และยังไม่ได้ถูกใช้จนหมด (ถ้ามีการ track ยอดคงเหลือตาม Lot)
        // ในที่นี้จะใช้ Logic แบบง่ายไปก่อน คือดึงทุก Lot ที่เคยรับเข้า
        return transactionRepository.findDistinctLotNumbersByMaterialIdAndTransactionType(materialId, "IN");
    }
}