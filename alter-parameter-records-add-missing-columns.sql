-- Safe migration script to align parameter_records schema with the Java entity
-- This script will add missing columns only if they do not already exist.
-- Run on your SQL Server database used by the application.

IF COL_LENGTH('parameter_records', 'product_quality_check') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD product_quality_check NVARCHAR(50) NULL;
END
GO

IF COL_LENGTH('parameter_records', 'machine_operation_check') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD machine_operation_check NVARCHAR(50) NULL;
END
GO

IF COL_LENGTH('parameter_records', 'safety_procedure_check') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD safety_procedure_check NVARCHAR(50) NULL;
END
GO

IF COL_LENGTH('parameter_records', 'additional_notes') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD additional_notes NVARCHAR(1000) NULL;
END
GO

PRINT 'parameter_records schema is aligned with ParameterRecord entity.';