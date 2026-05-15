package com.gdtahara.gdtaharabackend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 🔥 PERFORMANCE CONFIGURATION
 * Cache Configuration สำหรับเพิ่มประสิทธิภาพระบบ
 * ลดการ query ฐานข้อมูลซ้ำๆ สำหรับข้อมูลที่ไม่เปลี่ยนแปลงบ่อย
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * กำหนด cache manager สำหรับ in-memory caching
     * เหมาะสำหรับข้อมูลที่ไม่เปลี่ยนแปลงบ่อย เช่น machines, products
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // กำหนด cache names ที่ต้องการใช้
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "machines",     // Cache สำหรับข้อมูลเครื่องจักร
            "products",     // Cache สำหรับข้อมูลผลิตภัณฑ์
            "users",        // Cache สำหรับข้อมูล users/technicians  
            "reports"       // Cache สำหรับ summary reports
        ));
        
        // เปิด dynamic cache creation
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}