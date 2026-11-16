# C-02 · Waste Weight Recording + Edit Workflow (Self-edit + Doc Approval)

**Role:** CM (record) + Doc (approve cross-shift edits)
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M-L (Medium-Large · ~7-9 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment R40-42 + follow-up 10 Sep 2026
**Related:**
- **C-01** (CM Material Meter · same role · shift concepts)
- **Q-01** (approval workflow pattern · notification back)
- **T-01** (attachment_photo · optional for waste photos)
- **NG-WS-01** (Telegram bot · Doc room)
- **OP-WS-01** (session · shift · machine concepts)

---

## 📋 Summary · สรุป

CM บันทึกน้ำหนักของเสียต่อ machine · ระบบมี **2-tier edit permissions:**

| Situation | Who can edit | Approval? | Time window |
|---|---|---|---|
| **Within own shift · own record** | Only original CM | ❌ Self-serve | Until shift ends |
| **After shift · own or other CM's record** | Any CM (via request) | ✅ Doc approval | 1 day retroactive max |
| **Beyond 1 day** | Nobody | — | — |

**Full audit trail** · every change logged · original preserved

---

## 🎯 Design Decisions · Confirmed by User

| # | Topic | Decision |
|---|---|---|
| 1 | Entry fields | Machine · Waste Type · Weight · Note (+optional Photo · WO) |
| 2 | Self-edit scope | **Own shift + own record** only |
| 3 | Cross-shift edit | Via request → Doc approval |
| 4 | Cross-CM edit | Any CM can raise on behalf of another (helping colleague) |
| 5 | Time limit | **1 day retroactive** max |
| 6 | Notifications | WebSocket + Telegram (Doc room) |
| 7 | Audit | Log every change · never physically delete |
| 8 | WO field | Required in edit request · optional at initial entry? |

---

## 🎯 3 Workflows

### Workflow A · New Waste Entry (CM records)

```
CM opens waste entry screen
  ↓
Select Machine · Waste Type · Enter Weight · Note (optional) · Photo (optional)
  ↓
Save → waste_weight_record inserted
  ↓
Visible in own history (edit button available while in same shift)
```

### Workflow B · Self-Edit (own shift · own record)

```
CM notices typo · click "Edit" on own record
  ↓
Constraint check: shift not ended · record.recorded_by = current_user
  ↓
Edit modal · change weight/note · require change reason
  ↓
Save → 
  UPDATE waste_weight_record (weight, note, updated_at, updated_by)
  INSERT waste_edit_log (edit_type='SELF_EDIT', old/new values, reason)
  ↓
Full audit trail visible in history
```

### Workflow C · Cross-Shift Edit Request (Doc approval)

```
CM (any) selects a record (own or other's, within 1 day)
  ↓
Click "Request Edit" → Edit Request form
  ↓
Fill: Machine, WO, Waste Type, Weight, Reason (required)
  ↓
Submit → 
  INSERT waste_edit_request (status='PENDING')
  Broadcast WebSocket to Doc users
  Send Telegram to @gdtahara_doc room
  ↓
Doc receives popup + Telegram
  ↓
Doc reviews · Approve or Reject
  ↓
If APPROVE:
  UPDATE waste_weight_record with requested values
  INSERT waste_edit_log (edit_type='DOC_APPROVED')
  Notify requester (and original recorder if different) via WebSocket
  Telegram confirmation
If REJECT:
  Notify requester with reason
  No change to record
```

---

## 🎨 UI/UX Design

### 1️⃣ Waste Entry Screen (CM · Add new)

```
┌─ บันทึกน้ำหนักของเสีย · CM: Somchai ────────────────────┐
│                                                            │
│  Machine:  [RBL103 ▾]  (default: last used)               │
│  WO / Lot: [ABC-2026-09-10-01 ▾]  (auto-detect · optional)│
│  Shift:    A (07:00-15:00) · auto                         │
│  Date:     10 Sep 2026 · auto                             │
│                                                            │
│  ┌─ ชนิดของเสีย (Waste Type) ─────────────────────────┐   │
│  │  ○ ถุงเศษ PE (Bag Scrap PE) — ROH scrap            │   │
│  │  ○ ถุงเศษ EVOH                                     │   │
│  │  ○ เศษพลาสติกก่อนบด (Regrind)                     │   │
│  │  ○ ถุงเศษปนเปื้อน (Contaminated)                   │   │
│  │  ○ อื่นๆ (specify below)                             │   │
│  └────────────────────────────────────────────────────┘   │
│                                                            │
│  น้ำหนัก (KG): [___12.5___]                                │
│                                                            │
│  หมายเหตุ (optional):                                       │
│  ┌────────────────────────────────────────────────────┐   │
│  │ [textarea · max 500 chars]                          │   │
│  └────────────────────────────────────────────────────┘   │
│                                                            │
│  📷 แนบรูป (optional · reuse T-01 pattern)                │
│  [📸 Camera] [🖼️ Gallery]                                 │
│                                                            │
│                       [ยกเลิก]  [✓ บันทึก]                │
└────────────────────────────────────────────────────────────┘
```

---

### 2️⃣ Waste History (with edit + request buttons)

```
┌─ Waste History · Today (10 Sep 2026) · RBL103 ─────────┐
│                                                          │
│  Filter: [All types ▾] [All CM ▾] [Include edits ✓]     │
│                                                          │
│  Time   | Type         | Weight | CM        | Actions   │
│  ─────── ───────────── ────────  ──────────  ─────────── │
│  08:15  | Bag Scrap PE | 12.5KG | Somchai   | [✏️ Edit] │  ← own · same shift
│  09:20  | Regrind      | 8.2KG  | Somchai   | [✏️ Edit] │  ← own · same shift
│  11:30  | Bag Scrap PE | 15.0KG | Sarah     | [🔔 Request Edit]│ ← other's record
│  13:00  | Contaminated | 3.5KG  | Somchai   | [✏️ Edit] │  ← own · same shift
│                                                          │
│  Total waste today (RBL103): 39.2 KG                     │
│                                                          │
└──────────────────────────────────────────────────────────┘

Yesterday (9 Sep 2026):
┌─ (view history from prior day) ─────────────────────────┐
│  15:20 | Bag Scrap PE | 10.0KG | Somchai | [🔔 Request Edit] │ ← past · within 1 day
│                                                          │
│  ⚠️ Edits after shift end require Doc approval           │
└──────────────────────────────────────────────────────────┘

Older than 1 day: read-only · no edit possible
```

**Button visibility logic:**
- **Own record + same shift + shift not ended** → `[✏️ Edit]` (direct)
- **Own or other's record + within 1 day** → `[🔔 Request Edit]` (approval)
- **Older than 1 day** → no button · read-only

---

### 3️⃣ Self-Edit Modal (own shift)

```
┌─ แก้ไขบันทึก · Waste #12345 ────────────────────────┐
│                                                        │
│  Machine: RBL103                                       │
│  Original: Somchai · 08:15 · Bag Scrap PE · 12.5 KG  │
│                                                        │
│  แก้ไขข้อมูล:                                          │
│                                                        │
│  Waste Type: [Bag Scrap PE ▾]                         │
│  น้ำหนัก:    [___15.0___] KG (was 12.5)               │
│  หมายเหตุ:   [_________________________]              │
│                                                        │
│  ⚠️ เหตุผลการแก้ไข * (required):                       │
│  ┌────────────────────────────────────────────────┐   │
│  │ [กรอกผิด · จริงๆชั่งได้ 15 KG]                    │   │
│  └────────────────────────────────────────────────┘   │
│                                                        │
│  ℹ️ การแก้ไขจะถูกบันทึกใน Log สำหรับตรวจสอบ           │
│                                                        │
│                    [ยกเลิก]  [✓ บันทึกการแก้ไข]        │
└────────────────────────────────────────────────────────┘
```

**Rules:**
- Reason required (min 10 chars)
- Log kept (original values + change reason + who + when)
- History shows: current values + "edited 1x · click for history"

---

### 4️⃣ Edit Request Modal (cross-shift or other's record)

```
┌─ ขอแก้ไขบันทึก · ต้องขอ Doc อนุมัติ ──────────────┐
│                                                        │
│  📋 Original Record:                                   │
│  Recorded by: Sarah (CM) · 11:30 · Shift A · 9 Sep    │
│  Machine: RBL103                                       │
│  Type: Bag Scrap PE                                    │
│  Weight: 15.0 KG                                       │
│                                                        │
│  ─────────────────────────────                        │
│  📝 New Request:                                       │
│                                                        │
│  Machine: [RBL103 ▾]                                   │
│  WO:      [ABC-2026-09-09-02 ▾]  *required           │
│  Waste Type: [Bag Scrap PE ▾]                         │
│  Weight (KG): [___10.5___] (was 15.0)                 │
│                                                        │
│  ⚠️ เหตุผล * (required · Doc จะเห็น):                 │
│  ┌────────────────────────────────────────────────┐   │
│  │ Sarah บันทึกน้ำหนักผิด · จริงๆชั่งได้ 10.5 KG   │   │
│  │ ตรวจกับ scale log แล้ว                          │   │
│  └────────────────────────────────────────────────┘   │
│                                                        │
│  📷 แนบรูป (optional · เช่น scale reading photo)      │
│                                                        │
│  Requested by: Somchai (CM)                            │
│  Time: 10 Sep 2026 · 09:30                             │
│                                                        │
│  ⚠️ ส่งไป Doc เพื่ออนุมัติ · จะได้รับแจ้งเตือนกลับ    │
│                                                        │
│                    [ยกเลิก]  [✓ ส่งขออนุมัติ]          │
└────────────────────────────────────────────────────────┘
```

**Rules:**
- All fields required (Machine, WO, Type, Weight, Reason)
- Photo optional
- Submit → create request · status=PENDING · notify Doc

---

### 5️⃣ Doc Dashboard · Approval Queue

```
┌─ Doc Approval Queue · Waste Edit Requests ─────────────┐
│                                                          │
│  Filter: [Pending ▾] [Today ▾]                          │
│                                                          │
│  🔔 Pending: 3                                           │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🟡 Request #42                                    │  │
│  │ Requester: Somchai (CM) · 09:30                  │  │
│  │ Machine: RBL103 · WO: ABC-2026-09-09-02          │  │
│  │ Original: Sarah · 15.0 KG → Request: 10.5 KG    │  │
│  │ Reason: Sarah บันทึกผิด · จริงๆ 10.5 KG          │  │
│  │        [ดู Details] [✅ Approve] [❌ Reject]     │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🟡 Request #43                                    │  │
│  │ ...                                               │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Approve/Reject Modal:**
```
┌─ อนุมัติการแก้ไข · Request #42 ──────────────────────┐
│                                                        │
│  Original: Bag Scrap PE · 15.0 KG · Sarah (11:30)     │
│  Requested: Bag Scrap PE · 10.5 KG                    │
│  Reason: (requester's note)                            │
│                                                        │
│  Doc Note (optional):                                  │
│  [_________________________]                          │
│                                                        │
│                 [❌ Reject]  [✅ Approve]              │
└────────────────────────────────────────────────────────┘

Reject → require reason
Approve → apply change · notify CM (requester + original recorder)
```

---

### 6️⃣ Notification Back to CM

**Approved:**
```
┌─ ✅ Doc Approved Your Edit Request ─────────────┐
│                                                    │
│  Request #42 · RBL103 · Bag Scrap PE               │
│  Original: 15.0 KG → Now: 10.5 KG                  │
│  Approved by: Doc Pranee · 09:45                   │
│                                                    │
│                              [OK] [View]           │
└────────────────────────────────────────────────────┘
```

**Rejected:**
```
┌─ ❌ Doc Rejected Your Edit Request ─────────────┐
│                                                    │
│  Request #42 · RBL103                              │
│  Requested: 15.0 → 10.5 KG                         │
│  Doc reason: "โปรดตรวจซ้ำกับ scale log · unclear" │
│                                                    │
│                              [OK] [Re-submit]      │
└────────────────────────────────────────────────────┘
```

**Also notify original recorder** (Sarah) via WebSocket + Telegram
(so she knows her record was changed · transparency)

---

## 🗄️ Data Model Changes (V30 Migration)

```sql
-- V30__waste_weight_edit_workflow.sql

-- ============================================
-- 1. Waste Type Master
-- ============================================

CREATE TABLE waste_type_master (
  code VARCHAR(30) PRIMARY KEY,
  name_th VARCHAR(200) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  category VARCHAR(50),               -- 'ROH_SCRAP' | 'REGRIND' | 'CONTAMINATED' | 'OTHER'
  is_active BOOLEAN DEFAULT TRUE,
  sort_order INT DEFAULT 0
);

INSERT INTO waste_type_master (code, name_th, name_en, category, sort_order) VALUES
  ('BAG_SCRAP_PE',    'ถุงเศษ PE',              'Bag Scrap PE',        'ROH_SCRAP', 1),
  ('BAG_SCRAP_EVOH',  'ถุงเศษ EVOH',            'Bag Scrap EVOH',      'ROH_SCRAP', 2),
  ('REGRIND',         'เศษพลาสติกก่อนบด',       'Regrind',             'REGRIND',   3),
  ('CONTAMINATED',    'ถุงเศษปนเปื้อน',         'Contaminated waste',  'CONTAMINATED', 4),
  ('OTHER',           'อื่นๆ',                  'Other',               'OTHER',     99);

-- ============================================
-- 2. Waste Weight Record (main table)
-- ============================================

CREATE TABLE waste_weight_record (
  record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  machine_id VARCHAR(20) NOT NULL,
  wo_number VARCHAR(50) NULL,             -- optional at entry · required in edit request
  waste_type_code VARCHAR(30) NOT NULL,
  weight_kg DECIMAL(10,3) NOT NULL,       -- support 3 decimals for precise scale
  note VARCHAR(500),
  photo_ids JSON NULL,                     -- reuse attachment_photo
  
  recorded_by VARCHAR(50) NOT NULL,        -- CM username
  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  shift_code VARCHAR(10) NOT NULL,
  shift_date DATE NOT NULL,
  
  -- Edit tracking
  version INT NOT NULL DEFAULT 1,
  last_edited_by VARCHAR(50) NULL,
  last_edited_at TIMESTAMP NULL,
  edit_count INT DEFAULT 0,
  
  is_active BOOLEAN DEFAULT TRUE,
  
  CONSTRAINT fk_wwr_type FOREIGN KEY (waste_type_code) REFERENCES waste_type_master(code),
  INDEX idx_wwr_machine_shift (machine_id, shift_date, shift_code),
  INDEX idx_wwr_user (recorded_by, recorded_at),
  INDEX idx_wwr_time (recorded_at)
);

-- ============================================
-- 3. Edit Log (full audit · every change)
-- ============================================

CREATE TABLE waste_edit_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id BIGINT NOT NULL,
  edit_type VARCHAR(20) NOT NULL,          -- 'SELF_EDIT' | 'DOC_APPROVED'
  edited_by VARCHAR(50) NOT NULL,
  edited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  -- Before/after snapshot
  old_machine_id VARCHAR(20),
  old_wo_number VARCHAR(50),
  old_waste_type_code VARCHAR(30),
  old_weight_kg DECIMAL(10,3),
  old_note VARCHAR(500),
  
  new_machine_id VARCHAR(20),
  new_wo_number VARCHAR(50),
  new_waste_type_code VARCHAR(30),
  new_weight_kg DECIMAL(10,3),
  new_note VARCHAR(500),
  
  reason VARCHAR(500) NOT NULL,             -- always required
  edit_request_id BIGINT NULL,              -- FK if edit_type=DOC_APPROVED
  
  CONSTRAINT fk_wel_record FOREIGN KEY (record_id) REFERENCES waste_weight_record(record_id),
  CONSTRAINT chk_wel_type CHECK (edit_type IN ('SELF_EDIT','DOC_APPROVED')),
  INDEX idx_wel_record (record_id, edited_at)
);

-- ============================================
-- 4. Edit Request (approval workflow)
-- ============================================

CREATE TABLE waste_edit_request (
  request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id BIGINT NOT NULL,
  requester_id VARCHAR(50) NOT NULL,        -- CM raising the request
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  -- Requested changes
  requested_machine_id VARCHAR(20),
  requested_wo_number VARCHAR(50) NOT NULL, -- required in request
  requested_waste_type_code VARCHAR(30),
  requested_weight_kg DECIMAL(10,3),
  requested_note VARCHAR(500),
  reason VARCHAR(1000) NOT NULL,            -- required
  request_photo_ids JSON NULL,              -- optional (e.g. scale reading photo)
  
  -- Doc response
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  doc_id VARCHAR(50) NULL,
  doc_responded_at TIMESTAMP NULL,
  doc_note VARCHAR(500) NULL,
  
  -- Telegram tracking
  telegram_sent BOOLEAN DEFAULT FALSE,
  telegram_sent_at TIMESTAMP NULL,
  
  CONSTRAINT fk_wer_record FOREIGN KEY (record_id) REFERENCES waste_weight_record(record_id),
  CONSTRAINT chk_wer_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
  INDEX idx_wer_status (status),
  INDEX idx_wer_pending (status, requested_at)
);

-- ============================================
-- 5. Update attachment_photo (from T-01)
-- ============================================

ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST',
                       'QA_VERIFICATION', 'BOX_REPRINT', 'TANK_REFILL',
                       'WASTE_WEIGHT', 'WASTE_EDIT_REQUEST'));

-- ============================================
-- 6. Ensure Doc room exists (from NG-WS-01)
-- ============================================

INSERT INTO telegram_room_config (room_code, room_name, description) VALUES
  ('DOC', 'GDTAHARA Doc Team', 'Doc team · approve requests + document control')
ON CONFLICT (room_code) DO NOTHING;
```

---

## 🔌 API Endpoints

### Waste Recording
- `POST /api/cm/waste` — create waste record
- `GET /api/cm/waste/history?machineId=X&from=Y&to=Z` — list with edit status
- `GET /api/cm/waste/{id}` — full detail with edit history

### Self-Edit (within own shift + own record)
- `PATCH /api/cm/waste/{id}/self-edit` — allowed only if:
  - `record.recorded_by = current_user`
  - `now < shift_end_time` (or shift still active)
  - Require reason field
  - Increment version + log

### Edit Request
- `POST /api/cm/waste/{id}/edit-request` — create request
- `GET /api/doc/edit-requests?status=PENDING` — Doc queue
- `POST /api/doc/edit-request/{id}/approve` — Doc approves
- `POST /api/doc/edit-request/{id}/reject` — Doc rejects (require reason)
- `GET /api/cm/edit-request/mine?status=X` — my requests status
- `PATCH /api/cm/edit-request/{id}/cancel` — requester cancels (only if PENDING)

### Master
- `GET /api/waste-type-master?active=true` — dropdown list
- Admin CRUD (basic)

### WebSocket
- `WASTE_EDIT_REQUEST_NEW` — broadcast to Doc users
- `WASTE_EDIT_REQUEST_RESPONSE` — send to requester + original recorder

---

## 🖥️ Frontend Components (React)

```
src/components/CM/
  ├── WasteEntryForm.tsx           (NEW · machine + type + weight + note + photo)
  ├── WasteHistoryTable.tsx        (NEW · edit/request buttons based on rules)
  ├── WasteSelfEditModal.tsx       (NEW · edit own · same shift)
  ├── WasteEditRequestModal.tsx    (NEW · cross-shift request)
  ├── WasteEditHistoryView.tsx     (NEW · show all changes to one record)
  └── MyEditRequestStatus.tsx      (NEW · list my requests · status update)

src/components/Doc/
  ├── DocDashboard.tsx             (NEW · queue of pending requests)
  ├── EditRequestReviewModal.tsx   (NEW · full detail + approve/reject)
  └── EditRequestHistory.tsx       (NEW · all requests handled)

src/hooks/
  ├── useWasteEditPermissions.ts   (NEW · determine edit vs request based on rules)
  └── useEditRequestQueue.ts       (NEW · Doc realtime state)
```

---

## 🔒 Permission Logic (Frontend + Backend)

```typescript
function getEditPermission(record, currentUser, currentTime) {
  const shiftEnd = getShiftEnd(record.shift_date, record.shift_code);
  const recordAge = currentTime - record.recorded_at;
  const oneDay = 24 * 60 * 60 * 1000;
  
  // Own shift + own record → self-edit
  if (record.recorded_by === currentUser.id && 
      currentTime < shiftEnd) {
    return 'SELF_EDIT';
  }
  
  // Within 1 day (any CM) → request approval
  if (recordAge < oneDay && currentUser.role === 'CM') {
    return 'REQUEST_EDIT';
  }
  
  // Beyond 1 day → read-only
  return 'READ_ONLY';
}
```

**Backend enforces same logic** in every edit endpoint (defense in depth).

---

## ✅ Acceptance Criteria

### Basic Recording
- [ ] CM records waste with Machine + Type + Weight + Note + optional Photo
- [ ] Auto-detect shift (from time) + CM (from session)
- [ ] Save → waste_weight_record inserted

### Self-Edit
- [ ] Edit button visible only for own record + same shift active
- [ ] Reason required (min 10 chars)
- [ ] UPDATE record + INSERT waste_edit_log with old/new snapshot
- [ ] History shows "edited N times · click for detail"

### Cross-Shift Edit Request
- [ ] "Request Edit" button on records ≤ 1 day old
- [ ] Form requires: Machine + WO + Type + Weight + Reason
- [ ] Submit → INSERT request (PENDING)
- [ ] Broadcast WebSocket to Doc users
- [ ] Send Telegram to @gdtahara_doc room

### Doc Approval
- [ ] Doc dashboard shows pending queue realtime
- [ ] Approve → UPDATE record + INSERT waste_edit_log (DOC_APPROVED) + link request
- [ ] Reject → set status REJECTED + reason required
- [ ] Notify requester + original recorder (WebSocket + Telegram)

### Time Enforcement
- [ ] Backend rejects self-edit if not same shift or not own record
- [ ] Backend rejects edit request if record > 1 day old
- [ ] Frontend hides buttons matching backend rules

### Audit
- [ ] Every change logged with before/after snapshot + reason
- [ ] Never physically delete records
- [ ] History view shows chronological changes
- [ ] Original recorder notified when their record is edited

### Testing
- [ ] Unit: permission logic (all edge cases)
- [ ] Integration: self-edit within shift · with reason
- [ ] Integration: cross-shift request → Doc approve → both sides notified
- [ ] Integration: reject flow
- [ ] E2E: full workflow with Telegram delivery
- [ ] Edge: request > 1 day (should block)
- [ ] Edge: same-shift edit by different CM (should require request)
- [ ] Load: 100 concurrent requests · Doc dashboard responsive

---

## 🚀 Migration Plan

**V30** — see SQL above

**Deploy strategy:**
- Deploy V30 · schema in place
- Seed waste_type_master · verify with QA/PD
- Frontend deploy · Pilot machine only 1 week
- Enforce rules · roll out all

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V30 migration + seed | 0.5 day |
| Backend · WasteWeightRecord entity + CRUD | 0.75 day |
| Backend · Self-edit endpoint + permission check | 0.5 day |
| Backend · Edit request + approval workflow | 1 day |
| Backend · WebSocket integration + Telegram alerts | 0.75 day |
| Backend · Doc room + notifications | 0.5 day |
| Frontend · Waste Entry form | 0.75 day |
| Frontend · History table with permission-aware buttons | 1 day |
| Frontend · Self-Edit Modal | 0.5 day |
| Frontend · Edit Request Modal | 0.75 day |
| Frontend · Doc Dashboard + Review Modal | 1 day |
| Frontend · Edit history viewer | 0.5 day |
| Testing + integration | 1.5 days |
| **Total** | **~9.0 days (M-L)** |

---

## 🔗 Dependencies

- **T-01 (V22)** · attachment_photo (add WASTE_WEIGHT + WASTE_EDIT_REQUEST ref types)
- **NG-WS-01 (V26)** · Telegram bot service · add DOC room
- **OP-WS-01 (V25)** · session + shift concepts
- **C-01 (V29)** · shift boundary + shift_code (reuse config)
- **Q-01 (V24)** · approval workflow pattern (Doc = approver like QA)

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Self-edit within own shift + own record | Fast · trust · recorder knows best |
| 2 | Cross-shift = Doc approval | Control · avoid abuse · Doc = data custodian |
| 3 | Any CM can request (helping colleague) | Practical · original CM may be offline |
| 4 | 1 day retroactive max | Balance · beyond 1 day = data too stale |
| 5 | WO required in request (not initial) | Traceability · WO known by edit time |
| 6 | Log EVERY change | Full audit · IATF compliance |
| 7 | Notify original recorder too | Transparency · avoid ghost edits |
| 8 | Physical delete = never | Immutable · trust · trace |

---

## 🔮 Future Enhancements

- Bulk edit (multiple records same reason) · Phase 2
- Waste weight validation against machine's normal range · alert if outlier
- Auto-suggest weight from scale integration (Phase 7 IIoT)
- Weekly waste report per machine · trend analysis
- Cost impact of waste (once material cost data available)
- Doc dashboard KPI (avg response time · approval rate)

---

## 🎯 Integration with Daily Production Report

- Waste weight per shift per machine per type
- Correction rate (edits/total)
- Time-to-approval for edit requests
- Cost of waste (future · when material cost integrated)
