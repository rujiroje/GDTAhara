package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parameter_records")
public class ParameterRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "report_id", nullable = false)
    private Long reportId;
    
    @Column(name = "technician_id", nullable = false)
    private Long technicianId;
    
    @Column(name = "record_time", nullable = false)
    private String recordTime; // เช่น "10:00", "18:00", "02:00"
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Extruder Screw parameters - Main
    @Column(name = "extruder_main_screw_rpm")
    private BigDecimal extruderMainScrewRpm;
    
    @Column(name = "extruder_main_lo_limit")
    private BigDecimal extruderMainLoLimit;
    
    @Column(name = "extruder_main_resin_press")
    private BigDecimal extruderMainResinPress;
    
    @Column(name = "extruder_main_resin_temp")
    private BigDecimal extruderMainResinTemp;
    
    @Column(name = "extruder_main_motor_current")
    private BigDecimal extruderMainMotorCurrent;
    
    // Extruder Screw parameters - Admer (ตรงกับ Mapper)
    @Column(name = "extruder_admer_screw_rpm")
    private BigDecimal extruderAdmerScrewRpm;
    
    @Column(name = "extruder_admer_lo_limit")
    private BigDecimal extruderAdmerLoLimit;
    
    @Column(name = "extruder_admer_resin_press")
    private BigDecimal extruderAdmerResinPress;
    
    @Column(name = "extruder_admer_resin_temp")
    private BigDecimal extruderAdmerResinTemp;
    
    @Column(name = "extruder_admer_motor_current")
    private BigDecimal extruderAdmerMotorCurrent;
    
    // Extruder Screw parameters - EVOH (ตรงกับ Mapper)
    @Column(name = "extruder_evoh_screw_rpm")
    private BigDecimal extruderEvohScrewRpm;
    
    @Column(name = "extruder_evoh_lo_limit")
    private BigDecimal extruderEvohLoLimit;
    
    @Column(name = "extruder_evoh_resin_press")
    private BigDecimal extruderEvohResinPress;
    
    @Column(name = "extruder_evoh_resin_temp")
    private BigDecimal extruderEvohResinTemp;
    
    @Column(name = "extruder_evoh_motor_current")
    private BigDecimal extruderEvohMotorCurrent;
    
    // Extruder Screw parameters - Virgin (เพิ่ม Virgin fields)
    @Column(name = "extruder_virgin_screw_rpm")
    private BigDecimal extruderVirginScrewRpm;
    
    @Column(name = "extruder_virgin_lo_limit")
    private BigDecimal extruderVirginLoLimit;
    
    @Column(name = "extruder_virgin_resin_press")
    private BigDecimal extruderVirginResinPress;
    
    @Column(name = "extruder_virgin_resin_temp")
    private BigDecimal extruderVirginResinTemp;
    
    @Column(name = "extruder_virgin_motor_current")
    private BigDecimal extruderVirginMotorCurrent;

    // Temperature parameters - Main (เพิ่ม fields ที่ขาดหายไป)
    @Column(name = "temp_main_fb")
    private BigDecimal tempMainFb;
    
    @Column(name = "temp_main_c1")
    private BigDecimal tempMainC1;
    
    @Column(name = "temp_main_c2")
    private BigDecimal tempMainC2;
    
    @Column(name = "temp_main_c3")
    private BigDecimal tempMainC3;
    
    @Column(name = "temp_main_c4")
    private BigDecimal tempMainC4;
    
    @Column(name = "temp_main_c5")
    private BigDecimal tempMainC5;
    
    @Column(name = "temp_main_a1")
    private BigDecimal tempMainA1;
    
    @Column(name = "temp_main_a2")
    private BigDecimal tempMainA2;
    
    @Column(name = "temp_main_a3")
    private BigDecimal tempMainA3;
    
    @Column(name = "temp_main_a4")
    private BigDecimal tempMainA4;

    // Temperature parameters - Admer (เพิ่ม A และ H fields)
    @Column(name = "temp_admer_fb")
    private BigDecimal tempAdmerFb;
    
    @Column(name = "temp_admer_c1")
    private BigDecimal tempAdmerC1;
    
    @Column(name = "temp_admer_c2")
    private BigDecimal tempAdmerC2;
    
    @Column(name = "temp_admer_c3")
    private BigDecimal tempAdmerC3;
    
    @Column(name = "temp_admer_a1")
    private BigDecimal tempAdmerA1;
    
    @Column(name = "temp_admer_a2")
    private BigDecimal tempAdmerA2;
    
    @Column(name = "temp_admer_h1")
    private BigDecimal tempAdmerH1;
    
    @Column(name = "temp_admer_h2")
    private BigDecimal tempAdmerH2;
    
    @Column(name = "temp_admer_h3")
    private BigDecimal tempAdmerH3;

    // Temperature parameters - EVOH (เพิ่ม A และ H fields)
    @Column(name = "temp_evoh_fb")
    private BigDecimal tempEvohFb;
    
    @Column(name = "temp_evoh_c1")
    private BigDecimal tempEvohC1;
    
    @Column(name = "temp_evoh_c2")
    private BigDecimal tempEvohC2;
    
    @Column(name = "temp_evoh_c3")
    private BigDecimal tempEvohC3;
    
    @Column(name = "temp_evoh_a1")
    private BigDecimal tempEvohA1;
    
    @Column(name = "temp_evoh_a2")
    private BigDecimal tempEvohA2;
    
    @Column(name = "temp_evoh_h1")
    private BigDecimal tempEvohH1;
    
    @Column(name = "temp_evoh_h2")
    private BigDecimal tempEvohH2;
    
    @Column(name = "temp_evoh_h3")
    private BigDecimal tempEvohH3;

    // Temperature parameters - Virgin
    @Column(name = "temp_virgin_fb")
    private BigDecimal tempVirginFb;
    
    @Column(name = "temp_virgin_c1")
    private BigDecimal tempVirginC1;
    
    @Column(name = "temp_virgin_c2")
    private BigDecimal tempVirginC2;
    
    @Column(name = "temp_virgin_c3")
    private BigDecimal tempVirginC3;
    
    @Column(name = "temp_virgin_a1")
    private BigDecimal tempVirginA1;
    
    @Column(name = "temp_virgin_a2")
    private BigDecimal tempVirginA2;
    
    @Column(name = "temp_virgin_a3")
    private BigDecimal tempVirginA3;
    
    @Column(name = "temp_virgin_a4")
    private BigDecimal tempVirginA4;
    
    @Column(name = "temp_virgin_a5")
    private BigDecimal tempVirginA5;

    // Temperature parameters - Head D (4 หัวคูณ 4 จุด)
    @Column(name = "temp_head_d1_1")
    private BigDecimal tempHeadD1_1;
    
    @Column(name = "temp_head_d2_1")
    private BigDecimal tempHeadD2_1;
    
    @Column(name = "temp_head_d3_1")
    private BigDecimal tempHeadD3_1;
    
    @Column(name = "temp_head_d4_1")
    private BigDecimal tempHeadD4_1;
    
    @Column(name = "temp_head_d1_2")
    private BigDecimal tempHeadD1_2;
    
    @Column(name = "temp_head_d2_2")
    private BigDecimal tempHeadD2_2;
    
    @Column(name = "temp_head_d3_2")
    private BigDecimal tempHeadD3_2;
    
    @Column(name = "temp_head_d4_2")
    private BigDecimal tempHeadD4_2;
    
    @Column(name = "temp_head_d1_3")
    private BigDecimal tempHeadD1_3;
    
    @Column(name = "temp_head_d2_3")
    private BigDecimal tempHeadD2_3;
    
    @Column(name = "temp_head_d3_3")
    private BigDecimal tempHeadD3_3;
    
    @Column(name = "temp_head_d4_3")
    private BigDecimal tempHeadD4_3;
    
    @Column(name = "temp_head_d1_4")
    private BigDecimal tempHeadD1_4;
    
    @Column(name = "temp_head_d2_4")
    private BigDecimal tempHeadD2_4;
    
    @Column(name = "temp_head_d3_4")
    private BigDecimal tempHeadD3_4;
    
    @Column(name = "temp_head_d4_4")
    private BigDecimal tempHeadD4_4;

    // Temperature parameters - Head L
    @Column(name = "temp_head_l1")
    private BigDecimal tempHeadL1;
    
    @Column(name = "temp_head_l2")
    private BigDecimal tempHeadL2;
    
    @Column(name = "temp_head_l3")
    private BigDecimal tempHeadL3;
    
    @Column(name = "temp_head_l4")
    private BigDecimal tempHeadL4;

    // ส่วนที่ 3: ค่าการทำงานอื่นๆ
    @Column(name = "cycle_time_sec")
    private BigDecimal cycleTimeSec;
    
    @Column(name = "mold_temp")
    private BigDecimal moldTemp;
    
    @Column(name = "high_blow_mpa")
    private BigDecimal highBlowMpa;
    
    @Column(name = "low_pressure_mpa")
    private BigDecimal lowPressureMpa;
    
    @Column(name = "blow_ratio")
    private BigDecimal blowRatio;
    
    @Column(name = "blow_air_condition1")
    private BigDecimal blowAirCondition1;
    
    @Column(name = "blow_air_condition2")
    private BigDecimal blowAirCondition2;
    
    @Column(name = "parison_air1")
    private BigDecimal parisonAir1;
    
    @Column(name = "parison_air2")
    private BigDecimal parisonAir2;
    
    @Column(name = "zero_value")
    private BigDecimal zeroValue;
    
    @Column(name = "weight_value")
    private BigDecimal weightValue;
    
    @Column(name = "span_value")
    private BigDecimal spanValue;

    // Blow Pin parameters - Left
    @Column(name = "blow_pin_left_pin")
    private BigDecimal blowPinLeftPin;
    
    @Column(name = "blow_pin_left_front_a")
    private BigDecimal blowPinLeftFrontA;
    
    @Column(name = "blow_pin_left_front_b")
    private BigDecimal blowPinLeftFrontB;
    
    @Column(name = "blow_pin_left_front_c")
    private BigDecimal blowPinLeftFrontC;
    
    @Column(name = "blow_pin_left_front_d")
    private BigDecimal blowPinLeftFrontD;
    
    @Column(name = "blow_pin_left_front_e")
    private BigDecimal blowPinLeftFrontE;
    
    @Column(name = "blow_pin_left_front_f")
    private BigDecimal blowPinLeftFrontF;

    // Blow Pin parameters - Right
    @Column(name = "blow_pin_right_pin")
    private BigDecimal blowPinRightPin;
    
    @Column(name = "blow_pin_right_front_a")
    private BigDecimal blowPinRightFrontA;
    
    @Column(name = "blow_pin_right_front_b")
    private BigDecimal blowPinRightFrontB;
    
    @Column(name = "blow_pin_right_front_c")
    private BigDecimal blowPinRightFrontC;
    
    @Column(name = "blow_pin_right_front_d")
    private BigDecimal blowPinRightFrontD;
    
    @Column(name = "blow_pin_right_front_e")
    private BigDecimal blowPinRightFrontE;
    
    @Column(name = "blow_pin_right_front_f")
    private BigDecimal blowPinRightFrontF;

    // Other checks - เพิ่ม fields ที่หายไป
    @Column(name = "check_emergency_sw_status")
    private String checkEmergencySwStatus;
    
    @Column(name = "check_sq_ss_pieces")
    private Integer checkSqSsPieces;
    
    // ส่วนที่ 4: การตรวจสอบ (ตาม PRD)
    @Column(name = "product_quality_check")
    private String productQualityCheck;
    
    @Column(name = "machine_operation_check")
    private String machineOperationCheck;
    
    @Column(name = "safety_procedure_check")
    private String safetyProcedureCheck;
    
    @Column(name = "additional_notes")
    private String additionalNotes;

    // Constructors
    public ParameterRecord() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public Long getTechnicianId() {
        return technicianId;
    }
    
    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }
    
    public String getRecordTime() {
        return recordTime;
    }
    
    public void setRecordTime(String recordTime) {
        this.recordTime = recordTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Extruder Main getters/setters
    public BigDecimal getExtruderMainScrewRpm() {
        return extruderMainScrewRpm;
    }
    
    public void setExtruderMainScrewRpm(BigDecimal extruderMainScrewRpm) {
        this.extruderMainScrewRpm = extruderMainScrewRpm;
    }
    
    public BigDecimal getExtruderMainLoLimit() {
        return extruderMainLoLimit;
    }
    
    public void setExtruderMainLoLimit(BigDecimal extruderMainLoLimit) {
        this.extruderMainLoLimit = extruderMainLoLimit;
    }
    
    public BigDecimal getExtruderMainResinPress() {
        return extruderMainResinPress;
    }
    
    public void setExtruderMainResinPress(BigDecimal extruderMainResinPress) {
        this.extruderMainResinPress = extruderMainResinPress;
    }
    
    public BigDecimal getExtruderMainResinTemp() {
        return extruderMainResinTemp;
    }
    
    public void setExtruderMainResinTemp(BigDecimal extruderMainResinTemp) {
        this.extruderMainResinTemp = extruderMainResinTemp;
    }
    
    public BigDecimal getExtruderMainMotorCurrent() {
        return extruderMainMotorCurrent;
    }
    
    public void setExtruderMainMotorCurrent(BigDecimal extruderMainMotorCurrent) {
        this.extruderMainMotorCurrent = extruderMainMotorCurrent;
    }
    
    // Extruder Admer getters/setters
    public BigDecimal getExtruderAdmerScrewRpm() {
        return extruderAdmerScrewRpm;
    }
    
    public void setExtruderAdmerScrewRpm(BigDecimal extruderAdmerScrewRpm) {
        this.extruderAdmerScrewRpm = extruderAdmerScrewRpm;
    }
    
    public BigDecimal getExtruderAdmerLoLimit() {
        return extruderAdmerLoLimit;
    }
    
    public void setExtruderAdmerLoLimit(BigDecimal extruderAdmerLoLimit) {
        this.extruderAdmerLoLimit = extruderAdmerLoLimit;
    }
    
    public BigDecimal getExtruderAdmerResinPress() {
        return extruderAdmerResinPress;
    }
    
    public void setExtruderAdmerResinPress(BigDecimal extruderAdmerResinPress) {
        this.extruderAdmerResinPress = extruderAdmerResinPress;
    }
    
    public BigDecimal getExtruderAdmerResinTemp() {
        return extruderAdmerResinTemp;
    }
    
    public void setExtruderAdmerResinTemp(BigDecimal extruderAdmerResinTemp) {
        this.extruderAdmerResinTemp = extruderAdmerResinTemp;
    }
    
    public BigDecimal getExtruderAdmerMotorCurrent() {
        return extruderAdmerMotorCurrent;
    }
    
    public void setExtruderAdmerMotorCurrent(BigDecimal extruderAdmerMotorCurrent) {
        this.extruderAdmerMotorCurrent = extruderAdmerMotorCurrent;
    }
    
    // Extruder EVOH getters/setters
    public BigDecimal getExtruderEvohScrewRpm() {
        return extruderEvohScrewRpm;
    }
    
    public void setExtruderEvohScrewRpm(BigDecimal extruderEvohScrewRpm) {
        this.extruderEvohScrewRpm = extruderEvohScrewRpm;
    }
    
    public BigDecimal getExtruderEvohLoLimit() {
        return extruderEvohLoLimit;
    }
    
    public void setExtruderEvohLoLimit(BigDecimal extruderEvohLoLimit) {
        this.extruderEvohLoLimit = extruderEvohLoLimit;
    }
    
    public BigDecimal getExtruderEvohResinPress() {
        return extruderEvohResinPress;
    }
    
    public void setExtruderEvohResinPress(BigDecimal extruderEvohResinPress) {
        this.extruderEvohResinPress = extruderEvohResinPress;
    }
    
    public BigDecimal getExtruderEvohResinTemp() {
        return extruderEvohResinTemp;
    }
    
    public void setExtruderEvohResinTemp(BigDecimal extruderEvohResinTemp) {
        this.extruderEvohResinTemp = extruderEvohResinTemp;
    }
    
    public BigDecimal getExtruderEvohMotorCurrent() {
        return extruderEvohMotorCurrent;
    }
    
    public void setExtruderEvohMotorCurrent(BigDecimal extruderEvohMotorCurrent) {
        this.extruderEvohMotorCurrent = extruderEvohMotorCurrent;
    }
    
    // Extruder Virgin getters/setters
    public BigDecimal getExtruderVirginScrewRpm() {
        return extruderVirginScrewRpm;
    }
    
    public void setExtruderVirginScrewRpm(BigDecimal extruderVirginScrewRpm) {
        this.extruderVirginScrewRpm = extruderVirginScrewRpm;
    }
    
    public BigDecimal getExtruderVirginLoLimit() {
        return extruderVirginLoLimit;
    }
    
    public void setExtruderVirginLoLimit(BigDecimal extruderVirginLoLimit) {
        this.extruderVirginLoLimit = extruderVirginLoLimit;
    }
    
    public BigDecimal getExtruderVirginResinPress() {
        return extruderVirginResinPress;
    }
    
    public void setExtruderVirginResinPress(BigDecimal extruderVirginResinPress) {
        this.extruderVirginResinPress = extruderVirginResinPress;
    }
    
    public BigDecimal getExtruderVirginResinTemp() {
        return extruderVirginResinTemp;
    }
    
    public void setExtruderVirginResinTemp(BigDecimal extruderVirginResinTemp) {
        this.extruderVirginResinTemp = extruderVirginResinTemp;
    }
    
    public BigDecimal getExtruderVirginMotorCurrent() {
        return extruderVirginMotorCurrent;
    }
    
    public void setExtruderVirginMotorCurrent(BigDecimal extruderVirginMotorCurrent) {
        this.extruderVirginMotorCurrent = extruderVirginMotorCurrent;
    }

    // Temperature getters/setters
    public BigDecimal getTempMainFb() {
        return tempMainFb;
    }
    
    public void setTempMainFb(BigDecimal tempMainFb) {
        this.tempMainFb = tempMainFb;
    }
    
    public BigDecimal getTempMainC1() {
        return tempMainC1;
    }
    
    public void setTempMainC1(BigDecimal tempMainC1) {
        this.tempMainC1 = tempMainC1;
    }
    
    public BigDecimal getTempMainC2() {
        return tempMainC2;
    }
    
    public void setTempMainC2(BigDecimal tempMainC2) {
        this.tempMainC2 = tempMainC2;
    }
    
    public BigDecimal getTempMainC3() {
        return tempMainC3;
    }
    
    public void setTempMainC3(BigDecimal tempMainC3) {
        this.tempMainC3 = tempMainC3;
    }
    
    public BigDecimal getTempMainC4() {
        return tempMainC4;
    }
    
    public void setTempMainC4(BigDecimal tempMainC4) {
        this.tempMainC4 = tempMainC4;
    }
    
    public BigDecimal getTempMainC5() {
        return tempMainC5;
    }
    
    public void setTempMainC5(BigDecimal tempMainC5) {
        this.tempMainC5 = tempMainC5;
    }
    
    public BigDecimal getTempMainA1() {
        return tempMainA1;
    }
    
    public void setTempMainA1(BigDecimal tempMainA1) {
        this.tempMainA1 = tempMainA1;
    }
    
    public BigDecimal getTempMainA2() {
        return tempMainA2;
    }
    
    public void setTempMainA2(BigDecimal tempMainA2) {
        this.tempMainA2 = tempMainA2;
    }
    
    public BigDecimal getTempMainA3() {
        return tempMainA3;
    }
    
    public void setTempMainA3(BigDecimal tempMainA3) {
        this.tempMainA3 = tempMainA3;
    }
    
    public BigDecimal getTempMainA4() {
        return tempMainA4;
    }
    
    public void setTempMainA4(BigDecimal tempMainA4) {
        this.tempMainA4 = tempMainA4;
    }
    
    public BigDecimal getTempAdmerFb() {
        return tempAdmerFb;
    }
    
    public void setTempAdmerFb(BigDecimal tempAdmerFb) {
        this.tempAdmerFb = tempAdmerFb;
    }
    
    public BigDecimal getTempAdmerC1() {
        return tempAdmerC1;
    }
    
    public void setTempAdmerC1(BigDecimal tempAdmerC1) {
        this.tempAdmerC1 = tempAdmerC1;
    }
    
    public BigDecimal getTempAdmerC2() {
        return tempAdmerC2;
    }
    
    public void setTempAdmerC2(BigDecimal tempAdmerC2) {
        this.tempAdmerC2 = tempAdmerC2;
    }
    
    public BigDecimal getTempAdmerC3() {
        return tempAdmerC3;
    }
    
    public void setTempAdmerC3(BigDecimal tempAdmerC3) {
        this.tempAdmerC3 = tempAdmerC3;
    }
    
    public BigDecimal getTempAdmerA1() {
        return tempAdmerA1;
    }
    
    public void setTempAdmerA1(BigDecimal tempAdmerA1) {
        this.tempAdmerA1 = tempAdmerA1;
    }
    
    public BigDecimal getTempAdmerA2() {
        return tempAdmerA2;
    }
    
    public void setTempAdmerA2(BigDecimal tempAdmerA2) {
        this.tempAdmerA2 = tempAdmerA2;
    }
    
    public BigDecimal getTempAdmerH1() {
        return tempAdmerH1;
    }
    
    public void setTempAdmerH1(BigDecimal tempAdmerH1) {
        this.tempAdmerH1 = tempAdmerH1;
    }
    
    public BigDecimal getTempAdmerH2() {
        return tempAdmerH2;
    }
    
    public void setTempAdmerH2(BigDecimal tempAdmerH2) {
        this.tempAdmerH2 = tempAdmerH2;
    }
    
    public BigDecimal getTempAdmerH3() {
        return tempAdmerH3;
    }
    
    public void setTempAdmerH3(BigDecimal tempAdmerH3) {
        this.tempAdmerH3 = tempAdmerH3;
    }
    
    public BigDecimal getTempEvohFb() {
        return tempEvohFb;
    }
    
    public void setTempEvohFb(BigDecimal tempEvohFb) {
        this.tempEvohFb = tempEvohFb;
    }
    
    public BigDecimal getTempEvohC1() {
        return tempEvohC1;
    }
    
    public void setTempEvohC1(BigDecimal tempEvohC1) {
        this.tempEvohC1 = tempEvohC1;
    }
    
    public BigDecimal getTempEvohC2() {
        return tempEvohC2;
    }
    
    public void setTempEvohC2(BigDecimal tempEvohC2) {
        this.tempEvohC2 = tempEvohC2;
    }
    
    public BigDecimal getTempEvohC3() {
        return tempEvohC3;
    }
    
    public void setTempEvohC3(BigDecimal tempEvohC3) {
        this.tempEvohC3 = tempEvohC3;
    }
    
    public BigDecimal getTempEvohA1() {
        return tempEvohA1;
    }
    
    public void setTempEvohA1(BigDecimal tempEvohA1) {
        this.tempEvohA1 = tempEvohA1;
    }
    
    public BigDecimal getTempEvohA2() {
        return tempEvohA2;
    }
    
    public void setTempEvohA2(BigDecimal tempEvohA2) {
        this.tempEvohA2 = tempEvohA2;
    }
    
    public BigDecimal getTempEvohH1() {
        return tempEvohH1;
    }
    
    public void setTempEvohH1(BigDecimal tempEvohH1) {
        this.tempEvohH1 = tempEvohH1;
    }
    
    public BigDecimal getTempEvohH2() {
        return tempEvohH2;
    }
    
    public void setTempEvohH2(BigDecimal tempEvohH2) {
        this.tempEvohH2 = tempEvohH2;
    }
    
    public BigDecimal getTempEvohH3() {
        return tempEvohH3;
    }
    
    public void setTempEvohH3(BigDecimal tempEvohH3) {
        this.tempEvohH3 = tempEvohH3;
    }
    
    public BigDecimal getTempVirginFb() {
        return tempVirginFb;
    }
    
    public void setTempVirginFb(BigDecimal tempVirginFb) {
        this.tempVirginFb = tempVirginFb;
    }
    
    public BigDecimal getTempVirginC1() {
        return tempVirginC1;
    }
    
    public void setTempVirginC1(BigDecimal tempVirginC1) {
        this.tempVirginC1 = tempVirginC1;
    }
    
    public BigDecimal getTempVirginC2() {
        return tempVirginC2;
    }
    
    public void setTempVirginC2(BigDecimal tempVirginC2) {
        this.tempVirginC2 = tempVirginC2;
    }
    
    public BigDecimal getTempVirginC3() {
        return tempVirginC3;
    }
    
    public void setTempVirginC3(BigDecimal tempVirginC3) {
        this.tempVirginC3 = tempVirginC3;
    }
    
    public BigDecimal getTempVirginA1() {
        return tempVirginA1;
    }
    
    public void setTempVirginA1(BigDecimal tempVirginA1) {
        this.tempVirginA1 = tempVirginA1;
    }
    
    public BigDecimal getTempVirginA2() {
        return tempVirginA2;
    }
    
    public void setTempVirginA2(BigDecimal tempVirginA2) {
        this.tempVirginA2 = tempVirginA2;
    }
    
    public BigDecimal getTempVirginA3() {
        return tempVirginA3;
    }
    
    public void setTempVirginA3(BigDecimal tempVirginA3) {
        this.tempVirginA3 = tempVirginA3;
    }
    
    public BigDecimal getTempVirginA4() {
        return tempVirginA4;
    }
    
    public void setTempVirginA4(BigDecimal tempVirginA4) {
        this.tempVirginA4 = tempVirginA4;
    }
    
    public BigDecimal getTempVirginA5() {
        return tempVirginA5;
    }
    
    public void setTempVirginA5(BigDecimal tempVirginA5) {
        this.tempVirginA5 = tempVirginA5;
    }

    // เพิ่ม getters/setters สำหรับ Head D fields (ตัวอย่างบางส่วน - ทำเต็มทั้ง 16 ตัว)
    public BigDecimal getTempHeadD1_1() {
        return tempHeadD1_1;
    }
    
    public void setTempHeadD1_1(BigDecimal tempHeadD1_1) {
        this.tempHeadD1_1 = tempHeadD1_1;
    }
    
    public BigDecimal getTempHeadD2_1() {
        return tempHeadD2_1;
    }
    
    public void setTempHeadD2_1(BigDecimal tempHeadD2_1) {
        this.tempHeadD2_1 = tempHeadD2_1;
    }
    
    public BigDecimal getTempHeadD3_1() {
        return tempHeadD3_1;
    }
    
    public void setTempHeadD3_1(BigDecimal tempHeadD3_1) {
        this.tempHeadD3_1 = tempHeadD3_1;
    }
    
    public BigDecimal getTempHeadD4_1() {
        return tempHeadD4_1;
    }
    
    public void setTempHeadD4_1(BigDecimal tempHeadD4_1) {
        this.tempHeadD4_1 = tempHeadD4_1;
    }
    
    public BigDecimal getTempHeadD1_2() {
        return tempHeadD1_2;
    }
    
    public void setTempHeadD1_2(BigDecimal tempHeadD1_2) {
        this.tempHeadD1_2 = tempHeadD1_2;
    }
    
    public BigDecimal getTempHeadD2_2() {
        return tempHeadD2_2;
    }
    
    public void setTempHeadD2_2(BigDecimal tempHeadD2_2) {
        this.tempHeadD2_2 = tempHeadD2_2;
    }
    
    public BigDecimal getTempHeadD3_2() {
        return tempHeadD3_2;
    }
    
    public void setTempHeadD3_2(BigDecimal tempHeadD3_2) {
        this.tempHeadD3_2 = tempHeadD3_2;
    }
    
    public BigDecimal getTempHeadD4_2() {
        return tempHeadD4_2;
    }
    
    public void setTempHeadD4_2(BigDecimal tempHeadD4_2) {
        this.tempHeadD4_2 = tempHeadD4_2;
    }
    
    public BigDecimal getTempHeadD1_3() {
        return tempHeadD1_3;
    }
    
    public void setTempHeadD1_3(BigDecimal tempHeadD1_3) {
        this.tempHeadD1_3 = tempHeadD1_3;
    }
    
    public BigDecimal getTempHeadD2_3() {
        return tempHeadD2_3;
    }
    
    public void setTempHeadD2_3(BigDecimal tempHeadD2_3) {
        this.tempHeadD2_3 = tempHeadD2_3;
    }
    
    public BigDecimal getTempHeadD3_3() {
        return tempHeadD3_3;
    }
    
    public void setTempHeadD3_3(BigDecimal tempHeadD3_3) {
        this.tempHeadD3_3 = tempHeadD3_3;
    }
    
    public BigDecimal getTempHeadD4_3() {
        return tempHeadD4_3;
    }
    
    public void setTempHeadD4_3(BigDecimal tempHeadD4_3) {
        this.tempHeadD4_3 = tempHeadD4_3;
    }
    
    public BigDecimal getTempHeadD1_4() {
        return tempHeadD1_4;
    }
    
    public void setTempHeadD1_4(BigDecimal tempHeadD1_4) {
        this.tempHeadD1_4 = tempHeadD1_4;
    }
    
    public BigDecimal getTempHeadD2_4() {
        return tempHeadD2_4;
    }
    
    public void setTempHeadD2_4(BigDecimal tempHeadD2_4) {
        this.tempHeadD2_4 = tempHeadD2_4;
    }
    
    public BigDecimal getTempHeadD3_4() {
        return tempHeadD3_4;
    }
    
    public void setTempHeadD3_4(BigDecimal tempHeadD3_4) {
        this.tempHeadD3_4 = tempHeadD3_4;
    }
    
    public BigDecimal getTempHeadD4_4() {
        return tempHeadD4_4;
    }
    
    public void setTempHeadD4_4(BigDecimal tempHeadD4_4) {
        this.tempHeadD4_4 = tempHeadD4_4;
    }

    // เพิ่ม getters/setters สำหรับ Head L fields
    public BigDecimal getTempHeadL1() {
        return tempHeadL1;
    }
    
    public void setTempHeadL1(BigDecimal tempHeadL1) {
        this.tempHeadL1 = tempHeadL1;
    }
    
    public BigDecimal getTempHeadL2() {
        return tempHeadL2;
    }
    
    public void setTempHeadL2(BigDecimal tempHeadL2) {
        this.tempHeadL2 = tempHeadL2;
    }
    
    public BigDecimal getTempHeadL3() {
        return tempHeadL3;
    }
    
    public void setTempHeadL3(BigDecimal tempHeadL3) {
        this.tempHeadL3 = tempHeadL3;
    }
    
    public BigDecimal getTempHeadL4() {
        return tempHeadL4;
    }
    
    public void setTempHeadL4(BigDecimal tempHeadL4) {
        this.tempHeadL4 = tempHeadL4;
    }

    // เพิ่ม getters/setters สำหรับ Other fields
    public BigDecimal getCycleTimeSec() {
        return cycleTimeSec;
    }
    
    public void setCycleTimeSec(BigDecimal cycleTimeSec) {
        this.cycleTimeSec = cycleTimeSec;
    }
    
    public BigDecimal getMoldTemp() {
        return moldTemp;
    }
    
    public void setMoldTemp(BigDecimal moldTemp) {
        this.moldTemp = moldTemp;
    }
    
    public BigDecimal getHighBlowMpa() {
        return highBlowMpa;
    }
    
    public void setHighBlowMpa(BigDecimal highBlowMpa) {
        this.highBlowMpa = highBlowMpa;
    }
    
    public BigDecimal getLowPressureMpa() {
        return lowPressureMpa;
    }
    
    public void setLowPressureMpa(BigDecimal lowPressureMpa) {
        this.lowPressureMpa = lowPressureMpa;
    }
    
    public BigDecimal getBlowRatio() {
        return blowRatio;
    }
    
    public void setBlowRatio(BigDecimal blowRatio) {
        this.blowRatio = blowRatio;
    }
    
    public BigDecimal getBlowAirCondition1() {
        return blowAirCondition1;
    }
    
    public void setBlowAirCondition1(BigDecimal blowAirCondition1) {
        this.blowAirCondition1 = blowAirCondition1;
    }
    
    public BigDecimal getBlowAirCondition2() {
        return blowAirCondition2;
    }
    
    public void setBlowAirCondition2(BigDecimal blowAirCondition2) {
        this.blowAirCondition2 = blowAirCondition2;
    }
    
    public BigDecimal getParisonAir1() {
        return parisonAir1;
    }
    
    public void setParisonAir1(BigDecimal parisonAir1) {
        this.parisonAir1 = parisonAir1;
    }
    
    public BigDecimal getParisonAir2() {
        return parisonAir2;
    }
    
    public void setParisonAir2(BigDecimal parisonAir2) {
        this.parisonAir2 = parisonAir2;
    }
    
    public BigDecimal getZeroValue() {
        return zeroValue;
    }
    
    public void setZeroValue(BigDecimal zeroValue) {
        this.zeroValue = zeroValue;
    }
    
    public BigDecimal getWeightValue() {
        return weightValue;
    }
    
    public void setWeightValue(BigDecimal weightValue) {
        this.weightValue = weightValue;
    }
    
    public BigDecimal getSpanValue() {
        return spanValue;
    }
    
    public void setSpanValue(BigDecimal spanValue) {
        this.spanValue = spanValue;
    }

    // เพิ่ม getters/setters สำหรับ Blow Pin Left fields
    public BigDecimal getBlowPinLeftPin() {
        return blowPinLeftPin;
    }
    
    public void setBlowPinLeftPin(BigDecimal blowPinLeftPin) {
        this.blowPinLeftPin = blowPinLeftPin;
    }
    
    public BigDecimal getBlowPinLeftFrontA() {
        return blowPinLeftFrontA;
    }
    
    public void setBlowPinLeftFrontA(BigDecimal blowPinLeftFrontA) {
        this.blowPinLeftFrontA = blowPinLeftFrontA;
    }
    
    public BigDecimal getBlowPinLeftFrontB() {
        return blowPinLeftFrontB;
    }
    
    public void setBlowPinLeftFrontB(BigDecimal blowPinLeftFrontB) {
        this.blowPinLeftFrontB = blowPinLeftFrontB;
    }
    
    public BigDecimal getBlowPinLeftFrontC() {
        return blowPinLeftFrontC;
    }
    
    public void setBlowPinLeftFrontC(BigDecimal blowPinLeftFrontC) {
        this.blowPinLeftFrontC = blowPinLeftFrontC;
    }
    
    public BigDecimal getBlowPinLeftFrontD() {
        return blowPinLeftFrontD;
    }
    
    public void setBlowPinLeftFrontD(BigDecimal blowPinLeftFrontD) {
        this.blowPinLeftFrontD = blowPinLeftFrontD;
    }
    
    public BigDecimal getBlowPinLeftFrontE() {
        return blowPinLeftFrontE;
    }
    
    public void setBlowPinLeftFrontE(BigDecimal blowPinLeftFrontE) {
        this.blowPinLeftFrontE = blowPinLeftFrontE;
    }
    
    public BigDecimal getBlowPinLeftFrontF() {
        return blowPinLeftFrontF;
    }
    
    public void setBlowPinLeftFrontF(BigDecimal blowPinLeftFrontF) {
        this.blowPinLeftFrontF = blowPinLeftFrontF;
    }

    // เพิ่ม getters/setters สำหรับ Blow Pin Right fields
    public BigDecimal getBlowPinRightPin() {
        return blowPinRightPin;
    }
    
    public void setBlowPinRightPin(BigDecimal blowPinRightPin) {
        this.blowPinRightPin = blowPinRightPin;
    }
    
    public BigDecimal getBlowPinRightFrontA() {
        return blowPinRightFrontA;
    }
    
    public void setBlowPinRightFrontA(BigDecimal blowPinRightFrontA) {
        this.blowPinRightFrontA = blowPinRightFrontA;
    }
    
    public BigDecimal getBlowPinRightFrontB() {
        return blowPinRightFrontB;
    }
    
    public void setBlowPinRightFrontB(BigDecimal blowPinRightFrontB) {
        this.blowPinRightFrontB = blowPinRightFrontB;
    }
    
    public BigDecimal getBlowPinRightFrontC() {
        return blowPinRightFrontC;
    }
    
    public void setBlowPinRightFrontC(BigDecimal blowPinRightFrontC) {
        this.blowPinRightFrontC = blowPinRightFrontC;
    }
    
    public BigDecimal getBlowPinRightFrontD() {
        return blowPinRightFrontD;
    }
    
    public void setBlowPinRightFrontD(BigDecimal blowPinRightFrontD) {
        this.blowPinRightFrontD = blowPinRightFrontD;
    }
    
    public BigDecimal getBlowPinRightFrontE() {
        return blowPinRightFrontE;
    }
    
    public void setBlowPinRightFrontE(BigDecimal blowPinRightFrontE) {
        this.blowPinRightFrontE = blowPinRightFrontE;
    }
    
    public BigDecimal getBlowPinRightFrontF() {
        return blowPinRightFrontF;
    }
    
    public void setBlowPinRightFrontF(BigDecimal blowPinRightFrontF) {
        this.blowPinRightFrontF = blowPinRightFrontF;
    }

    // เพิ่ม getters/setters สำหรับ Check fields ที่ขาดหายไป
    public String getCheckEmergencySwStatus() {
        return checkEmergencySwStatus;
    }

    public void setCheckEmergencySwStatus(String checkEmergencySwStatus) {
        this.checkEmergencySwStatus = checkEmergencySwStatus;
    }

    public Integer getCheckSqSsPieces() {
        return checkSqSsPieces;
    }

    public void setCheckSqSsPieces(Integer checkSqSsPieces) {
        this.checkSqSsPieces = checkSqSsPieces;
    }
    
    // Getters/Setters สำหรับ Check fields ตาม PRD
    public String getProductQualityCheck() {
        return productQualityCheck;
    }

    public void setProductQualityCheck(String productQualityCheck) {
        this.productQualityCheck = productQualityCheck;
    }

    public String getMachineOperationCheck() {
        return machineOperationCheck;
    }

    public void setMachineOperationCheck(String machineOperationCheck) {
        this.machineOperationCheck = machineOperationCheck;
    }

    public String getSafetyProcedureCheck() {
        return safetyProcedureCheck;
    }

    public void setSafetyProcedureCheck(String safetyProcedureCheck) {
        this.safetyProcedureCheck = safetyProcedureCheck;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }
}
