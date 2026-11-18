// File: src/main/java/com/gdtahara/gdtaharabackend/dto/LoginRequest.java
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}

