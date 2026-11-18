-- C-04 redesign: tank role for materials is derived from materialType, not a separate column
-- Must drop the index before dropping the column (SQL Server blocks DROP COLUMN with dependent index)
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_mat_tank_role' AND object_id = OBJECT_ID('materials'))
    DROP INDEX IX_mat_tank_role ON materials;
ALTER TABLE materials DROP COLUMN tank_role;
