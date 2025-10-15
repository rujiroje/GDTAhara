// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/NgType.java
// (ไม่มีการเปลี่ยนแปลง)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ng_types")
public class NgType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ng_code", unique = true, nullable = false)
    private String ngCode;

    @Column(name = "ng_description_th")
    private String ngDescriptionTh;

    @Column(name = "ng_type")
    private String ngType;
}