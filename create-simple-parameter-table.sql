-- Simple Parameter Records Table Creation
-- Run this in SQL Server Management Studio or sqlcmd

USE GDTahara;

-- Drop table if exists
IF OBJECT_ID('parameter_records', 'U') IS NOT NULL 
    DROP TABLE parameter_records;

-- Create table with essential fields only (can add more later)
CREATE TABLE parameter_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    record_time DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    
    -- Essential parameter fields
    extruder_evoh_screw_rpm DECIMAL(10,2),
    extruder_virgin_screw_rpm DECIMAL(10,2),
    temp_evoh_fb DECIMAL(10,2),
    temp_virgin_fb DECIMAL(10,2),
    cycle_time_sec DECIMAL(10,2),
    mold_temp DECIMAL(10,2),
    
    -- Check fields
    product_quality_check NVARCHAR(50),
    machine_operation_check NVARCHAR(50),
    safety_procedure_check NVARCHAR(50),
    additional_notes NVARCHAR(MAX)
);

-- Add indexes
CREATE INDEX IX_parameter_records_report_id ON parameter_records (report_id);
CREATE INDEX IX_parameter_records_technician_id ON parameter_records (technician_id);
CREATE INDEX IX_parameter_records_record_time ON parameter_records (record_time);

PRINT 'Essential Parameter Records table created successfully!';