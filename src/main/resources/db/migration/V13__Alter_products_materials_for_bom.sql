-- W12.5a: Add BOM-related columns to existing tables

-- products: add SAP metadata columns
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'material_group')
    ALTER TABLE products ADD material_group NVARCHAR(50) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'alternative_number')
    ALTER TABLE products ADD alternative_number INT NULL CONSTRAINT df_prod_alt DEFAULT 1;
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'sap_material_type')
    ALTER TABLE products ADD sap_material_type NVARCHAR(10) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'plant_code')
    ALTER TABLE products ADD plant_code NVARCHAR(20) NULL;
GO

-- materials: add SAP type + plant
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'materials' AND COLUMN_NAME = 'material_type')
    ALTER TABLE materials ADD material_type NVARCHAR(10) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'materials' AND COLUMN_NAME = 'plant_code')
    ALTER TABLE materials ADD plant_code NVARCHAR(20) NULL;
GO

-- material_stock_transactions: add bom_item_id column + FK (nullable — historical rows unaffected)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_NAME = 'material_stock_transactions' AND COLUMN_NAME = 'bom_item_id')
    ALTER TABLE material_stock_transactions
        ADD bom_item_id BIGINT NULL
            CONSTRAINT fk_mst_bom_item FOREIGN KEY REFERENCES bom_item(id);
GO

-- Filtered index must be in a separate batch from the ALTER TABLE above
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'idx_mst_bom_item'
                 AND object_id = OBJECT_ID('material_stock_transactions'))
    CREATE INDEX idx_mst_bom_item ON material_stock_transactions(bom_item_id)
        WHERE bom_item_id IS NOT NULL;
GO
