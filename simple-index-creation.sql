-- =================================================================
-- Simple Database Index Creation Script
-- สร้าง Indexes สำคัญเพื่อปรับปรุงประสิทธิภาพ
-- =================================================================

-- สำหรับ parameter_records table (ตารางหลักที่ช้า)
-- Index 1: สำหรับ query by report_id และ created_at
CREATE NONCLUSTERED INDEX IX_parameter_records_report_created 
ON parameter_records (report_id, created_at DESC);

-- Index 2: สำหรับ query by technician_id
CREATE NONCLUSTERED INDEX IX_parameter_records_technician 
ON parameter_records (technician_id, created_at DESC);

-- Index 3: สำหรับ record_time filter
CREATE NONCLUSTERED INDEX IX_parameter_records_time 
ON parameter_records (record_time, created_at DESC);

-- สำหรับ production_reports table
-- Index 4: สำหรับ status และ date queries
CREATE NONCLUSTERED INDEX IX_production_reports_status_date 
ON production_reports (status, start_date, end_date);

-- สำหรับ material_stock_transactions table
-- Index 5: สำหรับ material และ lot number queries
CREATE NONCLUSTERED INDEX IX_material_stock_material_lot 
ON material_stock_transactions (material_id, lot_number, timestamp DESC);

-- สำหรับ ng_logs table
-- Index 6: สำหรับ report และ timestamp queries
CREATE NONCLUSTERED INDEX IX_ng_logs_report_time 
ON ng_logs (report_id, timestamp DESC);

-- Update Statistics
UPDATE STATISTICS parameter_records;
UPDATE STATISTICS production_reports;
UPDATE STATISTICS material_stock_transactions;
UPDATE STATISTICS ng_logs;

PRINT 'Database indexes created successfully!';
PRINT 'Performance should improve significantly for common queries.';