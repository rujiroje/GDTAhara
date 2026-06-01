# W10 Deployment Log

**Date:** 2026-06-01
**DB:** GDTahara @ 10.1.53.33 (SQL Server 2022 Standard)
**Owner:** Rujiroje
**Branch:** feature/phase1-w10-migrations

## Migrations Applied

| Version | File | Applied At | Status |
|---|---|---|---|
| V2 | Create_Audit_Logs_Table | 2026-05-29 17:56 | ✅ |
| V3 | Add_Performance_Indexes | 2026-05-29 17:56 | ✅ |
| V4 | Create_production_plan | 2026-05-29 18:22 | ✅ |
| V5 | Create_machine_setup_job | 2026-05-29 18:22 | ✅ |
| V6 | Create_sub_lot | 2026-06-01 10:00 | ✅ |
| V7 | Alter_production_reports_add_plan_link | 2026-06-01 10:00 | ✅ |
| V8 | Create_or_seed_plan_sheet_template | 2026-06-01 10:10 | ✅ |
| V9 | Add_indexes_and_import_log | 2026-06-01 10:10 | ✅ |

## Discoveries

1. **plan_sheet_template** existed before Flyway (manually created in SSMS).
   V8 uses IF NOT EXISTS + MERGE — preserved existing data.

2. **shift column** was missing from production_reports.
   Manually added via SSMS during W10.3; V7 made idempotent.

3. **actual_qty column** was missing from production_reports.
   V7 adds it (needed for diff_qty PERSISTED column).

4. **Phase 0 hidden bug — V2 audit_logs**: never applied because Flyway
   was not configured. AuditLog feature was non-functional in runtime
   until W10.1 installed Flyway. ALL audit log entries were silently
   discarded prior to 2026-05-29.

5. **Phase 0 hidden bug — V3 indexes**: same root cause. Query
   performance was sub-optimal.

## Backup

Pre-W10 backup taken via SSMS BACKUP DATABASE command on 2026-05-29.
File: C:\Backup\GDTahara_pre_phase1_w10_2026-05-29.bak

## Outstanding (Not Done — Future Work)

- Rollback test was NOT executed (DB is non-production with <500 rows,
  rollback scripts in docs/phase1/rollback/ are verified by inspection only)
- ProductionReport.java entity has no actualQty field — Phase 1 W11
  must add it to align with the new actual_qty column
- Existing audit_logs table is empty (no historical audit before today)
