-- BUG-05: Materials table uses varchar with SQL_Latin1_General_CP1_CI_AS collation,
-- which cannot store Thai Unicode characters (stored as '?').
-- Changing to nvarchar allows Unicode storage regardless of collation.
-- SQL Server requires dropping ALL dependent indexes/constraints before ALTER COLUMN.

-- 1. Drop unique constraint (Hibernate-generated; IF EXISTS in case prior run partially applied)
IF EXISTS (SELECT 1 FROM sys.key_constraints
           WHERE name = 'UK_8tqe18irmwnlkgfrc3mqa2pn'
             AND parent_object_id = OBJECT_ID('dbo.materials'))
    ALTER TABLE materials DROP CONSTRAINT UK_8tqe18irmwnlkgfrc3mqa2pn;

-- 2. Drop additional unique index on material_code (created by V9 migration)
IF EXISTS (SELECT 1 FROM sys.indexes
           WHERE name = 'IX_materials_code'
             AND object_id = OBJECT_ID('dbo.materials'))
    DROP INDEX IX_materials_code ON materials;

-- 3. Alter columns to nvarchar
ALTER TABLE materials ALTER COLUMN material_code  NVARCHAR(100) NOT NULL;
ALTER TABLE materials ALTER COLUMN material_name  NVARCHAR(255) NULL;
ALTER TABLE materials ALTER COLUMN material_type  NVARCHAR(50)  NULL;
ALTER TABLE materials ALTER COLUMN unit           NVARCHAR(20)  NULL;

-- 4. Re-add unique constraint on material_code
ALTER TABLE materials ADD CONSTRAINT UQ_materials_material_code UNIQUE (material_code);
