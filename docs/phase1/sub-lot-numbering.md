# Sub-Lot Numbering Specification

**Phase 1 · W9**

---

## Numbering Format

### Hierarchy

```
PRODUCTION_PLAN
    └── PRODUCTION_REPORT  (= Parent Lot)
            └── SUB_LOT    (= Box / Sub-Lot)
```

### Parent Lot Number

```
PL-{machine_code}-{YYYYMMDD}-{shift}
```

| Token | Source | Example |
|---|---|---|
| `PL` | Fixed prefix | `PL` |
| `{machine_code}` | `machine.machine_name` | `RBL101` |
| `{YYYYMMDD}` | `production_report.start_date` formatted | `20251001` |
| `{shift}` | `D` (06:00–18:00) or `N` (18:00–06:00) | `D` |

**Example:** `PL-RBL101-20251001-D`

### Sub-Lot Number

```
{parent_lot_number}-B{seq:04d}
```

Sequence is per-parent-lot, starts at `1`, zero-padded to 4 digits.

**Examples:**
- First box: `PL-RBL101-20251001-D-B0001`
- Second box: `PL-RBL101-20251001-D-B0002`
- 1000th box: `PL-RBL101-20251001-D-B1000`

### Pallet Number

```
{parent_lot_number}-PLT{seq:02d}
```

Multiple boxes (sub-lots) share a pallet number. Operator assigns the pallet during box confirmation.

**Examples:**
- `PL-RBL101-20251001-D-PLT01` — Pallet 1 containing boxes B0001–B0048
- `PL-RBL101-20251001-D-PLT02` — Pallet 2 continuing from B0049

---

## Uniqueness Enforcement

Uniqueness is guaranteed at two layers:

### Layer 1 — Database UNIQUE constraint

```sql
CONSTRAINT uk_sub_lot_number UNIQUE (sub_lot_number)
```

Any concurrent duplicate insert will raise a constraint violation before any data is written.

### Layer 2 — Service-level pre-check + atomic generation

The sequence number is generated atomically to eliminate TOCTOU race conditions.

**Algorithm (pseudo-SQL inside service):**

```sql
-- Atomic sequence fetch: update counter and return new value in one statement
-- Production_reports tracks next_sub_lot_seq
UPDATE production_reports
SET next_sub_lot_seq = ISNULL(next_sub_lot_seq, 0) + 1
OUTPUT INSERTED.next_sub_lot_seq, INSERTED.parent_lot_number
WHERE id = @reportId;
```

The service concatenates the returned seq to form `sub_lot_number` and inserts the SUB_LOT row. If the DB UNIQUE constraint fires (edge case: concurrent update), the service retries once with a fresh seq fetch.

---

## Box-Confirmation Workflow

```
Operator opens Active Parent Lot screen
    │
    ├─ Sees: machine name, product, shift, box count so far
    │
    ▼
Operator clicks "Confirm Box"
    │
    ├─ Optional: scan/enter pallet number (or reuse last pallet)
    ├─ Optional: enter box weight (kg)
    │
    ▼
Service layer:
    1. Fetch production_report → validate status = 'In Progress'
    2. UPDATE production_reports SET next_sub_lot_seq = next_sub_lot_seq + 1
       OUTPUT INSERTED.next_sub_lot_seq, INSERTED.parent_lot_number
    3. Build sub_lot_number = parent_lot_number + '-B' + LPAD(seq, 4, '0')
    4. INSERT INTO sub_lot (...) VALUES (...)
    5. Trigger ZPL label print (async) → update zpl_label_printed_ref
    │
    ▼
UI shows updated box count and prints label
```

---

## Pallet Grouping

- Operator selects an existing pallet number (from dropdown of open pallets for this lot) **or** creates a new one by clicking "New Pallet".
- A pallet is "closed" when the operator clicks "Close Pallet" — at that point `pallet_number` is frozen and further boxes go to a new pallet.
- Pallet sequence is independent of box sequence.
- One pallet may span a shift boundary only if the parent lot spans that shift (i.e. same `production_report_id`).

---

## ZPL Label Content Template

ZPL II format. Print target: Zebra ZT series or compatible thermal printer (203 dpi assumed).

```zpl
^XA
^FO20,20^A0N,30,30^FD${sub_lot_number}^FS
^FO20,60^BY2^BCN,60,Y,N,N^FD${sub_lot_number}^FS
^FO20,140^A0N,22,22^FDMachine: ${machine_code}^FS
^FO20,168^A0N,22,22^FDProduct: ${product_code}^FS
^FO20,196^A0N,22,22^FDDate:    ${plan_date}^FS
^FO20,224^A0N,22,22^FDShift:   ${shift}^FS
^FO20,252^A0N,22,22^FDBox Qty: ${box_quantity} pcs^FS
^FO20,280^A0N,22,22^FDPallet:  ${pallet_number}^FS
^FO20,308^A0N,18,18^FDPrinted: ${confirmed_at}^FS
^XZ
```

**Variables to substitute at print time:**

| Variable | Source |
|---|---|
| `${sub_lot_number}` | `sub_lot.sub_lot_number` |
| `${machine_code}` | `machine.machine_name` |
| `${product_code}` | `product.product_code` |
| `${plan_date}` | `production_report.start_date` |
| `${shift}` | `production_report.shift` |
| `${box_quantity}` | `sub_lot.box_quantity` |
| `${pallet_number}` | `sub_lot.pallet_number` |
| `${confirmed_at}` | `sub_lot.confirmed_at` formatted `dd/MM/yyyy HH:mm` |

Print job reference is stored in `sub_lot.zpl_label_printed_ref` (e.g. printer job ID or timestamp).

---

## Sequence Collision Handling

In the unlikely event of a DB `UNIQUE` constraint violation (two threads simultaneously attempting the same seq):

1. Service catches `DataIntegrityViolationException` (Spring) / `ConstraintViolationException` (JPA).
2. Service retries **once** with a new seq fetch (the `UPDATE ... OUTPUT` pattern re-executes).
3. If the second attempt also fails, the exception propagates to the controller as HTTP 409 Conflict with message `"Sub-lot sequence conflict — please retry"`.
4. The UI shows a toast notification and re-enables the "Confirm Box" button.

This is an edge case in normal single-Operator-per-lot flow. It can occur when two Operators accidentally open the same parent lot simultaneously.
