package com.gdtahara.gdtaharabackend.dto;

public class AddLabelStockRequest {
    private Integer quantityToAdd;

    public AddLabelStockRequest() {}

    public AddLabelStockRequest(Integer quantityToAdd) {
        this.quantityToAdd = quantityToAdd;
    }

    public Integer getQuantityToAdd() { return quantityToAdd; }
    public void setQuantityToAdd(Integer quantityToAdd) { this.quantityToAdd = quantityToAdd; }
}