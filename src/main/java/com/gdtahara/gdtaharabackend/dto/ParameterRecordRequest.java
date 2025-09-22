package com.gdtahara.gdtaharabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterRecordRequest {
    private String recordType;
    private ParameterData parameters;
    
    /**
     * ปรับปรุงเมธอด get เพื่อรองรับกรณีที่ object เป็น null
     */
    public ParameterData getParameters() {
        if (parameters == null) {
            parameters = new ParameterData();
        }
        return parameters;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterData {
        private ExtruderScrewDto extruderScrew;
        private TemperatureDto temperature;
        private OtherValuesDto otherValues;
        private OtherChecksDto otherChecks;
        
        public ExtruderScrewDto getExtruderScrew() {
            if (extruderScrew == null) {
                extruderScrew = new ExtruderScrewDto();
            }
            return extruderScrew;
        }
        
        public TemperatureDto getTemperature() {
            if (temperature == null) {
                temperature = new TemperatureDto();
            }
            return temperature;
        }
        
        public OtherValuesDto getOtherValues() {
            if (otherValues == null) {
                otherValues = new OtherValuesDto();
            }
            return otherValues;
        }
        
        public OtherChecksDto getOtherChecks() {
            if (otherChecks == null) {
                otherChecks = new OtherChecksDto();
            }
            return otherChecks;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtruderScrewDto {
        private Screw main;
        private Screw admer;
        private Screw evoh;
        private Screw virgin;
        
        // เพิ่ม null safety getter สำหรับทุก field
        public Screw getMain() {
            if (main == null) {
                main = new Screw();
            }
            return main;
        }
        
        public Screw getAdmer() {
            if (admer == null) {
                admer = new Screw();
            }
            return admer;
        }
        
        public Screw getEvoh() {
            if (evoh == null) {
                evoh = new Screw();
            }
            return evoh;
        }
        
        public Screw getVirgin() {
            if (virgin == null) {
                virgin = new Screw();
            }
            return virgin;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Screw {
        private Double screwRpm;
        private Double loLimit;
        private Double resinPress;
        private Double resinTemp;
        private Double motorCurrent;
    }
    
    // เพิ่ม nested class อื่นๆ ตามที่จำเป็น
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemperatureDto {
        private TempGroupMain main;
        private TempGroupAdmerEvoh admer;
        private TempGroupAdmerEvoh evoh;
        private TempGroupVirgin virgin;
        private HeadTemp head;
        
        // เพิ่ม null safety getter
        public TempGroupMain getMain() {
            if (main == null) {
                main = new TempGroupMain();
            }
            return main;
        }
        
        public TempGroupAdmerEvoh getAdmer() {
            if (admer == null) {
                admer = new TempGroupAdmerEvoh();
            }
            return admer;
        }
        
        public TempGroupAdmerEvoh getEvoh() {
            if (evoh == null) {
                evoh = new TempGroupAdmerEvoh();
            }
            return evoh;
        }
        
        public TempGroupVirgin getVirgin() {
            if (virgin == null) {
                virgin = new TempGroupVirgin();
            }
            return virgin;
        }
        
        public HeadTemp getHead() {
            if (head == null) {
                head = new HeadTemp();
            }
            return head;
        }
    }
    
    // ... เพิ่ม class อื่นๆ ตามที่จำเป็น ...
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempGroupMain {
        private Double fb;
        private Double c1;
        private Double c2;
        private Double c3;
        private Double c4;
        private Double c5;
        private Double a1;
        private Double a2;
        private Double a3;
        private Double a4;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempGroupAdmerEvoh {
        private Double fb;
        private Double c1;
        private Double c2;
        private Double c3;
        private Double a1;
        private Double a2;
        private Double h1;
        private Double h2;
        private Double h3;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempGroupVirgin {
        private Double fb;
        private Double c1;
        private Double c2;
        private Double c3;
        private Double a1;
        private Double a2;
        private Double a3;
        private Double a4;
        private Double a5;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeadTemp {
        private Double d1_1;
        private Double d2_1;
        private Double d3_1;
        private Double d4_1;
        private Double d1_2;
        private Double d2_2;
        private Double d3_2;
        private Double d4_2;
        private Double d1_3;
        private Double d2_3;
        private Double d3_3;
        private Double d4_3;
        private Double d1_4;
        private Double d2_4;
        private Double d3_4;
        private Double d4_4;
        private Double l1;
        private Double l2;
        private Double l3;
        private Double l4;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherValuesDto {
        private Double cycleTime;
        private Double moldTemp;
        private Double highBlow;
        private Double lowPressure;
        private Double blowRatio;
        private Double blowAirCondition1;
        private Double blowAirCondition2;
        private Double parisonAir1;
        private Double parisonAir2;
        private Double zero;
        private Double weight;
        private Double span;
        private Platen leftPlaten;
        private Platen rightPlaten;
        
        // เพิ่ม null safety getter
        public Platen getLeftPlaten() {
            if (leftPlaten == null) {
                leftPlaten = new Platen();
            }
            return leftPlaten;
        }
        
        public Platen getRightPlaten() {
            if (rightPlaten == null) {
                rightPlaten = new Platen();
            }
            return rightPlaten;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Platen {
        private Double blowPin;
        private Double frontA;
        private Double frontB;
        private Double frontC;
        private Double frontD;
        private Double frontE;
        private Double frontF;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherChecksDto {
        private Boolean emergencySw;
        private Boolean sqssCheck;
    }
}