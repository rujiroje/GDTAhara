package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "setup_checklist_templates")
public class SetupChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder = 0;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "machine_type", length = 20)
    private String machineType;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
