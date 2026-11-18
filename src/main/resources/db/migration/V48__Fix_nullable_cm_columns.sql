-- V39: Fix nullable columns in C-01 tables
-- shift_material_consumption: meter/computed columns should allow NULL (not yet recorded)
-- tank_refill: material_lot_code is optional

ALTER TABLE shift_material_consumption
    ALTER COLUMN starting_meter_kg DECIMAL(10,2) NULL;

ALTER TABLE shift_material_consumption
    ALTER COLUMN ending_meter_kg DECIMAL(10,2) NULL;

ALTER TABLE shift_material_consumption
    ALTER COLUMN computed_used_kg DECIMAL(10,2) NULL;

ALTER TABLE tank_refill
    ALTER COLUMN material_lot_code NVARCHAR(100) NULL;
