package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "machine_tank")
public class MachineTank {

    @Id
    private String tankId;

    private String machineId;
    private String tankCode;
    private String tankName;
    private String defaultMaterialCode;
    private BigDecimal capacityKg;
    private String physicalPosition;
    private Boolean isActive = true;
}
