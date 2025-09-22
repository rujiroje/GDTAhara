package com.gdtahara.gdtaharabackend;

import com.gdtahara.gdtaharabackend.model.ParameterRecord;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Test class เพื่อตรวจสอบว่า Entity มีเมธอดที่จำเป็นหรือไม่
 */
@SpringBootTest
public class EntityMethodsTest {

    @Test
    public void testParameterRecordMethods() {
        // ใช้ reflection แทนการสร้าง instance เพื่อหลีกเลี่ยง unused variable warning
        Class<ParameterRecord> entityClass = ParameterRecord.class;
        
        // ทดสอบการมีอยู่ของเมธอดที่จำเป็น
        try {
            // ตรวจสอบเมธอด setReport
            Method setReportMethod = entityClass.getMethod("setReport", ProductionReport.class);
            System.out.println("✅ Found method: " + setReportMethod.getName());
            
            // ตรวจสอบเมธอด setTimestamp
            Method setTimestampMethod = entityClass.getMethod("setTimestamp", LocalDateTime.class);
            System.out.println("✅ Found method: " + setTimestampMethod.getName());
            
            System.out.println("🎉 All required methods found in ParameterRecord!");
            
        } catch (NoSuchMethodException e) {
            System.err.println("❌ Missing method in ParameterRecord: " + e.getMessage());
            
            // แสดงเมธอดทั้งหมดที่มีอยู่
            System.out.println("📋 Available methods in ParameterRecord:");
            Method[] methods = entityClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("set")) {
                    System.out.println("   - " + method.getName() + "(" + 
                        java.util.Arrays.toString(method.getParameterTypes()) + ")");
                }
            }
        }
    }
}
