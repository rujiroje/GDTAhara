package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO สำหรับส่งข้อมูล Parameter Record กลับไปยัง Frontend
 * แปลง recordTime เป็น recordType ให้ตรงกับ Frontend expectation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRecordResponseDto {
    private Long id;
    private Long reportId;
    private Long technicianId;
    private String recordType; // Frontend expects this field name
    private LocalDateTime createdAt;
    
    // คอนสตรัคเตอร์สำหรับแปลงจาก ParameterRecord entity
    public ParameterRecordResponseDto(ParameterRecord entity) {
        this.id = entity.getId();
        this.reportId = entity.getReportId();
        this.technicianId = entity.getTechnicianId();
        this.recordType = entity.getRecordTime(); // แปลง recordTime เป็น recordType
        this.createdAt = entity.getCreatedAt();
    }
    
    // Static method สำหรับแปลงจาก Entity
    public static ParameterRecordResponseDto fromEntity(ParameterRecord entity) {
        return new ParameterRecordResponseDto(entity);
    }
}