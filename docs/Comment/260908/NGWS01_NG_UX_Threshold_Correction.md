# NG-WS-01 · NG Recording UX + Threshold Alert + Correction Workflow

**Type:** Workstream (covers 4 issues)
**Priority:** 🔥 Critical (mixed · O-09 O-14 O-16 High, O-04 O-08 Medium)
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** L (Large · ~11-13 dev days)
**Source:** Pilot Feedback 8 Sep 2026 + follow-up discussion 9 Sep 2026
**Covers Issues:** O-04, O-08, O-09, O-16
**Depends on:** OP-WS-01 (position-aware session · Component 3 · Tablet Session)
**Related:** T-03 (WebSocket notification infra + OTP pattern)

---

## 📋 Summary · สรุป

ปรับปรุงระบบบันทึก NG แบบครบวงจร · แก้ 4 ปัญหาหลัก:
1. **UI ปุ่ม NG:** 6 ปุ่มด่วน (fixed) + "อื่นๆ" grid · ลด mispress · font ใหญ่ · glove-friendly
2. **Threshold Alert:** Admin set limit ต่อ machine · เกินแล้วแจ้ง Leader + Tech · escalate Telegram หลัง 5 min
3. **Reset Logic:** Fixed 1-hour clock (07:00, 08:00, ...)
4. **Correction:** Undo 60s window · หลัง 1 นาที lock (no cross-shift)

---

## 🎯 Design Decisions · Confirmed by User

| # | Topic | Decision |
|---|---|---|
| 1 | Top buttons count | **6 ปุ่ม fixed** (Admin-set · locked · not auto) |
| 2 | "อื่นๆ" behavior | **Show all remaining as full grid** (no search/tree) |
| 3 | Save confirmation | **Skip** (no extra tap) |
| 4 | Alert priority | **Broadcast** to Leader + Tech simultaneously |
| 5 | Auto-dismiss on drop | **Not applicable** (NG count only grows) |
| 6 | Escalation | **After 5 min no response → Telegram** (per role room) |
| 7 | Threshold scope | **Per machine** (not per product) |
| 8 | Reset window | **Fixed 1-hour clock** (:00 boundary) |
| 9 | Correction approach | **Option E · Undo 60s + Full log display** |
| 10 | Approval needed | **No** · within 1 min OP self-fix allowed |
| 11 | Cross-shift correction | **Not allowed** · window is 1 min only |
| 12 | OTP pattern | **Reuse T-03** (Tech help arrival pattern) |

---

## 🎨 Component 1 · NG Button UX Redesign (O-04 + O-08)

### 6 Fixed Quick Buttons + "อื่นๆ" Grid

**Layout · Landscape (10.1" tablet):**

```
┌─ บันทึก NG · RBL103 · 🅛 LEFT · OP1 ──────────────────────────┐
│                                                                  │
│  ⭐ ปุ่มด่วน · Top 6 (Admin-configured)                          │
│                                                                  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                  │
│  │    🩹      │ │    ⚫      │ │    ✂️      │                  │
│  │            │ │            │ │            │                  │
│  │  Short     │ │  Black     │ │  Cut       │                  │
│  │  shot      │ │  spot      │ │  mark      │                  │
│  │  วันนี้ 15 │ │  วันนี้ 8  │ │  วันนี้ 3  │                  │
│  └────────────┘ └────────────┘ └────────────┘                  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                  │
│  │    💧      │ │    🌊      │ │    ⚠️      │                  │
│  │            │ │            │ │            │                  │
│  │  Bubble    │ │  Flow      │ │  Contam-   │                  │
│  │            │ │  mark      │ │  ination   │                  │
│  │  วันนี้ 2  │ │  วันนี้ 5  │ │  วันนี้ 1  │                  │
│  └────────────┘ └────────────┘ └────────────┘                  │
│                                                                  │
│  ┌──────────────────────────────────────────────┐              │
│  │ 📋 อื่นๆ (14 ชนิด)  →                          │              │
│  └──────────────────────────────────────────────┘              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Button Specifications:**
- **Size:** 160×160 px minimum (glove-friendly)
- **Font:** 18pt bold · high contrast (dark text on white/color)
- **Icon:** 40px prominent · visual recognition
- **Spacing:** 16px gaps · reduce edge-mispress
- **Live count:** shows today's count per type (helps spot pattern)
- **Tap feedback:** haptic + short animation

**"อื่นๆ" Modal · Show ALL remaining as grid:**

```
┌─ เลือกชนิด NG อื่นๆ ────────────────────────────────────┐
│                                                          │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│  │  🔵   │ │  📏   │ │  ⚡   │ │  🌐   │            │
│  │Weld   │ │Dim off│ │Burn   │ │Warp   │            │
│  │line   │ │spec   │ │mark   │       │            │
│  └────────┘ └────────┘ └────────┘ └────────┘            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│  │  🎨   │ │  🔶   │ │  💫   │ │  📐   │            │
│  │Color  │ │Sink   │ │Silver │ │Shape  │            │
│  │miss   │ │mark   │ │streak │ │      │            │
│  └────────┘ └────────┘ └────────┘ └────────┘            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│  │  🏷️   │ │  🧪   │ │  🕳️   │ │  📎   │            │
│  │Label  │ │Chem   │ │Hole   │ │Attach │            │
│  │issue  │ │spot   │ │defect │ │defect │            │
│  └────────┘ └────────┘ └────────┘ └────────┘            │
│  ┌────────┐ ┌────────┐                                   │
│  │  🌡️   │ │  ❓   │                                   │
│  │Temp   │ │Other  │                                   │
│  │issue  │ │       │                                   │
│  └────────┘ └────────┘                                   │
│                                                          │
│                                      [ปิด]              │
└──────────────────────────────────────────────────────────┘
```

**Behavior:**
- 6 fixed quick buttons + 1 "อื่นๆ" button → 7 buttons on main screen
- "อื่นๆ" opens modal showing all remaining NG types as grid
- Grid: 4 columns · button size ~140×140 · font 14pt (smaller than quick buttons but readable)
- Click any button → open Qty modal · commit save
- No search bar / no category tree (per user's decision: full grid)

---

## 🎨 Component 2 · Save Flow (No Confirmation Preview)

**Per user's decision #3:** ตัด confirmation preview ออก · save ทันทีหลังกด qty

**Flow:**
```
1. OP กดปุ่ม NG (เช่น "Short shot")
   ↓
2. Modal เปิด · แสดง qty picker
   ┌─────────────────────────────┐
   │  Short shot                  │
   │  จำนวน:  [ - ] [ 5 ] [ + ]   │
   │  หมายเหตุ (optional):        │
   │  [_______________]           │
   │  [ยกเลิก] [บันทึก]           │
   └─────────────────────────────┘
   ↓
3. กด "บันทึก" → COMMIT ทันที (no double-confirm)
   ↓
4. Toast แสดงพร้อม 60s Undo button (see Component 4)
```

**Quantity input options:**
- Default: 1
- Number buttons: [-] [+]
- Direct input via number pad (for 100+ scenarios like O-06)
- Save = POST /api/ng/record · atomic

---

## 🚨 Component 3 · Threshold Alert System (O-09)

### Admin Configuration Screen

**Scope: Per Machine** (per user decision #7)

```
┌─ NG Threshold · Machine RBL103 ────────────────────────────────┐
│                                                                  │
│  Reset window: HOURLY (fixed :00 clock)                         │
│                                                                  │
│  ┌─────────────┬──────┬─────────┬────────┬──────────────────┐  │
│  │ NG Type     │ Limit│ Severity│ Notify │ Actions          │  │
│  ├─────────────┼──────┼─────────┼────────┼──────────────────┤  │
│  │ Short shot  │  10  │ HIGH    │ LD+TECH│ [edit] [del]    │  │
│  │ Black spot  │   5  │ MED     │ LD     │ [edit] [del]    │  │
│  │ Cut mark    │  20  │ LOW     │ (log)  │ [edit] [del]    │  │
│  │ Bubble      │   8  │ HIGH    │LD+TECH+QA│ [edit] [del]  │  │
│  │ Flow mark   │  15  │ LOW     │ (log)  │ [edit] [del]    │  │
│  │ Contamination│  3   │ URGENT  │ LD+TECH+QA │ [edit]     │  │
│  └─────────────┴──────┴─────────┴────────┴──────────────────┘  │
│                                                                  │
│  [+ Add rule] [Import from another machine] [Bulk edit]         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Config fields:**
- `machine_id` (required)
- `ng_type_code` (required)
- `threshold_count` (per hour)
- `severity`: LOW / MED / HIGH / URGENT
- `notify_roles`: array of {LEADER, TECH, QA} (multiple)
- `is_active`: bool

---

### Alert Flow · Broadcast + Telegram Escalation

**Step 1 · NG Record Saved · Check Threshold**

```
POST /api/ng/record
Body: { machineId, ngTypeCode, qty, sessionId }

Backend:
  1. Insert ng_record (atomic)
  2. Get current hour count:
     SELECT SUM(qty) FROM ng_record
     WHERE machine_id = :m AND ng_type_code = :t
       AND recorded_at >= DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00');
  3. Load threshold config for (machine, ng_type)
  4. IF current_count > threshold:
     - Create ng_alert record (status=PENDING)
     - Broadcast WebSocket to configured roles
     - Log to ng_alert_event
```

**Step 2 · WebSocket Broadcast (Immediate)**

- Broadcast to **all** users with matching roles (LD/TECH/QA per config)
- No priority sequence · everyone gets popup simultaneously

**Popup (reuse T-03 pattern):**
```
┌─ 🚨 NG THRESHOLD BREACH ────────────────────────┐
│                                                   │
│  Machine:  RBL103 · 🅛 LEFT                       │
│  NG Type:  Short shot                             │
│  Count:    11 pcs in current hour (limit 10)     │
│  Time:     08:47 (~13 min into hour)             │
│  Severity: 🔴 HIGH                                │
│                                                   │
│  Trend chart · last 60 min: [inline sparkline]   │
│                                                   │
│  ┌──────────────┐  ┌────────────┐               │
│  │ ✅ รับเรื่อง │  │ 💤 SNOOZE  │               │
│  │  (ACCEPT)    │  │  5 นาที    │               │
│  └──────────────┘  └────────────┘               │
└───────────────────────────────────────────────────┘
```

**Step 3 · Accept → OTP Arrival (reuse T-03)**

- First person to Accept · claims the alert
- Others get "ALREADY ACCEPTED by [name]" notice · popup dismisses
- Accepted person walks to machine
- OP shows OTP on Tablet (like T-03 arrival flow)
- Verify → arrival logged · investigate

**Step 4 · Telegram Escalation · 5-min timeout**

**If no one accepts within 5 minutes:**

```
Backend scheduler (runs every 30s):
  SELECT * FROM ng_alert
    WHERE status = 'PENDING'
      AND created_at < NOW() - INTERVAL 5 MINUTE
      AND telegram_escalated = FALSE;
  
  FOR EACH alert:
    - Determine target Telegram room based on notify_roles
    - Send message via Telegram Bot API
    - Set telegram_escalated = TRUE
    - Log to ng_alert_event
```

**Telegram Room Structure (per user decision #6):**

| Room | Purpose | Members | Alerts |
|---|---|---|---|
| **@gdtahara_general** | ห้องรวม · everyone | LD + TECH + QA + Manager + PC + PI | Cross-role critical events |
| **@gdtahara_leader** | LD room | Leader team | Alerts to LEADER role |
| **@gdtahara_tech** | Tech room | Tech team | Alerts to TECH role |
| **@gdtahara_qa** | QA room | QA team | Alerts to QA role |

**Telegram message format:**
```
🚨 NG THRESHOLD BREACH · UNANSWERED 5min

Machine: RBL103 · LEFT
NG Type: Short shot
Count: 11/10 (breach at 08:47)
Time now: 08:52
Severity: HIGH
Notify: LEADER + TECH

⚠️ No one accepted in MES for 5 min.
Please respond ASAP.

Alert ID: NG-2026-09-09-00042
Link: https://mes.local/alert/NG-2026-09-09-00042
```

**After Telegram send:**
- Alert stays PENDING · waiting for MES Accept
- If still no accept in another 5 min · escalate to PD Manager room (@gdtahara_general with @manager mention)
- If accept happens · Telegram bot sends follow-up: "✅ ACCEPTED by [name]"

---

### 1-Hour Reset Logic (per user decision #8)

**Fixed clock hour boundary:**

```
Current hour count = SUM(qty) WHERE recorded_at >= '2026-09-09 08:00:00' 
                                AND recorded_at < '2026-09-09 09:00:00'

At 09:00:00 → new bucket · counter reset for next hour
```

**Implementation:**
- No physical "reset" · use timestamp filter in query
- Alert scheduler checks: `recorded_at >= DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00')`
- Simple · no state to manage
- Report also aligns with clock hour (matches shift schedule)

---

## ✏️ Component 4 · Correction · Undo 60s (O-16)

### Flow

**Immediate after save · 60s Undo button:**

```
┌─ ✅ บันทึกแล้ว: Short shot · +5 pcs ────────┐
│                                                │
│  🕐 ยกเลิกได้ภายใน 60 วินาที                 │
│  [❌ ยกเลิกบันทึก · 57s]                      │  ← countdown
│                                                │
│  หมายเหตุ: หลัง 60s ไม่สามารถแก้ไขได้       │
└────────────────────────────────────────────────┘
```

**Behavior:**
- Toast appears top-right after save · non-blocking
- Countdown: 60 → 0 seconds
- Click "ยกเลิกบันทึก" → soft-delete + log
- After 60s expire · toast fades · edit locked forever

**Undo (< 60s):**
```
DELETE /api/ng/record/{id}
  → Check: recorded_at > NOW() - 60 seconds
  → If yes: UPDATE ng_record SET is_deleted = TRUE, deleted_at = NOW(), deleted_by = :user
  → Log to ng_correction_log (see below)
  → Broadcast WebSocket · update counts realtime
```

**After 60s: LOCKED** (per user decision #9-11)
- No edit
- No delete
- No cross-shift correction
- ต้องยอมรับ record ที่บันทึกไปแล้ว

### Correction Log Display (per user decision #9)

**User wants: "เก็บ Log และแสดงว่ามีเหตุการณ์แบบนี้เกิดขึ้นไว้ด้วย"**

**NG History screen shows all events (including undos):**

```
┌─ NG History · RBL103 · 🅛 LEFT · Today ──────────────────┐
│                                                            │
│  Time     | NG Type    | Qty | By   | Status  | Action    │
│  ────────  ──────────── ───── ─────── ────────  ───────────│
│  08:32:15 | Short shot |  3  | OP1  | ✅ OK   |           │
│  08:35:42 | Black spot |  1  | OP1  | ✅ OK   |           │
│  08:36:08 | Short shot |  5  | OP1  | ❌ Undone| by OP1 · 08:36:45 (37s later)│
│  08:38:22 | Short shot |  2  | OP1  | ✅ OK   |           │
│  08:47:11 | Short shot |  1  | OP1  | ✅ OK   | 🚨 Alert (>10/hr) │
│                                                            │
│  Filter: [All ▾] [Show undone ✓]                          │
└────────────────────────────────────────────────────────────┘
```

**Key points:**
- Undone records **stay visible** with red "❌ Undone" tag · never hidden
- Show who + when the undo happened
- Alert breach events also inline
- Filter option "Show undone" · default = show all
- Analytics excludes undone from KPI · but audit trail intact

**Reports also include correction count:**
- Daily report: "Total NG: 45 (43 committed + 2 undone within 60s)"
- Trend: track undo rate per OP (should be low · > 5% = investigate training)

---

## 🗄️ Data Model Changes (V26 Migration)

```sql
-- V26__ng_workflow_upgrade.sql

-- ============================================
-- 1. NG Type master (if not exists · seed)
-- ============================================

CREATE TABLE IF NOT EXISTS ng_type_master (
  code VARCHAR(30) PRIMARY KEY,
  name_th VARCHAR(100) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  icon VARCHAR(20),                        -- emoji
  category VARCHAR(50),
  sort_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE
);

-- Seed common NG types
INSERT INTO ng_type_master (code, name_th, name_en, icon, category, sort_order) VALUES
  ('SHORT_SHOT',   'Short shot',   'Short shot',    '🩹', 'MOLDING', 1),
  ('BLACK_SPOT',   'Black spot',   'Black spot',    '⚫', 'CONTAMINATION', 2),
  ('CUT_MARK',     'Cut mark',     'Cut mark',      '✂️', 'TRIMMING', 3),
  ('BUBBLE',       'Bubble',       'Bubble',        '💧', 'MOLDING', 4),
  ('FLOW_MARK',    'Flow mark',    'Flow mark',     '🌊', 'MOLDING', 5),
  ('CONTAMINATION','Contamination','Contamination', '⚠️', 'CONTAMINATION', 6),
  ('WELD_LINE',    'Weld line',    'Weld line',     '🔵', 'MOLDING', 7),
  ('DIM_OFF_SPEC', 'Dim off spec', 'Dimension off spec','📏','DIMENSION', 8),
  ('BURN_MARK',    'Burn mark',    'Burn mark',     '⚡', 'MOLDING', 9),
  ('WARP',         'Warp',         'Warp',          '🌐', 'DIMENSION', 10),
  ('COLOR_MISS',   'Color miss',   'Color mismatch','🎨', 'COLOR', 11),
  ('SINK_MARK',    'Sink mark',    'Sink mark',     '🔶', 'MOLDING', 12),
  ('SILVER_STREAK','Silver streak','Silver streak', '💫', 'MATERIAL', 13),
  ('SHAPE',        'Shape defect', 'Shape defect',  '📐', 'DIMENSION', 14),
  ('LABEL',        'Label issue',  'Label issue',   '🏷️', 'FINISH', 15),
  ('CHEM_SPOT',    'Chem spot',    'Chemical spot', '🧪', 'CONTAMINATION', 16),
  ('HOLE',         'Hole defect',  'Hole defect',   '🕳️', 'MOLDING', 17),
  ('ATTACH_DEF',   'Attach defect','Attach defect', '📎', 'ASSEMBLY', 18),
  ('TEMP_ISSUE',   'Temperature issue','Temperature issue','🌡️','PROCESS',19),
  ('OTHER',        'อื่นๆ',        'Other',         '❓', 'MISC', 99);

-- ============================================
-- 2. Quick Button Configuration (per machine · 6 fixed)
-- ============================================

CREATE TABLE ng_quick_button_config (
  config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  slot_number INT NOT NULL,                -- 1-6
  ng_type_code VARCHAR(30) NOT NULL,
  updated_by VARCHAR(50) NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ngqb_type FOREIGN KEY (ng_type_code) REFERENCES ng_type_master(code),
  CONSTRAINT uk_ngqb UNIQUE (machine_id, slot_number),
  CONSTRAINT chk_ngqb_slot CHECK (slot_number BETWEEN 1 AND 6)
);

-- ============================================
-- 3. NG Record · add correction fields
-- ============================================

ALTER TABLE ng_record
  ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN deleted_at TIMESTAMP NULL,
  ADD COLUMN deleted_by VARCHAR(50) NULL;

-- Ensure position/session already in OP-WS-01 (V25) · here just verify
-- ALTER TABLE ng_record
--   ADD COLUMN IF NOT EXISTS position VARCHAR(10) NOT NULL DEFAULT 'LEFT',
--   ADD COLUMN IF NOT EXISTS ng_checker_id VARCHAR(50),
--   ADD COLUMN IF NOT EXISTS production_session_id BIGINT NULL;

-- ============================================
-- 4. Correction Log (audit)
-- ============================================

CREATE TABLE ng_correction_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ng_record_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,             -- 'UNDO' (within 60s)
  action_by VARCHAR(50) NOT NULL,
  action_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  original_qty INT NOT NULL,
  original_type VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,                -- optional
  time_since_original_ms INT NOT NULL,     -- how many ms after original save
  CONSTRAINT fk_ncl_ng FOREIGN KEY (ng_record_id) REFERENCES ng_record(id),
  CONSTRAINT chk_ncl_action CHECK (action IN ('UNDO')),
  INDEX idx_ncl_ng (ng_record_id),
  INDEX idx_ncl_time (action_at)
);

-- ============================================
-- 5. Threshold Configuration (per machine)
-- ============================================

CREATE TABLE ng_threshold_config (
  config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  ng_type_code VARCHAR(30) NOT NULL,
  threshold_count INT NOT NULL,            -- per hour
  severity VARCHAR(10) NOT NULL,           -- LOW / MED / HIGH / URGENT
  notify_leader BOOLEAN DEFAULT FALSE,
  notify_tech BOOLEAN DEFAULT FALSE,
  notify_qa BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  created_by VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50),
  updated_at TIMESTAMP,
  CONSTRAINT fk_ngtc_type FOREIGN KEY (ng_type_code) REFERENCES ng_type_master(code),
  CONSTRAINT uk_ngtc UNIQUE (machine_id, ng_type_code),
  CONSTRAINT chk_ngtc_severity CHECK (severity IN ('LOW','MED','HIGH','URGENT'))
);

-- ============================================
-- 6. NG Alert Records
-- ============================================

CREATE TABLE ng_alert (
  alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  position VARCHAR(10),                    -- optional · which side breached
  ng_type_code VARCHAR(30) NOT NULL,
  hour_window_start TIMESTAMP NOT NULL,    -- start of the hour
  breach_count INT NOT NULL,               -- count at time of breach
  threshold_count INT NOT NULL,            -- limit at time
  severity VARCHAR(10) NOT NULL,
  notify_leader BOOLEAN,
  notify_tech BOOLEAN,
  notify_qa BOOLEAN,
  
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  accepted_by VARCHAR(50),
  accepted_at TIMESTAMP,
  arrived_at TIMESTAMP,                    -- OTP verified (T-03 pattern)
  resolved_at TIMESTAMP,
  
  arrival_otp VARCHAR(6),
  arrival_otp_expiry TIMESTAMP,
  arrival_verified BOOLEAN DEFAULT FALSE,
  
  telegram_escalated BOOLEAN DEFAULT FALSE,
  telegram_escalated_at TIMESTAMP,
  telegram_room VARCHAR(50),               -- room name
  
  triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_nga_status CHECK (status IN ('PENDING','ACCEPTED','ARRIVED','RESOLVED','SNOOZED')),
  INDEX idx_nga_status (status),
  INDEX idx_nga_machine (machine_id, triggered_at),
  INDEX idx_nga_pending (status, triggered_at)  -- for escalation scheduler
);

-- ============================================
-- 7. NG Alert Event Log (audit)
-- ============================================

CREATE TABLE ng_alert_event (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alert_id BIGINT NOT NULL,
  event_type VARCHAR(30) NOT NULL,         -- CREATED / BROADCAST / ACCEPTED / SNOOZED / ARRIVED / RESOLVED / TELEGRAM_ESCALATED
  actor_id VARCHAR(50),
  actor_role VARCHAR(20),
  note VARCHAR(500),
  event_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ngae_alert FOREIGN KEY (alert_id) REFERENCES ng_alert(alert_id),
  INDEX idx_ngae_alert (alert_id, event_at)
);

-- ============================================
-- 8. Telegram Room Configuration
-- ============================================

CREATE TABLE telegram_room_config (
  room_code VARCHAR(30) PRIMARY KEY,       -- GENERAL / LEADER / TECH / QA / MANAGER
  room_name VARCHAR(100) NOT NULL,
  chat_id VARCHAR(50) NOT NULL,            -- Telegram chat ID
  bot_token_env_var VARCHAR(100),          -- env var name · not stored raw
  description VARCHAR(200),
  is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO telegram_room_config (room_code, room_name, description) VALUES
  ('GENERAL', 'GDTAHARA General', 'ห้องรวม · everyone'),
  ('LEADER',  'GDTAHARA Leaders', 'Leader team'),
  ('TECH',    'GDTAHARA Tech',    'Tech team'),
  ('QA',      'GDTAHARA QA',      'QA team'),
  ('MANAGER', 'GDTAHARA Manager', 'PD/EXEC · for escalation');
```

---

## 🔌 API Endpoints

### NG Recording
- `POST /api/ng/record` — save NG (returns record ID)
- `DELETE /api/ng/record/{id}` — undo (< 60s only) · returns 403 if expired
- `GET /api/ng/history?machineId=X&position=Y&from=Z&to=W&showUndone=true`

### Quick Button Config
- `GET /api/admin/ng-buttons?machineId=X` — return 6 button config
- `PUT /api/admin/ng-buttons` — update all 6 slots atomically
- `GET /api/ng/types?active=true` — all NG types

### Threshold Config
- `GET /api/admin/ng-threshold?machineId=X`
- `POST /api/admin/ng-threshold` · `PATCH /api/admin/ng-threshold/{id}` · `DELETE`
- `POST /api/admin/ng-threshold/import` — copy from another machine

### Alerts
- `GET /api/ng-alert?status=PENDING`
- `POST /api/ng-alert/{id}/accept`
- `POST /api/ng-alert/{id}/snooze`
- `POST /api/ng-alert/{id}/verify-arrival` (with OTP · reuse T-03)
- `POST /api/ng-alert/{id}/resolve`

### Telegram Bot
- `POST /api/telegram/send` (internal use)
- Bot config in env: `TELEGRAM_BOT_TOKEN`, chat IDs per room

### WebSocket
- `NG_ALERT_NEW` — broadcast on new alert (LD/TECH/QA rooms)
- `NG_ALERT_UPDATE` — accept/arrive/resolve
- `NG_COUNT_UPDATE` — realtime count update (for dashboard)

---

## 🖥️ Frontend Components (React)

```
src/components/NG/
  ├── NGRecordScreen.tsx           (MODIFY · 6 quick buttons + others grid)
  ├── NGQtyModal.tsx               (NEW · qty input · no confirm preview)
  ├── NGOthersModal.tsx            (NEW · grid of remaining types)
  ├── NGUndoToast.tsx              (NEW · 60s countdown)
  ├── NGHistoryTable.tsx           (NEW · with undone visible + alerts inline)
  └── NGCountBadge.tsx             (NEW · realtime count badge on buttons)

src/components/Admin/
  ├── QuickButtonConfig.tsx        (NEW · 6-slot picker per machine)
  ├── ThresholdConfig.tsx          (NEW · CRUD threshold rules)
  └── TelegramRoomConfig.tsx       (NEW · rooms + chat IDs)

src/components/Alert/
  ├── NGAlertPopup.tsx             (NEW · reuse T-03 pattern)
  └── AlertQueue.tsx               (MODIFY · include NG alerts)

src/services/
  └── telegramBot.ts               (NEW · backend service · not FE)

src/hooks/
  ├── useNGCounter.ts              (NEW · realtime hourly count)
  ├── useUndoTimer.ts              (NEW · 60s countdown)
  └── useNGAlerts.ts               (NEW · WebSocket state)
```

---

## ✅ Acceptance Criteria

### NG Button UX
- [ ] 6 fixed quick buttons per machine · admin-configurable
- [ ] Button size ≥ 160×160px · font ≥ 18pt · glove-friendly
- [ ] "อื่นๆ" button opens modal with ALL remaining NG types as grid
- [ ] Live count per button (today) updates realtime
- [ ] Qty modal · no confirm preview · save immediately after commit

### Undo (60s)
- [ ] Toast appears after save · shows countdown
- [ ] Click undo within 60s → soft delete + log
- [ ] After 60s · toast fades · edit locked
- [ ] Undone records visible in history · red "Undone" tag
- [ ] ng_correction_log tracks all undos

### Threshold Alert
- [ ] Admin can CRUD threshold per (machine, ng_type)
- [ ] Fixed hourly reset (:00 clock boundary)
- [ ] On NG save · check current hour count vs threshold
- [ ] If breach: create ng_alert + broadcast WebSocket to LD/TECH/QA (per config)
- [ ] Popup on tablets · reuse T-03 pattern
- [ ] Accept + OTP arrival flow works
- [ ] Snooze 5 min · re-notify

### Telegram Escalation
- [ ] Scheduler runs every 30s · checks PENDING alerts > 5 min
- [ ] Send Telegram message to correct room based on notify_roles
- [ ] Message includes machine · type · count · alert ID · link
- [ ] telegram_escalated flag set · no duplicate send
- [ ] After accept · bot sends follow-up "✅ ACCEPTED by [name]"

### Reset Logic
- [ ] Query uses `recorded_at >= start_of_current_hour`
- [ ] At :00 · new bucket automatically starts
- [ ] No manual reset needed

### Analytics / KPI
- [ ] Reports exclude undone records from counts
- [ ] Reports show undo rate per OP
- [ ] Historical data accessible via OP-WS-01 archive

### Testing
- [ ] Unit: threshold breach detection
- [ ] Unit: 60s undo window edge cases (59.9s vs 60.1s)
- [ ] Integration: NG save → threshold check → alert → Telegram after 5 min
- [ ] Integration: 2 users accept simultaneously · one wins
- [ ] E2E: Full flow · save NG → breach → alert LD+TECH → accept → OTP arrival → resolve
- [ ] Test บน Samsung Tab Active 5 Pro · glove + big buttons
- [ ] Test Telegram delivery to all 5 rooms
- [ ] Load test: 500 NG saves per minute across 15 machines

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V26 migration + seed data | 0.5 day |
| Backend · NG type + Quick button config service | 0.75 day |
| Backend · NG record + undo (60s window) + correction log | 1 day |
| Backend · Threshold config CRUD | 0.5 day |
| Backend · Alert creation + WebSocket broadcast (reuse T-03) | 1 day |
| Backend · Telegram bot service + room config | 1 day |
| Backend · Alert escalation scheduler | 0.5 day |
| Backend · OTP arrival flow (reuse T-03) | 0.25 day |
| Frontend · 6 button screen + Others modal | 1 day |
| Frontend · Qty modal + Undo toast (60s) | 0.75 day |
| Frontend · NG History with undo visible + alerts inline | 0.75 day |
| Frontend · Admin Quick Button Config screen | 0.5 day |
| Frontend · Admin Threshold Config screen | 0.75 day |
| Frontend · Admin Telegram Room Config | 0.5 day |
| Frontend · NG Alert popup (reuse T-03) | 0.5 day |
| Testing + integration + load test | 1.75 days |
| **Total** | **~12.0 days (L)** |

---

## 🔗 Dependencies & Reuse

**Reuses from other issues:**
- **OP-WS-01 (V25):** ng_record has position + ng_checker_id (session-based)
- **T-03 (V23):** WebSocket infra + notification popup + OTP arrival pattern
- **T-01 (V22):** attachment_photo (if we add photos to correction log later)

**Dependencies (blocks this):**
- V22, V23, V25 must be deployed first

---

## 📝 Open Questions (Future)

- Frequency-based threshold (not just count) · deferred (per user decision #5)
- Photo attach on NG record · defer to future (nice-to-have)
- Threshold per (machine + shift) · currently just per machine
- Historical trend analysis dashboard · separate issue
- Auto-suggest new threshold values based on data · Phase 2

---

## 🎯 Design Decision Log

| # | Decision | Rationale (user) |
|---|---|---|
| 1 | 6 buttons fixed (not auto) | Predictable · consistent UX |
| 2 | Others = full grid | Fast · no learning curve |
| 3 | No save confirmation | Speed · trust operator |
| 4 | Broadcast alert | Fast response · everyone knows |
| 5 | No auto-dismiss | NG only grows · not applicable now |
| 6 | Telegram after 5 min · rooms | Safety net · role-scoped noise reduction |
| 7 | Threshold per machine | Simple · machine-specific defects |
| 8 | Fixed 1-hour reset | Aligns with reports/shifts |
| 9 | Undo 60s only · lock after | Trust window · immutable after |
| 10 | No approval within 60s | Fast · low-risk |
| 11 | No cross-shift correction | Immutability · integrity |
| 12 | Reuse T-03 OTP | Consistent UX · less code |
