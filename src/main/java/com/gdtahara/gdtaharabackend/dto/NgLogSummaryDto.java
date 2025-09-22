package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NgLogSummaryDto {
    private String timestamp;
    private String ngDescription;
    private Integer quantity;
    private String source;
    private String userName;
}