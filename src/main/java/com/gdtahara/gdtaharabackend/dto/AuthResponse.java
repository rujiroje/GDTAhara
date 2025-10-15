// File: src/main/java/com/gdtahara/gdtaharabackend/dto/AuthResponse.java
package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * คลาสสำหรับส่งข้อมูล Token, Role และ Username กลับไปให้ Frontend
 */
@Data
@AllArgsConstructor
@NoArgsConstructor // เพิ่ม Default Constructor เพื่อความยืดหยุ่น
public class AuthResponse {
    private String token;
    private String role;
    private String username;
}
