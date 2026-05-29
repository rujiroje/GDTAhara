# Excel Plan Importer Specification

**Phase 1 · W9**

Source file type: PC's monthly plan matrix (`.xlsx`).  
Reference file: `Plan_TAHARA_June26_Rev00.xlsx`, sheet "Plan".

---

## Input File Structure

```
Row 1        : (headers / merged cells — varies)
Row 2        : Date header row — columns D onwards are dates of the month
Row 3+       : Data rows — each row is one (Machine × Product × Sub-row)
               Sub-rows per machine: Remark | Actual | Plan | Diff
               "Plan" sub-row contains the target quantities PC planned
```

**Column layout (TAHARA factory example):**

| Col A | Col B | Col C | Col D | Col E | … |
|---|---|---|---|---|---|
| Machine code | Product code | Sub-row type | Day 1 | Day 2 | … |
| `RBL101` | `PBCGAHBRB` | `Plan` | 2400 | 2400 | … |
| | | `Actual` | 2350 | — | … |
| | | `Diff` | -50 | — | … |

---

## 3-Layer Detection Strategy

### Layer 1 — Factory-specific config (preferred)

```
SELECT factory_code, sheet_name_hint, machine_col_index,
       product_col_index, date_header_row, data_start_row, skip_row_keywords
FROM plan_sheet_template
WHERE factory_code = ?
```

Use `sheet_name_hint` to select the correct sheet within the workbook.  
Column indices are 1-based.

### Layer 2 — Filename heuristic (fallback if Layer 1 returns no rows)

Filename pattern regex:

```
^Plan_([A-Z]+)_([A-Za-z]+|[฀-๿]+)(\d{2})_Rev(\d+)\.xlsx$
```

Capture groups:

| Group | Content | Example |
|---|---|---|
| 1 | `factory_code` | `TAHARA` |
| 2 | `month_name` | `June` or Thai month |
| 3 | `year_2digit` | `26` (→ 2026) |
| 4 | `revision` | `00` |

If captured `factory_code` matches a row in `plan_sheet_template` → use that config.

### Layer 3 — Smart structure auto-detection (last resort)

Used when both Layer 1 and Layer 2 fail.

```
for each sheet in workbook:
    scan rows 1..7
    count cells that parse successfully as a date value
    
pick sheet with highest date-cell count IF count >= 7
    (rationale: a monthly plan must have at least 7 date columns)

heuristic column detection:
    machine_col  = first column where >= 30% of rows match /^[A-Z]{2,4}\d{3}/
    product_col  = column immediately after machine_col

defaults:
    date_header_row   = 2
    data_start_row    = 3
    skip_row_keywords = 'Remark|Actual|Plan|Diff'
```

---

## Parser Behavior

1. **Carry-forward machine code** for merged cells: if cell (row, machine_col) is blank, use the machine code from the previous non-blank row in that column.

2. **Skip sub-rows**: skip any row where `product_col` matches the pattern `/^(Remark|Actual|Plan|Diff)$/i` from `skip_row_keywords`.

3. **Skip invalid cells**: skip date columns where the cell value is:
   - `#REF!` or any other formula error
   - `null`, blank, zero, or negative

4. **Date column header parsing**: cells in `date_header_row` may be:
   - An integer (Excel serial date) → convert to `LocalDate`
   - A string like `"1"`, `"01"` → interpret as day of the month (use file month/year from filename or user input)
   - A full date string → parse directly

5. **Yield** a `ProductionPlanRecord` for each `(machine, product) × date` cell where `target_qty > 0`.

```
ProductionPlanRecord {
    factoryCode    : String       -- from template or heuristic
    machineCode    : String       -- resolved to machine.id or rejected
    productCode    : String       -- resolved to product.id or rejected
    planDate       : LocalDate
    targetQty      : Integer
    sourceFilename : String
    sheetName      : String
}
```

---

## Import Policy (CRITICAL — Preserve Past Data)

| Condition | Action | Audit |
|---|---|---|
| `plan_date < today` | **SKIP** | Log as `rows_skipped_past` |
| `plan_date = today` AND `production_report` already exists for (machine, date) | **SKIP** | Log as `rows_skipped_started` |
| `plan_date = today` AND no `production_report` exists | **UPSERT** | Log in `rows_updated` or `rows_added` |
| `plan_date > today` | **UPSERT** | Log in `rows_updated` or `rows_added` |

**UPSERT logic:**
```sql
MERGE production_plan AS target
USING (VALUES (@machineId, @productId, @planDate, @targetQty, @source, @fileRef))
    AS src (machine_id, product_id, plan_date, target_qty, source, excel_file_ref)
ON target.machine_id = src.machine_id
   AND target.product_id = src.product_id
   AND target.plan_date = src.plan_date
WHEN MATCHED THEN UPDATE SET
    target_qty = src.target_qty,
    excel_file_ref = src.excel_file_ref,
    updated_at = SYSUTCDATETIME()
WHEN NOT MATCHED THEN INSERT
    (machine_id, product_id, plan_date, target_qty, source,
     excel_file_ref, status, created_by, created_at, updated_at)
VALUES
    (src.machine_id, src.product_id, src.plan_date, src.target_qty,
     src.source, src.excel_file_ref, 'draft', @userId,
     SYSUTCDATETIME(), SYSUTCDATETIME());
```

After a successful UPSERT, `SetupJobService.evaluateMachine(machineId)` is called to recalculate setup jobs for the affected machine.

---

## Audit Log — import_log Table

Every import run creates exactly one row in `import_log`:

```
import_log row:
    filename              = original uploaded filename
    factory_code          = detected or user-supplied
    imported_by           = authenticated user id
    imported_at           = now
    rows_added            = count of INSERT operations
    rows_skipped_past     = count of plan_date < today rows
    rows_skipped_started  = count of plan_date = today, report exists
    rows_updated          = count of UPDATE operations
    errors_json           = JSON array of per-row errors (see below)
```

**`errors_json` format:**
```json
[
  { "row": 12, "machine": "RBL999", "reason": "Machine code not found in master" },
  { "row": 47, "product": "UNKNOWN", "reason": "Product code not found in master" }
]
```

---

## Validation Rules (Per Row)

| Rule | Failure action |
|---|---|
| `machineCode` must exist in `machine` table | Reject row; log error |
| `productCode` must exist in `product` table | Reject row; log error |
| `targetQty` must be positive integer | Reject row; log error |
| `planDate` must be parseable as a valid date | Reject row; log error |
| Duplicate `(machine, product, planDate)` within same file | Keep first row; log second as duplicate |

Import does NOT fail entirely on row-level errors — it continues and reports all errors in `errors_json`.

---

## Test Scenarios

| Scenario | Expected result |
|---|---|
| Re-import same file | `rows_added = 0`; all future dates show as `rows_updated` (no change to values); `rows_skipped_past = n` for all past rows |
| Edit Day 15 target value before Day 15 | `rows_updated = 1` for that cell; plan value updated |
| Edit Day 5 target after Day 5 has passed | `rows_skipped_past += 1`; original plan preserved |
| Excel contains machine code `RBL999` not in master | Row rejected; `errors_json` entry; other rows processed normally |
| Excel contains product code `UNKNOWN` not in master | Row rejected; `errors_json` entry |
| Two rows for same (machine, date, product) in one file | First row imported; second logged as duplicate in `errors_json` |
| File missing plan sheet (wrong sheet name) | Layer 2 fallback attempted; then Layer 3; if still fails → error 422 returned to caller |
| File with 0 valid rows (all past, all invalid) | Import completes with `rows_added = 0`; response body includes summary |

---

## API Contract (W10 implementation)

```
POST /api/production/plans/import
Content-Type: multipart/form-data
Authorization: Bearer {jwt}  (role: Production Control or DataAdmin)

Body:
    file         : .xlsx file
    factory_code : string (optional — overrides auto-detection)
    dry_run      : boolean (optional, default false)
                   If true: parse and validate but do not write to DB

Response 200:
{
  "importLogId"         : 42,
  "filename"            : "Plan_TAHARA_June26_Rev00.xlsx",
  "factoryCode"         : "TAHARA",
  "rowsAdded"           : 31,
  "rowsUpdated"         : 0,
  "rowsSkippedPast"     : 15,
  "rowsSkippedStarted"  : 0,
  "errors"              : []
}

Response 422:
{
  "error": "Could not detect plan sheet structure. Provide factory_code or check file format."
}
```
