// File: src/main/java/com/gdtahara/gdtaharabackend/dto/LoginRequest.java
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

/**
 * คลาสสำหรับรับข้อมูล username และ password จาก Frontend
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}

