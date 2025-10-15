// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ResetPasswordRequest.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String newPassword;
}