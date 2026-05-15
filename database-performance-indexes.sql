-- =================================================================
-- GD Tahara Performance Optimization Script
-- สร้าง indexes เพิ่มเติมเพื่อเพิ่มประสิทธิภาพการค้นหาข้อมูล
-- =================================================================

USE GDTahara;
GO

-- 🔥 PERFORMANCE INDEXES: Production Reports
-- Index สำหรับการค้นหาตาม status (ใช้บ่อยใน dashboard)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_status')
CREATE INDEX IX_production_reports_status 
ON production_reports(status);
GO

-- Index สำหรับการค้นหาตาม machine_id (ใช้ใน production control)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_machine_id')
CREATE INDEX IX_production_reports_machine_id 
ON production_reports(machine_id);
GO

-- Index สำหรับการค้นหาตาม product_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_product_id')
CREATE INDEX IX_production_reports_product_id 
ON production_reports(product_id);
GO

-- Composite index สำหรับการค้นหาตาม status + date range
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_production_reports_status_dates')
CREATE INDEX IX_production_reports_status_dates 
ON production_reports(status, start_date, end_date);
GO

-- 🔥 PERFORMANCE INDEXES: Packaging Logs
-- Index สำหรับการค้นหาตาม report_id (ใช้ใน report summary)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_packaging_logs_report_id')
CREATE INDEX IX_packaging_logs_report_id 
ON packaging_logs(report_id);
GO

-- Composite index สำหรับการค้นหาตาม report_id + timestamp
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_packaging_logs_report_timestamp')
CREATE INDEX IX_packaging_logs_report_timestamp 
ON packaging_logs(report_id, timestamp);
GO

-- 🔥 PERFORMANCE INDEXES: NG Logs
-- Index สำหรับการค้นหาตาม report_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ng_logs_report_id')
CREATE INDEX IX_ng_logs_report_id 
ON ng_logs(report_id);
GO

-- 🔥 PERFORMANCE INDEXES: Material Usage Logs
-- Index สำหรับการค้นหาตาม report_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_material_usage_logs_report_id')
CREATE INDEX IX_material_usage_logs_report_id 
ON material_usage_logs(report_id);
GO

-- 🔥 PERFORMANCE INDEXES: Users
-- Index สำหรับการค้นหาตาม username (ใช้ใน authentication)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_username')
CREATE UNIQUE INDEX IX_users_username 
ON users(username);
GO

-- Index สำหรับการค้นหาตาม role
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_users_role')
CREATE INDEX IX_users_role 
ON users(role);
GO

-- 🔥 PERFORMANCE INDEXES: Materials
-- Index สำหรับการค้นหาตาม material_code
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_materials_code')
CREATE UNIQUE INDEX IX_materials_code 
ON materials(material_code);
GO

-- 🔥 PERFORMANCE STATISTICS
-- แสดงข้อมูลการใช้งาน indexes
SELECT 
    i.name AS IndexName,
    t.name AS TableName,
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates
FROM sys.indexes i
JOIN sys.tables t ON i.object_id = t.object_id
LEFT JOIN sys.dm_db_index_usage_stats s ON i.object_id = s.object_id AND i.index_id = s.index_id
WHERE t.name IN ('production_reports', 'packaging_logs', 'ng_logs', 'material_usage_logs', 'users', 'materials', 'parameter_records')
ORDER BY t.name, i.name;

PRINT '✅ Performance indexes created successfully!';
PRINT '📊 Database ready for high-performance queries';
GO
