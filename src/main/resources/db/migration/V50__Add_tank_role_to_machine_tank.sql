-- V50: C-04 BOM-Tank Role Linking — Step 2
-- Add tank_role column to machine_tank + insert BLEND tank (D) for all machines
-- All machines: A=RESIN_100, B=EVOH, C=ADMER, D=BLEND (confirmed 2026-11-17)
-- NOTE: column and D-tanks were pre-created manually in SSMS before app deploy
--       Guards below make this migration idempotent

IF COL_LENGTH('machine_tank','tank_role') IS NULL
BEGIN
    ALTER TABLE machine_tank
        ADD tank_role NVARCHAR(30) NULL
        CONSTRAINT CK_mt_tank_role
            CHECK (tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL);
END
GO

-- Insert BLEND (D) tanks for all 9 machines if not already present
INSERT INTO machine_tank (tank_id, machine_id, tank_code, tank_name, default_material_code, capacity_kg, physical_position, is_active, tank_role)
SELECT v.tank_id, v.machine_id, v.tank_code, v.tank_name, v.dmc, v.cap, v.pos, v.active, v.role
FROM (VALUES
    ('RBL201_D','RBL201','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL202_D','RBL202','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL203_D','RBL203','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL204_D','RBL204','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL205_D','RBL205','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL206_D','RBL206','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL207_D','RBL207','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL208_D','RBL208','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND'),
    ('RBL209_D','RBL209','D',N'ถังเปิดผสม','TBD',NULL,'D',1,'BLEND')
) AS v(tank_id, machine_id, tank_code, tank_name, dmc, cap, pos, active, role)
WHERE NOT EXISTS (SELECT 1 FROM machine_tank mt WHERE mt.tank_id = v.tank_id);
GO

-- Seed tank_role by tank_code (idempotent — only updates NULL rows)
UPDATE machine_tank SET tank_role = 'RESIN_100' WHERE tank_role IS NULL AND tank_code = 'A';
UPDATE machine_tank SET tank_role = 'EVOH'      WHERE tank_role IS NULL AND tank_code = 'B';
UPDATE machine_tank SET tank_role = 'ADMER'     WHERE tank_role IS NULL AND tank_code = 'C';
UPDATE machine_tank SET tank_role = 'BLEND'     WHERE tank_role IS NULL AND tank_code = 'D';
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_mt_tank_role' AND object_id = OBJECT_ID('machine_tank')
)
    CREATE INDEX IX_mt_tank_role ON machine_tank(tank_role) WHERE tank_role IS NOT NULL;
GO
