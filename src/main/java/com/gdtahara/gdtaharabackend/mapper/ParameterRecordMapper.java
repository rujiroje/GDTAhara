package com.gdtahara.gdtaharabackend.mapper;

import com.gdtahara.gdtaharabackend.dto.ParameterRecordRequest;
import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import java.math.BigDecimal;

/**
 * Mapper สำหรับแปลงข้อมูลระหว่าง ParameterRecord Entity และ DTO
 * 
 * Class นี้ถูกเรียกใช้โดย TechnicianService ในส่วนของการจัดการ parameter records
 * เนื่องจาก TechnicianService มีหน้าที่ในการบันทึกและดึงข้อมูล parameter โดยใช้ mapper นี้
 * ในการแปลงข้อมูลระหว่าง Entity และ DTO
 * 
 * ปัญหา type mismatch เกิดจาก DTO ใช้ Double แต่ Entity ใช้ BigDecimal
 * จึงจำเป็นต้องมีการแปลงค่าระหว่างกัน
 */

public class ParameterRecordMapper {

    /**
     * Utility method to convert Double to BigDecimal safely
     */
    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    /**
     * Utility method to convert BigDecimal to Double safely
     */
    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    // Helpers for otherChecks mapping differences between FE and DB
    private static String normalizeEmergency(String val) {
        if (val == null) return null;
        String v = val.trim();
        if (v.equalsIgnoreCase("true")) return "OK";
        if (v.equalsIgnoreCase("false")) return "NG";
        return v; // expect 'OK' or 'NG'
    }

    public static void mapDtoToEntity(ParameterRecordRequest.ParameterData dto, ParameterRecord entity) {
        if (dto == null) return;

        // Section 1: Extruder Screw
        if (dto.getExtruderScrew() != null) {
            if (dto.getExtruderScrew().getMain() != null) {
                entity.setExtruderMainScrewRpm(toBigDecimal(dto.getExtruderScrew().getMain().getScrewRpm()));
                entity.setExtruderMainLoLimit(toBigDecimal(dto.getExtruderScrew().getMain().getLoLimit()));
                entity.setExtruderMainResinPress(toBigDecimal(dto.getExtruderScrew().getMain().getResinPress()));
                entity.setExtruderMainResinTemp(toBigDecimal(dto.getExtruderScrew().getMain().getResinTemp()));
                entity.setExtruderMainMotorCurrent(toBigDecimal(dto.getExtruderScrew().getMain().getMotorCurrent()));
            }
            if (dto.getExtruderScrew().getAdmer() != null) {
                entity.setExtruderAdmerScrewRpm(toBigDecimal(dto.getExtruderScrew().getAdmer().getScrewRpm()));
                entity.setExtruderAdmerLoLimit(toBigDecimal(dto.getExtruderScrew().getAdmer().getLoLimit()));
                entity.setExtruderAdmerResinPress(toBigDecimal(dto.getExtruderScrew().getAdmer().getResinPress()));
                entity.setExtruderAdmerResinTemp(toBigDecimal(dto.getExtruderScrew().getAdmer().getResinTemp()));
                entity.setExtruderAdmerMotorCurrent(toBigDecimal(dto.getExtruderScrew().getAdmer().getMotorCurrent()));
            }
            if (dto.getExtruderScrew().getEvoh() != null) {
                entity.setExtruderEvohScrewRpm(toBigDecimal(dto.getExtruderScrew().getEvoh().getScrewRpm()));
                entity.setExtruderEvohLoLimit(toBigDecimal(dto.getExtruderScrew().getEvoh().getLoLimit()));
                entity.setExtruderEvohResinPress(toBigDecimal(dto.getExtruderScrew().getEvoh().getResinPress()));
                entity.setExtruderEvohResinTemp(toBigDecimal(dto.getExtruderScrew().getEvoh().getResinTemp()));
                entity.setExtruderEvohMotorCurrent(toBigDecimal(dto.getExtruderScrew().getEvoh().getMotorCurrent()));
            }
            if (dto.getExtruderScrew().getVirgin() != null) {
                entity.setExtruderVirginScrewRpm(toBigDecimal(dto.getExtruderScrew().getVirgin().getScrewRpm()));
                entity.setExtruderVirginLoLimit(toBigDecimal(dto.getExtruderScrew().getVirgin().getLoLimit()));
                entity.setExtruderVirginResinPress(toBigDecimal(dto.getExtruderScrew().getVirgin().getResinPress()));
                entity.setExtruderVirginResinTemp(toBigDecimal(dto.getExtruderScrew().getVirgin().getResinTemp()));
                entity.setExtruderVirginMotorCurrent(toBigDecimal(dto.getExtruderScrew().getVirgin().getMotorCurrent()));
            }
        }

        // Section 2: Temperature
        if (dto.getTemperature() != null) {
            if (dto.getTemperature().getMain() != null) {
                entity.setTempMainFb(toBigDecimal(dto.getTemperature().getMain().getFb()));
                entity.setTempMainC1(toBigDecimal(dto.getTemperature().getMain().getC1()));
                entity.setTempMainC2(toBigDecimal(dto.getTemperature().getMain().getC2()));
                entity.setTempMainC3(toBigDecimal(dto.getTemperature().getMain().getC3()));
                entity.setTempMainC4(toBigDecimal(dto.getTemperature().getMain().getC4()));
                entity.setTempMainC5(toBigDecimal(dto.getTemperature().getMain().getC5()));
                entity.setTempMainA1(toBigDecimal(dto.getTemperature().getMain().getA1()));
                entity.setTempMainA2(toBigDecimal(dto.getTemperature().getMain().getA2()));
                entity.setTempMainA3(toBigDecimal(dto.getTemperature().getMain().getA3()));
                entity.setTempMainA4(toBigDecimal(dto.getTemperature().getMain().getA4()));
            }
            if (dto.getTemperature().getAdmer() != null) {
                entity.setTempAdmerFb(toBigDecimal(dto.getTemperature().getAdmer().getFb()));
                entity.setTempAdmerC1(toBigDecimal(dto.getTemperature().getAdmer().getC1()));
                entity.setTempAdmerC2(toBigDecimal(dto.getTemperature().getAdmer().getC2()));
                entity.setTempAdmerC3(toBigDecimal(dto.getTemperature().getAdmer().getC3()));
                entity.setTempAdmerA1(toBigDecimal(dto.getTemperature().getAdmer().getA1()));
                entity.setTempAdmerA2(toBigDecimal(dto.getTemperature().getAdmer().getA2()));
                entity.setTempAdmerH1(toBigDecimal(dto.getTemperature().getAdmer().getH1()));
                entity.setTempAdmerH2(toBigDecimal(dto.getTemperature().getAdmer().getH2()));
                entity.setTempAdmerH3(toBigDecimal(dto.getTemperature().getAdmer().getH3()));
            }
            if (dto.getTemperature().getEvoh() != null) {
                entity.setTempEvohFb(toBigDecimal(dto.getTemperature().getEvoh().getFb()));
                entity.setTempEvohC1(toBigDecimal(dto.getTemperature().getEvoh().getC1()));
                entity.setTempEvohC2(toBigDecimal(dto.getTemperature().getEvoh().getC2()));
                entity.setTempEvohC3(toBigDecimal(dto.getTemperature().getEvoh().getC3()));
                entity.setTempEvohA1(toBigDecimal(dto.getTemperature().getEvoh().getA1()));
                entity.setTempEvohA2(toBigDecimal(dto.getTemperature().getEvoh().getA2()));
                entity.setTempEvohH1(toBigDecimal(dto.getTemperature().getEvoh().getH1()));
                entity.setTempEvohH2(toBigDecimal(dto.getTemperature().getEvoh().getH2()));
                entity.setTempEvohH3(toBigDecimal(dto.getTemperature().getEvoh().getH3()));
            }
            if (dto.getTemperature().getVirgin() != null) {
                entity.setTempVirginFb(toBigDecimal(dto.getTemperature().getVirgin().getFb()));
                entity.setTempVirginC1(toBigDecimal(dto.getTemperature().getVirgin().getC1()));
                entity.setTempVirginC2(toBigDecimal(dto.getTemperature().getVirgin().getC2()));
                entity.setTempVirginC3(toBigDecimal(dto.getTemperature().getVirgin().getC3()));
                entity.setTempVirginA1(toBigDecimal(dto.getTemperature().getVirgin().getA1()));
                entity.setTempVirginA2(toBigDecimal(dto.getTemperature().getVirgin().getA2()));
                entity.setTempVirginA3(toBigDecimal(dto.getTemperature().getVirgin().getA3()));
                entity.setTempVirginA4(toBigDecimal(dto.getTemperature().getVirgin().getA4()));
                entity.setTempVirginA5(toBigDecimal(dto.getTemperature().getVirgin().getA5()));
            }
            if (dto.getTemperature().getHead() != null) {
                entity.setTempHeadD1_1(toBigDecimal(dto.getTemperature().getHead().getD1_1()));
                entity.setTempHeadD2_1(toBigDecimal(dto.getTemperature().getHead().getD2_1()));
                entity.setTempHeadD3_1(toBigDecimal(dto.getTemperature().getHead().getD3_1()));
                entity.setTempHeadD4_1(toBigDecimal(dto.getTemperature().getHead().getD4_1()));
                entity.setTempHeadD1_2(toBigDecimal(dto.getTemperature().getHead().getD1_2()));
                entity.setTempHeadD2_2(toBigDecimal(dto.getTemperature().getHead().getD2_2()));
                entity.setTempHeadD3_2(toBigDecimal(dto.getTemperature().getHead().getD3_2()));
                entity.setTempHeadD4_2(toBigDecimal(dto.getTemperature().getHead().getD4_2()));
                entity.setTempHeadD1_3(toBigDecimal(dto.getTemperature().getHead().getD1_3()));
                entity.setTempHeadD2_3(toBigDecimal(dto.getTemperature().getHead().getD2_3()));
                entity.setTempHeadD3_3(toBigDecimal(dto.getTemperature().getHead().getD3_3()));
                entity.setTempHeadD4_3(toBigDecimal(dto.getTemperature().getHead().getD4_3()));
                entity.setTempHeadD1_4(toBigDecimal(dto.getTemperature().getHead().getD1_4()));
                entity.setTempHeadD2_4(toBigDecimal(dto.getTemperature().getHead().getD2_4()));
                entity.setTempHeadD3_4(toBigDecimal(dto.getTemperature().getHead().getD3_4()));
                entity.setTempHeadD4_4(toBigDecimal(dto.getTemperature().getHead().getD4_4()));
                entity.setTempHeadL1(toBigDecimal(dto.getTemperature().getHead().getL1()));
                entity.setTempHeadL2(toBigDecimal(dto.getTemperature().getHead().getL2()));
                entity.setTempHeadL3(toBigDecimal(dto.getTemperature().getHead().getL3()));
                entity.setTempHeadL4(toBigDecimal(dto.getTemperature().getHead().getL4()));
            }
        }

        // Section 3: Other Operating Values
        if (dto.getOtherValues() != null) {
            entity.setCycleTimeSec(toBigDecimal(dto.getOtherValues().getCycleTime()));
            entity.setMoldTemp(toBigDecimal(dto.getOtherValues().getMoldTemp()));
            entity.setHighBlowMpa(toBigDecimal(dto.getOtherValues().getHighBlow()));
            entity.setLowPressureMpa(toBigDecimal(dto.getOtherValues().getLowPressure()));
            entity.setBlowRatio(toBigDecimal(dto.getOtherValues().getBlowRatio()));
            entity.setBlowAirCondition1(toBigDecimal(dto.getOtherValues().getBlowAirCondition1()));
            entity.setBlowAirCondition2(toBigDecimal(dto.getOtherValues().getBlowAirCondition2()));
            entity.setParisonAir1(toBigDecimal(dto.getOtherValues().getParisonAir1()));
            entity.setParisonAir2(toBigDecimal(dto.getOtherValues().getParisonAir2()));
            entity.setZeroValue(toBigDecimal(dto.getOtherValues().getZero()));
            entity.setWeightValue(toBigDecimal(dto.getOtherValues().getWeight()));
            entity.setSpanValue(toBigDecimal(dto.getOtherValues().getSpan()));

            if (dto.getOtherValues().getLeftPlaten() != null) {
                entity.setBlowPinLeftPin(toBigDecimal(dto.getOtherValues().getLeftPlaten().getBlowPin()));
                entity.setBlowPinLeftFrontA(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontA()));
                entity.setBlowPinLeftFrontB(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontB()));
                entity.setBlowPinLeftFrontC(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontC()));
                entity.setBlowPinLeftFrontD(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontD()));
                entity.setBlowPinLeftFrontE(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontE()));
                entity.setBlowPinLeftFrontF(toBigDecimal(dto.getOtherValues().getLeftPlaten().getFrontF()));
            }
            if (dto.getOtherValues().getRightPlaten() != null) {
                entity.setBlowPinRightPin(toBigDecimal(dto.getOtherValues().getRightPlaten().getBlowPin()));
                entity.setBlowPinRightFrontA(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontA()));
                entity.setBlowPinRightFrontB(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontB()));
                entity.setBlowPinRightFrontC(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontC()));
                entity.setBlowPinRightFrontD(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontD()));
                entity.setBlowPinRightFrontE(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontE()));
                entity.setBlowPinRightFrontF(toBigDecimal(dto.getOtherValues().getRightPlaten().getFrontF()));
            }
        }

        // Section 4: Other Checks
        if (dto.getOtherChecks() != null) {
            // Frontend sends emergencySw as 'OK'/'NG' or boolean-like string, persist as string
            entity.setCheckEmergencySwStatus(normalizeEmergency(dto.getOtherChecks().getEmergencySw()));
            // Prefer numeric SQ/SS if provided; also store separate counts
            Integer sq = dto.getOtherChecks().getSqPieces();
            if (sq == null) sq = dto.getOtherChecks().getSq();
            Integer ss = dto.getOtherChecks().getSsPieces();
            if (ss == null) ss = dto.getOtherChecks().getSs();
            if (sq != null || ss != null) {
                int total = (sq != null ? sq : 0) + (ss != null ? ss : 0);
                entity.setCheckSqSsPieces(total);
                entity.setSqPieces(sq);
                entity.setSsPieces(ss);
            } else {
                Boolean flag = dto.getOtherChecks().getSqssCheck();
                entity.setCheckSqSsPieces(flag != null ? (flag ? 1 : 0) : null);
            }
        }
    }

    public static ParameterRecordRequest.ParameterData mapEntityToDto(ParameterRecord entity) {
        if (entity == null) return null;

        ParameterRecordRequest.ParameterData data = new ParameterRecordRequest.ParameterData();

        // Extruder Screw - ใช้ toDouble สำหรับทุกการแปลง BigDecimal
        ParameterRecordRequest.ExtruderScrewDto extruderDto = new ParameterRecordRequest.ExtruderScrewDto();
        extruderDto.setMain(new ParameterRecordRequest.Screw());
        extruderDto.getMain().setScrewRpm(toDouble(entity.getExtruderMainScrewRpm()));
        extruderDto.getMain().setLoLimit(toDouble(entity.getExtruderMainLoLimit()));
        extruderDto.getMain().setResinPress(toDouble(entity.getExtruderMainResinPress()));
        extruderDto.getMain().setResinTemp(toDouble(entity.getExtruderMainResinTemp()));
        extruderDto.getMain().setMotorCurrent(toDouble(entity.getExtruderMainMotorCurrent()));

        extruderDto.setAdmer(new ParameterRecordRequest.Screw());
        extruderDto.getAdmer().setScrewRpm(toDouble(entity.getExtruderAdmerScrewRpm()));
        extruderDto.getAdmer().setLoLimit(toDouble(entity.getExtruderAdmerLoLimit()));
        extruderDto.getAdmer().setResinPress(toDouble(entity.getExtruderAdmerResinPress()));
        extruderDto.getAdmer().setResinTemp(toDouble(entity.getExtruderAdmerResinTemp()));
        extruderDto.getAdmer().setMotorCurrent(toDouble(entity.getExtruderAdmerMotorCurrent()));

        extruderDto.setEvoh(new ParameterRecordRequest.Screw());
        extruderDto.getEvoh().setScrewRpm(toDouble(entity.getExtruderEvohScrewRpm()));
        extruderDto.getEvoh().setLoLimit(toDouble(entity.getExtruderEvohLoLimit()));
        extruderDto.getEvoh().setResinPress(toDouble(entity.getExtruderEvohResinPress()));
        extruderDto.getEvoh().setResinTemp(toDouble(entity.getExtruderEvohResinTemp()));
        extruderDto.getEvoh().setMotorCurrent(toDouble(entity.getExtruderEvohMotorCurrent()));

        extruderDto.setVirgin(new ParameterRecordRequest.Screw());
        extruderDto.getVirgin().setScrewRpm(toDouble(entity.getExtruderVirginScrewRpm()));
        extruderDto.getVirgin().setLoLimit(toDouble(entity.getExtruderVirginLoLimit()));
        extruderDto.getVirgin().setResinPress(toDouble(entity.getExtruderVirginResinPress()));
        extruderDto.getVirgin().setResinTemp(toDouble(entity.getExtruderVirginResinTemp()));
        extruderDto.getVirgin().setMotorCurrent(toDouble(entity.getExtruderVirginMotorCurrent()));
        data.setExtruderScrew(extruderDto);

        // Temperature - ใช้ toDouble สำหรับทุกการแปลง BigDecimal
        ParameterRecordRequest.TemperatureDto tempDto = new ParameterRecordRequest.TemperatureDto();
        tempDto.setMain(new ParameterRecordRequest.TempGroupMain());
        tempDto.getMain().setFb(toDouble(entity.getTempMainFb()));
        tempDto.getMain().setC1(toDouble(entity.getTempMainC1()));
        tempDto.getMain().setC2(toDouble(entity.getTempMainC2()));
        tempDto.getMain().setC3(toDouble(entity.getTempMainC3()));
        tempDto.getMain().setC4(toDouble(entity.getTempMainC4()));
        tempDto.getMain().setC5(toDouble(entity.getTempMainC5()));
        tempDto.getMain().setA1(toDouble(entity.getTempMainA1()));
        tempDto.getMain().setA2(toDouble(entity.getTempMainA2()));
        tempDto.getMain().setA3(toDouble(entity.getTempMainA3()));
        tempDto.getMain().setA4(toDouble(entity.getTempMainA4()));

        // แก้ไขการกำหนดค่าทั้งหมดให้ใช้ toDouble
        tempDto.setAdmer(new ParameterRecordRequest.TempGroupAdmerEvoh());
        tempDto.getAdmer().setFb(toDouble(entity.getTempAdmerFb()));
        tempDto.getAdmer().setC1(toDouble(entity.getTempAdmerC1()));
        tempDto.getAdmer().setC2(toDouble(entity.getTempAdmerC2()));
        tempDto.getAdmer().setC3(toDouble(entity.getTempAdmerC3()));
        tempDto.getAdmer().setA1(toDouble(entity.getTempAdmerA1()));
        tempDto.getAdmer().setA2(toDouble(entity.getTempAdmerA2()));
        tempDto.getAdmer().setH1(toDouble(entity.getTempAdmerH1()));
        tempDto.getAdmer().setH2(toDouble(entity.getTempAdmerH2()));
        tempDto.getAdmer().setH3(toDouble(entity.getTempAdmerH3()));

        tempDto.setEvoh(new ParameterRecordRequest.TempGroupAdmerEvoh());
        tempDto.getEvoh().setFb(toDouble(entity.getTempEvohFb()));
        tempDto.getEvoh().setC1(toDouble(entity.getTempEvohC1()));
        tempDto.getEvoh().setC2(toDouble(entity.getTempEvohC2()));
        tempDto.getEvoh().setC3(toDouble(entity.getTempEvohC3()));
        tempDto.getEvoh().setA1(toDouble(entity.getTempEvohA1()));
        tempDto.getEvoh().setA2(toDouble(entity.getTempEvohA2()));
        tempDto.getEvoh().setH1(toDouble(entity.getTempEvohH1()));
        tempDto.getEvoh().setH2(toDouble(entity.getTempEvohH2()));
        tempDto.getEvoh().setH3(toDouble(entity.getTempEvohH3()));

        tempDto.setVirgin(new ParameterRecordRequest.TempGroupVirgin());
        tempDto.getVirgin().setFb(toDouble(entity.getTempVirginFb()));
        tempDto.getVirgin().setC1(toDouble(entity.getTempVirginC1()));
        tempDto.getVirgin().setC2(toDouble(entity.getTempVirginC2()));
        tempDto.getVirgin().setC3(toDouble(entity.getTempVirginC3()));
        tempDto.getVirgin().setA1(toDouble(entity.getTempVirginA1()));
        tempDto.getVirgin().setA2(toDouble(entity.getTempVirginA2()));
        tempDto.getVirgin().setA3(toDouble(entity.getTempVirginA3()));
        tempDto.getVirgin().setA4(toDouble(entity.getTempVirginA4()));
        tempDto.getVirgin().setA5(toDouble(entity.getTempVirginA5()));

        tempDto.setHead(new ParameterRecordRequest.HeadTemp());
        tempDto.getHead().setD1_1(toDouble(entity.getTempHeadD1_1()));
        tempDto.getHead().setD2_1(toDouble(entity.getTempHeadD2_1()));
        tempDto.getHead().setD3_1(toDouble(entity.getTempHeadD3_1()));
        tempDto.getHead().setD4_1(toDouble(entity.getTempHeadD4_1()));
        tempDto.getHead().setD1_2(toDouble(entity.getTempHeadD1_2()));
        tempDto.getHead().setD2_2(toDouble(entity.getTempHeadD2_2()));
        tempDto.getHead().setD3_2(toDouble(entity.getTempHeadD3_2()));
        tempDto.getHead().setD4_2(toDouble(entity.getTempHeadD4_2()));
        tempDto.getHead().setD1_3(toDouble(entity.getTempHeadD1_3()));
        tempDto.getHead().setD2_3(toDouble(entity.getTempHeadD2_3()));
        tempDto.getHead().setD3_3(toDouble(entity.getTempHeadD3_3()));
        tempDto.getHead().setD4_3(toDouble(entity.getTempHeadD4_3()));
        tempDto.getHead().setD1_4(toDouble(entity.getTempHeadD1_4()));
        tempDto.getHead().setD2_4(toDouble(entity.getTempHeadD2_4()));
        tempDto.getHead().setD3_4(toDouble(entity.getTempHeadD3_4()));
        tempDto.getHead().setD4_4(toDouble(entity.getTempHeadD4_4()));
        tempDto.getHead().setL1(toDouble(entity.getTempHeadL1()));
        tempDto.getHead().setL2(toDouble(entity.getTempHeadL2()));
        tempDto.getHead().setL3(toDouble(entity.getTempHeadL3()));
        tempDto.getHead().setL4(toDouble(entity.getTempHeadL4()));
        data.setTemperature(tempDto);

        // Other Values - ใช้ toDouble สำหรับทุกการแปลง BigDecimal
        ParameterRecordRequest.OtherValuesDto otherDto = new ParameterRecordRequest.OtherValuesDto();
        otherDto.setCycleTime(toDouble(entity.getCycleTimeSec()));
        otherDto.setMoldTemp(toDouble(entity.getMoldTemp()));
        otherDto.setHighBlow(toDouble(entity.getHighBlowMpa()));
        otherDto.setLowPressure(toDouble(entity.getLowPressureMpa()));
        otherDto.setBlowRatio(toDouble(entity.getBlowRatio()));
    otherDto.setBlowAirCondition1(toDouble(entity.getBlowAirCondition1()));
    otherDto.setBlowAirCondition2(toDouble(entity.getBlowAirCondition2()));
        otherDto.setParisonAir1(toDouble(entity.getParisonAir1()));
        otherDto.setParisonAir2(toDouble(entity.getParisonAir2()));
        otherDto.setZero(toDouble(entity.getZeroValue()));
    otherDto.setWeight(toDouble(entity.getWeightValue()));
        otherDto.setSpan(toDouble(entity.getSpanValue()));

        otherDto.setLeftPlaten(new ParameterRecordRequest.Platen());
        otherDto.getLeftPlaten().setBlowPin(toDouble(entity.getBlowPinLeftPin()));
        otherDto.getLeftPlaten().setFrontA(toDouble(entity.getBlowPinLeftFrontA()));
        otherDto.getLeftPlaten().setFrontB(toDouble(entity.getBlowPinLeftFrontB()));
        otherDto.getLeftPlaten().setFrontC(toDouble(entity.getBlowPinLeftFrontC()));
        otherDto.getLeftPlaten().setFrontD(toDouble(entity.getBlowPinLeftFrontD()));
        otherDto.getLeftPlaten().setFrontE(toDouble(entity.getBlowPinLeftFrontE()));
        otherDto.getLeftPlaten().setFrontF(toDouble(entity.getBlowPinLeftFrontF()));

        otherDto.setRightPlaten(new ParameterRecordRequest.Platen());
        otherDto.getRightPlaten().setBlowPin(toDouble(entity.getBlowPinRightPin()));
        otherDto.getRightPlaten().setFrontA(toDouble(entity.getBlowPinRightFrontA()));
        otherDto.getRightPlaten().setFrontB(toDouble(entity.getBlowPinRightFrontB()));
        otherDto.getRightPlaten().setFrontC(toDouble(entity.getBlowPinRightFrontC()));
        otherDto.getRightPlaten().setFrontD(toDouble(entity.getBlowPinRightFrontD()));
        otherDto.getRightPlaten().setFrontE(toDouble(entity.getBlowPinRightFrontE()));
        otherDto.getRightPlaten().setFrontF(toDouble(entity.getBlowPinRightFrontF()));
        // Also set alias fields expected by some FE forms
        otherDto.setBlowPinPlatenLeft(otherDto.getLeftPlaten());
        otherDto.setBlowPinPlatenRight(otherDto.getRightPlaten());
        // Aliases for primitive fields used by some FE inputs
        otherDto.setBlowRate(otherDto.getBlowRatio());
        otherDto.setLearnWeight(otherDto.getWeight());
        data.setOtherValues(otherDto);

        // Other Checks - map back to DTO types
        ParameterRecordRequest.OtherChecksDto checksDto = new ParameterRecordRequest.OtherChecksDto();
        // Expose emergencySw as string 'OK'/'NG' to match FE
        if (entity.getCheckEmergencySwStatus() != null) {
            String v = entity.getCheckEmergencySwStatus();
            if ("OK".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v)) checksDto.setEmergencySw("OK");
            else if ("NG".equalsIgnoreCase(v) || "FALSE".equalsIgnoreCase(v)) checksDto.setEmergencySw("NG");
            else checksDto.setEmergencySw(null);
        }
        if (entity.getCheckSqSsPieces() != null) {
            // legacy flag: true when total > 0
            checksDto.setSqssCheck(entity.getCheckSqSsPieces() > 0);
        }
    // return separate counts when available
    checksDto.setSqPieces(entity.getSqPieces());
    checksDto.setSsPieces(entity.getSsPieces());
    // Set aliases used by some FE forms
    checksDto.setSq(entity.getSqPieces());
    checksDto.setSs(entity.getSsPieces());
        data.setOtherChecks(checksDto);

        return data;
    }
    
    public static ParameterRecordRequest mapEntityToRequestDto(ParameterRecord entity) {
        if (entity == null) return null;
        ParameterRecordRequest requestDto = new ParameterRecordRequest();
        // Note: recordType field ไม่มีใน ParameterRecord model ตอนนี้
        // requestDto.setRecordType(entity.getRecordType());
        // The ParameterData needs to have the ID of the record for updates.
        requestDto.setParameters(mapEntityToDto(entity));
        return requestDto;
    }
}
