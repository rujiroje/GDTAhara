package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String materialCode;
    private String materialName;
    private String materialType;
    private String unit;
    private String plantCode;
}