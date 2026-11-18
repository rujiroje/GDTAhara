package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class BomImportResultDto {
    private String fileType;
    private int rowsAdded;
    private int rowsUpdated;
    private int rowsSkipped;
    private int rowsError;
    private List<String> errors;
}
