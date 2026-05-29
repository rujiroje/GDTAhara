# Migration Plan — Phase 1 Lot/Sub-Lot Hierarchy

**Phase 1 · W9 · Reference document — skeletons to be executed in W10.**  
All SQL is SQL Server (SSMS-compatible). No `VARCHAR` — use `NVARCHAR`. No `TEXT` — use `NVARCHAR(MAX)`.

---

## Migration Files (W10)

| File | Purpose |
|---|---|
| `V4__Create_production_plan.sql` | New `production_plan` table |
| `V5__Create_machine_setup_job.sql` | New `machine_setup_job` table |
| `V6__Create_sub_lot.sql` | New `sub_lot` table |
| `V7__Alter_production_report_add_plan_link.sql` | Add 3 columns to existing `production_reports` |
| `V8__Create_or_seed_plan_sheet_template.sql` | Handle pre-existing `plan_sheet_template` on production DB |
| `V9__Add_indexes_phase1.sql` | All Phase 1 indexes + `import_log` table |

---

## V4 — Create production_plan

```sql
CREATE TABLE production_plan (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    plan_date       DATE           NOT NULL,
    machine_id      BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    target_qty      INT            NOT NULL,
    manpower_d_ratio DECIMAL(5,2)  NOT NULL CONSTRAINT df_pp_d_ratio DEFAULT 0.50,
    manpower_n_ratio DECIMAL(5,2)  NOT NULL CONSTRAINT df_pp_n_ratio DEFAULT 0.50,
    sap_wo_number   NVARCHAR(50)   NULL,
    source          NVARCHAR(20)   NOT NULL,
    excel_file_ref  NVARCHAR(200)  NULL,
    status          NVARCHAR(20)   NOT NULL CONSTRAINT df_pp_status DEFAULT 'draft',
    created_by      BIGINT         NOT NULL,
    created_at      DATETIME2      NOT NULL CONSTRAINT df_pp_created_at DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2      NOT NULL CONSTRAINT df_pp_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT pk_production_plan PRIMARY KEY (id),
    CONSTRAINT uk_production_plan_machine_date_product
        UNIQUE (machine_id, plan_date, product_id),
    CONSTRAINT fk_production_plan_machine
        FOREIGN KEY (machine_id) REFERENCES machine(id),
    CONSTRAINT fk_production_plan_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_production_plan_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
);
GO
```

**Rollback:**
```sql
ALTER TABLE machine_setup_job DROP CONSTRAINT fk_msj_plan;
ALTER TABLE production_reports DROP CONSTRAINT fk_pr_plan;
DROP TABLE production_plan;
GO
```

---

## V5 — Create machine_setup_job

```sql
CREATE TABLE machine_setup_job (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    machine_id            BIGINT        NOT NULL,
    from_product_id       BIGINT        NULL,
    to_product_id         BIGINT        NOT NULL,
    production_plan_id    BIGINT        NOT NULL,
    plan_date             DATE          NOT NULL,
    required_before       TIME          NULL,
    assigned_to_user_id   BIGINT        NULL,
    status                NVARCHAR(20)  NOT NULL CONSTRAINT df_msj_status DEFAULT 'PENDING',
    started_at            DATETIME2     NULL,
    completed_at          DATETIME2     NULL,
    duration_min          INT           NULL,
    mold_changed          BIT           NOT NULL CONSTRAINT df_msj_mold DEFAULT 0,
    mold_code_from        NVARCHAR(50)  NULL,
    mold_code_to          NVARCHAR(50)  NULL,
    temp_adjusted         BIT           NOT NULL CONSTRAINT df_msj_temp DEFAULT 0,
    cycle_adjusted        BIT           NOT NULL CONSTRAINT df_msj_cycle DEFAULT 0,
    blow_pin_aligned      BIT           NOT NULL CONSTRAINT df_msj_blow DEFAULT 0,
    fpi_passed            BIT           NOT NULL CONSTRAINT df_msj_fpi DEFAULT 0,
    skip_reason           NVARCHAR(500) NULL,
    notes                 NVARCHAR(MAX) NULL,
    completed_by_user_id  BIGINT        NULL,

    CONSTRAINT pk_machine_setup_job PRIMARY KEY (id),
    CONSTRAINT fk_msj_machine
        FOREIGN KEY (machine_id) REFERENCES machine(id),
    CONSTRAINT fk_msj_from_product
        FOREIGN KEY (from_product_id) REFERENCES product(id),
    CONSTRAINT fk_msj_to_product
        FOREIGN KEY (to_product_id) REFERENCES product(id),
    CONSTRAINT fk_msj_plan
        FOREIGN KEY (production_plan_id) REFERENCES production_plan(id),
    CONSTRAINT fk_msj_assigned
        FOREIGN KEY (assigned_to_user_id) REFERENCES users(id),
    CONSTRAINT fk_msj_completed_by
        FOREIGN KEY (completed_by_user_id) REFERENCES users(id)
);
GO
```

**Rollback:**
```sql
DROP TABLE machine_setup_job;
GO
```

---

## V6 — Create sub_lot

```sql
CREATE TABLE sub_lot (
    id                      BIGINT IDENTITY(1,1) NOT NULL,
    production_report_id    BIGINT         NOT NULL,
    sub_lot_number          NVARCHAR(100)  NOT NULL,
    pallet_number           NVARCHAR(50)   NULL,
    box_quantity            INT            NOT NULL,
    weight_kg               DECIMAL(10,3)  NULL,
    confirmed_at            DATETIME2      NOT NULL CONSTRAINT df_sl_confirmed_at DEFAULT SYSUTCDATETIME(),
    confirmed_by_user_id    BIGINT         NOT NULL,
    status                  NVARCHAR(20)   NOT NULL CONSTRAINT df_sl_status DEFAULT 'draft',
    zpl_label_printed_ref   NVARCHAR(100)  NULL,

    CONSTRAINT pk_sub_lot PRIMARY KEY (id),
    CONSTRAINT uk_sub_lot_number UNIQUE (sub_lot_number),
    CONSTRAINT fk_sub_lot_report
        FOREIGN KEY (production_report_id) REFERENCES production_reports(id),
    CONSTRAINT fk_sub_lot_confirmed_by
        FOREIGN KEY (confirmed_by_user_id) REFERENCES users(id)
);
GO
```

**Rollback:**
```sql
DROP TABLE sub_lot;
GO
```

---

## V7 — Alter production_report (add plan link + computed column)

```sql
-- Add nullable FK to production_plan (historical rows keep NULL)
ALTER TABLE production_reports
    ADD production_plan_id BIGINT NULL;
GO

ALTER TABLE production_reports
    ADD CONSTRAINT fk_pr_plan
    FOREIGN KEY (production_plan_id) REFERENCES production_plan(id);
GO

-- Add parent_lot_number
ALTER TABLE production_reports
    ADD parent_lot_number NVARCHAR(100) NULL;
GO

-- Add computed column (PERSISTED so it can be indexed)
-- ISNULL guards against NULL on historical rows lacking actual_qty / target_qty
ALTER TABLE production_reports
    ADD diff_qty AS (ISNULL(actual_qty, 0) - ISNULL(target_qty, 0)) PERSISTED;
GO
```

**Backfill parent_lot_number for existing rows:**
```sql
-- Generate parent_lot_number for historical reports
-- Format: PL-{machine_code}-{YYYYMMDD}-{shift_prefix}
-- Shift derived from report.shift column (D = day, N = night); default D if NULL
UPDATE pr
SET pr.parent_lot_number =
    'PL-' + m.machine_name + '-'
    + FORMAT(pr.start_date, 'yyyyMMdd') + '-'
    + ISNULL(LEFT(pr.shift, 1), 'D')
FROM production_reports pr
JOIN machine m ON m.id = pr.machine_id
WHERE pr.parent_lot_number IS NULL;
GO

-- If collision occurs (same machine + date + shift), append row sequence
-- Re-run this to fix any duplicates created by the step above
WITH numbered AS (
    SELECT id,
           parent_lot_number,
           ROW_NUMBER() OVER (
               PARTITION BY parent_lot_number
               ORDER BY id
           ) AS rn
    FROM production_reports
    WHERE parent_lot_number IS NOT NULL
)
UPDATE production_reports
SET parent_lot_number = n.parent_lot_number + '-' + CAST(n.rn AS NVARCHAR(3))
FROM production_reports pr
JOIN numbered n ON n.id = pr.id
WHERE n.rn > 1;
GO
```

**Rollback:**
```sql
ALTER TABLE production_reports DROP CONSTRAINT fk_pr_plan;
ALTER TABLE production_reports DROP COLUMN production_plan_id;
ALTER TABLE production_reports DROP COLUMN parent_lot_number;
ALTER TABLE production_reports DROP COLUMN diff_qty;
GO
```

---

## V8 — Create or Seed plan_sheet_template

> **Special handling:** `plan_sheet_template` was manually created in SSMS on 2026-05-29 before Flyway was written.
> Strategy: **(a) IF NOT EXISTS** — idempotent; preserves pre-existing data; seeds with MERGE.

```sql
-- Step 1: Create table only if it does not already exist
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'plan_sheet_template')
BEGIN
    CREATE TABLE plan_sheet_template (
        id                  BIGINT IDENTITY(1,1) NOT NULL,
        factory_code        NVARCHAR(20)   NOT NULL,
        sheet_name_hint     NVARCHAR(100)  NULL,
        machine_col_index   INT            NOT NULL,
        product_col_index   INT            NOT NULL,
        date_header_row     INT            NOT NULL,
        data_start_row      INT            NOT NULL,
        skip_row_keywords   NVARCHAR(500)  NULL,

        CONSTRAINT pk_plan_sheet_template PRIMARY KEY (id),
        CONSTRAINT uk_pst_factory_code UNIQUE (factory_code)
    );
END;
GO

-- Step 2: Idempotent seed — MERGE preserves any extra rows added manually
MERGE plan_sheet_template AS target
USING (VALUES
    ('TAHARA', N'Plan',               1, 2, 2, 8, N'Remark|Actual|Plan|Diff'),
    ('ASB',    N'IT (Plan 2 Level)',   1, 2, 2, 3, NULL)
) AS src (factory_code, sheet_name_hint, machine_col_index,
          product_col_index, date_header_row, data_start_row,
          skip_row_keywords)
ON target.factory_code = src.factory_code
WHEN MATCHED THEN UPDATE SET
    sheet_name_hint    = src.sheet_name_hint,
    machine_col_index  = src.machine_col_index,
    product_col_index  = src.product_col_index,
    date_header_row    = src.date_header_row,
    data_start_row     = src.data_start_row,
    skip_row_keywords  = src.skip_row_keywords
WHEN NOT MATCHED THEN INSERT
    (factory_code, sheet_name_hint, machine_col_index,
     product_col_index, date_header_row, data_start_row, skip_row_keywords)
VALUES
    (src.factory_code, src.sheet_name_hint, src.machine_col_index,
     src.product_col_index, src.date_header_row, src.data_start_row,
     src.skip_row_keywords);
GO
```

**Rollback:**
```sql
-- DO NOT drop plan_sheet_template — it existed before this migration
-- and may contain manually-added rows that are production data.
-- Rollback only restores the original seed values using the same MERGE.
-- If you need to drop the table, do so manually in SSMS after verifying
-- no application depends on it.
PRINT 'V8 rollback: no-op. plan_sheet_template was pre-existing; table preserved.';
GO
```

---

## V9 — Add indexes + create import_log

```sql
-- production_plan indexes
CREATE INDEX idx_pp_machine_date ON production_plan (machine_id, plan_date);
CREATE INDEX idx_pp_status       ON production_plan (status);
GO

-- machine_setup_job indexes
CREATE INDEX idx_msj_machine_date    ON machine_setup_job (machine_id, plan_date);
CREATE INDEX idx_msj_assigned_status ON machine_setup_job (assigned_to_user_id, status);
CREATE INDEX idx_msj_status          ON machine_setup_job (status);
GO

-- sub_lot indexes
CREATE INDEX idx_sl_report ON sub_lot (production_report_id);
CREATE INDEX idx_sl_pallet ON sub_lot (pallet_number);
GO

-- production_reports: unique index on parent_lot_number (filtered — ignores NULLs)
CREATE UNIQUE INDEX idx_pr_parent_lot
    ON production_reports (parent_lot_number)
    WHERE parent_lot_number IS NOT NULL;
GO

-- import_log table
CREATE TABLE import_log (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    filename              NVARCHAR(200) NOT NULL,
    factory_code          NVARCHAR(20)  NULL,
    imported_by           BIGINT        NOT NULL,
    imported_at           DATETIME2     NOT NULL CONSTRAINT df_il_imported_at DEFAULT SYSUTCDATETIME(),
    rows_added            INT           NOT NULL CONSTRAINT df_il_added DEFAULT 0,
    rows_skipped_past     INT           NOT NULL CONSTRAINT df_il_past DEFAULT 0,
    rows_skipped_started  INT           NOT NULL CONSTRAINT df_il_started DEFAULT 0,
    rows_updated          INT           NOT NULL CONSTRAINT df_il_updated DEFAULT 0,
    errors_json           NVARCHAR(MAX) NULL,

    CONSTRAINT pk_import_log PRIMARY KEY (id),
    CONSTRAINT fk_import_log_user FOREIGN KEY (imported_by) REFERENCES users(id)
);
GO
```

**Rollback:**
```sql
DROP TABLE import_log;
DROP INDEX idx_pr_parent_lot ON production_reports;
DROP INDEX idx_sl_pallet ON sub_lot;
DROP INDEX idx_sl_report ON sub_lot;
DROP INDEX idx_msj_status ON machine_setup_job;
DROP INDEX idx_msj_assigned_status ON machine_setup_job;
DROP INDEX idx_msj_machine_date ON machine_setup_job;
DROP INDEX idx_pp_status ON production_plan;
DROP INDEX idx_pp_machine_date ON production_plan;
GO
```

---

## Rollback Sequence (V9 → V4)

Execute in reverse order on target DB (stop Flyway first):

```
1. Run V9  rollback  → drop import_log, drop all phase1 indexes
2. Run V8  rollback  → no-op (preserve plan_sheet_template)
3. Run V7  rollback  → drop diff_qty, parent_lot_number, production_plan_id from production_reports
4. Run V6  rollback  → DROP TABLE sub_lot
5. Run V5  rollback  → DROP TABLE machine_setup_job
6. Run V4  rollback  → DROP TABLE production_plan
7. DELETE FROM flyway_schema_history WHERE version IN ('4','5','6','7','8','9');
```

> **Note on V8:** Dropping `plan_sheet_template` during rollback would destroy pre-W9 manually-entered data (TAHARA/ASB rows). Rollback for V8 is intentionally a no-op. If full rollback of `plan_sheet_template` is needed, restore from a SSMS backup taken before V8 execution.

---

## Risk Register

| Risk ID | Description | Probability | Impact | Score | Mitigation |
|---|---|---|---|---|---|
| R-08.1 | `diff_qty` computed column fails on NULLs in existing `production_reports` rows | 4 | 3 | 12 | Use `ISNULL(actual_qty, 0) - ISNULL(target_qty, 0)` in V7 |
| R-08.2 | `parent_lot_number` UNIQUE constraint conflict during backfill (same machine+date+shift) | 3 | 4 | 12 | V7 backfill adds `-{rn}` suffix on collision via CTE with ROW_NUMBER |
| R-08.3 | FK constraint failures on `production_plan_id` in existing rows | 2 | 3 | 6 | Column is nullable initially; populate via UI later |
| R-08.4 | Flyway state out of sync — `plan_sheet_template` exists but no V8 checksum | 5 | 4 | 20 | V8 uses `IF NOT EXISTS` + `MERGE`; idempotent on repeat runs |
| R-08.5 | Long-running `ALTER TABLE` on `production_reports` (may be large) | 3 | 5 | 15 | Schedule maintenance window; use `WITH (ONLINE = ON)` if SQL Server Enterprise edition |
