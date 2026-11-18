-- V52: Sync mold_master with actual DB state + add missing fields to Java model
-- Columns may already exist (added manually in SSMS) — all ADD are idempotent

-- product_id: strict 1:1 mold-product relationship
IF COL_LENGTH('mold_master', 'product_id') IS NULL
    ALTER TABLE mold_master
        ADD product_id BIGINT NULL
            CONSTRAINT FK_mm_product FOREIGN KEY REFERENCES products(id);
GO

-- mold_type: e.g. 'BLOW', 'INJECTION'
IF COL_LENGTH('mold_master', 'mold_type') IS NULL
    ALTER TABLE mold_master ADD mold_type NVARCHAR(50) NULL;
GO

-- standard_cavity_count / standard_cavity_no: original design spec
IF COL_LENGTH('mold_master', 'standard_cavity_count') IS NULL
    ALTER TABLE mold_master ADD standard_cavity_count INT NULL;
GO
IF COL_LENGTH('mold_master', 'standard_cavity_no') IS NULL
    ALTER TABLE mold_master ADD standard_cavity_no NVARCHAR(200) NULL;
GO

-- work_cavity_count / work_cavity_no: currently active cavities (may be fewer due to defects)
IF COL_LENGTH('mold_master', 'work_cavity_count') IS NULL
    ALTER TABLE mold_master ADD work_cavity_count INT NULL;
GO
IF COL_LENGTH('mold_master', 'work_cavity_no') IS NULL
    ALTER TABLE mold_master ADD work_cavity_no NVARCHAR(200) NULL;
GO

-- Index for product-based lookups (mold selection dropdown)
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_mm_product' AND object_id = OBJECT_ID('mold_master'))
    CREATE INDEX IX_mm_product ON mold_master(product_id) WHERE product_id IS NOT NULL;
GO
