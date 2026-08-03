package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PalletDto {
    private Long id;
    private String palletNumber;
    private String productCode;
    private String productName;
    private Integer actualQty;
    private Integer targetQty;
    private Integer boxCount;
    private String status;
    private LocalDate palletDate;
    private LocalDate lotDateMin;
    private LocalDate lotDateMax;
    private LocalDateTime createdAt;
    private String createdByName;
    private LocalDateTime closedAt;
    private String closedByName;
    private LocalDateTime printedAt;
    private Integer printCount;
    private String notes;
    private Long parentPalletId;
    private String parentPalletNumber;
    private String revisionSuffix;
    private String rearrangeReason;
    private List<PalletBoxDto> boxes;
}
