package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NgTypeSummaryDto {
    private String ngDescription;
    private Long count;
}