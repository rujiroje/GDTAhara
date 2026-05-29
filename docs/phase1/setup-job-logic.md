# Machine Setup Job — Business Logic Specification

**Phase 1 · W9**

---

## Purpose

When PC changes the product assigned to a machine between two plan dates, the physical machine requires a setup change (mold swap, temperature re-profile, cycle time adjustment, blow-pin alignment, first-piece inspection). The `MACHINE_SETUP_JOB` entity captures this work, assigns it to a Technician, and feeds the OEE downtime calculation automatically on completion.

---

## Trigger Conditions

Setup jobs are evaluated **per machine** by comparing sorted-by-date plan cells.

| Scenario | Action |
|---|---|
| First plan cell ever for a machine (no prior product) | **CREATE** setup job (`from_product_id = NULL`) |
| Adjacent plan cells (date₁ < date₂) with **different** products | **CREATE** setup job |
| Adjacent plan cells with **same** product | No action |
| Day-shift → Night-shift change with **same** product | No action |
| Day-shift → Night-shift change with **different** products | **CREATE** setup job |

"Adjacent" means the two nearest plan dates for the same machine when sorted ascending — gaps (weekends, holidays) do not prevent a setup job.

### Trigger Decision Pseudocode

```
for each machine M:
    plans = SELECT * FROM production_plan WHERE machine_id = M.id
            ORDER BY plan_date ASC

    prev_product = null
    for each plan P in plans:
        if prev_product IS NULL or prev_product != P.product_id:
            createSetupJob(M, prev_product, P.product_id, P)
        prev_product = P.product_id
```

---

## Re-Trigger on Plan Edit (PRESERVE HISTORY — Critical)

When PC edits a production plan (changes product, date, or cancels a cell), the setup job evaluation re-runs for the affected machine. The following rules **preserve all history**:

| Existing setup status | Action on re-trigger |
|---|---|
| `PENDING` | Mark existing job `SUPERSEDED`; CREATE new job |
| `IN_PROGRESS` | **Do NOT auto-cancel.** Flag for Technician decision (see UI note below). |
| `COMPLETED` | Keep record untouched. If product changes again, CREATE an additional job. The completed record remains as audit trail. |
| `SKIPPED` | CREATE new job (treat same as PENDING) |
| `SUPERSEDED` | No further action — already superseded |

> **NEVER** delete or modify rows in `COMPLETED` or `SUPERSEDED` status.

### UI Warning for IN_PROGRESS

When re-trigger finds an `IN_PROGRESS` job:

```
Warning dialog:
"Setup job #<id> for <machine> is currently in progress by <technician>.
 Changing the plan may invalidate this work.
 
 Options:
   [Continue Setup]  — Technician continues; plan change is held until setup completes
   [Abort Setup]     — Technician enters abort reason; job status → SKIPPED; new job created
"
```

The plan change is not persisted until the Technician chooses one of the two options.

### Status Transition Diagram

```
         plan created
              │
              ▼
           PENDING ──── plan edited (product diff) ──► SUPERSEDED (frozen)
              │
              │ Technician starts
              ▼
          IN_PROGRESS
              │          │
              │ completes │ plan edited (product diff) → see UI warning
              ▼          │
          COMPLETED ◄────┘ (if "Continue Setup" chosen)
          (frozen)

   PENDING/SKIPPED ──── plan edited (product diff) ──► SUPERSEDED
                                                        + new PENDING created
```

---

## Integration with DOWNTIME_EVENT

On `COMPLETED` transition, the service **auto-inserts** a `DowntimeEvent` row:

```
On MachineSetupJob.status → COMPLETED:
    INSERT INTO downtime_events (
        report_id,          -- linked production_report if available, else NULL
        machine_id,         -- setup.machine_id
        category,           -- 'SETUP'
        start_time,         -- setup.started_at
        end_time,           -- setup.completed_at
        reason,             -- 'Machine setup: ' + from_product + ' → ' + to_product
        solution,           -- 'Auto-recorded from MachineSetupJob #' + setup.id
        technician_id       -- setup.completed_by_user_id
    )
    
    UPDATE machine_setup_job SET duration_min =
        DATEDIFF(MINUTE, started_at, completed_at)
    WHERE id = setup.id
```

OEE Availability is already calculated as:
```
Availability = (Planned time - Downtime) / Planned time
```
No changes are required in the existing OEE service — the new `SETUP` category downtime rows are automatically included.

---

## Required-Before Time Defaults

| Shift | Required-before | Rationale |
|---|---|---|
| Day (D) | `07:00` | Production starts at 07:00; setup must complete before shift begins |
| Night (N) | `19:00` | Night shift starts at 19:00 |

These defaults are stored in `machine_setup_job.required_before` at job creation.

**Future enhancement (out of Phase 1 scope):** configurable per machine in a `machine_shift_config` table. The column is already on the entity, so no schema change will be needed.

---

## Setup Job Creation Logic (Service Method)

```
createSetupJobIfNeeded(machine, fromProductId, toPlanEntry):

    1. Check for existing PENDING/IN_PROGRESS job for same (machine, plan_date)
       
       2a. If PENDING exists → mark SUPERSEDED, proceed to step 3
       2b. If IN_PROGRESS exists → return CONFLICT (UI shows warning, no creation yet)
       2c. If none → proceed to step 3

    3. Determine required_before:
       shift = toPlanEntry.shift ?? deriveShift(toPlanEntry.plan_date)
       required_before = (shift == 'D') ? '07:00' : '19:00'

    4. INSERT machine_setup_job (
           machine_id            = machine.id,
           from_product_id       = fromProductId,    -- NULL for first setup
           to_product_id         = toPlanEntry.product_id,
           production_plan_id    = toPlanEntry.id,
           plan_date             = toPlanEntry.plan_date,
           required_before       = required_before,
           status                = 'PENDING'
       )

    5. (Optional) notify assigned Technician via NotificationService
```

---

## Checklist Items per Setup Job

The setup job captures these boolean checkpoints (all default `false`):

| Field | Description | Required for completion |
|---|---|---|
| `mold_changed` | Physical mold swap completed | Depends on product |
| `mold_code_from` / `mold_code_to` | Mold tracking codes | If `mold_changed = true` |
| `temp_adjusted` | Extruder/head temperatures re-profiled | Yes |
| `cycle_adjusted` | Cycle time re-set for new product | Yes |
| `blow_pin_aligned` | Blow pin gap aligned for new bottle geometry | Yes |
| `fpi_passed` | First-piece inspection passed by QA | Yes |

Status → `COMPLETED` is only allowed when `fpi_passed = true` (enforced in service layer).
