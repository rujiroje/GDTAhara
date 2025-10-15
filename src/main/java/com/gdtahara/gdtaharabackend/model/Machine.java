// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/model/Machine.java
// (ไม่มีการเปลี่ยนแปลง)
// =================================================================
package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "machines")
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_code", unique = true, nullable = false)
    private String machineCode;

    @Column(name = "machine_name")
    private String machineName;
}