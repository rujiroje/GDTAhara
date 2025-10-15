package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO สำหรับข้อมูล Summary ของ Parameter Record
 * ใช้เฉพาะข้อมูลที่จำเป็นในการแสดงผลหน้า List
 */
@Data
@NoArgsConstructor
public class ParameterRecordSummaryDto {
    
    // ข้อมูลหลัก
    private Long id;
    private Long reportId;
    private Long technicianId;
    private String recordTime;
    private LocalDateTime createdAt;
    
    // ข้อมูลสำคัญที่แสดงใน Summary (10-15 fields หลัก)
    // Temperature หลัก
    private BigDecimal tempMainC1;
    private BigDecimal tempMainC2;
    private BigDecimal tempMainC3;
    
    // Extruder หลัก
    private BigDecimal extruderMainScrewRpm;
    private BigDecimal extruderMainResinPress;
    private BigDecimal extruderMainResinTemp;
    
    // ค่าการทำงานหลัก
    private BigDecimal cycleTimeSec;
    private BigDecimal moldTemp;
    private BigDecimal highBlowMpa;
    private BigDecimal lowPressureMpa;
    
    // การตรวจสอบ
    private String productQualityCheck;
    private String machineOperationCheck;
    private String safetyProcedureCheck;
    
    // เพิ่มข้อมูลที่มีการใช้งานบ่อย
    private String additionalNotes;
    
    // Constructor สำหรับ JPQL Query
    public ParameterRecordSummaryDto(
            Long id, Long reportId, Long technicianId, String recordTime, LocalDateTime createdAt,
            BigDecimal tempMainC1, BigDecimal tempMainC2, BigDecimal tempMainC3,
            BigDecimal extruderMainScrewRpm, BigDecimal extruderMainResinPress, BigDecimal extruderMainResinTemp,
            BigDecimal cycleTimeSec, BigDecimal moldTemp, BigDecimal highBlowMpa, BigDecimal lowPressureMpa,
            String productQualityCheck, String machineOperationCheck, String safetyProcedureCheck,
            String additionalNotes) {
        this.id = id;
        this.reportId = reportId;
        this.technicianId = technicianId;
        this.recordTime = recordTime;
        this.createdAt = createdAt;
        this.tempMainC1 = tempMainC1;
        this.tempMainC2 = tempMainC2;
        this.tempMainC3 = tempMainC3;
        this.extruderMainScrewRpm = extruderMainScrewRpm;
        this.extruderMainResinPress = extruderMainResinPress;
        this.extruderMainResinTemp = extruderMainResinTemp;
        this.cycleTimeSec = cycleTimeSec;
        this.moldTemp = moldTemp;
        this.highBlowMpa = highBlowMpa;
        this.lowPressureMpa = lowPressureMpa;
        this.productQualityCheck = productQualityCheck;
        this.machineOperationCheck = machineOperationCheck;
        this.safetyProcedureCheck = safetyProcedureCheck;
        this.additionalNotes = additionalNotes;
    }
}