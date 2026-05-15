# GDTAHARA — System Overview

> Blow Molding Production Management System  
> Toyo Seikan (Thailand) Co., Ltd.  
> Last updated: 2026-04-30

---

## Table of Contents

1. [System Summary](#1-system-summary)
2. [Tech Stack](#2-tech-stack)
3. [Features](#3-features)
4. [Architecture](#4-architecture)
5. [Database Schema](#5-database-schema)
6. [API Endpoints](#6-api-endpoints)
7. [Security & Roles](#7-security--roles)
8. [Frontend Components](#8-frontend-components)
9. [Users & Scale](#9-users--scale)
10. [Integrations & Configuration](#10-integrations--configuration)

---

## 1. System Summary

GDTAHARA เป็นระบบบริหารการผลิต Blow Molding แบบครบวงจร ออกแบบมาสำหรับโรงงาน Toyo Seikan (Thailand) ครอบคลุมตั้งแต่การรับวัตถุดิบ → การตั้งค่าการผลิต → การบันทึก Parameters → การควบคุมคุณภาพ → การรายงาน โดยรองรับผู้ใช้งานหลายบทบาทพร้อมกัน

---

## 2. Tech Stack

### Backend

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 3.5.6 |
| Language | Java | 17 |
| Build | Maven | 3.x |
| ORM | Spring Data JPA / Hibernate | managed |
| Security | Spring Security + JWT (jjwt) | 0.11.5 |
| Database | Microsoft SQL Server | 2019+ |
| CSV | OpenCSV | 5.9 |
| Utilities | Lombok, Jackson | managed |
| Validation | Jakarta Validation + Hibernate Validator | managed |

### Frontend

| Layer | Technology |
|-------|-----------|
| Framework | React (Vite) |
| Build Tool | Vite |
| Package Manager | npm |
| HTTP Client | Axios |
| Routing | React Router |
| i18n | Custom i18n (Thai/English) |

### Infrastructure

| Item | Value |
|------|-------|
| Backend Port | 8080 |
| Frontend Dev Port | 5173 |
| Database Host | 10.1.53.33:1433 |
| Database Name | GDTahara |
| Session Strategy | Stateless (JWT) |

---

## 3. Features

### Production Management
- สร้าง/จัดการ Production Report ประจำวัน (Work Order, เครื่อง, ผลิตภัณฑ์, กะ)
- ติดตาม Target Qty vs Actual Qty
- รองรับการ Finalize รายงาน

### Parameter Recording (Technician)
- บันทึก Parameter เครื่องจักร 100+ ค่า ต่อ record
  - Extruder Screw: Main / Admer / EVOH / Virgin — RPM, LO Limit, Resin Pressure/Temp, Motor Current
  - Temperature Zones: Main, Admer, EVOH, Virgin, HEAD (D1–D4, L zones) ทุก Zone (°C)
  - Cycle: Cycle Time, Mold Temp, Blow Pressure, Blow Ratio, Air Conditions, Parison Air, Zero/Weight/Span
  - Blow Pin: Left/Right Platen ×6 positions (A–F) each
  - Quality Checks: Emergency Switch, SQ/SS Pieces, Safety Procedures, Notes

### Defect / NG Tracking
- บันทึก NG Log แยกตาม Type
- NG Type Master Data พร้อม Description ภาษาอังกฤษ
- สรุป NG ต่อ Report

### Material & Inventory
- บริหาร Stock วัตถุดิบ (Material Stock Transaction)
- ติดตาม Lot Number
- บันทึก Material Usage per Machine
- CM Operator Stock-Out Workflow

### OEE Calculation
- คำนวณ OEE อัตโนมัติ (Availability × Performance × Quality)
- แสดงใน Dashboard แบบ Real-time

### Machine Status Monitoring
- บันทึก Machine Status Log
- ประวัติสถานะเครื่องจักร
- MachineStatusPanel แสดงผลใน Dashboard

### Downtime Management
- บันทึก Downtime Events พร้อม Category และ Solution
- สรุป Downtime Reason ต่อ Report/Machine

### Preventive Maintenance (PM)
- PM Schedule per Machine
- PmSchedulePanel สำหรับ Technician

### Recipe / Formula Management
- จัดการ Recipe ต่อ Product
- RecipePanel สำหรับ Production Control

### Quality Assurance
- QA Workflow
- Scrap Weight Log

### Reporting & Dashboard
- Dashboard สรุปการผลิตรายวัน/รายกะ
- Historical Report (ค้นหาย้อนหลัง)
- Report Summary รองรับ 2 ภาษา (ไทย/อังกฤษ)
- Export / Data Import (CSV)

### Notification System
- แจ้งเตือน Problem Alert ภายในระบบ

### Label Stock
- ติดตาม Label Inventory

### User & Access Control
- สร้าง User โดย Admin
- กำหนดสิทธิ์ตาม Role (8 บทบาท)
- JWT Token อายุ 24 ชม.

---

## 4. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (React/Vite)                │
│  Operator │ Technician │ PC │ ShiftLeader │ Admin │ Mgmt   │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST + JWT
                     ▼
┌─────────────────────────────────────────────────────────────┐
│               Spring Boot 3.5.6 (Port 8080)                 │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │Controller│→ │ Service  │→ │Repository│→ │   Model   │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘  │
│                                                             │
│  Security Layer: Spring Security + JWT Filter               │
└────────────────────────────┬────────────────────────────────┘
                             │ JDBC (mssql-jdbc)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│          Microsoft SQL Server 2019+ @ 10.1.53.33            │
│                    Database: GDTahara                        │
└─────────────────────────────────────────────────────────────┘
```

**Package Structure**
```
com.gdtahara.gdtaharabackend/
├── config/          # JwtProperties, CORS
├── controller/      # 26 REST Controllers
├── service/         # 16 Business Services
├── repository/      # 21 JPA Repositories
├── model/           # 21 JPA Entities
├── dto/             # 58 DTOs
├── security/        # JWT Filter, SecurityConfig, UserDetailsService
├── exception/       # Custom Exceptions
└── DevLauncher.java
```

---

## 5. Database Schema

### Core Tables

#### `users`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-increment |
| username | VARCHAR | Unique |
| password | VARCHAR | BCrypt hash |
| role | VARCHAR | See roles section |
| created_at | DATETIME | |
| updated_at | DATETIME | |

#### `machines`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| machine_code | VARCHAR | Unique |
| machine_name | VARCHAR | |

#### `products`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| product_code | VARCHAR | |
| product_name | VARCHAR | |

#### `production_reports`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| order_number | VARCHAR | Work Order |
| machine_id | BIGINT FK | → machines |
| product_id | BIGINT FK | → products |
| pc_user_id | BIGINT FK | → users (PC) |
| shift | VARCHAR | กะการผลิต |
| start_date | DATETIME | |
| end_date | DATETIME | |
| status | VARCHAR | OPEN/FINALIZED |
| target_qty | INT | |
| actual_qty | INT | |
| created_at | DATETIME | |
| finalized_at | DATETIME | |

#### `parameter_records`
> 100+ columns — รายการหลักมีดังนี้

| Column Group | Fields |
|---|---|
| Reference | id, report_id FK, technician_id FK, record_time, created_at |
| Main Extruder | main_rpm, main_lo_limit, main_resin_pressure, main_resin_temp, main_motor_current |
| Admer (LO-Left) | admer_rpm, admer_lo_limit, admer_resin_pressure, admer_resin_temp, admer_motor_current |
| EVOH (LO-Right) | evoh_rpm, evoh_lo_limit, evoh_resin_pressure, evoh_resin_temp, evoh_motor_current |
| Virgin (UPL) | virgin_rpm, virgin_lo_limit, virgin_resin_pressure, virgin_resin_temp, virgin_motor_current |
| Temperature | main_c/a_zone_*, admer_c/a_zone_*, evoh_c/a_zone_*, virgin_c/a_zone_* |
| HEAD Temp | head_d1–d4_*, head_l*_* |
| Cycle | cycle_time, mold_temp, blow_pressure, blow_ratio, air_conditions, parison_air |
| Zero/Weight/Span | zero_*, weight_*, span_* |
| Blow Pin | left_platen_pos_a–f, right_platen_pos_a–f |
| Checks | emergency_switch, sq_pieces, ss_pieces, product_quality_check, machine_operation_check, safety_check, notes |

#### `ng_log`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| report_id | BIGINT FK |
| ng_type_id | BIGINT FK |
| quantity | INT |
| noted_by | BIGINT FK → users |
| created_at | DATETIME |

#### `ng_types`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| type_code | VARCHAR |
| type_name | VARCHAR |
| description_en | VARCHAR |

#### `materials`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| material_code | VARCHAR |
| material_name | VARCHAR |
| unit | VARCHAR |

#### `material_stock_transactions`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| material_id | BIGINT FK |
| lot_number | VARCHAR |
| quantity | DECIMAL |
| transaction_type | VARCHAR (IN/OUT) |
| machine_id | BIGINT FK |
| created_at | DATETIME |
| created_by | BIGINT FK → users |

#### `downtime_events`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| report_id | BIGINT FK |
| machine_id | BIGINT FK |
| category | VARCHAR |
| reason | VARCHAR |
| solution | VARCHAR |
| duration_minutes | INT |
| start_time | DATETIME |
| end_time | DATETIME |

#### `machine_status_log`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| machine_id | BIGINT FK |
| status | VARCHAR |
| logged_at | DATETIME |
| noted_by | BIGINT FK |

#### `pm_schedule`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| machine_id | BIGINT FK |
| task_description | VARCHAR |
| scheduled_date | DATE |
| completed | BIT |
| completed_at | DATETIME |

#### `recipes`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| product_id | BIGINT FK |
| recipe_name | VARCHAR |
| parameters | TEXT/JSON |

#### Supporting Tables
- `scrap_weight_log` — บันทึก Scrap Weight ต่อ Report
- `checked_machine_log` — ประวัติการตรวจสอบเครื่อง
- `packaging_log` — บันทึก Packaging operations
- `label_stock` — Label Inventory
- `material_usage_log` — การใช้วัตถุดิบต่อเครื่อง
- `problem_alerts` — การแจ้งปัญหา
- `parameter_checklists` — Template Checklist

---

## 6. API Endpoints

Base URL: `http://<host>:8080`

### Authentication

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Login → JWT Token | Public |
| POST | `/api/auth/register-initial-users` | สร้าง Default Users | Public |

### Production

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/api/production/dashboard-summary` | Dashboard + OEE | PC, DataAdmin, Mgmt, Doc, CMOp |
| GET | `/api/production/reports` | รายการ Report ทั้งหมด | same |
| GET | `/api/production/reports/{id}/report-summary` | สรุป Report (รองรับ lang param) | same |
| GET | `/api/production/machines` | รายการเครื่อง (dropdown) | same |
| GET | `/api/production/products` | รายการสินค้า (dropdown) | same |

### Master Data

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/api/master-data/ng-types` | ประเภท NG | Authenticated |
| GET | `/api/master-data/materials` | รายการวัตถุดิบ | Authenticated |
| GET | `/api/master-data/machines` | รายการเครื่องทั้งหมด | Authenticated |
| GET | `/api/master-data/products` | รายการสินค้าทั้งหมด | Authenticated |

### CM Operator (Stock)

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | `/api/cm-operator/stock-out` | บันทึก Stock-Out | CMOp, DataAdmin |
| GET | `/api/cm-operator/materials/{materialId}/lot-numbers` | Lot Numbers ของวัตถุดิบ | CMOp, DataAdmin |

### Other Controllers (Endpoints ละเอียดแล้วแต่ implementation)

| Controller | Path | Purpose |
|-----------|------|---------|
| MachineController | `/api/machine` | CRUD เครื่องจักร |
| MachineStatusController | `/api/machine-status` | Status Logging |
| OperatorController | `/api/operator` | Operator Operations |
| TechnicianController | `/api/technician` | Parameter Recording |
| ReportController | `/api/reports` | Report Management |
| HistoricalReportController | `/api/historical-reports` | Historical Data |
| OeeController | `/api/oee` | OEE Calculations |
| NgLogController | `/api/ng-log` | NG Logging |
| QaController | `/api/qa` | Quality Assurance |
| ProductController | `/api/product` | Product Management |
| RecipeController | `/api/recipe` | Recipe Management |
| PmScheduleController | `/api/pm-schedule` | PM Scheduling |
| ShiftLeaderController | `/api/shift-leader` | Shift Leader Dashboard |
| DashboardSummaryController | `/api/dashboard-summary` | Summary Data |
| NotificationController | `/api/notification` | Notifications |
| DataImportController | `/api/data-import` | CSV Bulk Import |
| ProductionControlController | `/api/pc` | PC Dashboard |
| PcCompatibilityController | `/api/pc-compatibility` | Compatibility Checks |
| EmergencyController | `/api/emergency` | Emergency Procedures |
| AdminController | `/api/admin` | Admin Operations |

---

## 7. Security & Roles

### Authentication Flow
```
Client → POST /api/auth/login { username, password }
       ← { token: "JWT...", role: "Production Control" }

Client → GET /api/... { Authorization: Bearer <token> }
       ← Data (if authorized)
```

### Roles & Permissions

| Role | Thai | สิทธิ์หลัก |
|------|------|----------|
| `ADMIN` | ผู้ดูแลระบบ | Full access |
| `Operator` | ผู้ปฏิบัติงาน | Production operations |
| `Production Control` | PC | Production dashboard, reports, parameter review |
| `Technician` | ช่างเทคนิค | Parameter recording, PM schedule |
| `DataAdmin` | ผู้ดูแลข้อมูล | Full data access |
| `CM Operator` | CM Operator | Stock-out operations |
| `Document` | Document/รายงาน | Read-only reports |
| `Management` | ฝ่ายบริหาร | Dashboard + reporting view |

### JWT Configuration
- **Algorithm**: HMAC-SHA (jjwt 0.11.5)
- **Secret Key**: กำหนดใน `application.properties` → `jwt.secret`
- **Expiration**: 86,400,000 ms (24 hours)
- **Session**: STATELESS — ไม่มี Server Session

### CORS
- Allowed Origins: `localhost:5173`, `localhost:8081`, `localhost:3000`
- Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH
- Credentials: Allowed
- Max Age: 3600 seconds

---

## 8. Frontend Components

```
gdtahara-frontend/src/
├── App.jsx                          # Root App + Routing
├── api/
│   └── axios.jsx                    # Axios instance + JWT interceptor
├── i18n/                            # Thai/English translations
├── pages/
│   └── Dashboard.jsx                # Main dashboard wrapper
└── components/
    ├── common/
    │   └── MachineStatusPanel.jsx   # แสดงสถานะเครื่องจักร
    ├── operator/
    │   └── OperatorDashboard.jsx    # Dashboard สำหรับ Operator
    ├── pc/
    │   ├── ProductionControlDashboard.jsx  # PC Dashboard
    │   └── RecipePanel.jsx          # จัดการ Recipe
    └── technician/
        ├── TechnicianDashboard.jsx  # Dashboard ช่างเทคนิค
        └── PmSchedulePanel.jsx      # PM Schedule
```

**Role → Component Mapping**

| Role | Component |
|------|-----------|
| Operator | OperatorDashboard |
| Production Control | ProductionControlDashboard + RecipePanel |
| Technician | TechnicianDashboard + PmSchedulePanel |
| Management / Document | Dashboard (read-only view) |
| Admin | Admin panel (via AdminController) |
| CM Operator | Material stock-out flow |

---

## 9. Users & Scale

### Current Scale
- **Production DB**: Remote SQL Server @ 10.1.53.33
- **Concurrent Users**: รองรับ Multi-user พร้อมกัน (Stateless JWT, ไม่มี session lock)
- **Data Volume**: 100+ parameters ต่อ record, บันทึกทุกกะ ทุกเครื่อง → ปีละหลายแสน records

### User Roles Count (Default Setup)
ระบบสร้าง Default Users ตอน Bootstrap: `admin`, `operator`, `pc`, `document`, `management`

### Scalability Notes
- **Horizontal Scale**: Spring Boot Stateless → รองรับ Load Balancer ได้
- **DB Bottleneck**: SQL Server single instance @ 10.1.53.33 — ยังไม่มี Read Replica
- **Connection Pooling**: HikariCP (Spring Boot default)
- **Lazy Loading Disabled**: `open-in-view=false` ป้องกัน N+1 query
- **SQL Logging**: `show-sql=true` (ควร disable ใน Production เพื่อ performance)

---

## 10. Integrations & Configuration

### Database Connection
```
Host:     10.1.53.33:1433
DB Name:  GDTahara
Encrypt:  true (trustServerCertificate=true)
Unicode:  sendStringParametersAsUnicode=true
DDL:      none (manual SQL migrations)
```

### SQL Migration Files
ระบบจัดการ Schema ด้วย Manual SQL Scripts (ไม่ใช้ Flyway/Liquibase):

| File | Purpose |
|------|---------|
| `add-downtime-category.sql` | เพิ่ม Downtime Categories |
| `add-machine-status-log.sql` | สร้างตาราง Machine Status |
| `add-ng-description-en.sql` | เพิ่ม English Description ให้ NG Types |
| `add-pm-schedule.sql` | สร้างตาราง PM Schedule |
| `add-recipe.sql` | สร้างตาราง Recipe |
| `database-unicode-migration.sql` | แก้ Unicode Collation |
| *(และไฟล์อื่นๆ อีก 18 ไฟล์)* | Test data, cleanup, fixes |

### Logging Configuration
```properties
logging.level.com.gdtahara.gdtaharabackend = INFO
logging.level.org.springframework.security = INFO
logging.level.org.hibernate.SQL = DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder = TRACE
```

### No External Integrations
ปัจจุบันระบบทำงาน Self-contained ไม่มี Integration กับ:
- ERP / SAP
- Email / SMS Notification
- Cloud Storage
- BI Tool (Power BI, Tableau)
- Message Queue (Kafka, RabbitMQ)

> หากต้องการ Integration สามารถ extend ได้ผ่าน Service Layer

---

## Appendix — DTO List (58 DTOs)

| Category | DTOs |
|----------|------|
| Auth | LoginRequest, AuthResponse |
| Production | ProductionReportDto, ProductionReportListDto, ProductionReportViewDto, ProductionReportSimpleViewDto, ProductionReportSummaryDto, DetailedProductionReportDto |
| Daily Report | DailyReportDto, DailyReportSummaryDto, DailyShiftSummaryDto, DailyProductionSummaryDto |
| Historical | HistoricalReportDto, HistoricalReportSummaryDto |
| Summary | ReportSummaryDto, PcDashboardSummaryDto, ShiftLeaderDashboardDto |
| Parameters | ParameterRecordDto, ParameterRecordSummaryDto, ParameterRecordViewDto |
| Machine | MachineSimpleDto, MachineStatusLogDto |
| Product | ProductSimpleDto |
| Materials | MaterialSimpleDto, MaterialWithStockDto, MaterialStockCardDto, MaterialStockTransactionDto, StockCardDto, StockCardViewDto, StockTransactionDto, StockTransactionRequestDto, StockOutRequestDto, UpdateStockRequestDto, MaterialTransactionRequestDto, MaterialUsageLogDto, MaterialUsageSummaryDto |
| Quality | NgLogRequestDto, NgLogSummaryDto, NgSummaryDto, NgTypeSummaryDto, ScrapWeightLogDto, ScrapWeightLogRequestDto |
| Operations | PackagingLogRequestDto, PackagingLogViewDto, CheckedMachineLogRequestDto |
| Label | LabelStockDto, LabelStockViewDto, AddLabelStockRequest |
| Alerts | ProblemAlertRequestDto, ProblemAlertViewDto |
| Downtime | DowntimeEventRequestDto, DowntimeEventSummaryDto, DowntimeReasonSummaryDto |
| Other | RecipeDto, OeeResultDto, ShiftDataDto, TechnicianDto, UserDto, CreateUserRequest |
