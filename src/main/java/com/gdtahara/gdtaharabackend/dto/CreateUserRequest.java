// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/CreateUserRequest.java
// (**สร้างไฟล์ใหม่** ใน package dto)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO สำหรับรับข้อมูลการสร้างผู้ใช้งานใหม่
 */
@Data
public class CreateUserRequest {

    /**
     * ชื่อผู้ใช้งาน
     */
    @NotBlank(message = "Username is required.")
    private String username;

    /**
     * รหัสผ่าน
     */
    @NotBlank(message = "Password is required.")
    private String password;

    /**
     * บทบาทของผู้ใช้งาน
     */
    @NotBlank(message = "Role is required.")
    private String role;
}