package com.gdtahara.gdtaharabackend.dto;

import java.math.BigDecimal;

@SuppressWarnings("unused") // Add this annotation to suppress warnings about unused fields
public class ParameterRecordDto {

    private ExtruderScrew extruderScrew;
    private Temperature temperature;
    private OtherValues otherValues;
    private OtherChecks otherChecks;

    
    public static class ExtruderScrew {
        private Screw main;
        private Screw admer;
        private Screw evoh;
        private Screw virgin;
    }

    
    public static class Screw {
        private BigDecimal screwRpm;
        private BigDecimal loLimit; // Changed from limitPress
        private BigDecimal resinPress;
        private BigDecimal motorCurrent; // Changed from current
        private BigDecimal resinTemp;
    }

    
    public static class Temperature {
        private TempGroup main;
        private TempGroup admer;
        private TempGroup evoh;
        private TempGroup virgin;
        private HeadTemp head;
    }

    
    public static class TempGroup {
        private BigDecimal fb;
        private BigDecimal c1;
        private BigDecimal c2;
        private BigDecimal c3;
        private BigDecimal a1;
        private BigDecimal a2;
        private BigDecimal a3;
        private BigDecimal a4;
    }

    
    public static class HeadTemp {
        private BigDecimal d1_1, d2_1, d3_1, d4_1;
        private BigDecimal d1_2, d2_2, d3_2, d4_2;
        private BigDecimal d1_3, d2_3, d3_3, d4_3;
        private BigDecimal d1_4, d2_4, d3_4, d4_4;
        private BigDecimal l1, l2, l3, l4;
    }

    
    public static class OtherValues {
        private BigDecimal cycleTime;
        private BigDecimal moldTemp;
        private BigDecimal highBlow;
        private BigDecimal lowPressure;
        private BigDecimal blowRate;
        // Removed Map<String, BigDecimal> blowPinPlaten;
        private BlowPinPlatenLeft blowPinPlatenLeft; // New field
        private BlowPinPlatenRight blowPinPlatenRight; // New field
        private BigDecimal blowAirCondition1; // New field
        private BigDecimal blowAirCondition2; // New field
        private BigDecimal parisonAir1; // New field
        private BigDecimal parisonAir2; // New field
        private BigDecimal zero; // New field
        private BigDecimal learnWeight;
        private BigDecimal span;
    }

    
    public static class BlowPinPlatenLeft { // New nested class
        private BigDecimal blowPin;
        private BigDecimal frontA;
        private BigDecimal frontB;
        private BigDecimal frontC;
        private BigDecimal frontD;
        private BigDecimal frontE;
        private BigDecimal frontF;
    }

    
    public static class BlowPinPlatenRight { // New nested class
        private BigDecimal blowPin;
        private BigDecimal frontA;
        private BigDecimal frontB;
        private BigDecimal frontC;
        private BigDecimal frontD;
        private BigDecimal frontE;
        private BigDecimal frontF;
    }

    
    public static class OtherChecks {
        private String emergencySw;
        // Removed Integer sqssCheck;
        private Integer sq; // New field
        private Integer ss; // New field
    }
}