package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

/** C-04: suggested material for a tank derived from active BOM */
@Data
public class BomSuggestedMaterialDto {
    private String materialCode;
    private String materialName;
    private String tankRole;
}
