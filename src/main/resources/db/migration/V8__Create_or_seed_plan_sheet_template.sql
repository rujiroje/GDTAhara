-- Step 1: Create table IF NOT EXISTS
-- Preserves pre-existing table (created manually 2026-05-29) and its data
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
        notes               NVARCHAR(MAX)  NULL,
        created_at          DATETIME2      NOT NULL CONSTRAINT df_pst_created DEFAULT SYSUTCDATETIME(),
        CONSTRAINT pk_plan_sheet_template PRIMARY KEY (id),
        CONSTRAINT uk_plan_sheet_template_factory UNIQUE (factory_code)
    );
END;
GO

-- Step 2: Idempotent MERGE for TAHARA + ASB seed
-- MATCHED rows are updated to spec values; NOT MATCHED rows are inserted
MERGE plan_sheet_template AS target
USING (VALUES
    ('TAHARA', N'Plan',               1, 2, 2, 8, N'Remark|Actual|Plan|Diff'),
    ('ASB',    N'IT (Plan 2 Level)',   1, 2, 2, 3, NULL)
) AS src (factory_code, sheet_name_hint, machine_col_index,
          product_col_index, date_header_row, data_start_row,
          skip_row_keywords)
ON target.factory_code = src.factory_code
WHEN MATCHED THEN UPDATE SET
    sheet_name_hint   = src.sheet_name_hint,
    machine_col_index = src.machine_col_index,
    product_col_index = src.product_col_index,
    date_header_row   = src.date_header_row,
    data_start_row    = src.data_start_row,
    skip_row_keywords = src.skip_row_keywords
WHEN NOT MATCHED THEN INSERT
    (factory_code, sheet_name_hint, machine_col_index,
     product_col_index, date_header_row, data_start_row, skip_row_keywords)
VALUES
    (src.factory_code, src.sheet_name_hint, src.machine_col_index,
     src.product_col_index, src.date_header_row, src.data_start_row,
     src.skip_row_keywords);
GO
