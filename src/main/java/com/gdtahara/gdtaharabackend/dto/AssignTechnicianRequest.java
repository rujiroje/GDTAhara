package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTechnicianRequest {
    @NotNull
    private Long technicianUserId;
}
