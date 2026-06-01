package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrintLabelRequest {

    @NotBlank(message = "printerTarget must not be blank")
    private String printerTarget;
}
