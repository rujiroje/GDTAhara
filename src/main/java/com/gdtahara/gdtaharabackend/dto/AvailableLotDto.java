package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AvailableLotDto {
    private Long stockId;
    private String materialCode;
    private String materialName;
    private String lotNumber;
    private BigDecimal quantity;
    private BigDecimal remainingQty;
    private LocalDateTime timestamp;
}
