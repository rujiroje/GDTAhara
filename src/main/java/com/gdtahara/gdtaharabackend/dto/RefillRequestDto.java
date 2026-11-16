package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RefillRequestDto {
    private String materialCode;
    private Long materialStockId; // nullable — links to material_stock_transactions for lot tracking
    private String materialLotCode;
    private BigDecimal qtyKg;
    private String shiftCode;
    private LocalDate shiftDate;
    private Boolean materialTypeOverride;
    private String note;
    private String photoIds;
}
