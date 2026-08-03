package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PalletLabelDto {
    private String palletNumber;
    private String productCode;
    private String productName;
    private LocalDate lotDateMin;
    private LocalDate lotDateMax;
    private Integer totalQty;
    private Integer boxCount;
    private LocalDate closeDate;
    /** key = YYMMDD string (e.g. "260719"), value = ordered list of sub-lot numbers */
    private Map<String, List<String>> lotBoxMatrix;
}
