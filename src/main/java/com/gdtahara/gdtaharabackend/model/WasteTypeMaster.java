package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "waste_type_master")
public class WasteTypeMaster {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "name_th", nullable = false)
    private String nameTh;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "category")
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
