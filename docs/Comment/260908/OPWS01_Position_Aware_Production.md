# OP-WS-01 · Position-Aware Multi-OP Production Workstream

**Type:** Workstream (covers 5 issues + 3 admin features)
**Priority:** 🔥 Critical
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** L (Large · ~15-18 dev days)
**Source:** Pilot Feedback 8 Sep 2026 + follow-up discussion
**Covers Issues:** O-02, O-07, O-13, O-14, O-15
**New Features:** Admin Tablet Reg · Admin Printer Reg · Auto Archive

---

## 📋 Summary · สรุป

ระบบผลิตปัจจุบันไม่แยก LEFT/RIGHT · OP 2 ทีมทำงานคู่ 1 machine (ฝั่งซ้าย + ฝั่งขวา) · ไม่มีการ tag position ที่ NG record · Box label · หรือ session · ทำให้ traceability ไม่สมบูรณ์และเกิด race condition ที่ box numbering

**Workstream นี้ overhaul workflow ทั้งหมด:**
1. Position-aware architecture (LEFT/RIGHT ทุกที่)
2. Admin-managed device registration (Tablet + Printer per side per machine)
3. Session-based auto-tagging (0 clicks for position)
4. Atomic box numbering per Lot (server-side + FE polling)
5. Enhanced traceability + Auto archive 5-year

---

## 🎯 Workflow Overview

```
┌───────────────── Machine RBL103 · Lot ABC-2026-09-09-01 ─────────────────┐
│                                                                            │
│                            🏭 Blow Molding                                 │
│                                 │                                          │
│                          ┌──────┴──────┐                                   │
│                     Product ↙         ↘ Product                            │
│                                                                            │
│    🅛 LEFT SIDE                            🅡 RIGHT SIDE                   │
│    (คนหันหน้าเข้าเครื่อง · ซ้ายมือ)   (คนหันหน้าเข้าเครื่อง · ขวามือ)     │
│                                                                            │
│    📱 Tablet A (fixed · Admin-reg)         📱 Tablet B (fixed · Admin-reg)│
│       ↓ OP1 login → NG check                  ↓ OP3 login → NG check      │
│                                                                            │
│    🔦 Handheld (mobile · OP picks)          🔦 Handheld (mobile · OP picks)│
│       ↓ OP2 login → select LEFT                ↓ OP4 login → select RIGHT │
│       ↓ Pack Box                                ↓ Pack Box                 │
│                                                                            │
│    🖨️ Printer L (Admin-reg · network)      🖨️ Printer R (Admin-reg · net)│
│       ← Label prints here (LEFT boxes)         ← Label prints here (RIGHT)│
│                                                                            │
│    📊 SHARED: Box sequence per (machine + lot) · atomic increment          │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Architecture Components

### Component 1 · Admin Device Registration (Tablet + Printer)

**Tablet Registration Screen** (Admin only)

```
┌─ Admin > Device Registration > Tablet ────────────────────┐
│                                                            │
│  📱 Detected Device (from tablet's initial connect)        │
│  Device ID:    TAB-A5PRO-XXXX-1234  (auto-detect)         │
│  MAC Address:  A4:5E:60:XX:YY:ZZ  (network interface)      │
│  Model:        Samsung Tab Active 5 Pro                    │
│                                                            │
│  ⚙️ Assign to:                                             │
│  Machine:      [RBL103 ▾]                                 │
│  Position:     ( ) 🅛 LEFT  ( ) 🅡 RIGHT                  │
│                (ตำแหน่งเมื่อหันหน้าเข้าเครื่องจักร)         │
│  Device Name:  [RBL103-Left-Tablet__]                      │
│                                                            │
│  ✅ Preview:                                               │
│  Tablet นี้จะแสดง Login screen:                            │
│  "GDTAHARA · RBL103 · 🅛 LEFT · Machine Setup"            │
│                                                            │
│  ⚠️ Note: ตำแหน่ง LEFT/RIGHT ต้องเทียบจากคนที่ยืนหน้าเครื่อง│
│  (ไม่ใช่ view จากด้านหลัง)                                 │
│                                                            │
│                    [Cancel]  [💾 Save & Deploy]           │
└────────────────────────────────────────────────────────────┘
```

**Printer Registration Screen** (Admin only) 🆕

```
┌─ Admin > Device Registration > Printer ───────────────────┐
│                                                            │
│  🖨️ Add Network Printer                                   │
│  Discovery: [🔍 Auto-detect] or [Manual IP/Hostname]      │
│                                                            │
│  Device ID:      ZT411-XXXX-5678  (from Zebra config)     │
│  MAC Address:    B8:27:EB:XX:YY:ZZ                         │
│  Hostname:       zebra-rbl103-left                         │
│  Network Path:   tcp://zebra-rbl103-left:9100             │
│  Model:          Zebra ZT411 (300 dpi)                    │
│                                                            │
│  ⚙️ Assign to:                                             │
│  Machine:        [RBL103 ▾]                               │
│  Position:       ( ) 🅛 LEFT  ( ) 🅡 RIGHT                │
│  Printer Type:   ( ) Thermal (Box Label)                  │
│                  ( ) Laser (Pallet Tag)                    │
│  Friendly Name:  [RBL103-Left-Thermal__]                  │
│                                                            │
│  ✅ Test Print:  [🖨️ Print Test Label]                    │
│                  → OP ที่ฝั่ง LEFT ควรเห็น label ออกที่นี่│
│                                                            │
│                    [Cancel]  [💾 Save & Deploy]           │
└────────────────────────────────────────────────────────────┘
```

**Design Decisions (per user):**
- ✅ ใช้ **Device ID + MAC** เป็น identifier (ไม่ใช้ IP · IP เปลี่ยนบ่อย)
- ✅ Test Print button ยืนยันการติดตั้งถูกฝั่ง

---

### Component 2 · Handheld Session Selection (OP mobile)

**Login → Session Selection Flow**

**Screen 1 · Login**
```
┌─ 🔦 Handheld Login ─────────────────────────┐
│                                              │
│  Scan ID card หรือใส่ password              │
│  [_____________]                             │
│                                              │
│              [Login]                         │
└──────────────────────────────────────────────┘
```

**Screen 2 · Select Work (หลัง login)**
```
┌─ เลือกงานที่กำลังทำ · OP2 ──────────────────┐
│                                              │
│  Machine:                                    │
│  [🔍 Select machine ▾]                       │
│  → RBL101, RBL102, RBL103, RBL104...        │
│                                              │
│  Position: (หันหน้าเข้าเครื่องจักร)         │
│  ┌────────────┐  ┌────────────┐             │
│  │ 🅛 LEFT     │  │ 🅡 RIGHT   │             │
│  │ (ซ้ายมือ)   │  │ (ขวามือ)   │             │
│  └────────────┘  └────────────┘             │
│                                              │
│  ⚠️ RIGHT · OP4 กำลังทำอยู่ (07:30-current)  │
│      → เลือกไม่ได้                            │
│                                              │
│  Lot: (auto-detected · read-only)            │
│  → ABC-2026-09-09-01 · NBKPMY1000            │
│                                              │
│  Current Box #: 42 (last packed)             │
│  Next Box #:    43 (if you pack next)        │
│                                              │
│                       [เริ่มบันทึก]           │
└──────────────────────────────────────────────┘
```

**Behavior:**
- Position selection **required ทุกครั้งที่ login** (ตาม user's rule)
- Auto-release position on logout
- Session timeout · 30 min inactive → auto-release (warn ก่อน 15 min)
- Position mutex: 1 คน/ฝั่ง/เครื่อง

**Position Mutex Enforcement:**
```
POST /api/handheld/session/start
Body: { userId: "op2", machineId: "RBL103", position: "LEFT" }

Server logic:
  SELECT * FROM handheld_session 
    WHERE machine_id = 'RBL103' 
      AND position = 'LEFT'
      AND status = 'ACTIVE'
      AND logout_at IS NULL;
  
  IF result exists AND NOT expired:
    → RETURN 409 Conflict
    → Body: { error: "POSITION_OCCUPIED", 
              occupiedBy: "op1", 
              since: "07:30" }
  ELSE:
    → INSERT new session
    → RETURN 200 { sessionId, machineId, position }
```

---

### Component 3 · Tablet Session (Fixed at Machine)

**Login screen (tablet ยึดที่เครื่อง)**
```
┌─ GDTAHARA · RBL103 · 🅛 LEFT ──────────────┐
│                                              │
│  📱 Machine: RBL103 (Tahara Blow · 30-lane)  │
│  📍 Position: LEFT                          │
│  📋 Current Lot: ABC-2026-09-09-01           │
│                                              │
│  Login:                                      │
│  Employee ID: [_________]                    │
│  Password:    [_________]                    │
│                                              │
│              [Login as NG Checker]           │
└──────────────────────────────────────────────┘
```

**หลัง login → NG Recording screen (auto-tagged position)**
```
┌─ NG Record · RBL103 · 🅛 LEFT · OP1 ────────┐
│  (Position: LEFT · auto from tablet reg.)   │
│                                              │
│  ประเภท NG:                                  │
│  [Type1] [Type2] [Type3] [Type4] [Type5]    │
│  [อื่นๆ ▾]                                   │
│                                              │
│  จำนวน: [+1] [+10] [text: ___]              │
│  หมายเหตุ: [_____________]                   │
│                                              │
│                    [บันทึก NG]                │
└──────────────────────────────────────────────┘
```

**Behavior:**
- Position ผูกกับ tablet · ไม่ต้องเลือก
- All NG records auto-tag: `machine_id`, `position`, `ng_checker_id` (from session)

**Multi-OP switching (change shift):**
- OP1 logout · OP5 login on same tablet
- New session created · OP5 = new NG checker for LEFT
- Position ไม่เปลี่ยน (ผูก tablet)

---

### Component 4 · Atomic Box Numbering (Server-side + FE Polling)

**Layer 1 · FE Polling (UX · ทุก 15s)**

```typescript
// Every 15 seconds, poll for current state
useEffect(() => {
  const interval = setInterval(async () => {
    const status = await api.get(`/box/current-status`, {
      params: { machineId, lotId }
    });
    setLastBoxNumber(status.lastBoxNumber);
    setActivePackers(status.activePackers);
  }, 15000);
  return () => clearInterval(interval);
}, [machineId, lotId]);
```

**Response:**
```json
{
  "machineId": "RBL103",
  "lotId": "ABC-2026-09-09-01",
  "lastBoxNumber": 42,
  "activePackers": [
    { "userId": "op2", "position": "LEFT", "currentBox": null, "packedCount": 15 },
    { "userId": "op4", "position": "RIGHT", "currentBox": null, "packedCount": 12 }
  ],
  "ngCheckers": [
    { "userId": "op1", "position": "LEFT" },
    { "userId": "op3", "position": "RIGHT" }
  ]
}
```

**Layer 2 · BE Atomic Save (correctness)**

```
POST /api/box/save
Body: { handheldSessionId, boxContents: {...} }

Server logic (transactional):
  BEGIN TRANSACTION;
  
  -- 1. Validate session active + position mutex still valid
  SELECT * FROM handheld_session WHERE id = :sessionId AND status = 'ACTIVE' FOR UPDATE;
  
  -- 2. Validate NG checker exists (per user rule · Option 2)
  SELECT COUNT(*) FROM production_session
    WHERE machine_id = :machineId AND position = :position 
      AND role = 'NG_CHECKER' AND logout_at IS NULL;
  IF count = 0:
    ROLLBACK;
    RETURN 400 { error: "NO_NG_CHECKER", 
                 message: "ต้องมี NG Checker ประจำฝั่งก่อน pack box" };
  
  -- 3. Atomic increment · SELECT FOR UPDATE (row lock)
  SELECT current_number FROM machine_lot_box_sequence
    WHERE machine_id = :machineId AND lot_id = :lotId
    FOR UPDATE;
  UPDATE machine_lot_box_sequence
    SET current_number = current_number + 1
    WHERE machine_id = :machineId AND lot_id = :lotId;
  SELECT current_number INTO :nextBoxNum;
  
  -- 4. Insert box_label
  INSERT INTO box_label (
    box_number, machine_id, lot_id, position, 
    packer_id, ng_checker_id, 
    packed_at, ...
  ) VALUES (
    :nextBoxNum, :machineId, :lotId, :position,
    :packerId, :ngCheckerId,
    NOW(), ...
  );
  
  -- 5. Route to correct printer (based on position)
  SELECT network_path FROM printer_registration
    WHERE machine_id = :machineId AND position = :position 
      AND printer_type = 'THERMAL' AND is_active = TRUE;
  → Enqueue print job to that printer
  
  COMMIT;
  RETURN 200 { boxNumber, position, printerUsed };
```

**Concurrency guarantee:**
- 2 OPs กด save พร้อมกัน · `SELECT FOR UPDATE` serialize · no duplicate
- Order = commit order (fair · time-based)
- Print goes to correct printer (position-routed)

---

### Component 5 · Enhanced Box Label

**New Label Format:**
```
┌────────────────────────────────────────┐
│  📦 GDTAHARA · TAHARA                  │
│                                        │
│  Machine:      RBL103                  │
│  SubLot:       ABC-2026-09-09-01       │
│  Product:      NBKPMY1000              │
│                                        │
│  Box #:        43                      │ ← Running No. (shared L+R per Lot)
│  Position:     🅛 LEFT                 │ ← from handheld session
│                                        │
│  Packed by:    OP2 (Handheld · 08:15) │ ← from handheld session
│  NG Check:     OP1 (Tablet · Left)    │ ← from active tablet session
│                                        │
│  ┌──────────────────────────────┐      │
│  │ Barcode: RBL103-L-43         │      │ ← includes position for scan
│  └──────────────────────────────┘      │
│                                        │
│  Date:  2026-09-09 08:29               │
│  Shift: A (07:00-15:00)                │
└────────────────────────────────────────┘
```

**Key features:**
- Position obvious (🅛 icon + text)
- Barcode format: `MACHINE-POSITION-BOXNUM` (`RBL103-L-43`)
- Both operator names visible (traceability)
- Printer routes correctly (LEFT box → LEFT printer)

---

### Component 6 · Data Retention & Auto Archive 🆕

**Retention Policy (per user's rule):**
- **Hot data** (0-12 months): main tables · fast query
- **Warm archive** (12 months - 5 years): archive tables in same DB · queryable
- **Cold/Delete** (> 5 years): export to S3 Glacier · delete from DB

**Data lifecycle:**
```
Month 1-12:      hot table (e.g. `box_label`) · full index · fast
Month 13-60:     archive table (`box_label_archive`) · basic index
Month 61+:       exported to S3 Glacier archive · deleted from DB
                 (queryable via async request · takes hours)
```

**Auto-Archive Job (Scheduled · runs monthly · 1st of month · 02:00):**

```java
@Scheduled(cron = "0 0 2 1 * ?")  // 1st of month at 02:00
public void runMonthlyArchive() {
    LocalDate archiveOlderThan = LocalDate.now().minusMonths(12);
    LocalDate deleteOlderThan = LocalDate.now().minusYears(5);
    
    List<String> tables = List.of(
        "box_label", "ng_record", "packaging_log", 
        "setup_time_log", "machine_downtime_log",
        "handheld_session", "production_session",
        "attachment_photo", ...
    );
    
    for (String table : tables) {
        archiveService.moveToArchive(table, archiveOlderThan);
        archiveService.exportToGlacier(table + "_archive", deleteOlderThan);
        archiveService.deleteFromArchive(table + "_archive", deleteOlderThan);
    }
    
    notify Admin: "Monthly archive complete · X rows archived · Y rows to Glacier";
}
```

**Query Unification (Historical view):**

```
GET /api/history/box-label?machineId=RBL103&from=2024-01-01&to=2026-09-09

Server logic:
  1. Query hot table (last 12 months)
  2. If date range overlaps > 12 months: also query archive table
  3. If date range overlaps > 5 years: async request Glacier restore
     → return partial result + notification when Glacier ready
  4. Merge + return unified result
```

**Admin Screen · Archive Status**
```
┌─ Admin > Archive Management ────────────────────┐
│                                                  │
│  📊 Data Statistics                              │
│  Hot (0-12mo):   box_label = 45,000 rows · 12MB │
│  Archive:        box_label_archive = 285,000 · 78MB│
│  Glacier:        Sep 2020 - Aug 2021 · restored?│
│                                                  │
│  🔧 Manual Actions                               │
│  [Run Archive Now] [Restore from Glacier ▾]     │
│                                                  │
│  📅 Schedule:                                    │
│  Next run: 1 Oct 2026 02:00                     │
│  Last run: 1 Sep 2026 02:00 · SUCCESS           │
│    · box_label: 3,200 rows → archive            │
│    · ng_record: 12,500 rows → archive           │
│                                                  │
│  📈 Trend Chart · Data size growth              │
│  [Chart · monthly growth per table]              │
└──────────────────────────────────────────────────┘
```

---

## 🗄️ Data Model Changes (V25 Migration)

```sql
-- V25__position_aware_production.sql

-- ============================================
-- 1. Device Registration
-- ============================================

CREATE TABLE tablet_registration (
  tablet_id VARCHAR(100) PRIMARY KEY,          -- Device ID (unique per tablet)
  mac_address VARCHAR(17) NOT NULL UNIQUE,     -- MAC · confirms identity
  machine_id VARCHAR(20) NOT NULL,
  position VARCHAR(10) NOT NULL,
  device_name VARCHAR(100),
  model VARCHAR(100),
  registered_by VARCHAR(50) NOT NULL,
  registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  CONSTRAINT chk_tab_pos CHECK (position IN ('LEFT','RIGHT')),
  CONSTRAINT uk_tab_machine_pos UNIQUE (machine_id, position)  -- 1 tablet per side per machine
);

CREATE TABLE printer_registration (
  printer_id VARCHAR(100) PRIMARY KEY,
  mac_address VARCHAR(17) NOT NULL UNIQUE,
  machine_id VARCHAR(20) NOT NULL,
  position VARCHAR(10) NOT NULL,
  printer_type VARCHAR(20) NOT NULL,           -- 'THERMAL' | 'LASER'
  network_path VARCHAR(200) NOT NULL,          -- tcp://host:9100
  hostname VARCHAR(100),
  friendly_name VARCHAR(100),
  model VARCHAR(100),
  registered_by VARCHAR(50) NOT NULL,
  registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  CONSTRAINT chk_prt_pos CHECK (position IN ('LEFT','RIGHT')),
  CONSTRAINT chk_prt_type CHECK (printer_type IN ('THERMAL','LASER')),
  CONSTRAINT uk_prt_machine_pos_type UNIQUE (machine_id, position, printer_type)
);

-- ============================================
-- 2. Sessions
-- ============================================

CREATE TABLE production_session (        -- Tablet session (NG checker)
  session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  tablet_id VARCHAR(100) NOT NULL,
  machine_id VARCHAR(20) NOT NULL,
  position VARCHAR(10) NOT NULL,           -- Inherited from tablet_registration
  role VARCHAR(20) NOT NULL DEFAULT 'NG_CHECKER',
  shift_code VARCHAR(10),
  login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  logout_at TIMESTAMP NULL,
  CONSTRAINT fk_ps_tablet FOREIGN KEY (tablet_id) REFERENCES tablet_registration(tablet_id),
  INDEX idx_ps_active (machine_id, position, logout_at),
  INDEX idx_ps_user (user_id, login_at)
);

CREATE TABLE handheld_session (            -- Mobile handheld session (Packer)
  session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  handheld_device_id VARCHAR(100),         -- optional · which handheld
  machine_id VARCHAR(20) NOT NULL,
  position VARCHAR(10) NOT NULL,           -- User selects at start
  role VARCHAR(20) NOT NULL DEFAULT 'PACKER',
  shift_code VARCHAR(10),
  login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  logout_at TIMESTAMP NULL,
  last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  auto_released BOOLEAN DEFAULT FALSE,     -- true if timeout
  CONSTRAINT chk_hh_pos CHECK (position IN ('LEFT','RIGHT')),
  INDEX idx_hh_active (machine_id, position, logout_at),
  INDEX idx_hh_activity (last_activity_at)
);

-- ============================================
-- 3. Box Numbering (atomic sequence per lot)
-- ============================================

CREATE TABLE machine_lot_box_sequence (
  machine_id VARCHAR(20) NOT NULL,
  lot_id VARCHAR(50) NOT NULL,
  current_number INT NOT NULL DEFAULT 0,
  last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (machine_id, lot_id)
);

-- ============================================
-- 4. Add position + traceability to existing tables
-- ============================================

ALTER TABLE ng_record 
  ADD COLUMN position VARCHAR(10) NOT NULL DEFAULT 'LEFT',
  ADD COLUMN ng_checker_id VARCHAR(50),       -- session-derived
  ADD COLUMN production_session_id BIGINT NULL;
CREATE INDEX idx_ng_position ON ng_record(machine_id, position, created_at);

ALTER TABLE box_label
  ADD COLUMN position VARCHAR(10) NOT NULL,
  ADD COLUMN packer_id VARCHAR(50) NOT NULL,
  ADD COLUMN ng_checker_id VARCHAR(50) NOT NULL,   -- Must exist (Option 2 rule)
  ADD COLUMN handheld_session_id BIGINT NULL,
  ADD COLUMN production_session_id BIGINT NULL,
  ADD COLUMN printer_id VARCHAR(100),
  ADD COLUMN print_status VARCHAR(20) DEFAULT 'PENDING',
  ADD COLUMN print_completed_at TIMESTAMP NULL,
  ADD CONSTRAINT chk_box_pos CHECK (position IN ('LEFT','RIGHT'));
CREATE INDEX idx_box_lot_num ON box_label(machine_id, lot_id, box_number);
CREATE INDEX idx_box_position ON box_label(machine_id, position, packed_at);

ALTER TABLE packaging_log
  ADD COLUMN position VARCHAR(10) NOT NULL,
  ADD COLUMN handheld_session_id BIGINT NULL;

-- ============================================
-- 5. Archive tables (structure mirrors originals)
-- ============================================

CREATE TABLE box_label_archive LIKE box_label;
CREATE TABLE ng_record_archive LIKE ng_record;
CREATE TABLE packaging_log_archive LIKE packaging_log;
CREATE TABLE production_session_archive LIKE production_session;
CREATE TABLE handheld_session_archive LIKE handheld_session;
CREATE TABLE setup_time_log_archive LIKE setup_time_log;
CREATE TABLE machine_downtime_log_archive LIKE machine_downtime_log;
CREATE TABLE attachment_photo_archive LIKE attachment_photo;

-- ============================================
-- 6. Archive tracking
-- ============================================

CREATE TABLE archive_job_log (
  job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_run_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  archive_older_than DATE NOT NULL,
  delete_older_than DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  tables_processed INT DEFAULT 0,
  rows_archived INT DEFAULT 0,
  rows_deleted INT DEFAULT 0,
  glacier_export_paths JSON NULL,          -- S3 paths of exported data
  error_message TEXT NULL,
  duration_seconds INT NULL,
  CONSTRAINT chk_aj_status CHECK (status IN ('RUNNING','SUCCESS','FAILED','PARTIAL'))
);
```

---

## 🔌 API Endpoints

### Admin Device Management
- `GET /api/admin/tablet` — list all registered tablets
- `POST /api/admin/tablet` — register new tablet (Device ID + MAC + machine + position)
- `PATCH /api/admin/tablet/{tabletId}` — update (change machine or position)
- `DELETE /api/admin/tablet/{tabletId}` — retire
- Same pattern for `/api/admin/printer`

### Session Management
- `POST /api/session/tablet/login` — auto-detect tablet · create session
- `POST /api/session/tablet/logout`
- `POST /api/handheld/session/start` — with position selection · check mutex
- `POST /api/handheld/session/heartbeat` — keep alive · update last_activity
- `POST /api/handheld/session/end` — release position

### NG Recording
- `POST /api/ng/record` — auto-tag position + ng_checker_id from session
- `GET /api/ng/records?machineId=X&position=LEFT&from=Y&to=Z`

### Box Packing (atomic)
- `POST /api/box/save` — atomic issue box_number + validate NG checker exists + route to printer
- `GET /api/box/current-status?machineId=X&lotId=Y` — polling endpoint (15s)
- `GET /api/box/{boxNumber}` — full detail incl. names

### Historical / Archive
- `GET /api/history/query?table=X&from=Y&to=Z` — unified hot + archive
- `POST /api/admin/archive/run-now` — manual trigger
- `GET /api/admin/archive/status` — dashboard data
- `POST /api/admin/archive/restore-from-glacier?path=X`

---

## 🖥️ Frontend Components (React)

```
src/components/Admin/
  ├── TabletRegistration.tsx           (NEW · scan+register)
  ├── PrinterRegistration.tsx          (NEW · scan+register+test)
  └── ArchiveManagement.tsx            (NEW · dashboard + manual actions)

src/components/OP/
  ├── HandheldLogin.tsx                (NEW · scan ID + password)
  ├── HandheldSessionSelector.tsx      (NEW · machine+position picker · mutex check)
  ├── HandheldPacking.tsx              (MODIFY · include polling · show partner status)
  └── HandheldLabelPrint.tsx           (MODIFY · route to correct printer)

src/components/Tablet/
  ├── TabletLogin.tsx                  (NEW · auto-detect machine+position)
  ├── TabletNGRecord.tsx               (MODIFY · auto-tag position)
  └── TabletDashboard.tsx              (MODIFY · L/R split view)

src/hooks/
  ├── useBoxPolling.ts                 (NEW · 15s polling)
  ├── useHandheldSession.ts            (NEW · position mutex + heartbeat)
  ├── useTabletSession.ts              (NEW · auto-detect from device ID)
  └── usePositionMutex.ts              (NEW · check other OPs)
```

---

## ✅ Acceptance Criteria

### Admin (Tablet + Printer Registration)
- [ ] Admin สร้าง tablet_registration ได้ · ระบุ Machine + Position
- [ ] 1 tablet/side/machine · unique constraint
- [ ] Admin สร้าง printer_registration ได้ · Thermal + Laser types
- [ ] Test Print button · ยืนยันตำแหน่งถูก
- [ ] Reconfigure · retire ได้

### Tablet Session (NG Checker)
- [ ] Tablet auto-detect (Device ID + MAC) → show machine+position
- [ ] OP login → session created · position inherited
- [ ] NG record auto-tag position + ng_checker_id · 0 clicks
- [ ] Logout → session ends
- [ ] Different OP login on same tablet → new session · position same

### Handheld Session (Packer)
- [ ] OP login → must select machine + position · required
- [ ] Position mutex per machine · block if occupied
- [ ] Error message clear: "OP1 กำลังทำ LEFT (07:30)"
- [ ] Position label: "หันหน้าเข้าเครื่อง · ซ้ายมือ/ขวามือ"
- [ ] Logout → auto-release position
- [ ] Inactive 30 min → auto-release (warn at 15 min)
- [ ] Re-login → must select again (per user's rule)

### Box Numbering
- [ ] Sequence per (machine + lot) · atomic
- [ ] 2 concurrent saves → no duplicate · order by commit time
- [ ] FE poll every 15s → show last box + active packers
- [ ] Lot change → sequence reset to 1
- [ ] Same lot across shifts → continues (per user's rule)

### Box Save Validation
- [ ] MUST have active NG checker on same (machine + position) · else block
- [ ] Error: "ต้องมี NG Checker ประจำฝั่งก่อน pack box" (per user's Option 2)
- [ ] Box label has: box#, position, packer name, ng checker name

### Printer Routing
- [ ] Box label auto-print at correct printer (machine + position + THERMAL)
- [ ] Print failure → retry 3x · then alert
- [ ] Print status tracked in box_label table

### Tablet Broken Scenario
- [ ] Tablet เสีย (no login) → ฝั่งนั้นถือว่าไม่มีงาน (per user's rule)
- [ ] Handheld cannot save box on that side (no NG checker) → block
- [ ] Admin ต้อง register tablet ใหม่ก่อนงานเริ่ม

### Auto Archive
- [ ] Scheduled job runs monthly · 1st @ 02:00
- [ ] Data > 12 months → move to archive tables
- [ ] Data > 5 years → export to S3 Glacier · delete from DB
- [ ] archive_job_log records every run
- [ ] Admin dashboard shows stats + trend
- [ ] Manual trigger works
- [ ] Historical query unified (hot + archive · async Glacier)

### Testing
- [ ] Unit: Position mutex logic
- [ ] Unit: Atomic sequence issuance
- [ ] Integration: 2 concurrent box saves → no duplicate
- [ ] Integration: Tablet down → box save blocked (no NG checker)
- [ ] E2E: Full flow · OP login → pack box → label at correct printer
- [ ] E2E: Admin register → OP use flow works
- [ ] Load test: 100 concurrent box saves across 15 machines
- [ ] Archive test: 3-year data · move to archive + Glacier restore

---

## 🚀 Migration Plan

**V25 · Position-aware overhaul** (see SQL above)

Deploy strategy:
1. **Phase 25a (week 1):** Deploy V25 · new tables + columns · backward-compatible defaults
2. **Phase 25b (week 2):** Admin registers all tablets + printers · Pilot machine (1 machine)
3. **Phase 25c (week 3):** Handheld + Tablet UI on Pilot · shadow-mode (log but don't enforce)
4. **Phase 25d (week 4):** Enforce mutex + validation · Pilot only
5. **Phase 25e (week 5+):** Roll to all machines after Pilot stable

---

## 🎯 Effort Estimate

| Component | Effort |
|---|---|
| Flyway V25 migration + archive tables | 1 day |
| Backend · Device registration entities + APIs (tablet + printer) | 1.5 days |
| Backend · Session management (tablet + handheld · with mutex) | 2 days |
| Backend · Box save atomic + validation + printer routing | 1.5 days |
| Backend · Polling endpoint | 0.5 day |
| Backend · Archive scheduler + Glacier integration | 2 days |
| Backend · Unified historical query | 1 day |
| Frontend · Admin registration screens (tablet + printer + test print) | 1.5 days |
| Frontend · Handheld session selector with position | 1 day |
| Frontend · Handheld polling + partner display | 0.75 day |
| Frontend · Tablet auto-detect + NG auto-tag | 1 day |
| Frontend · Enhanced Box Label template | 0.5 day |
| Frontend · Admin Archive management screen | 1 day |
| Testing + integration + load test | 2 days |
| **Total** | **~17.25 days (L)** |

---

## 🔗 Related Issues Consolidated

| Issue | Original | ครอบคลุมโดย component |
|---|---|---|
| **O-02** เลขกล่อง sequence | Local · จำค่า/ข้าม | Component 4 (Atomic + Polling) |
| **O-07** OP 2 คน · เลือกใครทำ | Manual select | Component 2 (Handheld Session Selector) |
| **O-13** Label มีชื่อ Packer + NG Checker | Manual add | Component 5 (Enhanced Box Label · auto from session) |
| **O-14** NG แยกซ้าย/ขวา | Manual position tag | Component 3 (Tablet Session · auto-tag) |
| **O-15** Print Label ซ้าย/ขวา · ตัวเลขต่อเนื่อง | Local sequence | Component 4 + 5 (Global sequence + position tag) |

---

## 📝 Design Decision Log

**Q1: Device identification · IP vs MAC + Device ID?**
A: **MAC + Device ID** (per user) · IP เปลี่ยนบ่อย · MAC เป็น hardware unique

**Q2: NG Checker required for box save?**
A: **Yes · block if not exists** (Option 2 per user) · quality assurance คือหัวใจ

**Q3: Position selection frequency (Handheld)?**
A: **Every login · release on logout** (per user)

**Q4: Poll interval?**
A: **15s** (per user · battery-friendly)

**Q5: Numbering reset trigger?**
A: **Lot change** · shift ไม่ reset

**Q6: 1 OP · 2 sides simultaneously?**
A: **Not possible** (per user · confirmed)

**Q7: Tablet broken?**
A: **That side stops until replacement** (per user)

**Q8: Data retention?**
A: **5 years total** · 0-12mo hot · 13-60mo archive · > 60mo Glacier

**Q9: Printer per side?**
A: **Yes · register per (machine + position + type)** · print routes to correct printer

**Q10: Handheld device tracking?**
A: **Not registered per position** · OP selects at each login · handheld_device_id optional (for tracking usage stats · not identity)

---

## 🔄 Future Enhancements (Post-Pilot)

- Position swap during shift (edge case)
- Voice login (hands-free)
- QR code login (faster than password)
- Machine dashboard for supervisors (see all 15 machines · L/R stats)
- Predictive alerts (if NG rate on 1 side > threshold)
- Historical playback (see how a shift progressed)
