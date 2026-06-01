-- Step 1: Add columns as NULL first (safe for existing rows)
ALTER TABLE production_reports
    ADD production_plan_id BIGINT NULL,
        parent_lot_number  NVARCHAR(100) NULL,
        diff_qty AS (ISNULL(actual_qty, 0) - ISNULL(target_qty, 0)) PERSISTED;
GO

-- Step 2: Backfill parent_lot_number for existing rows
-- Format: PL-{machine_code}-{YYYYMMDD}-{shift first letter}
-- Handle UNIQUE collision via ROW_NUMBER suffix (-rn) if duplicates exist
;WITH numbered AS (
    SELECT
        pr.id,
        pr.machine_id,
        pr.start_date,
        pr.shift,
        ROW_NUMBER() OVER (
            PARTITION BY pr.machine_id, CAST(pr.start_date AS DATE), pr.shift
            ORDER BY pr.id
        ) AS rn
    FROM production_reports pr
)
UPDATE pr
SET parent_lot_number =
    'PL-' + ISNULL(m.machine_code, 'UNK')
         + '-' + FORMAT(CAST(pr.start_date AS DATE), 'yyyyMMdd')
         + '-' + ISNULL(LEFT(pr.shift, 1), 'X')
         + CASE WHEN n.rn > 1 THEN '-' + CAST(n.rn AS NVARCHAR(10)) ELSE '' END
FROM production_reports pr
JOIN numbered n ON pr.id = n.id
LEFT JOIN machines m ON pr.machine_id = m.id;
GO

-- Step 3: Add FK + UNIQUE constraints (data is populated)
ALTER TABLE production_reports
    ADD CONSTRAINT fk_pr_plan
        FOREIGN KEY (production_plan_id) REFERENCES production_plan(id);
GO

ALTER TABLE production_reports
    ADD CONSTRAINT uk_pr_parent_lot_number UNIQUE (parent_lot_number);
GO
