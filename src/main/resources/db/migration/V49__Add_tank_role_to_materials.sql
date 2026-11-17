-- V49: C-04 BOM-Tank Role Linking — Step 1
-- Add tank_role column to materials table
-- 4 roles: RESIN_100 (Virgin), EVOH, ADMER, BLEND (Mix/บดผสม)

ALTER TABLE materials
    ADD tank_role NVARCHAR(30) NULL
    CONSTRAINT CK_mat_tank_role
        CHECK (tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL);
GO

CREATE INDEX IX_mat_tank_role ON materials(tank_role) WHERE tank_role IS NOT NULL;
GO

-- Auto-seed from material_name patterns
-- Admin MUST review untagged materials after this migration
-- via GET /admin/materials?tankRole=untagged

UPDATE materials
SET tank_role = 'EVOH'
WHERE tank_role IS NULL
  AND (material_name LIKE '%EVOH%' OR material_code LIKE '%EVOH%');

UPDATE materials
SET tank_role = 'ADMER'
WHERE tank_role IS NULL
  AND (material_name LIKE '%Admer%' OR material_name LIKE '%ADMER%');

UPDATE materials
SET tank_role = 'RESIN_100'
WHERE tank_role IS NULL
  AND (material_name LIKE '%Virgin%' OR material_name LIKE '%virgin%');

UPDATE materials
SET tank_role = 'BLEND'
WHERE tank_role IS NULL
  AND (
    material_name LIKE '%Mix%'
    OR material_name LIKE N'%บด%'
    OR material_name LIKE N'%ผสม%'
  );
GO
