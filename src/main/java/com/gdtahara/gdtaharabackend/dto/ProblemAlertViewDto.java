package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProblemAlertViewDto {
    private Long id;
    private String machineName;
    private String productName;
    private String message;
    private String operatorName;
    private String timestamp;
}