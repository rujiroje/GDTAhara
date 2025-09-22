package com.gdtahara.gdtaharabackend.dto;

import java.util.List;

public class MaterialStockCardDto {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Double currentStock;
    private List<MaterialStockTransactionDto> history;

    public MaterialStockCardDto() {}

    public MaterialStockCardDto(Long materialId, String materialCode, String materialName, Double currentStock, List<MaterialStockTransactionDto> history) {
        this.materialId = materialId;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.currentStock = currentStock;
        this.history = history;
    }

    // Getters and Setters
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

    public List<MaterialStockTransactionDto> getHistory() { return history; }
    public void setHistory(List<MaterialStockTransactionDto> history) { this.history = history; }
}