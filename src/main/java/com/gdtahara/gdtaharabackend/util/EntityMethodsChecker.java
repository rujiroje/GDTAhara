package com.gdtahara.gdtaharabackend.util;

import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
public class EntityMethodsChecker {
    
    public void printParameterRecordMethods() {
        System.out.println("=== ParameterRecord Available Methods ===");
        Method[] methods = ParameterRecord.class.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.getName().startsWith("set")) {
                System.out.println(method.getName() + "(" + 
                    java.util.Arrays.toString(method.getParameterTypes()) + ")");
            }
        }
        
        // ตรวจสอบเมธอดที่คาดหวัง
        try {
            // ลองหาเมธอดต่างๆ ที่อาจมี
            String[] possibleReportMethods = {"setReport", "setProductionReport", "setReportId"};
            String[] possibleTimeMethods = {"setTimestamp", "setCreatedAt", "setRecordedAt"};
            
            System.out.println("\n=== Checking possible report setter methods ===");
            for (String methodName : possibleReportMethods) {
                try {
                    ParameterRecord.class.getMethod(methodName, Object.class);
                    System.out.println("✅ Found: " + methodName);
                } catch (NoSuchMethodException e) {
                    System.out.println("❌ Not found: " + methodName);
                }
            }
            
            System.out.println("\n=== Checking possible timestamp setter methods ===");
            for (String methodName : possibleTimeMethods) {
                try {
                    ParameterRecord.class.getMethod(methodName, LocalDateTime.class);
                    System.out.println("✅ Found: " + methodName);
                } catch (NoSuchMethodException e) {
                    System.out.println("❌ Not found: " + methodName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error checking methods: " + e.getMessage());
        }
    }
}
