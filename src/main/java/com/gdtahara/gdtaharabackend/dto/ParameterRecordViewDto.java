package com.gdtahara.gdtaharabackend.dto;

import com.gdtahara.gdtaharabackend.mapper.ParameterRecordMapper;
import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * View DTO for Technician parameter records that matches the frontend form shape.
 * - Includes record metadata and a nested `parameters` object
 * - Maps entity.recordTime -> recordType for UI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRecordViewDto {
    private Long id;
    private Long reportId;
    private Long technicianId;
    private String recordType; // UI expects recordType
    private LocalDateTime createdAt;
    private ParameterRecordRequest.ParameterData parameters; // nested data for the form

    public static ParameterRecordViewDto fromEntity(ParameterRecord entity) {
        ParameterRecordViewDto dto = new ParameterRecordViewDto();
        dto.setId(entity.getId());
        dto.setReportId(entity.getReportId());
        dto.setTechnicianId(entity.getTechnicianId());
        dto.setRecordType(entity.getRecordTime());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setParameters(ParameterRecordMapper.mapEntityToDto(entity));
        return dto;
    }
}
