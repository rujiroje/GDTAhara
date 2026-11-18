# Phase 1 — Design Documents

**W9 · M3 deliverable · Branch: `feature/phase1-lot-design`**

Implementation begins W10. All documents here are design-only (no code changes in W9).

---

## Documents

| Document | Description |
|---|---|
| [lot-hierarchy-er.md](lot-hierarchy-er.md) | ER diagram + field tables for all new entities (PRODUCTION_PLAN, MACHINE_SETUP_JOB, SUB_LOT, IMPORT_LOG) and extensions to PRODUCTION_REPORT |
| [migration-plan.md](migration-plan.md) | Flyway migration files V4–V9: forward SQL skeletons, rollback procedures, backfill strategy, risk register; special handling for pre-existing `plan_sheet_template` table |
| [sub-lot-numbering.md](sub-lot-numbering.md) | Sub-lot and pallet numbering format, uniqueness enforcement (DB constraint + atomic service-layer generation), box-confirmation workflow, ZPL label template |
| [setup-job-logic.md](setup-job-logic.md) | Machine setup job trigger conditions, re-trigger rules (PRESERVE HISTORY), OEE/downtime integration, required-before time defaults, checklist items |
| [excel-importer-spec.md](excel-importer-spec.md) | 3-layer Excel detection strategy (DB config → filename heuristic → auto-detect), import policy (past rows always skipped, future rows upserted), audit log, test scenarios, API contract |

---

## Cross-References

- Phase 0 hardening report: [docs/phase0/acceptance-report.md](../phase0/acceptance-report.md)
- Root documentation index: [docs/README.md](../README.md)

---

## 3-Level Production Hierarchy

```
PRODUCTION_PLAN       (L1) — PC's daily target: Machine × Product × Date
       │
       └── PRODUCTION_REPORT   (L2) — Operator execution = "Parent Lot"
                  │                   1 Plan → N Reports (Day / Night shift)
                  └── SUB_LOT  (L3) — Box confirmation (e.g. 600 pcs/box)
```

Plus `MACHINE_SETUP_JOB` auto-triggered when adjacent plan cells change product.

---

## Pre-Existing Production DB State

> `plan_sheet_template` was manually created in SSMS on 2026-05-29 before W9.  
> V8 migration uses `IF NOT EXISTS` + `MERGE` (idempotent). See [migration-plan.md § V8](migration-plan.md).

---

## W10 Deliverables (Next Sprint)

- Java entity classes: `ProductionPlan`, `MachineSetupJob`, `SubLot`, `ImportLog`
- Flyway migration files V4–V9
- Service layer: `PlanService`, `SetupJobService`, `SubLotService`
- REST endpoints: plan CRUD, Excel import, box confirmation
- Frontend: PC Plan Matrix view, Technician Setup Job panel, Operator Box Confirmation screen
