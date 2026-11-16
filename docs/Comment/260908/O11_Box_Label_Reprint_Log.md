# O-11 · Box Label Reprint · Reason Logging + Traceability

**Role:** Operator
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M (Medium · ~3-4 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment sheet R20 + follow-up 10 Sep 2026
**Related:** OP-WS-01 (box_label + position + printer routing) · T-01 (attachment_photo · optional)

---

## 📋 Summary · สรุป

เมื่อ Box label เสียหรือหาย · OP ต้อง reprint · ปัจจุบันไม่มี log · ตรวจสอบย้อนกลับไม่ได้ · เสี่ยง label ซ้ำ/หายในระบบ

**Feature นี้:** เพิ่ม reprint workflow ที่บังคับกรอกเหตุผล + audit log สมบูรณ์

---

## 🎯 User Story

**As an Operator,**
เมื่อ Box label เสียหาย (พิมพ์ไม่ชัด · โดนน้ำ · ฉีกขาด),
I want to:
- กด "Reprint Label" จาก Box history
- กรอกเหตุผล (จาก dropdown หรือ free-text)
- ระบบออก label ใหม่ที่ printer ฝั่งเดียวกัน (per OP-WS-01)
- ระบบเก็บ log ครบ · ตรวจย้อนหลังได้

**As a Supervisor/QA,**
I want to:
- เห็นสถิติ reprint (ต่อ shift · per OP · per machine)
- ตรวจสอบเหตุผลแต่ละครั้ง
- Alert ถ้า reprint rate สูงผิดปกติ (เกิดปัญหา printer หรือ label stock?)

---

## 📸 Current State

**Box History Screen** (จาก Handheld · หน้า Packing history)
- แสดง list ของ box ที่บรรจุแล้ว
- **ไม่มี** reprint button ปัจจุบัน
- **ไม่มี** reason tracking · reprint = manual walk to printer

---

## 🎨 UI/UX Requirements

### 1️⃣ Reprint Button on Box History

```
┌─ Box History · RBL103 · Today ──────────────────────┐
│                                                       │
│  Box #  Position  Packed by   Time      Actions      │
│  ─────  ────────  ─────────   ────────  ─────────────│
│  #42    🅛 LEFT   OP1         08:15    [🖨️ Reprint] │
│  #43    🅡 RIGHT  OP4         08:17    [🖨️ Reprint] │
│  #44    🅛 LEFT   OP1         08:22    [🖨️ Reprint] │  🔴 reprinted 1x
│  #45    🅛 LEFT   OP1         08:29    [🖨️ Reprint] │  🔴 reprinted 2x
│                                                       │
└───────────────────────────────────────────────────────┘
```

**Behavior:**
- Reprint button per box row · always visible (no permission gate)
- Reprint count badge showing if > 0 (visible warning)
- Click → open Reason Modal

---

### 2️⃣ Reprint Reason Modal

```
┌─ Reprint Box Label · #44 · 🅛 LEFT ─────────────────┐
│                                                       │
│  Box Details:                                        │
│  Box #:      44                                      │
│  Position:   LEFT                                    │
│  Packed by:  OP1 (08:22)                             │
│  Reprints so far: 1 (last: 08:31 by OP1)            │
│                                                       │
│  ⚠️ Reprint reason (required):                       │
│                                                       │
│  ┌────────────────────────────────────────────────┐ │
│  │ ○ Label เสียหาย (พิมพ์ไม่ชัด · ฉีก · เปื้อน)   │ │
│  │ ○ Label หาย                                      │ │
│  │ ○ พิมพ์ผิด (barcode/text ไม่ถูก)                │ │
│  │ ○ Printer ค้าง · ทำใหม่                          │ │
│  │ ○ เปลี่ยนข้อมูล box (ระบุ)                        │ │
│  │ ○ อื่นๆ (ระบุ)                                    │ │
│  └────────────────────────────────────────────────┘ │
│                                                       │
│  รายละเอียดเพิ่มเติม (required):                     │
│  ┌────────────────────────────────────────────────┐ │
│  │ [textarea · min 10 chars · max 500]              │ │
│  └────────────────────────────────────────────────┘ │
│                                                       │
│  📷 แนบรูป (optional · label เสีย)                   │
│  [📸 Camera] [🖼️ Gallery]  [thumbnails]              │
│                                                       │
│                    [ยกเลิก]  [🖨️ Reprint]           │
└───────────────────────────────────────────────────────┘
```

**Rules:**
- Reason category = required (radio · single select)
- Detail text = required (min 10 chars)
- Photo = optional (reuse T-01 attachment_photo · ref_type='BOX_REPRINT')
- Submit → POST /api/box/{id}/reprint + trigger print

---

### 3️⃣ Post-Reprint Confirmation

```
┌─ ✅ Reprint Successful ──────────────────┐
│                                            │
│  Box #44 · LEFT · reprint #2               │
│  Printed at: RBL103-Left-Thermal          │
│  Time: 08:35                               │
│  Reason: Label เสียหาย                    │
│                                            │
│                              [OK]          │
└────────────────────────────────────────────┘
```

**Note:** If reprint > 3 times for same box → **warn Supervisor** (Telegram alert · reuse NG-WS-01 pattern)

---

### 4️⃣ Admin/Supervisor Dashboard

```
┌─ Reprint Statistics · Today ──────────────────────────┐
│                                                        │
│  Total Reprints Today: 12 (of 245 boxes = 4.9%)       │
│                                                        │
│  By Reason:                                            │
│  ┌────────────────────┬──────┬──────────────┐        │
│  │ Reason              │ Count│ % of reprints│        │
│  ├────────────────────┼──────┼──────────────┤        │
│  │ Label เสียหาย       │  6   │ 50%          │        │
│  │ Printer ค้าง        │  3   │ 25%          │        │
│  │ Label หาย           │  2   │ 17%          │        │
│  │ อื่นๆ                │  1   │  8%          │        │
│  └────────────────────┴──────┴──────────────┘        │
│                                                        │
│  By Machine:                                           │
│  RBL103: 5 reprints · RBL102: 3 · RBL104: 4          │
│                                                        │
│  By OP:                                                │
│  OP1: 4 · OP2: 3 · OP4: 5 (⚠️ high · investigate?)   │
│                                                        │
│  🚩 Boxes reprinted 3+ times:                          │
│  Box #124 · RBL103 · 3 reprints · investigate          │
│                                                        │
└────────────────────────────────────────────────────────┘
```

**Thresholds (configurable):**
- Reprint rate > 5% per day → alert
- Same box reprinted > 3 times → Telegram Supervisor
- OP reprint > 10/shift → training review

---

## 🗄️ Data Model Changes (V27 Migration)

```sql
-- V27__box_reprint_log.sql

CREATE TABLE box_reprint_log (
  reprint_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  box_label_id BIGINT NOT NULL,             -- FK to box_label
  reprint_sequence INT NOT NULL,            -- 1, 2, 3... per box
  reprinted_by VARCHAR(50) NOT NULL,        -- OP username
  reprinted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reason_code VARCHAR(30) NOT NULL,         -- FK to reason master
  reason_detail VARCHAR(500) NOT NULL,      -- required · min 10 chars
  photo_ids JSON NULL,                       -- reuse attachment_photo (ref_type='BOX_REPRINT')
  printer_id VARCHAR(100),                   -- which printer used (from OP-WS-01)
  print_status VARCHAR(20) DEFAULT 'PENDING',
  print_completed_at TIMESTAMP NULL,
  handheld_session_id BIGINT NULL,           -- from OP-WS-01
  CONSTRAINT fk_brl_box FOREIGN KEY (box_label_id) REFERENCES box_label(id),
  CONSTRAINT fk_brl_reason FOREIGN KEY (reason_code) REFERENCES box_reprint_reason_master(code),
  CONSTRAINT uk_brl_box_seq UNIQUE (box_label_id, reprint_sequence),
  INDEX idx_brl_box (box_label_id),
  INDEX idx_brl_user (reprinted_by, reprinted_at),
  INDEX idx_brl_time (reprinted_at)
);

CREATE TABLE box_reprint_reason_master (
  code VARCHAR(30) PRIMARY KEY,
  name_th VARCHAR(200) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  sort_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO box_reprint_reason_master (code, name_th, name_en, sort_order) VALUES
  ('LABEL_DAMAGED', 'Label เสียหาย (พิมพ์ไม่ชัด · ฉีก · เปื้อน)', 'Label damaged', 1),
  ('LABEL_LOST',    'Label หาย',                                    'Label lost', 2),
  ('PRINT_WRONG',   'พิมพ์ผิด (barcode/text ไม่ถูก)',              'Print wrong', 3),
  ('PRINTER_ISSUE', 'Printer ค้าง · ทำใหม่',                        'Printer stuck', 4),
  ('DATA_CHANGE',   'เปลี่ยนข้อมูล box',                            'Data change', 5),
  ('OTHER',         'อื่นๆ (ระบุใน detail)',                        'Other', 99);

-- Add reprint tracking to box_label
ALTER TABLE box_label
  ADD COLUMN reprint_count INT NOT NULL DEFAULT 0,
  ADD COLUMN last_reprinted_at TIMESTAMP NULL;

-- Update attachment_photo constraint (from T-01)
ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST',
                       'QA_VERIFICATION', 'BOX_REPRINT'));
```

---

## 🔌 API Endpoints

**POST `/api/box/{boxId}/reprint`**
```json
{
  "reasonCode": "LABEL_DAMAGED",
  "reasonDetail": "label โดนน้ำเปื้อน · barcode อ่านไม่ได้",
  "photoIds": [123, 124],
  "handheldSessionId": 5678
}
```
Returns:
```json
{
  "reprintId": 42,
  "reprintSequence": 2,
  "printerId": "ZT411-RBL103-L",
  "printStatus": "PENDING"
}
```

**GET `/api/box/{boxId}/reprint-history`**
- Return list of all reprints for this box · with timing + reasons

**GET `/api/admin/reprint-stats?from=X&to=Y&machineId=Z&opId=W`**
- Aggregated stats · counts by reason/machine/OP
- Boxes with reprint > 3 (attention list)

**GET `/api/reprint-reasons?active=true`**
- Dropdown list · admin-managed

---

## 🖥️ Frontend Components (React)

```
src/components/OP/
  ├── BoxHistoryTable.tsx          (MODIFY · add Reprint button + count badge)
  └── ReprintReasonModal.tsx       (NEW · reason picker + detail + photo)

src/components/Admin/
  ├── ReprintStatsDashboard.tsx    (NEW · stats + trends)
  └── ReprintReasonConfig.tsx      (NEW · manage reason master)

src/components/shared/
  └── PhotoAttachmentField.tsx     (reuse from T-01)
```

---

## ✅ Acceptance Criteria

### Reprint Action
- [ ] Reprint button visible on every box in history
- [ ] Reprint count badge (if > 0) on box row
- [ ] Click → Reason Modal opens
- [ ] Reason category = required (radio)
- [ ] Detail text = required (min 10 chars)
- [ ] Photo attach = optional (reuse T-01)
- [ ] Submit → API call · trigger print at correct printer (position-routed per OP-WS-01)

### Logging
- [ ] `box_reprint_log` records: who, when, what box, what sequence #, reason, detail, photos
- [ ] `box_label.reprint_count` increments · `last_reprinted_at` updates
- [ ] Session tracked (handheld_session_id)

### Printer Routing
- [ ] Reprint goes to same position's printer (LEFT box → LEFT printer)
- [ ] Print failure → retry · alert if 3x fail

### Stats & Alerts
- [ ] Admin dashboard · reprint rate today/week/month
- [ ] By reason · by machine · by OP
- [ ] Attention list · boxes reprinted > 3 times
- [ ] Alert (Telegram Supervisor room) if box > 3 reprints
- [ ] Alert if daily reprint rate > 5%

### Testing
- [ ] Unit: reprint sequence increment
- [ ] Integration: reprint → print at correct printer → log complete
- [ ] E2E: full flow (OP click → reason → print → verify log)
- [ ] Test บน Samsung Tab Active 5 Pro
- [ ] Test alert threshold (mock 4th reprint · verify Telegram sent)

---

## 🚀 Migration Plan

**V27** — see SQL above

**Deploy strategy:**
- Deploy after V25 (OP-WS-01) and V22 (T-01) · uses both tables
- Backward compatible · existing box_label rows work fine
- Reprint feature opt-in via UI (button appears once deployed)

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V27 migration + seed reasons | 0.25 day |
| Backend · BoxReprintLog entity + Service + audit | 0.5 day |
| Backend · Reprint endpoint (with position routing + threshold check) | 0.75 day |
| Backend · Stats aggregation endpoint | 0.5 day |
| Backend · Telegram alert integration (reuse NG-WS-01) | 0.25 day |
| Frontend · Box History table update + reprint button | 0.5 day |
| Frontend · ReprintReasonModal | 0.75 day |
| Frontend · Admin Stats Dashboard | 0.75 day |
| Frontend · Reason master CRUD screen | 0.25 day |
| Testing + integration | 0.5 day |
| **Total** | **~4.0 days (M)** |

---

## 🔗 Dependencies & Reuse

- **OP-WS-01 (V25):** box_label, position, printer_registration, handheld_session
- **T-01 (V22):** attachment_photo (add BOX_REPRINT ref_type)
- **NG-WS-01 (V26):** Telegram bot service (reuse for supervisor alert)

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Reason = required (from dropdown) | Structured data · reporting |
| 2 | Detail text = required min 10 chars | Prevent lazy "test" entries |
| 3 | Photo = optional | Not always needed · but useful for label defect analysis |
| 4 | Auto-route to same position's printer | Per OP-WS-01 · consistent flow |
| 5 | Alert at 3 reprints per box | Threshold to catch systemic issues |
| 6 | Track reprint_sequence per box | Ordered · shows history clearly |
| 7 | No approval needed | Trust OP · but log everything for audit |
| 8 | Attention list for supervisor | Proactive quality control |

---

## 🔮 Future Enhancements (Post-Pilot)

- Bulk reprint (select multiple boxes)
- Auto-detect: if barcode scan fails at customer/warehouse · flag for reprint
- Reason auto-suggest based on ML (from historical patterns)
- Reprint audit trail export to Excel/PDF for ISO audits
