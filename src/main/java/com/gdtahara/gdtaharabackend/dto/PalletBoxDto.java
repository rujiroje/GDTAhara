package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PalletBoxDto {
    private Long subLotId;
    private String subLotNumber;
    private String displayLotDate;   // YYMMDD e.g. "260719"
    private LocalDate lotDate;
    private Integer boxQuantity;
    private BigDecimal weightKg;
}
