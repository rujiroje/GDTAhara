// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/UserDto.java
// (ไม่มีการเปลี่ยนแปลง)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String role;
}