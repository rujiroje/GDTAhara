-- V19: Pallet support — add missing product pallet columns + fix FK gap in pallet_close_log

-- ── products: qty_per_pallet, qty_per_bag ────────────────────────────────────
-- These fields are mapped in Product.java; add them if not already present
-- (products table was created before Flyway was introduced)

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'qty_per_pallet'
)
    ALTER TABLE products ADD qty_per_pallet INT NULL;

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'products' AND COLUMN_NAME = 'qty_per_bag'
)
    ALTER TABLE products ADD qty_per_bag INT NULL;

-- ── pallet_close_log: add missing FK for ref_pallet_id ───────────────────────
-- ref_pallet_id stores the source pallet during a REARRANGED_FROM action
-- FK was omitted from V18; add it now

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_NAME = 'fk_pcl_ref_pallet' AND TABLE_NAME = 'pallet_close_log'
)
    ALTER TABLE pallet_close_log
        ADD CONSTRAINT fk_pcl_ref_pallet FOREIGN KEY (ref_pallet_id) REFERENCES pallet(id);

-- Index to support queries like "what pallets were rearranged from this one?"
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ix_pcl_ref_pallet' AND object_id = OBJECT_ID('pallet_close_log')
)
    CREATE INDEX ix_pcl_ref_pallet ON pallet_close_log(ref_pallet_id)
        WHERE ref_pallet_id IS NOT NULL;
