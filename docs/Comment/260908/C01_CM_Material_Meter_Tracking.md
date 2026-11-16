# C-01 · CM Material Meter Tracking · Refill + Shift Handoff + Consumption Report

**Role:** CM (Change Mold / Charge Material)
**Priority:** 🔥 Critical
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** L (Large · ~10-12 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment R30-39 + follow-up 10 Sep 2026
**Related:**
- **O-12** (Packaging deduction · this handles ROH/pellet side)
- **OP-WS-01** (session · machine · shift concepts)
- **NG-WS-01** (Telegram bot reuse for alerts)
- **T-01** (attachment_photo · optional for refill photos)

---

## 📋 Summary · สรุป

CM ต้องบันทึกการใช้เม็ดพลาสติก (ROH) โดยหลักการ **Meter Simulation** (ไม่ใช่ realtime sensor):
- แต่ละ Machine มี Tanks (ตายตัว · ตามชนิดเม็ด)
- Meter = ระดับเม็ดในถัง · เพิ่มเมื่อเติม · ลดตามผลิต
- Shift Handoff: กะก่อนบันทึกเลข End · กะใหม่ใช้เป็นเลข Start
- Consumption per shift = Start + Refills - End

**3 Workflows:**
1. **Refill Entry** · เมื่อเติมเม็ด (ระหว่างกะ · ไม่จำกัดจำนวนครั้ง)
2. **End-of-Shift Meter** · บันทึก Meter สุดท้าย → auto handoff
3. **Correction** · กรอกผิด → **บันทึกรายการติดลบ** (ไม่ลบข้อมูล) · full audit

**Alerts:**
- 30 นาที หลังเวลาตัดกะ · Machine ไหนยังไม่มี End Meter → alert CM (in-app + Telegram)

---

## 🎯 Design Decisions · Confirmed by User

| # | Topic | Decision |
|---|---|---|
| 1 | CM/Machine relationship | 1 CM → many machines · หลาย CM แบ่งงานได้ · **เลือก machine ทุกครั้ง** |
| 2 | Refill event | **1 refill = 1 event** · ระบุจำนวนจริง (partial lot OK) |
| 3 | Tank config | **ตายตัวต่อ machine** · Admin config · ระบบแนะนำเม็ดที่ตรง tank |
| 4 | Realtime meter | **No sensor** · pure shift-based simulation |
| 5 | Correction | **No edit · allow negative qty** · require note if negative · full audit |
| 6 | Missing meter alert | **30 min after shift boundary** · in-app + Telegram (CM room) |
| 7 | Report scope | **Per machine · per shift** (focus) |
| 8 | Stock integration | **Deduct actual** from LD/Doc stock-in · track remaining |

---

## 🏗️ Data Flow Diagram

```
                                      ┌──────────────────────┐
                                      │ Material Stock-In    │
                                      │ (LD/Doc existing)    │
                                      │ Lot A123: 500 KG     │
                                      │ Lot A124: 500 KG     │
                                      │ Lot A125: 500 KG     │
                                      └────────┬─────────────┘
                                               │ Available
                                               ↓
                          ┌────────────────────────────────┐
                          │ CM Refill Entry                 │
                          │ Machine RBL103 · Tank A         │
                          │ Material NPE-F1 (tank suggests) │
                          │ Lot A123 · +300 KG              │
                          └────────┬───────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ↓                    ↓                    ↓
       tank_refill log      material_stock_in    Tank A Current
       (audit)              remaining -= 300      Meter += 300


📅 Shift Cycle (Shift A · 07:00-15:00 · Machine RBL103)
───────────────────────────────────────────────────────

07:00  Start · Start Meter = 1520 (auto from Shift Y's End Meter)
       ↓
09:30  Refill · Tank A · Lot A123 · +500 KG · Meter now: 2020
       ↓
12:00  Refill · Tank A · Lot A124 · +300 KG · Meter now: 2320
       ↓
14:50  CM records End Meter: 1850 KG
       ↓
15:00  System: Consumption Report
       Tank A · Start 1520 + Refills 800 - End 1850 = Used 470 KG
       → Handoff: Shift B's Start Meter = 1850

🚨 Missed Recording Scenario
─────────────────────────────
15:00  Shift boundary
15:30  Check: RBL103 · Tank A · no End Meter · Shift A
       → In-app alert to CM users
       → Telegram to @gdtahara_cm room
       "⚠️ RBL103 Tank A · missed End Meter · Shift A · 15 min ago"
```

---

## 🎨 UI/UX Design

### 1️⃣ CM Dashboard (Landing)

```
┌─ CM Dashboard · Shift A · 10 Sep 2026 · CM: Somchai ────┐
│                                                            │
│  Machine ▾  [RBL103 ▾]  · [เปลี่ยน Machine]              │
│                                                            │
│  🏭 RBL103 · Tank Status (current shift)                  │
│  ┌────────────┬─────────┬──────────┬───────────────┐    │
│  │ Tank       │ Type    │ Current  │ Last Activity │    │
│  ├────────────┼─────────┼──────────┼───────────────┤    │
│  │ 🅰 Tank A  │ NPE-F1  │ 2320 KG  │ Refill 12:00  │    │
│  │ 🅱 Tank B  │ EVOH-FA │ 1850 KG  │ Refill 09:15  │    │
│  │ 🅲 Tank C  │ ADS-LA  │  850 KG  │ (no today)    │    │
│  └────────────┴─────────┴──────────┴───────────────┘    │
│                                                            │
│  Actions:                                                  │
│  [➕ บันทึกการเติมเม็ด]                                    │
│  [📊 บันทึก Meter · จบกะ]                                  │
│  [✏️ บันทึกแก้ไข (ติดลบ)]                                 │
│                                                            │
│  📅 Refill History Today (RBL103):                         │
│  09:15 · Tank B · EVOH-FA · +200 KG · Lot B450 · CM Sarah │
│  09:30 · Tank A · NPE-F1 · +500 KG · Lot A123 · CM Somchai│
│  12:00 · Tank A · NPE-F1 · +300 KG · Lot A124 · CM Somchai│
│                                                            │
│  🔗 Other Machines · Quick Status:                         │
│  RBL102: All tanks OK · Last activity 11:30              │
│  RBL104: ⚠️ Tank C low (350 KG) · consider refill        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Features:**
- Machine picker (default = last used)
- Realtime tank status (computed from refills + last meter)
- Quick access to 3 main actions
- Overview across all machines for CM shift planning

---

### 2️⃣ บันทึกการเติมเม็ด · Refill Entry

**Flow: cascading dropdowns (per user's 5-step description)**

```
┌─ บันทึกการเติมเม็ด ─────────────────────────────────────┐
│                                                            │
│  🕐 เวลา: 12:00 · 10 Sep 2026 (auto)                     │
│  👤 CM: Somchai (auto · session)                          │
│  📅 Shift: A (07:00-15:00) (auto based on time)           │
│                                                            │
│  Step 1 · เลือก Machine                                   │
│  [RBL103 ▾]                                                │
│                                                            │
│  Step 2 · เลือก Tank (แสดง tanks ของ machine นี้)         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                     │
│  │ 🅰 Tank A│ │ 🅱 Tank B│ │ 🅲 Tank C│                     │
│  │ NPE-F1  │ │ EVOH-FA │ │ ADS-LA  │                     │
│  │ 2020 KG │ │ 1850 KG │ │  850 KG │                     │
│  │  [◉]    │ │  [○]    │ │  [○]    │                     │
│  └─────────┘ └─────────┘ └─────────┘                     │
│                                                            │
│  Step 3 · ชนิดเม็ด (auto-suggest ตาม tank type)           │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Tank A ปกติใช้: NPE-F1                              │  │
│  │ ✅ [NPE-F1 ▾] · locked (default match)              │  │
│  │                                                      │  │
│  │ ⚠️ หากต้องเลือกเม็ดอื่น (เปลี่ยน tank type):        │  │
│  │ [แสดง alternate materials ▾] · require note         │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  Step 4 · เลือก Lot (จาก stock ที่ LD/Doc เบิกเข้ามา)     │
│  ┌────────────────────────────────────────────────────┐  │
│  │ Available Lots · NPE-F1 · RBL103:                   │  │
│  │ ○ Lot A124 · 25 KG remain (in from 08:00)          │  │
│  │ ○ Lot A125 · 500 KG remain (in from 11:30) · fresh │  │
│  │ ○ Lot A126 · 500 KG remain (in from 11:45)         │  │
│  │                                                      │  │
│  │ Sort by: [FIFO ▾]  (oldest first · recommended)     │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  Step 5 · จำนวนที่เติม                                     │
│  Selected Lot: A125 · 500 KG remain                       │
│  จำนวนที่เติม: [_300_] KG                                  │
│  ⚠️ ไม่เกิน Lot remaining                                  │
│                                                            │
│  📊 Preview:                                               │
│  Tank A: Before 2020 KG → After 2320 KG (+300)            │
│  Lot A125: Before 500 KG → After 200 KG                   │
│                                                            │
│  หมายเหตุ (optional): [_________________________]         │
│                                                            │
│                       [ยกเลิก]  [✓ บันทึกการเติม]         │
└────────────────────────────────────────────────────────────┘
```

**Rules:**
- Tank type → material dropdown auto-suggests correct type
- If CM overrides material type (rare) → require note (audit)
- Lot list filtered by: material type + available stock at this machine
- Qty max = selected Lot remaining
- Save → INSERT tank_refill + UPDATE material_stock_in remaining

---

### 3️⃣ End-of-Shift Meter Recording

```
┌─ บันทึก Meter · จบกะ Shift A · 14:50 · RBL103 ──────────┐
│                                                            │
│  📋 Summary · Shift A                                     │
│  Machine: RBL103 · CM: Somchai · 07:00-15:00              │
│                                                            │
│  ⏱️ กรุณาบันทึก Meter สุดท้ายของทุก tank:                │
│                                                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 🅰 Tank A · NPE-F1                                  │  │
│  │ ตั้งต้น (07:00):  1520 KG                          │  │
│  │ เติมระหว่างกะ:   +800 KG (2 events · below)         │  │
│  │ Expected (calc): 2320 KG                            │  │
│  │ 🔢 Meter สุดท้าย: [___1850___] KG                  │  │
│  │ → คำนวณ Used = 1520 + 800 - 1850 = 470 KG          │  │
│  │                                                      │  │
│  │  Refill events this shift:                          │  │
│  │  · 09:30 · +500 KG · Lot A123                       │  │
│  │  · 12:00 · +300 KG · Lot A124                       │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 🅱 Tank B · EVOH-FA                                 │  │
│  │ ตั้งต้น: 1650 KG · เติม +200 · Expected: 1850 KG   │  │
│  │ 🔢 Meter สุดท้าย: [___1720___] KG                  │  │
│  │ → Used = 130 KG                                     │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 🅲 Tank C · ADS-LA                                  │  │
│  │ ตั้งต้น: 850 KG · เติม +0 · Expected: 850 KG       │  │
│  │ 🔢 Meter สุดท้าย: [___820___] KG                   │  │
│  │ → Used = 30 KG                                      │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  ⚠️ Anomaly check: consumption > 500 KG?                  │
│  → Tank A used 470 KG · within normal range ✓             │
│                                                            │
│  หมายเหตุ (optional): [__________________________]         │
│                                                            │
│         [ยกเลิก]  [✓ บันทึกจบกะ · Handoff Shift B]       │
└────────────────────────────────────────────────────────────┘
```

**On Save (transactional):**
1. INSERT `tank_meter_reading` (type=SHIFT_END) for each tank
2. Compute + INSERT `shift_material_consumption` (per tank · precomputed)
3. Auto-INSERT next shift's SHIFT_START = this SHIFT_END (handoff)
4. Generate report preview
5. Notify Shift B CM (in-app + optional Telegram)

**Post-save Report Preview:**
```
┌─ ✅ Shift A · Summary · RBL103 ────────────────────────────┐
│                                                              │
│  Date: 10 Sep 2026 · Shift A · 07:00-15:00 · CM: Somchai   │
│                                                              │
│  Tank    | Type     | Start | Refill | End   | Used        │
│  ─────── ──────────  ─────── ──────── ─────── ──────────    │
│  Tank A  | NPE-F1   | 1520  | +800   | 1850  | 470 KG     │
│  Tank B  | EVOH-FA  | 1650  | +200   | 1720  | 130 KG     │
│  Tank C  | ADS-LA   | 850   | 0      | 820   | 30 KG      │
│  Total consumption:                            | 630 KG     │
│                                                              │
│  📦 Lot Balance After Shift:                                 │
│  · NPE-F1: 700 KG remaining (Lot A125: 200, Lot A126: 500) │
│  · EVOH-FA: 400 KG remaining (Lot B450: 400)               │
│  · ADS-LA: 850 KG remaining                                 │
│                                                              │
│  ⚠️ Alert LO for reorder: NPE-F1 low (< 1000 KG safety)     │
│                                                              │
│  🔗 Handoff to Shift B:                                      │
│  · Start Meters recorded                                     │
│  · Shift B CM notified                                       │
│                                                              │
│                          [Print Report] [Send to Manager]    │
└──────────────────────────────────────────────────────────────┘
```

---

### 4️⃣ Correction · Negative Entry (per user decision #5)

**Rationale:** No edit · no delete · add negative record instead · audit safe

```
┌─ บันทึกแก้ไข · Correction Entry ────────────────────────┐
│                                                            │
│  ⚠️ ระบบไม่อนุญาตแก้ไข refill ที่บันทึกแล้ว              │
│  หากกรอกผิด · ใส่รายการติดลบเพื่อชดเชย                    │
│                                                            │
│  🔍 หา Refill ที่ผิด:                                     │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 09:30 · Tank A · +500 KG · Lot A123 · Somchai      │  │
│  │                                    [เลือก]         │  │
│  │ 12:00 · Tank A · +300 KG · Lot A124 · Somchai      │  │
│  │                                    [เลือก]         │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  Selected: 09:30 · +500 KG · Lot A123                     │
│                                                            │
│  📝 New correction entry:                                  │
│  Machine:  RBL103                                          │
│  Tank:     🅰 Tank A                                       │
│  Material: NPE-F1                                          │
│  Lot:      A123 (auto · matches original)                 │
│  จำนวน:    [-100] KG  ← negative value                    │
│                                                            │
│  ⚠️ ระบบตรวจพบเลขติดลบ · จำเป็นต้องมีหมายเหตุ            │
│  หมายเหตุ * (required):                                    │
│  ┌────────────────────────────────────────────────────┐  │
│  │ [กรอกผิด · จริงๆ เติมแค่ 400 KG (500 - 100)]      │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│  📷 แนบรูป (optional · reuse T-01 pattern)                │
│                                                            │
│  📊 Effect:                                                │
│  Tank A total after correction: 2320 - 100 = 2220 KG      │
│  Lot A123 stock: 0 + 100 back = 100 KG return             │
│                                                            │
│                       [ยกเลิก]  [✓ บันทึก Correction]     │
└────────────────────────────────────────────────────────────┘
```

**Rules:**
- Correction = negative qty in tank_refill table (with is_correction=TRUE flag)
- Note field REQUIRED if qty < 0 (validated)
- Photo optional (reuse attachment_photo · ref_type='TANK_REFILL')
- Both records visible in history (original + correction)
- Stock returns to Lot (add back qty)
- All aggregates recompute correctly (SUM includes negatives)

**Display in history:**
```
09:30 · Tank A · +500 KG · Lot A123 · Somchai · 💚
14:30 · Tank A · -100 KG · Lot A123 · Somchai · 🔴 CORRECTION
       Note: "กรอกผิด · จริงๆ เติมแค่ 400 KG"
```

---

### 5️⃣ Missed-Meter Alert (30 min after shift boundary)

**Scheduler runs every minute · checks boundaries:**

```
Shift boundaries (configurable · admin):
- 03:00 · end of Shift C / start of Shift A
- 15:00 · end of Shift A / start of Shift B
- 23:00 · end of Shift B / start of Shift C
  (example · adjust per factory)

At each boundary + 30 min (03:30, 15:30, 23:30):
  For each active machine:
    For each tank:
      Check: SHIFT_END meter recorded for the ending shift?
      If NO:
        Create shift_meter_reminder
        Broadcast WebSocket to CM (in-app popup)
        Send Telegram to @gdtahara_cm room
```

**Alert Message:**
```
🚨 CM Meter Missing · RBL103 · Tank A

Shift A ended 15:00 · but no Meter recorded
Elapsed: 30 min

Please record ASAP · block next shift consumption calc

Machine: RBL103
Tank: A (NPE-F1)
Expected Meter based on refills: ~2320 KG

Link: https://mes.local/cm/end-of-shift?machine=RBL103
```

**Repeat:**
- Every 30 min if still missing
- Escalate to Manager after 2 hours (via @gdtahara_manager room)

---

## 🗄️ Data Model Changes (V29 Migration)

```sql
-- V29__cm_material_meter_tracking.sql

-- ============================================
-- 1. Machine Tank Master (config per machine)
-- ============================================

CREATE TABLE machine_tank (
  tank_id VARCHAR(50) PRIMARY KEY,           -- e.g. 'RBL103_A'
  machine_id VARCHAR(20) NOT NULL,
  tank_code VARCHAR(20) NOT NULL,            -- 'A' | 'B' | 'C'
  tank_name VARCHAR(100),
  default_material_code VARCHAR(50) NOT NULL, -- expected pellet
  capacity_kg DECIMAL(10,2),
  physical_position VARCHAR(50),             -- optional · location info
  is_active BOOLEAN DEFAULT TRUE,
  UNIQUE (machine_id, tank_code),
  INDEX idx_mt_machine (machine_id)
);

-- ============================================
-- 2. Meter Readings (start/end/adhoc per shift)
-- ============================================

CREATE TABLE tank_meter_reading (
  reading_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tank_id VARCHAR(50) NOT NULL,
  machine_id VARCHAR(20) NOT NULL,           -- denorm for query speed
  reading_type VARCHAR(20) NOT NULL,         -- 'SHIFT_START' | 'SHIFT_END' | 'ADHOC'
  meter_value DECIMAL(10,2) NOT NULL,        -- KG
  shift_code VARCHAR(10) NOT NULL,           -- 'A' | 'B' | 'C'
  shift_date DATE NOT NULL,
  recorded_by VARCHAR(50) NOT NULL,
  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  note VARCHAR(500),
  auto_generated BOOLEAN DEFAULT FALSE,      -- true if handoff auto-copied
  source_reading_id BIGINT NULL,             -- FK if auto-copied from END → START
  CONSTRAINT fk_tmr_tank FOREIGN KEY (tank_id) REFERENCES machine_tank(tank_id),
  CONSTRAINT chk_tmr_type CHECK (reading_type IN ('SHIFT_START','SHIFT_END','ADHOC')),
  INDEX idx_tmr_shift (tank_id, shift_date, shift_code, reading_type),
  INDEX idx_tmr_machine_shift (machine_id, shift_date, shift_code)
);

-- ============================================
-- 3. Refill Events (with lot traceability)
-- ============================================

CREATE TABLE tank_refill (
  refill_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tank_id VARCHAR(50) NOT NULL,
  machine_id VARCHAR(20) NOT NULL,           -- denorm
  material_code VARCHAR(50) NOT NULL,         -- denorm
  material_stock_id BIGINT NOT NULL,          -- FK to material_stock_in (LD/Doc's Lot)
  material_lot_code VARCHAR(50) NOT NULL,     -- denorm
  qty_kg DECIMAL(10,2) NOT NULL,              -- can be NEGATIVE (correction)
  is_correction BOOLEAN DEFAULT FALSE,        -- true if correction entry
  corrects_refill_id BIGINT NULL,             -- FK to original if is_correction
  refilled_by VARCHAR(50) NOT NULL,
  refilled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  shift_code VARCHAR(10) NOT NULL,
  shift_date DATE NOT NULL,
  material_type_override BOOLEAN DEFAULT FALSE, -- true if CM overrode tank's default
  note VARCHAR(500),                          -- REQUIRED if qty_kg < 0
  photo_ids JSON NULL,                        -- optional · reuse attachment_photo
  CONSTRAINT fk_tr_tank FOREIGN KEY (tank_id) REFERENCES machine_tank(tank_id),
  CONSTRAINT fk_tr_stock FOREIGN KEY (material_stock_id) REFERENCES material_stock_in(id),
  CONSTRAINT fk_tr_corrects FOREIGN KEY (corrects_refill_id) REFERENCES tank_refill(refill_id),
  INDEX idx_tr_tank_time (tank_id, refilled_at),
  INDEX idx_tr_shift (machine_id, shift_date, shift_code),
  INDEX idx_tr_lot (material_stock_id)
);

-- ============================================
-- 4. Shift Consumption Summary (denormalized · precomputed)
-- ============================================

CREATE TABLE shift_material_consumption (
  consumption_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tank_id VARCHAR(50) NOT NULL,
  machine_id VARCHAR(20) NOT NULL,
  material_code VARCHAR(50) NOT NULL,
  shift_code VARCHAR(10) NOT NULL,
  shift_date DATE NOT NULL,
  starting_meter_kg DECIMAL(10,2) NOT NULL,
  total_refills_kg DECIMAL(10,2) NOT NULL,   -- SUM including negative corrections
  ending_meter_kg DECIMAL(10,2) NOT NULL,
  computed_used_kg DECIMAL(10,2) NOT NULL,   -- start + refills - end
  refill_count INT,
  correction_count INT DEFAULT 0,
  computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  computed_by VARCHAR(50),
  is_final BOOLEAN DEFAULT FALSE,             -- true when SHIFT_END recorded
  UNIQUE (tank_id, shift_date, shift_code),
  INDEX idx_smc_machine_shift (machine_id, shift_date, shift_code)
);

-- ============================================
-- 5. Shift Boundary Configuration
-- ============================================

CREATE TABLE shift_boundary_config (
  boundary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  shift_code VARCHAR(10) NOT NULL UNIQUE,
  start_time TIME NOT NULL,                   -- e.g. 07:00
  end_time TIME NOT NULL,                     -- e.g. 15:00
  reminder_grace_minutes INT DEFAULT 30,
  is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO shift_boundary_config (shift_code, start_time, end_time) VALUES
  ('A', '07:00', '15:00'),
  ('B', '15:00', '23:00'),
  ('C', '23:00', '07:00');
-- Admin can adjust per factory

-- ============================================
-- 6. Missed Meter Reminder (audit + notifications)
-- ============================================

CREATE TABLE shift_meter_reminder (
  reminder_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  tank_id VARCHAR(50) NOT NULL,
  shift_code VARCHAR(10) NOT NULL,
  shift_date DATE NOT NULL,
  expected_by TIMESTAMP NOT NULL,             -- shift_end + grace
  first_alerted_at TIMESTAMP,
  last_alerted_at TIMESTAMP,
  alert_count INT DEFAULT 0,
  telegram_sent BOOLEAN DEFAULT FALSE,
  escalated_to_manager BOOLEAN DEFAULT FALSE,
  resolved_at TIMESTAMP NULL,                 -- when SHIFT_END recorded
  status VARCHAR(20) DEFAULT 'PENDING',
  CONSTRAINT chk_smr_status CHECK (status IN ('PENDING','RESOLVED','ESCALATED')),
  INDEX idx_smr_pending (status, expected_by)
);

-- ============================================
-- 7. Update attachment_photo constraint (from T-01)
-- ============================================

ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST',
                       'QA_VERIFICATION', 'BOX_REPRINT', 'TANK_REFILL'));

-- ============================================
-- 8. Add CM room to telegram_room_config (from NG-WS-01)
-- ============================================

INSERT INTO telegram_room_config (room_code, room_name, description) VALUES
  ('CM', 'GDTAHARA CM Team', 'CM team · material tracking + meter reminders');
```

---

## 🔌 API Endpoints

### Refill
- `POST /api/cm/refill` — save refill (allow negative qty · require note)
- `GET /api/cm/refill/history?machineId=X&from=Y&to=Z`
- `GET /api/cm/tank-status?machineId=X` — realtime current meter per tank

### Meter Recording
- `POST /api/cm/meter/end-of-shift` — batch save all tanks + generate report
- `GET /api/cm/meter/current?machineId=X&shift=Y&date=Z` — for start meter suggest
- `POST /api/cm/meter/adhoc` — mid-shift meter reading (rare · e.g., audit)

### Stock Integration
- `GET /api/cm/available-lots?machineId=X&materialCode=Y` — filter available stock for refill
- (Stock deduction happens atomically in refill endpoint)

### Report
- `GET /api/cm/shift-report?machineId=X&shift=Y&date=Z` — consumption + lot balance

### Correction
- `POST /api/cm/refill/correction` — create negative entry (validates note)
- `GET /api/cm/refill/{id}/corrections` — list corrections for this refill

### Admin
- `GET/POST/PATCH /api/admin/machine-tank` — tank config CRUD
- `GET/POST/PATCH /api/admin/shift-boundary` — shift times config

### Alerts
- Scheduler internal: checks + creates reminders + Telegram

---

## 🖥️ Frontend Components (React)

```
src/components/CM/
  ├── CMDashboard.tsx              (NEW · landing · realtime tank status)
  ├── RefillEntry.tsx              (NEW · cascading dropdown · 5 steps)
  ├── EndOfShiftMeter.tsx          (NEW · batch meter for all tanks + report)
  ├── CorrectionEntry.tsx          (NEW · negative qty · require note)
  ├── RefillHistory.tsx            (NEW · table with corrections visible)
  ├── ShiftReport.tsx              (NEW · consumption + balance)
  └── LotBalanceView.tsx           (NEW · quick view remaining per material)

src/components/Admin/
  ├── MachineTankConfig.tsx        (NEW · CRUD tank per machine)
  └── ShiftBoundaryConfig.tsx      (NEW · shift times + reminder grace)

src/hooks/
  ├── useCMTankStatus.ts           (NEW · realtime state)
  ├── useAvailableLots.ts          (NEW · fetch stock filtered)
  └── useShiftReport.ts            (NEW · aggregate query)
```

---

## ✅ Acceptance Criteria

### Refill Entry
- [ ] Cascading dropdown: Machine → Tank → Material (auto-suggest) → Lot → Qty
- [ ] Tank type suggests default material · override requires note
- [ ] Lot filter: material match + available at this machine
- [ ] Qty ≤ Lot remaining
- [ ] On save: INSERT tank_refill + UPDATE material_stock_in atomically
- [ ] Can be negative (correction) with mandatory note

### End-of-Shift Meter
- [ ] Show all tanks with: starting + refills + expected + input for actual
- [ ] Anomaly check (unusual consumption warning)
- [ ] On save: batch INSERT SHIFT_END + auto-INSERT next SHIFT_START (handoff)
- [ ] Compute + INSERT shift_material_consumption per tank
- [ ] Display post-save report preview

### Correction (Negative Entry)
- [ ] Cannot edit existing records (immutable)
- [ ] "Correction" flow: new record with negative qty
- [ ] System validates: qty < 0 → require note (block if missing)
- [ ] Link to original via corrects_refill_id
- [ ] Stock returns to Lot (add back)
- [ ] Both visible in history

### Missing Meter Alert
- [ ] Scheduler runs every minute
- [ ] After shift_end + 30 min · check for missing SHIFT_END readings
- [ ] Create shift_meter_reminder record
- [ ] In-app popup + Telegram to @gdtahara_cm room
- [ ] Repeat every 30 min · escalate to Manager after 2 hours

### Stock Integration
- [ ] Refill deducts from material_stock_in remaining (atomic)
- [ ] Negative correction returns qty to Lot
- [ ] End-of-shift report shows lot balances
- [ ] Alert when material low (threshold configurable)

### Dashboard
- [ ] Machine picker (default = last used per CM)
- [ ] Realtime tank status (computed from meter + refills)
- [ ] Refill history today (with corrections marked)
- [ ] Quick status of other machines

### Testing
- [ ] Unit: consumption calculation (start + refills - end)
- [ ] Unit: negative qty validation
- [ ] Integration: refill → stock deduct → consumption
- [ ] Integration: end-of-shift → handoff to next shift
- [ ] E2E: full flow · refill 2x → end-of-shift → verify report
- [ ] E2E: correction · original then negative · verify aggregates
- [ ] Scheduler test: missed meter → alert → Telegram
- [ ] Cross-shift handoff test (Shift A → Shift B automatic)

---

## 🚀 Migration Plan

**V29** — see SQL above

**Data seeding:**
- Admin config all tanks per machine (one-time · ~15 machines × 3 tanks = 45 rows)
- Shift boundaries (3 rows · adjust per factory)
- Bootstrap: initial meter reading (starting values from physical inventory)

**Deploy phase:**
1. Deploy V29 · schema in place
2. Admin config tanks · bootstrap initial meters
3. Frontend CM screens live · Pilot machine only (1 week shadow)
4. Enable alerts on Pilot
5. Roll out all machines

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V29 migration + seed | 0.75 day |
| Backend · Tank + refill + meter services | 1.5 days |
| Backend · Stock integration (deduct + return) | 0.75 day |
| Backend · Consumption calculator + handoff logic | 1 day |
| Backend · Missed-meter scheduler + alerts | 1 day |
| Backend · Correction endpoint + validation | 0.5 day |
| Frontend · CM Dashboard | 1 day |
| Frontend · Refill Entry (cascading dropdowns) | 1 day |
| Frontend · End-of-Shift Meter batch input | 1 day |
| Frontend · Correction Entry with negative | 0.75 day |
| Frontend · Refill History + Shift Report | 0.75 day |
| Frontend · Admin config screens (tank + shift) | 0.75 day |
| Testing + integration | 1.5 days |
| **Total** | **~12.25 days (L)** |

---

## 🔗 Dependencies

- **material_stock_in table** (LD/Doc's existing system) · must be able to atomically deduct/return
- **T-01 (V22)** · attachment_photo (add TANK_REFILL ref_type)
- **NG-WS-01 (V26)** · Telegram bot service + CM room
- **OP-WS-01 (V25)** · session concepts · (CM has similar session · but on different device?)

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Multi-CM · multi-machine | Real workflow · flexible |
| 2 | 1 refill = 1 event · partial lot OK | Simple · accurate |
| 3 | Tank config fixed per machine | Realistic · prevents wrong material |
| 4 | No realtime sensor · shift simulation | Practical for GDTAHARA phase 1 |
| 5 | No edit · negative qty correction | Audit safe · elegant · full history |
| 6 | 30 min grace + Telegram alert | Ensures shift handoff data |
| 7 | Report per machine per shift | Focus · daily report ready |
| 8 | Deduct real stock | Accurate inventory · trigger reorder |

---

## 🔮 Future Enhancements

- Realtime sensor integration (Phase 7 IIoT · when installed)
- Predictive: low-stock forecast per machine (ML on refill patterns)
- Auto-reorder to LO when threshold hit
- Mobile app for LO to receive reorder requests
- Material cost tracking per shift (once cost data available)
- Cross-shift consumption trend analysis
- Machine efficiency dashboard (KG/hour per machine)

---

## 🎯 Integration with Daily Production Report

- Per-shift consumption per machine per tank
- Aggregated: material used per product per day
- Balance: stock remaining · reorder alerts
- CM shift KPIs (accuracy · timely recording · correction rate)
