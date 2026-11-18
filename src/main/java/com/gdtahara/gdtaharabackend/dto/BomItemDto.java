package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.BomItem;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BomItemDto {
    private Long id;
    private Integer itemNumber;
    private String rmCode;
    private String rmName;
    private BigDecimal quantityPer;
    private String unit;
    private BigDecimal lossPercent;
    private String materialType;
    private Boolean isScrap;

    public static BomItemDto from(BomItem item) {
        BomItemDto dto = new BomItemDto();
        dto.id = item.getId();
        dto.itemNumber = item.getItemNumber();
        dto.rmCode = item.getRmCode();
        dto.rmName = item.getRmName();
        dto.quantityPer = item.getQuantityPer();
        dto.unit = item.getUnit();
        dto.lossPercent = item.getLossPercent();
        dto.materialType = item.getMaterialType();
        dto.isScrap = item.getIsScrap();
        return dto;
    }
}
