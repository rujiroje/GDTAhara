-- V50: C-04 BOM-Tank Role Linking — Step 2
-- Add tank_role column to machine_tank table
-- All machines use: A=RESIN_100, B=EVOH, C=ADMER, D=BLEND (confirmed by user)

ALTER TABLE machine_tank
    ADD tank_role NVARCHAR(30) NULL
    CONSTRAINT CK_mt_tank_role
        CHECK (tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL);
GO

-- Seed by tank_code — same layout across all machines
UPDATE machine_tank SET tank_role = 'RESIN_100' WHERE tank_code = 'A';
UPDATE machine_tank SET tank_role = 'EVOH'      WHERE tank_code = 'B';
UPDATE machine_tank SET tank_role = 'ADMER'     WHERE tank_code = 'C';
UPDATE machine_tank SET tank_role = 'BLEND'     WHERE tank_code = 'D';
GO

CREATE INDEX IX_mt_tank_role ON machine_tank(tank_role) WHERE tank_role IS NOT NULL;
GO
