# C-03 · CM Material Stock View · Stock Card Dashboard

**Role:** CM (view) · LO (informed via low-stock alert)
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M (Medium · ~5-6 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment R43 + follow-up 10 Sep 2026
**Related:**
- **C-01** (Material Meter Tracking · data source for OUT/refill)
- **C-02** (Waste weight · related waste analytics)
- **LD/Doc existing** (material_stock_in · data source for IN)
- **NG-WS-01** (Telegram bot · reuse for low-stock alerts)

---

## 📋 Summary · สรุป

CM ต้องการ dashboard ตรวจสอบยอดคงเหลือของเม็ดพลาสติก (materials) เพื่อ:
- เช็คว่ามีอะไรเหลือเท่าไหร่ (per material · per lot)
- ดูประวัติการเคลื่อนไหว (stock card format · IN/OUT/Balance)
- วางแผนเบิกเพิ่มจาก LO
- ยืนยันยอดกับ physical count

**Data-heavy read-only dashboard** · aggregate จาก existing tables · ไม่ต้อง entity ใหม่มาก

---

## 🎯 Design Decisions

| # | Topic | Decision |
|---|---|---|
| 1 | Data source | material_stock_in (IN) + tank_refill (OUT · from C-01) |
| 2 | Display format | Stock Card style (chronological · IN/OUT/Balance columns) |
| 3 | Aggregation | Per material · per lot · per date range |
| 4 | Filter | Material · date range · machine · lot · shift |
| 5 | Real-time | Query on-demand (materialized view refresh 5 min) |
| 6 | Low-stock alert | Configurable threshold per material · Telegram CM+LO rooms |
| 7 | Export | Excel + PDF |
| 8 | Read-only | No edit from this screen (data comes from C-01/LD/Doc) |

---

## 🎨 UI/UX Design

### 1️⃣ Landing · Material Selector + Overview

```
┌─ CM Stock Dashboard · 10 Sep 2026 · CM: Somchai ────────┐
│                                                            │
│  🔍 Filter                                                │
│  Material: [Search or select ▾]  [All materials]         │
│  Machine:  [All ▾]  Date range: [Today ▾]                 │
│  [🔄 Refresh]  [📊 Export Excel]  [📄 Export PDF]        │
│                                                            │
│  📊 Overview · All Materials (RBL103)                     │
│  ┌──────────────┬───────────┬────────────┬─────────────┐│
│  │ Material     │ Total Bal │ Lots       │ Status       ││
│  ├──────────────┼───────────┼────────────┼─────────────┤│
│  │ NPE-F1       │ 1,700 KG  │ 3 lots     │ ✅ Normal    ││
│  │ EVOH-FA      │   400 KG  │ 1 lot      │ ⚠️ Low       ││
│  │ ADS-LA       │   850 KG  │ 2 lots     │ ✅ Normal    ││
│  │ BPEGF31NPB   │ 2,300 KG  │ 4 lots     │ ✅ Normal    ││
│  └──────────────┴───────────┴────────────┴─────────────┘│
│                                                            │
│  Total: 5,250 KG · 10 lots active                         │
│                                                            │
│  🚨 Alerts:                                                │
│  ⚠️ EVOH-FA · 400 KG · below reorder point (500 KG)      │
│     → Recommend reorder from LO                            │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

### 2️⃣ Material Detail · Per-Lot Breakdown

**Click material row → open detail:**

```
┌─ NPE-F1 · Detail · 10 Sep 2026 ─────────────────────────┐
│                                                            │
│  📊 Total Balance: 1,700 KG                                │
│  Reorder Point: 1,000 KG  ✅ Above                        │
│                                                            │
│  📦 Active Lots (FIFO order · oldest first):              │
│  ┌────────────┬─────────┬────────────┬──────────────────┐│
│  │ Lot Code   │ Balance │ Received   │ Days on hand    ││
│  ├────────────┼─────────┼────────────┼──────────────────┤│
│  │ Lot A123   │  100 KG │ 09/09 08:00│ 1 day           ││
│  │ Lot A124   │  100 KG │ 09/09 14:30│ 0 day           ││
│  │ Lot A125   │  500 KG │ 10/09 11:30│ 0 day (fresh)   ││
│  │ Lot A126   │ 1000 KG │ 10/09 11:45│ 0 day (fresh)   ││
│  └────────────┴─────────┴────────────┴──────────────────┘│
│                                                            │
│  📈 Usage Trend · Last 7 days:                            │
│  [Simple bar chart · daily consumption]                    │
│  Daily avg: 350 KG · Est. days remaining: 4.8 days        │
│                                                            │
│                              [ดู Stock Card เต็ม]         │
└────────────────────────────────────────────────────────────┘
```

---

### 3️⃣ Stock Card · Transaction Log

**The main view CM wants (per user's ask):**

```
┌─ Stock Card · NPE-F1 · 1-10 Sep 2026 ────────────────────┐
│                                                            │
│  🔍 Filter: Machine [All ▾] · Type [All ▾] · Lot [All ▾]  │
│                                                            │
│  ┌──────┬────────┬─────────────┬──────┬──────┬────────┐│
│  │ Date │ Time   │ Description │  In  │ Out  │ Balance ││
│  ├──────┼────────┼─────────────┼──────┼──────┼────────┤│
│  │ 09/09│ 08:00  │ IN · Lot A123│ 500  │      │  500    ││
│  │      │        │ from LD (P.Boy)│      │      │        ││
│  │ 09/09│ 09:30  │ OUT · Refill │      │ 500  │    0    ││
│  │      │        │ RBL103·TankA │      │      │        ││
│  │      │        │ CM Somchai   │      │      │        ││
│  │ 09/09│ 14:30  │ IN · Lot A124│ 500  │      │  500    ││
│  │      │        │ from LD (P.Boy)│      │      │        ││
│  │ 09/09│ 15:00  │ OUT · Refill │      │ 300  │  200    ││
│  │      │        │ RBL103·TankA │      │      │        ││
│  │      │        │ CM Sarah     │      │      │        ││
│  │ 10/09│ 08:00  │ ⚠️ CORRECTION│      │ -100 │  300    ││ ← negative (add back)
│  │      │        │ RBL103·TankA │      │      │        ││
│  │      │        │ CM Somchai   │      │      │        ││
│  │      │        │ Note: กรอกผิด │      │      │        ││
│  │ 10/09│ 11:30  │ IN · Lot A125│ 500  │      │  800    ││
│  │ 10/09│ 11:45  │ IN · Lot A126│ 1000 │      │ 1800    ││
│  │ 10/09│ 12:00  │ OUT · Refill │      │ 300  │ 1500    ││
│  │      │        │ RBL103·TankA │      │      │        ││
│  │      │        │ CM Somchai   │      │      │        ││
│  └──────┴────────┴─────────────┴──────┴──────┴────────┘│
│                                                            │
│  📊 Summary (period):                                      │
│  Total IN:   2,000 KG                                      │
│  Total OUT:    900 KG  (Refills: 1,000 · Corrections: -100)│
│  Ending balance: 1,500 KG (was 500 at start)              │
│                                                            │
│  [📤 Export Excel]  [📄 Export PDF]  [🖨️ Print]         │
└────────────────────────────────────────────────────────────┘
```

**Key features:**
- Chronological · running balance in rightmost column
- Correction entries highlighted (yellow/negative)
- All references clickable (jump to detail)
- Export formats: Excel (raw data · pivot-ready) + PDF (formatted report)

---

### 4️⃣ Multi-Material View · Summary Grid

```
┌─ All Materials · Balance Summary · 10 Sep 2026 ─────────┐
│                                                            │
│  Group by: [Material ▾]  Compare: [7 days ▾]              │
│                                                            │
│  ┌─────────────┬─────────┬─────────┬─────────┬──────────┐│
│  │ Material    │ Balance │ Used    │ Received│ Change   ││
│  │             │ Now     │ 7 days  │ 7 days  │ 7 days   ││
│  ├─────────────┼─────────┼─────────┼─────────┼──────────┤│
│  │ NPE-F1      │ 1700 KG │ 2400 KG │ 3200 KG │ +800 ↑   ││
│  │ EVOH-FA     │  400 KG │  850 KG │  600 KG │ -250 ↓   ││
│  │ ADS-LA      │  850 KG │  180 KG │  200 KG │  +20 →   ││
│  │ BPEGF31NPB  │ 2300 KG │  550 KG │  600 KG │  +50 →   ││
│  └─────────────┴─────────┴─────────┴─────────┴──────────┘│
│                                                            │
│  💡 Insights:                                              │
│  • EVOH-FA · consuming faster than receiving · alert LO   │
│  • NPE-F1 · healthy stock · no action                     │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

### 5️⃣ Low-Stock Alert (Automated)

**Scheduled job runs every 15 min:**

```
For each material:
  balance = SUM(IN) - SUM(OUT)
  IF balance < reorder_point AND NOT already_alerted_today:
    Create low_stock_alert record
    Send Telegram to @gdtahara_cm + @gdtahara_lo rooms
    In-app popup for CM users
```

**Telegram message:**
```
🚨 LOW STOCK ALERT · EVOH-FA

Current balance: 400 KG
Reorder point:   500 KG
Daily avg usage: 120 KG
Est. days left:  3.3 days

Suggested action: Request LD/Doc to prepare next stock-in
Recent activity: Last received 09/09 08:00 (Lot B450 · 600 KG)

@LO please prepare · @CM please plan usage
```

**Reorder point config (Admin):**
```
material_reorder_config:
  material_code
  reorder_point_kg     (e.g. 500 for EVOH-FA)
  safety_stock_kg      (e.g. 200 · absolute minimum)
  reorder_qty_kg       (e.g. 1000 · suggested reorder size)
  lead_time_days       (e.g. 2 · from LO to floor)
  is_active
```

---

## 🗄️ Data Model (Minimal · V31)

**Most data comes from existing tables · few additions:**

```sql
-- V31__cm_stock_dashboard.sql

-- ============================================
-- 1. Reorder Configuration
-- ============================================

CREATE TABLE material_reorder_config (
  material_code VARCHAR(50) PRIMARY KEY,
  reorder_point_kg DECIMAL(10,2) NOT NULL,
  safety_stock_kg DECIMAL(10,2),
  reorder_qty_kg DECIMAL(10,2),
  lead_time_days INT DEFAULT 2,
  is_active BOOLEAN DEFAULT TRUE,
  updated_by VARCHAR(50),
  updated_at TIMESTAMP
);

-- Seed common materials (from BOM example)
INSERT INTO material_reorder_config (material_code, reorder_point_kg, safety_stock_kg, reorder_qty_kg, lead_time_days) VALUES
  ('NPE-F1',      1000, 500,  1500, 2),
  ('EVOH-FA',      500, 200,   800, 3),
  ('ADS-LA',       300, 100,   500, 2),
  ('BPEGF31NPB', 1000, 500,  1500, 2);

-- ============================================
-- 2. Low Stock Alert Log
-- ============================================

CREATE TABLE low_stock_alert (
  alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_code VARCHAR(50) NOT NULL,
  balance_at_alert DECIMAL(10,2),
  reorder_point DECIMAL(10,2),
  est_days_remaining DECIMAL(5,1),
  alert_date DATE NOT NULL,
  telegram_sent BOOLEAN DEFAULT FALSE,
  telegram_sent_at TIMESTAMP,
  resolved_at TIMESTAMP NULL,               -- when balance rises above point
  UNIQUE (material_code, alert_date),
  INDEX idx_lsa_pending (resolved_at, alert_date)
);

-- ============================================
-- 3. Materialized View (for performance)
-- ============================================

-- Option A: SQL view (real-time · slower)
CREATE VIEW v_stock_movement AS
  SELECT 
    'IN' AS movement_type,
    msi.material_code,
    msi.lot_code AS lot_ref,
    msi.qty_kg,
    NULL AS negative_qty,
    msi.received_at AS movement_at,
    msi.received_by AS actor,
    NULL AS machine_id,
    NULL AS tank_id,
    NULL AS note,
    NULL AS is_correction,
    msi.id AS source_id,
    'material_stock_in' AS source_table
  FROM material_stock_in msi
  WHERE msi.is_active = TRUE
  
  UNION ALL
  
  SELECT 
    CASE WHEN tr.qty_kg < 0 THEN 'CORRECTION' ELSE 'OUT' END AS movement_type,
    tr.material_code,
    tr.material_lot_code AS lot_ref,
    CASE WHEN tr.qty_kg > 0 THEN tr.qty_kg ELSE NULL END AS qty_kg,
    CASE WHEN tr.qty_kg < 0 THEN tr.qty_kg ELSE NULL END AS negative_qty,
    tr.refilled_at AS movement_at,
    tr.refilled_by AS actor,
    tr.machine_id,
    tr.tank_id,
    tr.note,
    tr.is_correction,
    tr.refill_id AS source_id,
    'tank_refill' AS source_table
  FROM tank_refill tr;

-- Option B: Materialized view (faster query · refresh 5 min)
-- Use if performance issues arise

-- ============================================
-- 4. Balance View (aggregate)
-- ============================================

CREATE VIEW v_material_balance AS
  SELECT
    material_code,
    SUM(CASE WHEN movement_type = 'IN' THEN qty_kg ELSE 0 END) AS total_in_kg,
    SUM(CASE WHEN movement_type = 'OUT' THEN qty_kg ELSE 0 END) AS total_out_kg,
    SUM(CASE WHEN movement_type = 'CORRECTION' THEN negative_qty ELSE 0 END) AS total_correction_kg,
    SUM(CASE 
      WHEN movement_type = 'IN' THEN qty_kg
      WHEN movement_type = 'OUT' THEN -qty_kg
      WHEN movement_type = 'CORRECTION' THEN -negative_qty  -- negative_qty is negative · flip sign to add back
    END) AS balance_kg,
    COUNT(DISTINCT lot_ref) AS lot_count,
    MAX(movement_at) AS last_movement_at
  FROM v_stock_movement
  GROUP BY material_code;

-- Per-lot balance
CREATE VIEW v_material_lot_balance AS
  SELECT
    material_code,
    lot_ref,
    SUM(CASE 
      WHEN movement_type = 'IN' THEN qty_kg
      WHEN movement_type = 'OUT' THEN -qty_kg
      WHEN movement_type = 'CORRECTION' THEN -negative_qty
    END) AS balance_kg,
    MIN(CASE WHEN movement_type = 'IN' THEN movement_at END) AS received_at,
    MAX(movement_at) AS last_activity_at
  FROM v_stock_movement
  GROUP BY material_code, lot_ref
  HAVING SUM(CASE ... END) > 0;  -- only active lots
```

**Performance note:**
- Start with SQL views · monitor query time
- If slow (> 500ms) · switch to materialized view + 5-min refresh cron

---

## 🔌 API Endpoints

### Query
- `GET /api/cm/stock/overview?date=Y` — all materials summary
- `GET /api/cm/stock/{materialCode}` — material detail + per-lot
- `GET /api/cm/stock/{materialCode}/stock-card?from=X&to=Y&machineId=Z` — transaction log
- `GET /api/cm/stock/multi-material-summary?days=7` — comparison grid
- `GET /api/cm/stock/low-stock` — current alerts

### Export
- `GET /api/cm/stock/export?format=xlsx&materialCode=X&from=Y&to=Z` — Excel
- `GET /api/cm/stock/export?format=pdf&materialCode=X&from=Y&to=Z` — PDF

### Reorder Config (Admin)
- `GET /api/admin/reorder-config` — list all
- `PATCH /api/admin/reorder-config/{materialCode}` — update thresholds

### Alerts
- (scheduled internal · not exposed as public API)

---

## 🖥️ Frontend Components (React)

```
src/components/CM/
  ├── StockOverviewDashboard.tsx     (NEW · landing · summary grid)
  ├── MaterialDetailView.tsx         (NEW · per-material + lots + trend)
  ├── StockCardTable.tsx             (NEW · transaction log · main feature)
  ├── MultiMaterialSummary.tsx       (NEW · comparison grid)
  ├── LowStockBanner.tsx             (NEW · alerts inline)
  └── StockExportModal.tsx           (NEW · Excel/PDF export)

src/components/Admin/
  └── ReorderConfigEditor.tsx        (NEW · threshold config)

src/hooks/
  ├── useStockBalance.ts             (NEW · query v_material_balance)
  ├── useStockCard.ts                (NEW · query v_stock_movement filtered)
  └── useLowStockAlerts.ts           (NEW · realtime alerts)

src/services/
  └── stockExportService.ts          (NEW · Excel + PDF generation)
```

---

## ✅ Acceptance Criteria

### Dashboard
- [ ] Landing shows all materials with balance + status
- [ ] Filter by material · machine · date range
- [ ] Refresh button + auto-refresh every 5 min
- [ ] Low-stock materials highlighted

### Material Detail
- [ ] Per-lot breakdown (FIFO order)
- [ ] Days-on-hand per lot
- [ ] 7-day usage trend chart
- [ ] Estimated days remaining calculation

### Stock Card
- [ ] Chronological transaction log
- [ ] Columns: Date, Time, Description, In, Out, Balance
- [ ] Running balance in each row
- [ ] Corrections highlighted (yellow/negative)
- [ ] Filters: machine, type (IN/OUT/CORRECTION), lot
- [ ] Clickable references to source (refill detail · stock-in detail)

### Multi-Material View
- [ ] Compare balance + used + received across materials
- [ ] 7-day trend arrows (↑ ↓ →)
- [ ] Insights auto-generated (declining materials)

### Low-Stock Alerts
- [ ] Scheduled check every 15 min
- [ ] Telegram to CM + LO rooms
- [ ] In-app banner for CM users
- [ ] Auto-resolve when balance rises above threshold
- [ ] Configurable reorder point per material

### Export
- [ ] Excel export · raw data · pivot-ready columns
- [ ] PDF export · formatted stock card · print-friendly
- [ ] Include summary + transactions

### Performance
- [ ] Overview query < 2 sec
- [ ] Stock card (1 month) < 3 sec
- [ ] Export 3 months data < 10 sec

### Testing
- [ ] Unit: balance calculation with corrections (negative qty)
- [ ] Unit: per-lot balance edge cases
- [ ] Integration: full stock card flow
- [ ] E2E: view overview → click material → view stock card → export
- [ ] Load test: 15 machines · 20 materials · 6 months data
- [ ] Test corrections included correctly in balance

---

## 🚀 Migration Plan

**V31** — see SQL above

**Data seeding:**
- Reorder config for known materials (from BOM analysis)
- Verify views run correctly on existing data

**Deploy:**
- Views deploy · immediate query
- Frontend deploy · CM users see dashboard
- Configure reorder points with LO/PC team

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V31 + views + seed | 0.75 day |
| Backend · Balance query service (with corrections) | 0.75 day |
| Backend · Stock card query with filters | 0.75 day |
| Backend · Export service (Excel + PDF) | 1 day |
| Backend · Low-stock alert scheduler + Telegram | 0.75 day |
| Frontend · Overview Dashboard | 0.75 day |
| Frontend · Material Detail (lots + trend chart) | 0.75 day |
| Frontend · Stock Card table | 0.75 day |
| Frontend · Multi-Material Summary | 0.5 day |
| Frontend · Export modal + trigger | 0.5 day |
| Frontend · Admin Reorder Config | 0.25 day |
| Testing + performance tuning | 1 day |
| **Total** | **~8.5 days (M)** |

---

## 🔗 Dependencies

- **C-01 (V29)** · tank_refill data source (OUT + CORRECTION)
- **LD/Doc existing** · material_stock_in (IN data)
- **NG-WS-01 (V26)** · Telegram bot · add CM + LO rooms if not exist
- Chart library (e.g., Recharts) for trend visualization
- Excel library (e.g., ExcelJS) + PDF library (e.g., pdfmake) for exports

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Read-only from existing data | No new source of truth · use C-01 + LD/Doc |
| 2 | SQL views (start) · materialized if slow | Simplest · monitor performance |
| 3 | FIFO order for lots | Standard practice · oldest first for usage |
| 4 | Include corrections in balance | Accurate reflection of stock |
| 5 | 15-min scheduled low-stock check | Balance freshness vs load |
| 6 | Telegram + in-app for alerts | Reach CM + LO both channels |
| 7 | Excel + PDF export | Flexibility for reporting + audit |
| 8 | Configurable reorder per material | Different lead times · different criticality |

---

## 🔮 Future Enhancements

- Predictive forecasting (ML on usage patterns · Phase 2)
- Auto-reorder to LO (workflow · Phase 2)
- Cost tracking (material cost per KG · Phase 3)
- Waste analytics integrated (from C-02)
- Cross-machine comparison (which machine uses more)
- SAP inventory sync (Phase 3)
- Barcode/QR scan for lot verification

---

## 🎯 Integration with Daily Production Report

- Daily consumption per material per machine (from C-01)
- Balance at end of day
- Reorder recommendations
- Low-stock incidents count
- Material cost trend (future)
