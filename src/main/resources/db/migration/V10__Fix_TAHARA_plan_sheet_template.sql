-- W16.1-FIX: Correct TAHARA plan_sheet_template for real file layout
-- Real file: "Plan_TAHARA_June26_Rev00.xlsx" / "Plan TAHARA June'26 Rev00.xlsx"
--   sheet_name_hint   = 'Plan'          (not 'Plan PM' or 'IT (Plan 2 Level)')
--   machine_col_index = 2               (1-based = column B; contains "MACHINE   PRODUCT_CODE")
--   product_col_index = 2               (1-based = column B; SAME cell as machine)
--   date_header_row   = 6               (1-based; dates at columns N..BY ≈ 60 date cells)
--   data_start_row    = 8               (1-based; row 7 is a sub-header, data from row 8)
--   skip_row_keywords = 'Remark|Actual|Plan|Diff'

MERGE plan_sheet_template AS target
USING (VALUES
    ('TAHARA', N'Plan', 2, 2, 6, 8, N'Remark|Actual|Plan|Diff')
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
