package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MachineRefillHistoryDto {
    private LocalDateTime recordedAt;
    private String tankId;
    private String tankName;
    private String material;
    private BigDecimal quantity;
    private String unit;
    private String shiftCode;
    private String entryType; // "REFILL" | "CORRECTION"
}
