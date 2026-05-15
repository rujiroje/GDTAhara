package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class MachineStatusUpdateRequest {
    private Long machineId;
    // RUNNING | IDLE | SETUP | PLANNED_STOP | UNPLANNED_STOP
    private String status;
    private String reason;
}
