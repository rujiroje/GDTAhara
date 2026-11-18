package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreatePalletRequest {
    private Long productId;
    private String palletNumber;
    private LocalDate palletDate;   // null → today
    private String notes;
}
