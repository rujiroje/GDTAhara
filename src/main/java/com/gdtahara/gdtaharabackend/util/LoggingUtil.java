package com.gdtahara.gdtaharabackend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class LoggingUtil {
    private static final Logger logger = LoggerFactory.getLogger(LoggingUtil.class);
    
    /**
     * บันทึก log สำหรับผลลัพธ์การค้นหา
     */
    public static void logSearchResult(String searchType, List<?> results) {
        if (results == null) {
            logger.warn("ค้นหา {} ได้ผลลัพธ์เป็น null", searchType);
            return;
        }
        
        logger.info("ค้นหา {} พบข้อมูลทั้งหมด {} รายการ", searchType, results.size());
        if (results.isEmpty()) {
            logger.info("ไม่พบข้อมูล {} ตามเงื่อนไขที่ระบุ", searchType);
        } else if (results.get(0) != null) {
            logger.info("ตัวอย่างข้อมูลแรก: {}", results.get(0));
        }
    }
    
    /**
     * บันทึก log สำหรับค่าพารามิเตอร์ที่ใช้ในการค้นหา
     */
    public static void logSearchParameters(String methodName, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("เรียกใช้เมธอด ").append(methodName).append(" ด้วยพารามิเตอร์: ");
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) sb.append(", ");
            if (i + 1 < params.length) {
                sb.append(params[i]).append("=").append(params[i + 1]);
            }
        }
        logger.info(sb.toString());
    }
    
    /**
     * บันทึก log สำหรับข้อผิดพลาด
     */
    public static void logError(String message, Exception e) {
        logger.error("เกิดข้อผิดพลาด: {} - {}", message, e.getMessage(), e);
    }
    
    /**
     * บันทึก log HTTP request
     */
    public static void logHttpRequest(String method, String endpoint, String params) {
        logger.info("HTTP Request: {} {} - Params: {}", method, endpoint, params);
    }
    
    /**
     * บันทึก log สำหรับ API Response เพื่อตรวจสอบข้อมูลที่ส่งกลับ
     */
    public static void logApiResponse(String endpoint, Object response, int statusCode) {
        logger.info("API Response from {}: Status={}, Body={}", endpoint, statusCode, response);
        
        // ถ้า response เป็น List ว่างเปล่า
        if (response instanceof List && ((List<?>)response).isEmpty()) {
            logger.warn("Response จาก {} เป็น List ว่างเปล่า", endpoint);
        }
        // ถ้า response เป็น null
        else if (response == null) {
            logger.warn("Response จาก {} เป็น null", endpoint);
        }
    }
}
