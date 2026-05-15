package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MachineStatusLogDto {
    private Long id;
    private Long machineId;
    private String machineName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String source;
    private Long durationMinutes;
}
