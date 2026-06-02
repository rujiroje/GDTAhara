-- Add label-specific fields to products table for packaging box label printing
-- Guards prevent failure if columns were added manually before this migration runs.
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'customer_code')
    ALTER TABLE products ADD customer_code NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'label_variant')
    ALTER TABLE products ADD label_variant NVARCHAR(200) NULL;
