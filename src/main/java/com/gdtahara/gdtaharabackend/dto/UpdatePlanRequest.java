package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlanRequest {
    private Integer targetQty;
    private BigDecimal manpowerDRatio;
    private BigDecimal manpowerNRatio;
    private String status;
}
