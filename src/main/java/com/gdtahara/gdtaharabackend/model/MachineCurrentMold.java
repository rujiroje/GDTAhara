package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "machine_current_mold")
public class MachineCurrentMold {

    /** machine_id is both PK and FK to machines.id */
    @Id
    @Column(name = "machine_id")
    private Long machineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mold_id", nullable = false)
    private MoldMaster mold;

    /** FK to machine_setup_job that triggered this mold install */
    @Column(name = "setup_job_id")
    private Long setupJobId;

    @Column(nullable = false, length = 50)
    private String installedBy;

    @Column(nullable = false)
    private LocalDateTime installedAt;

    @PrePersist
    protected void onCreate() {
        if (this.installedAt == null) this.installedAt = LocalDateTime.now();
    }
}
