# Documentation Index — GDTAHARA Backend

**Stack:** Spring Boot 3.5 / Java 21 · React 18 + Vite + MUI · SQL Server 2022 · Flyway V48  
**Branch:** `feature/phase1-w11-backend` · Last update: 2026-11-17

---

## Phase 0 — Security Hardening

- [phase0/acceptance-report.md](phase0/acceptance-report.md) — Phase 0 acceptance checklist: security, audit, performance, CI/CD, monitoring status

---

## Phase 1 — CM Material Tracking (W9+)

### Design Documents
- [phase1/README.md](phase1/README.md) — Index: ER, migration plan, sub-lot numbering, setup job logic, Excel importer spec

### Sprint Logs
- [phase1/W11-session-log.md](phase1/W11-session-log.md) — W11 changes: DocHub, ReorderConfigPanel, shift indicator, duplicate fix, API contracts

### Feature Specs
| Feature | Spec | Status |
|---------|------|--------|
| C-01 CM Material Meter | [Comment/260908/C01_CM_Material_Meter_Tracking.md](Comment/260908/C01_CM_Material_Meter_Tracking.md) | ✅ Done (W11 bug fixes applied) |
| C-02 Waste Weight Edit | [Comment/260908/C02_Waste_Weight_Edit_Workflow.md](Comment/260908/C02_Waste_Weight_Edit_Workflow.md) | ✅ Done (DocHub added W11) |
| C-03 CM Stock Dashboard | [Comment/260908/C03_CM_Stock_Card_Dashboard.md](Comment/260908/C03_CM_Stock_Card_Dashboard.md) | ✅ Done (ReorderConfigPanel added W11) |
| C-04 BOM-Tank Role Linking | [phase1/C04-BOM-Tank-Role-Spec.md](phase1/C04-BOM-Tank-Role-Spec.md) | 🔵 Design ready, pending migration |

### API Contracts
- [api-contracts/CONTRACT-C01-cm-meter.md](api-contracts/CONTRACT-C01-cm-meter.md)
- [api-contracts/CONTRACT-C02-cm-waste.md](api-contracts/CONTRACT-C02-cm-waste.md)
- [api-contracts/CONTRACT-C03-cm-stock.md](api-contracts/CONTRACT-C03-cm-stock.md)

### QA
- [phase1/QA-TEST-GUIDE-C01-C02-C03.md](phase1/QA-TEST-GUIDE-C01-C02-C03.md) — Manual UAT test cases TC-01 through TC-03

---

## Pending Phase 1b

| รายการ | หมายเหตุ |
|--------|---------|
| default_material_code 27 ถัง | รอ material code จริงจาก user |
| Excel/PDF export C-03 | Apache POI + iText |
| Telegram notification | TODO stub ใน Scheduler |
| C-04 BOM-Tank migration | รอยืนยัน tank_code A/B/C/D per machine |

---

## Monitoring

- [grafana/gdtahara-dashboard.json](grafana/gdtahara-dashboard.json) — Grafana dashboard JSON (HTTP, HikariCP, JVM panels)
