package com.gdtahara.gdtaharabackend.dto;

public class LabelStockDto {
    private Long productId;
    private String productCode;
    private String productName;
    private Long currentStock;

    public LabelStockDto() {}

    public LabelStockDto(Long productId, String productCode, String productName, Long currentStock) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.currentStock = currentStock;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getCurrentStock() { return currentStock; }
    public void setCurrentStock(Long currentStock) { this.currentStock = currentStock; }
}