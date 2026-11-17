-- V49: C-04 BOM-Tank Role Linking — Step 1
-- Add tank_role column to materials table
-- 4 roles: RESIN_100 (Virgin/100%), EVOH, ADMER, BLEND (Regrind/บดคืน)
-- NOTE: column was pre-created manually in SSMS on 2026-11-17 before app deploy
--       Guards below make this migration idempotent

IF COL_LENGTH('materials','tank_role') IS NULL
BEGIN
    ALTER TABLE materials
        ADD tank_role NVARCHAR(30) NULL
        CONSTRAINT CK_mat_tank_role
            CHECK (tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_mat_tank_role' AND object_id = OBJECT_ID('materials')
)
    CREATE INDEX IX_mat_tank_role ON materials(tank_role) WHERE tank_role IS NOT NULL;
GO

-- Conservative auto-seed — only patterns with low false-positive risk
-- Admin tags remaining materials via Admin UI (Phase D of C-04)

UPDATE materials SET tank_role = 'EVOH'
WHERE tank_role IS NULL
  AND (material_name LIKE '%EVOH%' OR material_code LIKE '%EVOH%');

UPDATE materials SET tank_role = 'ADMER'
WHERE tank_role IS NULL
  AND (material_name LIKE '%Admer%' OR material_name LIKE '%ADMER%');

UPDATE materials SET tank_role = 'RESIN_100'
WHERE tank_role IS NULL
  AND (material_name LIKE '%Virgin%' OR material_name LIKE '%virgin%');

-- BLEND = regrind only (narrow pattern — avoid false positives from ผสมสี/Mixing chemicals)
UPDATE materials SET tank_role = 'BLEND'
WHERE tank_role IS NULL
  AND (
    material_name LIKE N'%บดคืน%'
    OR material_name LIKE N'%เม็ดบด%'
    OR material_name LIKE N'%Regrind%'
    OR material_name LIKE N'%regrind%'
    OR material_name LIKE N'%recycle%'
    OR material_name LIKE N'%Recycle%'
  );
GO
