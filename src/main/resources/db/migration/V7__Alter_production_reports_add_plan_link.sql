-- V7: Alter production_reports — add plan link, parent_lot, diff_qty
-- Idempotent: handles pre-existing shift column (added manually 2026-05-29)
-- Also adds actual_qty (was missing — used by diff_qty PERSISTED column)

-- Step 0a: Add shift column if missing
IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('production_reports')
                 AND name = 'shift')
BEGIN
    ALTER TABLE production_reports ADD shift NVARCHAR(20) NULL;
END
GO

-- Step 0b: Add actual_qty column if missing (needed for diff_qty)
IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('production_reports')
                 AND name = 'actual_qty')
BEGIN
    ALTER TABLE production_reports ADD actual_qty INT NULL;
END
GO

-- Step 1: Add 3 new columns
ALTER TABLE production_reports
    ADD production_plan_id BIGINT NULL,
        parent_lot_number  NVARCHAR(100) NULL,
        diff_qty AS (ISNULL(actual_qty, 0) - ISNULL(target_qty, 0)) PERSISTED;
GO

-- Step 2: Backfill parent_lot_number for existing rows
-- Format: PL-{machine_code}-{YYYYMMDD}-{shift first letter}
-- Handle uniqueness via ROW_NUMBER suffix on collision
;WITH numbered AS (
    SELECT
        pr.id,
        ROW_NUMBER() OVER (
            PARTITION BY pr.machine_id, CAST(pr.start_date AS DATE), pr.shift
            ORDER BY pr.id
        ) AS rn
    FROM production_reports pr
)
UPDATE pr
SET parent_lot_number =
    'PL-' + COALESCE(m.machine_code, CAST(pr.machine_id AS NVARCHAR(20)), 'UNK')
         + '-' + FORMAT(CAST(pr.start_date AS DATE), 'yyyyMMdd')
         + '-' + ISNULL(LEFT(pr.shift, 1), 'X')
         + CASE WHEN n.rn > 1 THEN '-' + CAST(n.rn AS NVARCHAR(10)) ELSE '' END
FROM production_reports pr
JOIN numbered n ON pr.id = n.id
LEFT JOIN machines m ON pr.machine_id = m.id;
GO

-- Step 3: Add FK + UNIQUE constraints
ALTER TABLE production_reports
    ADD CONSTRAINT fk_pr_plan
        FOREIGN KEY (production_plan_id) REFERENCES production_plan(id);
GO

ALTER TABLE production_reports
    ADD CONSTRAINT uk_pr_parent_lot_number UNIQUE (parent_lot_number);
GO
