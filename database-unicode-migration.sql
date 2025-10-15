/*
SQL Server Unicode Hardening Script for GDTahara
- Convert key VARCHAR columns to NVARCHAR to preserve Thai text
- Apply Thai-friendly case-insensitive collation
- Safe to run multiple times (checks types before altering)

Run order: UAT first, verify, then PROD during maintenance window.
Backup recommendation:
  BACKUP DATABASE [GDTahara] TO DISK = N'C:\\backup\\GDTahara_pre_unicode.bak' WITH INIT, NAME = N'GDTahara pre-unicode';
*/

USE [GDTahara];
GO

-- Helper: change column to NVARCHAR with Thai_100 collation only if it's not already NVARCHAR
DECLARE @sql NVARCHAR(MAX) = N'';

-- Table: downtime_events.reason
IF EXISTS (
    SELECT 1 FROM sys.columns c
    JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.downtime_events') AND c.name = 'reason' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering downtime_events.reason to NVARCHAR(MAX) ...';
    SET @sql += N'ALTER TABLE dbo.downtime_events ALTER COLUMN reason NVARCHAR(MAX) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END

-- Table: machines.machine_name
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.machines') AND c.name = 'machine_name' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering machines.machine_name to NVARCHAR(255) ...';
    SET @sql += N'ALTER TABLE dbo.machines ALTER COLUMN machine_name NVARCHAR(255) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END

-- Table: machines.machine_code
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.machines') AND c.name = 'machine_code' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering machines.machine_code to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.machines ALTER COLUMN machine_code NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NOT NULL;\n';
END

-- Table: products.product_name
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.products') AND c.name = 'product_name' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering products.product_name to NVARCHAR(255) ...';
    SET @sql += N'ALTER TABLE dbo.products ALTER COLUMN product_name NVARCHAR(255) COLLATE Thai_100_CI_AI_SC_UTF8 NOT NULL;\n';
END

-- Table: products.product_code
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.products') AND c.name = 'product_code' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering products.product_code to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.products ALTER COLUMN product_code NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NOT NULL;\n';
END

-- Table: ng_types.ng_description_th
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.ng_types') AND c.name = 'ng_description_th' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering ng_types.ng_description_th to NVARCHAR(255) ...';
    SET @sql += N'ALTER TABLE dbo.ng_types ALTER COLUMN ng_description_th NVARCHAR(255) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END

-- Table: ng_types.ng_code
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.ng_types') AND c.name = 'ng_code' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering ng_types.ng_code to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.ng_types ALTER COLUMN ng_code NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NOT NULL;\n';
END

-- Table: production_reports.order_number
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.production_reports') AND c.name = 'order_number' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering production_reports.order_number to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.production_reports ALTER COLUMN order_number NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END

-- Table: users.username (for Thai names if any)
IF EXISTS (
    SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.users') AND c.name = 'username' AND t.name <> 'nvarchar'
)
BEGIN
    PRINT 'Altering users.username to NVARCHAR(255) ...';
    SET @sql += N'ALTER TABLE dbo.users ALTER COLUMN username NVARCHAR(255) COLLATE Thai_100_CI_AI_SC_UTF8 NOT NULL;\n';
END

-- Table: materials (names and types)
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id WHERE c.object_id = OBJECT_ID('dbo.materials') AND c.name = 'material_name' AND t.name <> 'nvarchar')
BEGIN
    PRINT 'Altering materials.material_name to NVARCHAR(255) ...';
    SET @sql += N'ALTER TABLE dbo.materials ALTER COLUMN material_name NVARCHAR(255) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id WHERE c.object_id = OBJECT_ID('dbo.materials') AND c.name = 'material_code' AND t.name <> 'nvarchar')
BEGIN
    PRINT 'Altering materials.material_code to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.materials ALTER COLUMN material_code NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id WHERE c.object_id = OBJECT_ID('dbo.materials') AND c.name = 'material_type' AND t.name <> 'nvarchar')
BEGIN
    PRINT 'Altering materials.material_type to NVARCHAR(100) ...';
    SET @sql += N'ALTER TABLE dbo.materials ALTER COLUMN material_type NVARCHAR(100) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id WHERE c.object_id = OBJECT_ID('dbo.materials') AND c.name = 'unit' AND t.name <> 'nvarchar')
BEGIN
    PRINT 'Altering materials.unit to NVARCHAR(50) ...';
    SET @sql += N'ALTER TABLE dbo.materials ALTER COLUMN unit NVARCHAR(50) COLLATE Thai_100_CI_AI_SC_UTF8 NULL;\n';
END

-- Execute accumulated ALTER statements
IF (@sql <> N'')
BEGIN
    PRINT 'Executing Unicode column alterations...';
    EXEC sp_executesql @sql;
    PRINT 'Unicode column alterations completed.';
END
ELSE
BEGIN
    PRINT 'All target columns are already NVARCHAR. No changes applied.';
END

-- Optional: set database default collation for new objects (requires downtime and plan)
-- ALTER DATABASE [GDTahara] COLLATE Thai_100_CI_AI_SC_UTF8;
GO

/* Quick verification: insert a Thai sample as NVARCHAR literal */
-- DECLARE @msg NVARCHAR(50) = N'ทดสอบภาษาไทย';
-- SELECT @msg AS thai_sample;
