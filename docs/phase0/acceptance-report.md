# Phase 0 Hardening — Acceptance Report

**Project:** GDTAHARA Backend  
**Stack:** Spring Boot 3.5.6 / Java 17 / SQL Server  
**Report date:** 2026-05-22  
**Author:** Rujiroje  

---

## Summary

| # | Criterion | Status | Notes |
|---|---|---|---|
| 1 | Security — @PreAuthorize + tests | ⚠️ Partial | Tests written & passing; M-1–M-5, M-7 pending; M-6 closed 2026-05-22 |
| 2 | AuditLog — every write logged + tests | ✅ Done | 6 tests passing |
| 3 | DB Performance — N+1 fixed + indexes | ✅ Done | findAllWithFetch, 8 indexes, 1 test |
| 4 | Benchmark — 1 000 ops/sec | ⏳ Pending | Requires running environment |
| 5 | CI/CD — green from 1 push, gate on fail | ✅ Done | `.github/workflows/ci.yml` |
| 6 | Monitoring — /actuator/prometheus + Grafana | ✅ Done | Dashboard JSON in docs/grafana/ |
| 7 | Stability — 48-hour test | ⏳ Pending | Requires running environment |
| 8 | Docs — this report | ✅ Done | — |

---

## 1. Security

### Completed fixes

| ID | Fix | File |
|---|---|---|
| S-1/S-2 | Default credential warning on startup | `config/SecurityCredentialValidator.java` |
| AC-1 | `LoginRateLimitFilter` — 10 attempts/min per IP | `filter/LoginRateLimitFilter.java` |
| MA-1 | `saveParameterRecord` strips `id`/`technicianId` from request body | `service/TechnicianService.java` |
| AC-2 | `@Valid` added to all write-endpoint `@RequestBody` params | Multiple controllers |
| AC-3 | `@PreAuthorize` added to `EmergencyController`, `ProductionControlController`, `CmOperatorController`, `OperatorController`, `ShiftLeaderController`, `TechnicianController`, `AdminController` | Multiple controllers |
| AQ-1 | `MasterDataController` no longer injects repositories directly | `controller/MasterDataController.java` |

### Pending security fixes (M-1 – M-7)

These items were identified in the hardening checklist but not yet implemented:

| ID | Description | File |
|---|---|---|
| M-1 | Add `@PreAuthorize` to 5 individual `TechnicianController` endpoints | `controller/TechnicianController.java` |
| M-2 | Add `@PreAuthorize` to `HistoricalReportController` | `controller/HistoricalReportController.java` |
| M-3 | Add `@PreAuthorize` to `ReportController` | `controller/ReportController.java` |
| M-4 | IDOR check on `NgLogController` PUT/DELETE (ownership verify) | `controller/NgLogController.java` |
| M-5 | IDOR check on `ShiftLeaderController` stock-transaction PUT | `controller/ShiftLeaderController.java` |
| M-7 | Standardize `hasAnyAuthority` → `hasAnyRole` in `QaController`, `NotificationController` | Two controllers |

> **Risk:** M-4/M-5 are IDOR vulnerabilities — any authenticated user can modify another user's records. Prioritise before go-live.

### Closed since 2026-05-22

| ID | Description | Resolution |
|---|---|---|
| M-6 | IDOR check on `TechnicianController` parameter record PUT | Extracted `isAdminOrOwner()` helper in `TechnicianService`; bypasses DataAdmin only (aligned with NgLogService). 3 tests added. |

### Tests written (21 passing)

| Test class | Coverage |
|---|---|
| `AuditLogServiceTest` | save() called; exception swallowing |
| `TechnicianServiceTest` | id stripped; technicianId forced from JWT; audit logs for downtime/scrap/NG/paramRecord; IDOR owner/admin/other-tech scenarios |
| `ProductionServiceQueryTest` | findAllWithFetch() called, findAll() never called |
| `LoginRateLimitFilterTest` | 10 pass / 11th blocked; 429 body; separate IP buckets; path routing |
| `SecurityAnnotationComplianceTest` | @PreAuthorize present on all write controllers; AuthController intentionally open |

---

## 2. AuditLog

All write operations in the following services now emit an `AuditLog` row via `AuditLogService.log()`:

- `TechnicianService` — `recordDowntime`, `recordScrapWeight`, `recordTechnicianNg`, `saveParameterRecord`, `createParameterRecord`, `updateParameterRecord`
- `ShiftLeaderService` — `recordStockTransaction`
- `DataImportController` — all 4 bulk-import endpoints
- `CmOperatorService` — `recordStockOut`

`AuditLogService` uses `@Transactional(REQUIRES_NEW)` with full try/catch — a logging failure never rolls back the business transaction.

---

## 3. DB Performance

### N+1 fixes

| Service | Before | After |
|---|---|---|
| `ProductionService.getAllProductionReports()` | `findAll()` → N+1 on machine/product | `findAllWithFetch()` with `LEFT JOIN FETCH` |
| `QaService.getActiveReportsForQa()` | `findByStatusIn()` | `findByStatusInWithFetch()` |
| `OperatorService.getActiveReportsForOperator()` | `findByStatusIn()` | `findByStatusInWithFetch()` |

### Indexes added (`V3__Add_Performance_Indexes.sql`)

| Index | Table | Columns |
|---|---|---|
| `idx_pr_status` | `production_reports` | `status` |
| `idx_pr_dates` | `production_reports` | `start_date, end_date` |
| `idx_ngl_report_ts` | `ng_logs` | `report_id, timestamp` |
| `idx_pkg_report_lot` | `packaging_logs` | `report_id, lot_number` |
| `idx_mst_material` | `material_stock_transactions` | `material_id` |
| `idx_msl_machine_end` | `machine_status_logs` | `machine_id, end_time` |
| `idx_dte_report` | `downtime_events` | `report_id` |
| `idx_swl_report` | `scrap_weight_logs` | `report_id` |

---

## 4. Benchmark

**Status: ⏳ Not yet measured**

Target: sustain ≥ 1 000 requests/second on the production endpoint `GET /api/production/reports`.

Recommended tool: **k6**

```bash
k6 run --vus 50 --duration 60s docs/phase0/load-test.js
```

---

## 5. CI/CD

Pipeline: `.github/workflows/ci.yml`

```
push / PR → main
    │
    ├─ [test]          mvn verify          → gate: test failure
    └─ [security-scan] mvn dependency-check:check -P security-scan
                                            → gate: CVSS ≥ 7
```

**Pending:** Register NVD API key at nvd.nist.gov and add as GitHub secret `NVD_API_KEY` (Cloudflare error prevented registration — retry with Firefox/Incognito, VPN off).

---

## 6. Monitoring

| Component | Detail |
|---|---|
| Endpoint | `GET /actuator/prometheus` (open, restrict at firewall level in production) |
| Health | `GET /actuator/health` (public, used by load balancer) |
| Metrics tagged | `application=gdtahara-backend`, `environment=${APP_ENV}` |
| Grafana dashboard | `docs/grafana/gdtahara-dashboard.json` |

**Dashboard panels:** HTTP req/s, p95/p99 latency, 5xx error rate, HikariCP pool, acquire time p95, JVM heap, CPU, threads.

**Recommended Prometheus alert rules (add to Prometheus config):**

```yaml
groups:
  - name: gdtahara
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{application="gdtahara-backend",status=~"5.."}[5m]) > 0.05
        for: 2m
        labels: { severity: warning }
        annotations: { summary: "5xx error rate > 5%" }

      - alert: SlowResponse
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="gdtahara-backend"}[5m])) > 2
        for: 5m
        labels: { severity: warning }
        annotations: { summary: "p95 latency > 2s" }

      - alert: DBPoolExhausted
        expr: hikaricp_connections_pending{application="gdtahara-backend"} > 5
        for: 1m
        labels: { severity: critical }
        annotations: { summary: "HikariCP pending connections > 5" }
```

---

## 7. Stability

**Status: ⏳ Not yet run**

Requires the application running on the production SQL Server for ≥ 48 hours.  
Monitor via Grafana dashboard for memory leaks, connection pool exhaustion, and error rate spikes.

---

## Outstanding items before go-live

| Priority | Item | Owner |
|---|---|---|
| 🔴 HIGH | Fix IDOR on NgLog/StockTransaction (M-4, M-5) | Dev |
| 🔴 HIGH | Add @PreAuthorize to HistoricalReportController, ReportController (M-2, M-3) | Dev |
| 🟡 MED | Register NVD API key + add GitHub secret `NVD_API_KEY` | Ops |
| 🟡 MED | Create dedicated SQL Server login (least-privilege, not `sa`) | DBA |
| 🟡 MED | Run k6 benchmark — verify ≥ 1 000 req/s | Dev/Ops |
| 🟡 MED | Run 48-hour stability test | Ops |
| 🟢 LOW | Reduce JWT expiry from 24h to 4h (coordinate with FE team) | Dev |
