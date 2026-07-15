package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.BillOfMaterials;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BomViewDto {
    private Long id;
    private String fgCode;
    private Integer alternativeNumber;
    private String plantCode;
    private String materialGroup;
    private String sapMaterialType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private String importedFromFile;
    private List<BomItemDto> items;

    public static BomViewDto from(BillOfMaterials bom, List<BomItemDto> items) {
        BomViewDto dto = new BomViewDto();
        dto.id = bom.getId();
        dto.fgCode = bom.getFgCode();
        dto.alternativeNumber = bom.getAlternativeNumber();
        dto.plantCode = bom.getPlantCode();
        dto.materialGroup = bom.getMaterialGroup();
        dto.sapMaterialType = bom.getSapMaterialType();
        dto.effectiveFrom = bom.getEffectiveFrom();
        dto.effectiveTo = bom.getEffectiveTo();
        dto.status = bom.getStatus();
        dto.importedFromFile = bom.getImportedFromFile();
        dto.items = items;
        return dto;
    }
}
