// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/LabelStockService.java
// (ฉบับแก้ไข)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.LabelStockViewDto;
import com.gdtahara.gdtaharabackend.model.LabelStock;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.repository.LabelStockRepository;
import com.gdtahara.gdtaharabackend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabelStockService {

    @Autowired private LabelStockRepository labelStockRepository;
    @Autowired private ProductRepository productRepository;

    public List<LabelStockViewDto> getAllLabelStocks() {
        return productRepository.findAll().stream().map(product -> {
            LabelStock stock = labelStockRepository.findByProductId(product.getId())
                    .orElseGet(() -> {
                        LabelStock newStock = new LabelStock();
                        newStock.setProduct(product);
                        newStock.setCurrentStock(0);
                        return newStock;
                    });
            return convertToDto(stock);
        }).collect(Collectors.toList());
    }

    @Transactional
    public LabelStockViewDto addStock(Long productId, Integer quantityToAdd) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        LabelStock stock = labelStockRepository.findByProductId(productId)
                .orElseGet(() -> {
                    LabelStock newStock = new LabelStock();
                    newStock.setProduct(product);
                    newStock.setCurrentStock(0);
                    return newStock;
                });

        stock.setCurrentStock(stock.getCurrentStock() + quantityToAdd);
        LabelStock savedStock = labelStockRepository.save(stock);
        
        // **[แก้ไข]** แปลง Entity ที่บันทึกแล้วเป็น DTO ก่อนส่งกลับ
        return convertToDto(savedStock);
    }

    // **[ใหม่]** เพิ่ม Helper method สำหรับแปลง Entity เป็น DTO
    private LabelStockViewDto convertToDto(LabelStock stock) {
        return new LabelStockViewDto(
            stock.getProduct().getId(),
            stock.getProduct().getProductCode(),
            stock.getProduct().getProductName(),
            stock.getCurrentStock()
        );
    }
}