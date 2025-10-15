package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagingLogViewDto {
    private LocalDateTime timestamp;
    private String lotNumber;
    private Integer boxNo;
    private String operatorName;
}
