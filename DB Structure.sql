-- Quick Database Structure Check
-- Simple script to check what tables and columns actually exist

PRINT 'Quick Database Structure Check for GDTahara';
PRINT '============================================';
PRINT '';

-- Check if database exists and is accessible
PRINT 'Current Database: ' + DB_NAME();
PRINT '';

-- Show all tables
PRINT '=== ALL EXISTING TABLES ===';
SELECT TABLE_NAME as 'Table Name'
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;

PRINT '';
PRINT '=== MACHINES TABLE DETAILS ===';
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Machines')
BEGIN
    PRINT 'Machines table exists. Columns:';
    SELECT 
        ORDINAL_POSITION as 'Pos',
        COLUMN_NAME as 'Column Name',
        DATA_TYPE as 'Type',
        ISNULL(CAST(CHARACTER_MAXIMUM_LENGTH AS VARCHAR), 'N/A') as 'Length',
        IS_NULLABLE as 'Nullable'
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Machines'
    ORDER BY ORDINAL_POSITION;
    
    PRINT '';
    PRINT 'Sample data from Machines (first 3 rows):';
    SELECT TOP 3 * FROM Machines;
END
ELSE
BEGIN
    PRINT 'Machines table does NOT exist';
END

PRINT '';
PRINT '=== TB_USERNAME TABLE DETAILS ===';
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Tb_Username')
BEGIN
    PRINT 'Tb_Username table exists. Columns:';
    SELECT 
        ORDINAL_POSITION as 'Pos',
        COLUMN_NAME as 'Column Name',
        DATA_TYPE as 'Type',
        ISNULL(CAST(CHARACTER_MAXIMUM_LENGTH AS VARCHAR), 'N/A') as 'Length',
        IS_NULLABLE as 'Nullable'
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Tb_Username'
    ORDER BY ORDINAL_POSITION;
    
    PRINT '';
    PRINT 'Sample data from Tb_Username (first 3 rows):';
    SELECT TOP 3 * FROM Tb_Username;
END
ELSE
BEGIN
    PRINT 'Tb_Username table does NOT exist';
END

PRINT '';
PRINT '=== PRODUCTION_ORDERS TABLE DETAILS ===';
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Production_Orders')
BEGIN
    PRINT 'Production_Orders table exists. Columns:';
    SELECT 
        ORDINAL_POSITION as 'Pos',
        COLUMN_NAME as 'Column Name',
        DATA_TYPE as 'Type',
        ISNULL(CAST(CHARACTER_MAXIMUM_LENGTH AS VARCHAR), 'N/A') as 'Length',
        IS_NULLABLE as 'Nullable'
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Production_Orders'
    ORDER BY ORDINAL_POSITION;
END
ELSE
BEGIN
    PRINT 'Production_Orders table does NOT exist';
END

PRINT '';
PRINT '=== KEY COMPATIBILITY ISSUES ===';

-- Check specific columns that caused the insert error
DECLARE @MachineColumns TABLE (ColumnName VARCHAR(100));
DECLARE @ExpectedColumns TABLE (ColumnName VARCHAR(100));

-- Get actual Machines columns
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Machines')
BEGIN
    INSERT INTO @MachineColumns
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Machines';
END

-- Expected columns for insert
INSERT INTO @ExpectedColumns VALUES ('MachineCode'), ('MachineName'), ('LineNumber'), ('Status'), ('Capacity'), ('LastMaintenanceDate'), ('NextMaintenanceDate');

-- Show missing columns
PRINT 'Missing columns in Machines table:';
SELECT 'Missing: ' + e.ColumnName as 'Issue'
FROM @ExpectedColumns e
LEFT JOIN @MachineColumns m ON e.ColumnName = m.ColumnName
WHERE m.ColumnName IS NULL;

-- Show extra columns
PRINT '';
PRINT 'Actual columns in Machines table:';
SELECT 'Has: ' + m.ColumnName as 'Column'
FROM @MachineColumns m;
-- =================================================================
-- Parameter Records Table สำหรับระบบบันทึก Parameter Checklist
-- ตาม PRD: ระบบบันทึก Parameter Checklist สำหรับ Technician
-- =================================================================

-- Drop table if exists
IF EXISTS (SELECT * FROM sysobjects WHERE name='parameter_records' AND xtype='U')
    DROP TABLE parameter_records;

-- Create parameter_records table
CREATE TABLE parameter_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    record_time VARCHAR(10) NOT NULL, -- เช่น "10:00", "18:00", "02:00"
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    -- ส่วนที่ 1: Extruder Screw
    -- MAIN (EL3101)
    extruder_main_screw_rpm DECIMAL(10,2),
    extruder_main_lo_limit DECIMAL(10,2),
    extruder_main_resin_press DECIMAL(10,2),
    extruder_main_resin_temp DECIMAL(10,2),
    extruder_main_motor_current DECIMAL(10,2),
    
    -- ADMER (LO-Left)
    extruder_admer_screw_rpm DECIMAL(10,2),
    extruder_admer_lo_limit DECIMAL(10,2),
    extruder_admer_resin_press DECIMAL(10,2),
    extruder_admer_resin_temp DECIMAL(10,2),
    extruder_admer_motor_current DECIMAL(10,2),
    
    -- EVOH (LO-Right)
    extruder_evoh_screw_rpm DECIMAL(10,2),
    extruder_evoh_lo_limit DECIMAL(10,2),
    extruder_evoh_resin_press DECIMAL(10,2),
    extruder_evoh_resin_temp DECIMAL(10,2),
    extruder_evoh_motor_current DECIMAL(10,2),
    
    -- VIRGIN (UPL)
    extruder_virgin_screw_rpm DECIMAL(10,2),
    extruder_virgin_lo_limit DECIMAL(10,2),
    extruder_virgin_resin_press DECIMAL(10,2),
    extruder_virgin_resin_temp DECIMAL(10,2),
    extruder_virgin_motor_current DECIMAL(10,2),
    
    -- ส่วนที่ 2: Temperature (อุณหภูมิ) °C
    -- MAIN (FL)
    temp_main_fb DECIMAL(10,2),
    temp_main_c1 DECIMAL(10,2),
    temp_main_c2 DECIMAL(10,2),
    temp_main_c3 DECIMAL(10,2),
    temp_main_c4 DECIMAL(10,2),
    temp_main_c5 DECIMAL(10,2),
    temp_main_a1 DECIMAL(10,2),
    temp_main_a2 DECIMAL(10,2),
    temp_main_a3 DECIMAL(10,2),
    temp_main_a4 DECIMAL(10,2),
    
    -- ADMER (LO-Left)
    temp_admer_fb DECIMAL(10,2),
    temp_admer_c1 DECIMAL(10,2),
    temp_admer_c2 DECIMAL(10,2),
    temp_admer_c3 DECIMAL(10,2),
    temp_admer_a1 DECIMAL(10,2),
    temp_admer_a2 DECIMAL(10,2),
    temp_admer_h1 DECIMAL(10,2),
    temp_admer_h2 DECIMAL(10,2),
    temp_admer_h3 DECIMAL(10,2),
    
    -- EVOH (LO-Right)
    temp_evoh_fb DECIMAL(10,2),
    temp_evoh_c1 DECIMAL(10,2),
    temp_evoh_c2 DECIMAL(10,2),
    temp_evoh_c3 DECIMAL(10,2),
    temp_evoh_a1 DECIMAL(10,2),
    temp_evoh_a2 DECIMAL(10,2),
    temp_evoh_h1 DECIMAL(10,2),
    temp_evoh_h2 DECIMAL(10,2),
    temp_evoh_h3 DECIMAL(10,2),
    
    -- VIRGIN (UPL)
    temp_virgin_fb DECIMAL(10,2),
    temp_virgin_c1 DECIMAL(10,2),
    temp_virgin_c2 DECIMAL(10,2),
    temp_virgin_c3 DECIMAL(10,2),
    temp_virgin_a1 DECIMAL(10,2),
    temp_virgin_a2 DECIMAL(10,2),
    temp_virgin_a3 DECIMAL(10,2),
    temp_virgin_a4 DECIMAL(10,2),
    temp_virgin_a5 DECIMAL(10,2),
    
    -- HEAD
    temp_head_d1_1 DECIMAL(10,2),
    temp_head_d2_1 DECIMAL(10,2),
    temp_head_d3_1 DECIMAL(10,2),
    temp_head_d4_1 DECIMAL(10,2),
    temp_head_d1_2 DECIMAL(10,2),
    temp_head_d2_2 DECIMAL(10,2),
    temp_head_d3_2 DECIMAL(10,2),
    temp_head_d4_2 DECIMAL(10,2),
    temp_head_d1_3 DECIMAL(10,2),
    temp_head_d2_3 DECIMAL(10,2),
    temp_head_d3_3 DECIMAL(10,2),
    temp_head_d4_3 DECIMAL(10,2),
    temp_head_d1_4 DECIMAL(10,2),
    temp_head_d2_4 DECIMAL(10,2),
    temp_head_d3_4 DECIMAL(10,2),
    temp_head_d4_4 DECIMAL(10,2),
    temp_head_l1 DECIMAL(10,2),
    temp_head_l2 DECIMAL(10,2),
    temp_head_l3 DECIMAL(10,2),
    temp_head_l4 DECIMAL(10,2),
    
    -- ส่วนที่ 3: ค่าการทำงานอื่นๆ
    cycle_time_sec DECIMAL(10,2),
    mold_temp DECIMAL(10,2),
    high_blow_mpa DECIMAL(10,2),
    low_pressure_mpa DECIMAL(10,2),
    blow_ratio DECIMAL(10,2),
    blow_air_condition1 DECIMAL(10,2),
    blow_air_condition2 DECIMAL(10,2),
    parison_air1 DECIMAL(10,2),
    parison_air2 DECIMAL(10,2),
    zero_value DECIMAL(10,2),
    weight_value DECIMAL(10,2),
    span_value DECIMAL(10,2),
    
    -- Blow Pin PLATEN Left
    blow_pin_left_pin DECIMAL(10,2),
    blow_pin_left_front_a DECIMAL(10,2),
    blow_pin_left_front_b DECIMAL(10,2),
    blow_pin_left_front_c DECIMAL(10,2),
    blow_pin_left_front_d DECIMAL(10,2),
    blow_pin_left_front_e DECIMAL(10,2),
    blow_pin_left_front_f DECIMAL(10,2),
    
    -- Blow Pin PLATEN Right
    blow_pin_right_pin DECIMAL(10,2),
    blow_pin_right_front_a DECIMAL(10,2),
    blow_pin_right_front_b DECIMAL(10,2),
    blow_pin_right_front_c DECIMAL(10,2),
    blow_pin_right_front_d DECIMAL(10,2),
    blow_pin_right_front_e DECIMAL(10,2),
    blow_pin_right_front_f DECIMAL(10,2),
    
    -- ส่วนที่ 4: การตรวจสอบอื่นๆ
    emergency_sw_status VARCHAR(10), -- 'OK' หรือ 'NG'
    sq_ss_pieces INT, -- จำนวนชิ้นที่ตรวจสอบ
    
    -- Foreign keys
    CONSTRAINT FK_parameter_records_report 
        FOREIGN KEY (report_id) REFERENCES production_reports(id),
    CONSTRAINT FK_parameter_records_technician 
        FOREIGN KEY (technician_id) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX IX_parameter_records_report_id ON parameter_records (report_id);
CREATE INDEX IX_parameter_records_technician_id ON parameter_records (technician_id);
CREATE INDEX IX_parameter_records_record_time ON parameter_records (record_time);
CREATE INDEX IX_parameter_records_created_at ON parameter_records (created_at);

-- NOTE: Sample data insertion removed to avoid FK constraint issues
-- Data will be inserted through the application when actual reports exist

PRINT 'Parameter Records table created successfully with all PRD fields!';
PRINT '';
PRINT '=== SUMMARY ===';
PRINT 'This will help determine what needs to be fixed for data insertion to work.';
