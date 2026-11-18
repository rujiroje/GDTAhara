-- C-04 redesign: tank role for materials is derived from materialType, not a separate column
-- Must drop constraint and index before dropping the column (SQL Server requirement)
IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE name = 'CK_mat_tank_role' AND parent_object_id = OBJECT_ID('materials'))
    ALTER TABLE materials DROP CONSTRAINT CK_mat_tank_role;
GO

IF EXISTS (SELECT 1 FROM sys.indexes
           WHERE name = 'IX_mat_tank_role' AND object_id = OBJECT_ID('materials'))
    DROP INDEX IX_mat_tank_role ON materials;
GO

IF COL_LENGTH('materials', 'tank_role') IS NOT NULL
    ALTER TABLE materials DROP COLUMN tank_role;
GO
