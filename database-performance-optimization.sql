-- =================================================================
-- DATABASE PERFORMANCE OPTIMIZATION FOR GDTAHARA SYSTEM
-- การปรับปรุงประสิทธิภาพฐานข้อมูลระบบ GDTahara
-- =================================================================

-- ===============================
-- PERFORMANCE ANALYSIS SUMMARY
-- ===============================

/*
🔍 IDENTIFIED PERFORMANCE ISSUES:

1. **parameter_records Table (60+ columns)**
   - ตาราง parameter_records มี 60+ columns ทำให้ SELECT * ช้า
   - ไม่มี composite indexes สำหรับ query patterns ที่ใช้บ่อย
   - DECIMAL(10,2) ใช้พื้นที่เยอะและประมวลผลช้า

2. **Missing Indexes**
   - ไม่มี composite index สำหรับ (report_id, created_at)
   - ไม่มี index สำหรับ record_time filter
   - Foreign key relationships ไม่ได้ optimize

3. **JPA N+1 Query Problem**
   - Repository queries ไม่ใช้ JOIN FETCH
   - Lazy loading ทำให้เกิด multiple queries

4. **Database Connection Pool**
   - ไม่ได้ configure connection pool settings
   - Default timeout อาจจะยาวเกินไป

5. **Large Data Transfer**
   - ส่งข้อมูลทั้งหมด 60+ fields แม้ใช้เพียงบางส่วน
   - ไม่มี pagination ใน parameter records
*/

-- ===============================
-- SOLUTION 1: OPTIMIZE INDEXES
-- ===============================

-- เพิ่ม Composite Indexes สำหรับ query patterns ที่ใช้บ่อย
USE GDTahara;

-- 1. Composite index สำหรับ parameter_records (query หลัก)
CREATE NONCLUSTERED INDEX IX_parameter_records_report_time 
ON parameter_records (report_id, created_at DESC, record_time);

-- 2. Index สำหรับ filter by technician และ date range
CREATE NONCLUSTERED INDEX IX_parameter_records_technician_date 
ON parameter_records (technician_id, created_at DESC);

-- 3. Covering index สำหรับ summary queries (เก็บข้อมูลสำคัญ)
CREATE NONCLUSTERED INDEX IX_parameter_records_summary 
ON parameter_records (report_id, record_time, created_at)
INCLUDE (technician_id, extruder_main_screw_rpm, temp_main_fb, cycle_time_sec);

-- 4. Index สำหรับ production_reports (JOIN optimization)
CREATE NONCLUSTERED INDEX IX_production_reports_status_date 
ON production_reports (status, start_date, end_date);

-- 5. Index สำหรับ material stock queries
CREATE NONCLUSTERED INDEX IX_material_stock_material_lot 
ON material_stock_transactions (material_id, lot_number, timestamp DESC);

-- 6. Index สำหรับ ng_logs performance
CREATE NONCLUSTERED INDEX IX_ng_logs_report_timestamp 
ON ng_logs (report_id, timestamp DESC);

-- ===============================
-- SOLUTION 2: TABLE PARTITIONING (สำหรับข้อมูลเยอะ)
-- ===============================

-- ถ้าข้อมูล parameter_records เยอะมาก สามารถทำ partitioning ตาม created_at
-- (ใช้เมื่อมีข้อมูลมากกว่า 1 ล้าน records)

/*
-- Partition Function (แบ่งตามเดือน)
CREATE PARTITION FUNCTION pf_parameter_records_monthly (DATETIME2)
AS RANGE RIGHT FOR VALUES (
    '2025-01-01', '2025-02-01', '2025-03-01', '2025-04-01',
    '2025-05-01', '2025-06-01', '2025-07-01', '2025-08-01',
    '2025-09-01', '2025-10-01', '2025-11-01', '2025-12-01'
);

-- Partition Scheme
CREATE PARTITION SCHEME ps_parameter_records_monthly
AS PARTITION pf_parameter_records_monthly
ALL TO ([PRIMARY]);
*/

-- ===============================
-- SOLUTION 3: OPTIMIZE DATA TYPES
-- ===============================

-- สำหรับ parameter values ที่มี precision จำกัด ให้ใช้ FLOAT แทน DECIMAL เพื่อ performance
-- (ถ้า precision ไม่สำคัญมาก)

-- ตัวอย่าง: แปลง temperature fields เป็น FLOAT
/*
ALTER TABLE parameter_records 
ALTER COLUMN temp_main_fb FLOAT;

ALTER TABLE parameter_records 
ALTER COLUMN temp_virgin_fb FLOAT;
*/

-- ===============================
-- SOLUTION 4: CREATE SUMMARY TABLES
-- ===============================

-- สร้างตาราง summary สำหรับข้อมูลที่ query บ่อย
CREATE TABLE parameter_summary (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    record_count INT DEFAULT 0,
    avg_cycle_time DECIMAL(10,2),
    avg_mold_temp DECIMAL(10,2),
    avg_main_screw_rpm DECIMAL(10,2),
    last_updated DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_parameter_summary_report 
        FOREIGN KEY (report_id) REFERENCES production_reports(id)
);

-- Index สำหรับ summary table
CREATE UNIQUE INDEX IX_parameter_summary_report_date 
ON parameter_summary (report_id, record_date);

-- ===============================
-- SOLUTION 5: STATISTICS UPDATE
-- ===============================

-- อัพเดท table statistics เพื่อให้ query optimizer ทำงานได้ดีขึ้น
UPDATE STATISTICS parameter_records;
UPDATE STATISTICS production_reports;
UPDATE STATISTICS material_stock_transactions;
UPDATE STATISTICS ng_logs;

-- ===============================
-- SOLUTION 6: QUERY OPTIMIZATION HINTS
-- ===============================

-- ตัวอย่าง optimized queries ที่ควรใช้ใน Repository

-- แทนที่ SELECT * ให้ระบุ columns ที่ต้องการ
/*
-- ❌ Slow query
SELECT * FROM parameter_records WHERE report_id = 1 ORDER BY created_at DESC;

-- ✅ Optimized query
SELECT id, report_id, record_time, created_at, 
       extruder_main_screw_rpm, temp_main_fb, cycle_time_sec
FROM parameter_records 
WHERE report_id = 1 
ORDER BY created_at DESC;
*/

-- ===============================
-- SOLUTION 7: CONNECTION POOLING
-- ===============================

/*
เพิ่มใน application.properties:

# HikariCP Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.leak-detection-threshold=60000

# JPA Performance Settings
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
*/

-- ===============================
-- PERFORMANCE MONITORING QUERIES
-- ===============================

-- 1. ตรวจสอบ index usage
SELECT 
    i.name AS index_name,
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates
FROM sys.indexes i
JOIN sys.dm_db_index_usage_stats s ON i.object_id = s.object_id AND i.index_id = s.index_id
WHERE OBJECT_NAME(i.object_id) = 'parameter_records'
ORDER BY s.user_seeks + s.user_scans + s.user_lookups DESC;

-- 2. ตรวจสอบ slow queries
SELECT 
    total_elapsed_time,
    total_worker_time,
    execution_count,
    SUBSTRING(st.text, (qs.statement_start_offset/2) + 1,
        ((CASE statement_end_offset 
            WHEN -1 THEN DATALENGTH(st.text)
            ELSE qs.statement_end_offset END 
            - qs.statement_start_offset)/2) + 1) AS statement_text
FROM sys.dm_exec_query_stats AS qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) as st
WHERE st.text LIKE '%parameter_records%'
ORDER BY total_elapsed_time DESC;

-- 3. ตรวจสอบ table sizes
SELECT 
    t.name AS TableName,
    p.rows AS RowCounts,
    SUM(a.total_pages) * 8 AS TotalSpaceKB,
    SUM(a.used_pages) * 8 AS UsedSpaceKB
FROM sys.tables t
INNER JOIN sys.indexes i ON t.object_id = i.object_id
INNER JOIN sys.partitions p ON i.object_id = p.object_id AND i.index_id = p.index_id
INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id
WHERE t.name IN ('parameter_records', 'production_reports', 'material_stock_transactions')
GROUP BY t.name, p.rows
ORDER BY TotalSpaceKB DESC;

PRINT '=== DATABASE OPTIMIZATION COMPLETED ===';
PRINT 'Next Steps:';
PRINT '1. Update JPA Repository methods to use optimized queries';
PRINT '2. Add connection pool configuration to application.properties';
PRINT '3. Implement pagination for large datasets';
PRINT '4. Monitor query performance using the provided monitoring queries';