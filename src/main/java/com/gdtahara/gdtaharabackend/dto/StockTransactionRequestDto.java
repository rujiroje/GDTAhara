package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockTransactionRequestDto {
    private Long materialId;
    private String transactionType; // "IN" or "OUT"
    private BigDecimal quantity;
    private String lotNumber;
    private Long productionReportId;
}