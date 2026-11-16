package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "shift_boundary_config")
public class ShiftBoundaryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String shiftCode;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer reminderGraceMinutes;
    private Boolean isActive = true;
}
