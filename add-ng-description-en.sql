-- Add English description column for NG types
-- SQL Server syntax
IF COL_LENGTH('ng_types', 'ng_description_en') IS NULL
BEGIN
    ALTER TABLE ng_types ADD ng_description_en NVARCHAR(255) NULL;
END

-- Optional: seed English descriptions by copying from Thai initially (edit as needed)
-- UPDATE ng_types SET ng_description_en = ng_description_th WHERE ng_description_en IS NULL;
