package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CorrectionRequestDto {
    private BigDecimal correctedQtyKg;
    private String note;
}
