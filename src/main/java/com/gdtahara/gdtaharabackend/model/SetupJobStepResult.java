package com.gdtahara.gdtaharabackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "setup_job_step_results")
public class SetupJobStepResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setup_job_id", nullable = false)
    private MachineSetupJob setupJob;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private SetupChecklistTemplate template;

    @Column(name = "is_done", nullable = false)
    private boolean done = false;

    @Column(name = "photo_filename", length = 200)
    private String photoFilename;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
