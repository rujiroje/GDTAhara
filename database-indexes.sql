-- Database Performance Optimization - ปรับปรุงประสิทธิภาพฐานข้อมูล
-- GDTahara Backend System
-- Date: 2025-09-20

USE [GD_PRODUCTION_DB];
GO

-- ตรวจสอบ indexes ที่มีอยู่แล้ว
SELECT 
    t.name AS 'Table',
    i.name AS 'Index',
    i.type_desc AS 'Type',
    i.is_unique AS 'IsUnique',
    STRING_AGG(c.name, ', ') AS 'Columns'
FROM sys.indexes i
INNER JOIN sys.tables t ON i.object_id = t.object_id
INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE t.name IN ('parameter_records', 'production_reports', 'users', 'machines', 'products')
GROUP BY t.name, i.name, i.type_desc, i.is_unique
ORDER BY t.name, i.name;
GO

-- 1. Indexes สำหรับตาราง parameter_records (ตารางหลักที่มี 60+ fields)
PRINT 'Creating indexes for parameter_records table...';

-- Index สำหรับ foreign keys ที่ใช้บ่อย
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_parameter_records_production_report_id')
CREATE NONCLUSTERED INDEX IX_parameter_records_production_report_id
ON parameter_records (production_report_id)
INCLUDE (technician_id, shift, work_date);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_parameter_records_technician_id')
CREATE NONCLUSTERED INDEX IX_parameter_records_technician_id
ON parameter_records (technician_id)
INCLUDE (production_report_id, work_date, shift);
GO

-- Index สำหรับการค้นหาตาม date range (ใช้บ่อยในรายงาน)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_parameter_records_work_date')
CREATE NONCLUSTERED INDEX IX_parameter_records_work_date
ON parameter_records (work_date DESC)
INCLUDE (technician_id, production_report_id, shift);
GO

-- Composite index สำหรับการค้นหาแบบซับซ้อน
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_parameter_records_composite')
CREATE NONCLUSTERED INDEX IX_parameter_records_composite
ON parameter_records (work_date, technician_id, production_report_id)
INCLUDE (shift, created_at, updated_at);
GO

-- 2. Indexes สำหรับตาราง production_reports
PRINT 'Creating indexes for production_reports table...';

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_machine_product')
CREATE NONCLUSTERED INDEX IX_production_reports_machine_product
ON production_reports (machine_id, product_id)
INCLUDE (shift, production_date, target_quantity, actual_quantity);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_date')
CREATE NONCLUSTERED INDEX IX_production_reports_date
ON production_reports (production_date DESC)
INCLUDE (machine_id, product_id, shift, actual_quantity);
GO

-- 3. Indexes สำหรับตาราง users (technicians)
PRINT 'Creating indexes for users table...';

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_username')
CREATE UNIQUE NONCLUSTERED INDEX IX_users_username
ON users (username)
INCLUDE (employee_id, first_name, last_name, role);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_employee_id')
CREATE UNIQUE NONCLUSTERED INDEX IX_users_employee_id
ON users (employee_id)
INCLUDE (username, first_name, last_name);
GO

-- 4. Statistics Update สำหรับประสิทธิภาพ
PRINT 'Updating statistics...';

UPDATE STATISTICS parameter_records;
UPDATE STATISTICS production_reports;
UPDATE STATISTICS users;
UPDATE STATISTICS machines;
UPDATE STATISTICS products;
GO

-- 5. ตรวจสอบการใช้งาน indexes
PRINT 'Index usage analysis:';

SELECT 
    OBJECT_NAME(s.object_id) AS 'Table',
    i.name AS 'Index',
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates,
    s.last_user_seek,
    s.last_user_scan
FROM sys.dm_db_index_usage_stats s
INNER JOIN sys.indexes i ON s.object_id = i.object_id AND s.index_id = i.index_id
WHERE OBJECT_NAME(s.object_id) IN ('parameter_records', 'production_reports', 'users', 'machines', 'products')
ORDER BY s.user_seeks + s.user_scans + s.user_lookups DESC;
GO

-- 6. Query Performance Monitoring
PRINT 'Performance monitoring setup complete.';
PRINT 'Indexes created for optimal database performance.';
PRINT 'Monitor query execution plans to validate improvements.';
GO