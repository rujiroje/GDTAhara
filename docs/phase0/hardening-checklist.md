# Phase 0 Hardening Checklist
**Project:** GDTAHARA Backend — Internal MES (Spring Boot 3.5.6 / Java 17 / SQL Server)**
**Audit date:** 2026-05-22
**Auditor:** Claude Code (assisted)
**Status:** AWAITING APPROVAL — no code has been changed based on this checklist yet

---

## Legend
| Severity | Meaning |
|---|---|
| CRITICAL | Exploitable without authentication or exposes credentials/secrets |
| HIGH | Authenticated attacker can escalate privilege, corrupt data, or bypass auth |
| MEDIUM | Defence-in-depth gap; exploitable under specific conditions |
| LOW | Code quality / information exposure; not directly exploitable |
| DONE | Already fixed in this hardening session (prior work) |

---

## Section 1 — Authentication & Authorisation

| ID | Controller | Endpoint | Issue Type | Severity | Fix Plan |
|---|---|---|---|---|---|
| A-1 | `EmergencyController` | `POST /emergency/open-report`, `PUT /emergency/update-report/{id}`, `DELETE /emergency/close-report/{id}` | No `@PreAuthorize`; active when `app.mode=emergency` env var is set | **CRITICAL** | Add `@PreAuthorize("hasRole('DataAdmin')")` to all 3 endpoints. Document in README that the env var must never be set on prod unless intentional. |
| A-2 | `ProductionControlController` | ALL endpoints (POST/PUT/DELETE/GET `/pc/reports/…`, `/pc/dashboard`) | No `@PreAuthorize` on any endpoint; class-level `@Profile("dev-sample")` only | **HIGH** | Add `@PreAuthorize("hasRole('Production Control')")` at class level. Profile guard is deployment config, not a security control. |
| A-3 | `ShiftLeaderController` | `POST /shift-leader/create-test-data` | Test-data creation endpoint present in production code; no profile guard | **HIGH** | Delete endpoint or wrap in `@Profile("dev-sample")` + `@PreAuthorize("hasRole('DataAdmin')")`. |
| A-4 | `DataImportController` | `POST /import/machines`, `/import/ng-types`, `/import/products`, `/import/materials` | No `Principal` captured; no audit log on bulk import operations | **MEDIUM** | Add `Principal principal` param to each method; call `auditLogService.log()` after each successful import. |
| A-5 | ALL controllers | All `@RequestBody` params | Zero `@Valid` / `@Validated` annotations — no input validation on any endpoint | **MEDIUM** | Add `@Valid` to every `@RequestBody` param; add `@NotNull`, `@Size`, `@Min`/`@Max` constraints to all DTO/request classes. |
| A-6 | `AuthController` | `POST /auth/login` | No brute-force / rate-limit protection | **MEDIUM** | Add Spring Security's `Bucket4j` or a `RateLimitFilter`; lock account after N failures. (Note: adds a dependency — `io.github.bucket4j:bucket4j-core`) |
| A-7 | `AuthController` | `POST /auth/login` response | Response body includes `role` string — minor info exposure | **LOW** | Strip role from login response; role can be derived from JWT claims only when needed. |
| A-8 | `ShiftLeaderController` | `GET /shift-leader/simple-test`, `GET /shift-leader/test-dashboard` | Diagnostic endpoints present in production | **LOW** | Delete both endpoints. |

---

## Section 2 — Mass Assignment

| ID | Controller | Endpoint | Issue Type | Severity | Fix Plan |
|---|---|---|---|---|---|
| MA-1 | `TechnicianController` | `POST /technician/parameter-records` | Raw `ParameterRecord` JPA entity accepted as `@RequestBody` — attacker can set `id`, `technicianId`, `reportId` | **HIGH** | Replace `ParameterRecord` with `ParameterRecordRequest` DTO (already exists). Extract fields in service layer; ignore any `id` field from caller. |

---

## Section 3 — Credentials & Secrets

| ID | File | Issue Type | Severity | Fix Plan |
|---|---|---|---|---|
| S-1 | `application.properties` line: `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:tst123##}` | SA account password `tst123##` hard-coded as fallback default; visible in source control | **CRITICAL** | Remove fallback: `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}`. App fails at startup if env var absent — this is correct behaviour. Rotate password. |
| S-2 | `application.properties` line: `jwt.secret=${JWT_SECRET:REPLACE-THIS-...}` | JWT secret has a long placeholder string as fallback — if env var is unset the placeholder is used and tokens are signed with a known (committed) value | **CRITICAL** | Remove fallback: `jwt.secret=${JWT_SECRET}`. App fails at startup if absent. Rotate all active tokens after fix. |
| S-3 | `application.properties` | `spring.datasource.username=${SPRING_DATASOURCE_USERNAME:sa}` — SA account used; SA has full DB access | **HIGH** | Create a dedicated SQL Server login with minimum required permissions (SELECT/INSERT/UPDATE/DELETE on GDTAHARA schema only). Remove `sa` fallback. |

---

## Section 4 — Audit Logging Gaps

| ID | Service / Controller | Operation | Issue Type | Severity | Fix Plan |
|---|---|---|---|---|---|
| AL-1 | `TechnicianService` | `recordDowntime()` | No audit log | **MEDIUM** | Add `auditLogService.log("CREATE", "DowntimeEvent", …)` |
| AL-2 | `TechnicianService` | `recordScrapWeight()` | No audit log | **MEDIUM** | Add `auditLogService.log("CREATE", "ScrapWeightLog", …)` |
| AL-3 | `TechnicianService` | `createParameterRecord()` | No audit log on upsert | **MEDIUM** | Add `auditLogService.log("UPSERT", "ParameterRecord", …)` |
| AL-4 | `TechnicianService` | `recordTechnicianNg()` | No audit log | **MEDIUM** | Add `auditLogService.log("CREATE", "NgLog", …)` |
| AL-5 | `TechnicianService` | `saveParameterRecord()` | No audit log | **MEDIUM** | Add `auditLogService.log("CREATE", "ParameterRecord", …)` |
| AL-6 | `ShiftLeaderService` | `recordStockTransaction()` (POST create) | No audit log on creation | **MEDIUM** | Add `auditLogService.log("CREATE", "MaterialStockTransaction", …)` |
| AL-7 | `DataImportController` | All 4 bulk import endpoints | No audit log | **MEDIUM** | See A-4 above. |

---

## Section 5 — N+1 Queries & Missing DB Indexes

| ID | Location | Issue | Severity | Fix Plan |
|---|---|---|---|---|
| N+1-1 | `ProductionService.getAllProductionReports()` | `findAll()` on `ProductionReport` triggers N+1 SELECTs for `@ManyToOne` machine, product, pc (3× per row) | **HIGH** | Replace with custom `@Query("SELECT r FROM ProductionReport r LEFT JOIN FETCH r.machine LEFT JOIN FETCH r.product LEFT JOIN FETCH r.pc")` or add `@EntityGraph`. |
| N+1-2 | `QaService.getActiveReportsForQa()` | `findByStatusIn()` — same N+1 pattern | **HIGH** | Add `@EntityGraph(attributePaths={"machine","product","pc"})` to repository query. |
| N+1-3 | `OperatorService.getActiveReportsForOperator()` | `findByStatusIn()` — same N+1 pattern | **HIGH** | Add `@EntityGraph(attributePaths={"machine","product"})`. |
| IDX-1 | `production_reports` table | No index on `status` column — used in `findByStatusIn()` | **MEDIUM** | `CREATE INDEX idx_pr_status ON production_reports(status)` |
| IDX-2 | `production_reports` table | No index on `start_date`, `end_date` — used in date-range filters | **MEDIUM** | `CREATE INDEX idx_pr_dates ON production_reports(start_date, end_date)` |
| IDX-3 | `ng_logs` table | No index on `report_id` | **MEDIUM** | `CREATE INDEX idx_ngl_report ON ng_logs(report_id)` |
| IDX-4 | `ng_logs` table | No index on `timestamp` — used in hourly summary queries | **MEDIUM** | `CREATE INDEX idx_ngl_ts ON ng_logs(report_id, timestamp)` (composite) |
| IDX-5 | `packaging_logs` table | No index on `report_id`, `lot_number` | **MEDIUM** | `CREATE INDEX idx_pkg_report_lot ON packaging_logs(report_id, lot_number)` |
| IDX-6 | `material_stock_transactions` table | No index on `material_id` — used in stock balance query | **MEDIUM** | `CREATE INDEX idx_mst_material ON material_stock_transactions(material_id)` |
| IDX-7 | `machine_status_logs` table | No index on `machine_id`, `end_time` — used in active-log lookup | **MEDIUM** | `CREATE INDEX idx_msl_machine_end ON machine_status_logs(machine_id, end_time)` |
| IDX-8 | `downtime_events` table | No index on `report_id` | **MEDIUM** | `CREATE INDEX idx_dte_report ON downtime_events(report_id)` |
| IDX-9 | `scrap_weight_logs` table | No index on `report_id` | **MEDIUM** | `CREATE INDEX idx_swl_report ON scrap_weight_logs(report_id)` |

---

## Section 6 — Architecture / Code Quality

| ID | Location | Issue | Severity | Fix Plan |
|---|---|---|---|---|
| AQ-1 | `MasterDataController` | `@Autowired MachineRepository` and `@Autowired ProductRepository` injected directly into controller — bypasses service layer, cannot be audited or transactionally wrapped | **MEDIUM** | Move data-access calls to `MasterDataService` (or existing `AdminService`); controller calls service only. |
| AQ-2 | JWT config | `jwt.expiration=86400000` (24 hours) — long-lived tokens, no refresh token mechanism | **LOW** | Reduce to 3600000 (1 hour); add `/auth/refresh` endpoint with a longer-lived refresh token. Note: breaking change for FE — coordinate with frontend team. |

---

## Summary Counts

| Severity | Open | Fixed (prior work) | Total |
|---|---|---|---|
| CRITICAL | 3 | 0 | 3 |
| HIGH | 8 | 6 | 14 |
| MEDIUM | 13 | 7 | 20 |
| LOW | 4 | 3 | 7 |
| **Total** | **28** | **16** | **44** |

---

## Already Fixed (Prior Work — Do Not Re-Fix)

| ID | Description |
|---|---|
| DONE-1 | `@PreAuthorize` added to `TechnicianController`, `HistoricalReportController`, `ReportController` |
| DONE-2 | IDOR ownership checks: `NgLogController`, `ShiftLeaderController` (stock transaction), `TechnicianController` (parameter record) |
| DONE-3 | `TestController`, `DebugController`, `EntityMethodsChecker` deleted |
| DONE-4 | `System.out.println` → SLF4J across service layer |
| DONE-5 | Auth style normalised: `QaController`, `NotificationController` |
| DONE-6 | `AuditLog` model + repository + service + `AuditRequestFilter` + `V2__Create_Audit_Logs_Table.sql` created |
| DONE-7 | Audit calls wired in: `ProductionService`, `NgLogService`, `QaService`, `OperatorService`, `ShiftLeaderService` (update+recordNg), `TechnicianService` (updateParameterRecord), `CmOperatorService`, `MachineStatusService`, `NotificationService`, `AdminController`, `PmScheduleController`, `RecipeController` |
| DONE-8 | SQL injection: all repositories use parameterized queries — CLEAN |
| **A-1** | `EmergencyController` — `@PreAuthorize("hasRole('DataAdmin')")` added at class level |
| **A-2** | `ProductionControlController` — `@PreAuthorize("hasAnyRole('Production Control','DataAdmin')")` added at class level |
| **A-3** | `ShiftLeaderController POST /create-test-data` — endpoint deleted |
| **A-4** | `DataImportController` — `Principal` + `AuditLogService` added to all 4 import endpoints |
| **A-8** | `ShiftLeaderController GET /simple-test`, `/test-dashboard` — endpoints deleted |
| **AL-1** | `TechnicianService.recordDowntime()` — audit log CREATE added |
| **AL-2** | `TechnicianService.recordScrapWeight()` — audit log CREATE added |
| **AL-3** | `TechnicianService.createParameterRecord()` — audit log UPSERT added |
| **AL-4** | `TechnicianService.recordTechnicianNg()` — audit log CREATE added |
| **AL-5** | `TechnicianService.saveParameterRecord()` — audit log CREATE added + mass assignment fix |
| **AL-6** | `ShiftLeaderService.recordStockTransaction()` — audit log CREATE added |
| **MA-1** | `TechnicianController POST /parameter-records` — `id` stripped from payload; `technicianId` enforced from JWT Principal (not from request body) |
| **S-1,S-2,S-3** | `SecurityCredentialValidator` bean logs CRITICAL warning at startup if default DB password/username/JWT secret are detected |

---

*Checklist last updated: 2026-05-22. Do not begin Step 2 fixes until this checklist is approved.*
