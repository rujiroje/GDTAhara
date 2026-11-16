# Q-01 · QA Setup Verification Workflow (Tech → QA → Tech)

**Role:** QA (Quality Assurance)
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** L (Large · ~7-9 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment sheet R6
**Related:** T-01 (Setup activity + photo) · T-03 (WebSocket notification infra) · O-10 (notification pattern)

---

## 📋 Summary · สรุป

Workflow ใหม่สำหรับการตรวจสอบ Setup โดย QA:
1. **Tech** ทำ Setup เครื่องจักร → เลือก Activity code "**Q**" (QA Check)
2. **System** → ส่งแจ้งเตือนไป **QA**
3. **QA** กด "รับเรื่อง" → ระบบเริ่ม **จับเวลา** (start QA verification timer)
4. **QA** ตรวจสอบเรียบร้อย → กด **บันทึกผล**:
   - ✅ อนุมัติ (Approve) หรือ ❌ ไม่อนุมัติ (Reject)
   - ใส่เหตุผล / รายละเอียด
   - แนบรูปประกอบ
5. **System** → หยุดจับเวลา · บันทึก duration
6. **System** → แจ้งเตือนกลับไป **Tech** → ดำเนินการต่อ (produce · หรือ redo setup)

---

## 🎯 User Story

**As a Technician,**
เมื่อ Setup เครื่องเสร็จ (เปลี่ยนแม่พิมพ์ · ตั้งค่า · ทดสอบ),
I want to:
- เลือก Activity code "Q" ตอนจบ Setup → ระบบขอ QA verify อัตโนมัติ
- ได้รับ notification กลับเมื่อ QA อนุมัติหรือไม่อนุมัติ
- อ่านเหตุผล + ดูรูป QA ถ้าถูก Reject → ปรับปรุงและขอ verify ใหม่

**As a QA,**
เมื่อ Tech ขอ QA verify Setup,
I want to:
- ได้รับ notification realtime (บน tablet)
- กด "รับเรื่อง" → เริ่มไปตรวจ (timer เริ่ม)
- ตรวจ mold/setup/dimension/first article → บันทึกผล approve/reject
- ใส่เหตุผลชัดเจน + แนบรูป (mold ที่ตรวจ · dimension chart · first article)
- ระบบส่งแจ้งกลับให้ Tech อัตโนมัติ

**So that:**
- ลด mistake production start ก่อน QA verify
- Traceability สมบูรณ์ · หลักฐานการตรวจ + เวลา
- Response time measurable · QA KPI
- Root cause analysis กรณี defect ไป customer

---

## 📸 Current State

**Screen: Setup Modal (Tech)** — ตาม T-01 screenshots
- Activity Code dropdown: `M — เปลี่ยนแม่พิมพ์` (Change Mold)
- คาดว่ามี code อื่นๆ (เช่น Cleaning · Adjust · ฯลฯ)
- **Missing:** Code `Q` สำหรับ QA verification request

**Screen: QA** — ยังไม่มีในระบบ · ต้องสร้างใหม่

---

## 🎨 UI/UX Requirements

### 1️⃣ Tech Side · Trigger QA Verification

**Where:** SetupTimeLogModal (T-01) · Activity Code dropdown

**Add new activity code:**
```
Activity Code * dropdown:
  ● M — เปลี่ยนแม่พิมพ์
  ● A — ปรับตั้งเครื่อง (Adjust)
  ● C — ทำความสะอาด (Cleaning)
  ● T — ทดสอบ (Test)
  ✨ Q — ขอ QA ตรวจสอบ (Request QA Verification)  ← NEW
```

**Behavior when Q selected:**
- Start timer เหมือน activity อื่น
- เมื่อกด "จบ Activity นี้" → **auto-create QA verification request** (ไม่ต้องกดปุ่มพิเศษ)
- แสดง status badge: `⏳ รอ QA ตรวจสอบ` บน setup job
- Tech ทำ Setup ต่อไม่ได้จนกว่า QA จะ verify (block SETUP เสร็จสิ้น button)

**Alternative:** ปุ่มพิเศษ "🔔 ขอ QA verify" แยกออกจาก Activity code (ถ้าอยากให้ triggered manually)

---

### 2️⃣ QA Side · Notification Bell + Queue

**Reuse T-03 pattern** (bell icon + queue panel + WebSocket)

**Where:** QA login header
```
GDTahara                       สวัสดี QA1  🔔②  (QA) TH EN [ออกจากระบบ]
```

**Notification popup:**
```
┌─ 🔔 QA VERIFICATION REQUEST ─────────────────────┐
│                                                    │
│  Machine:    RBL103 · TAHARA                       │
│  Setup by:   Tech1                                 │
│  Job:        NBKPMY1000 (SubLot ABC-2026-09-09-01)│
│  Requested:  2 นาทีที่แล้ว · 08:29                 │
│  Activity:   Q — ขอ QA ตรวจสอบ                    │
│                                                    │
│  รายละเอียด (จาก Tech · optional):                │
│  ┌────────────────────────────────────────────────┐│
│  │ เปลี่ยน mold ใหม่ · adjust temp · ready first   ││
│  │ article check                                   ││
│  └────────────────────────────────────────────────┘│
│                                                    │
│  📷 Photos จาก Tech (3)                           │
│  [thumbnail] [thumbnail] [thumbnail]              │
│                                                    │
│  ┌──────────────┐  ┌────────────┐                 │
│  │ ✅ รับเรื่อง │  │ 💤 SNOOZE  │                 │
│  │  (ACCEPT)    │  │  5 นาที    │                 │
│  └──────────────┘  └────────────┘                 │
└────────────────────────────────────────────────────┘
```

**Actions:**
- **Accept:** เริ่มจับเวลา · popup ปิด · เปิด Verification Card
- **Snooze:** ซ่อน 5 นาที · re-notify

---

### 3️⃣ QA Side · Active Verification Card + Result Modal

**After Accept · Active Card (persistent · top-right):**
```
┌─ กำลังตรวจ Setup ────────────┐
│  RBL103 · Tech1 · NBKPMY1000 │
│  ⏱️ 00:08:42 (elapsed)       │
│  [📝 บันทึกผล] [❌ ยกเลิก]    │
└──────────────────────────────┘
```

**Click "บันทึกผล" → Result Modal:**
```
┌─ บันทึกผลการตรวจสอบ · RBL103 ───────────────────┐
│                                                    │
│  Machine:    RBL103 · TAHARA · NBKPMY1000          │
│  Setup by:   Tech1                                 │
│  ⏱️ Duration: 00:12:34                             │
│                                                    │
│  ผลการตรวจสอบ *                                    │
│  ┌─────────────────┐  ┌──────────────────┐        │
│  │ ✅ อนุมัติ       │  │ ❌ ไม่อนุมัติ    │        │
│  │  (APPROVE)      │  │  (REJECT)         │        │
│  └─────────────────┘  └──────────────────┘        │
│                                                    │
│  📝 เหตุผล / รายละเอียด *                          │
│  ┌────────────────────────────────────────────────┐│
│  │ [multiline · required if reject · optional if  ]││
│  │  approve · max 1000 chars]                     ]││
│  └────────────────────────────────────────────────┘│
│                                                    │
│  🏷️ Reject reason category (if reject)             │
│  [Mold alignment ▾] [Dimension out of spec ▾]     │
│  [Surface defect ▾] [Other ▾]                     │
│                                                    │
│  📷 แนบรูปประกอบ (0-5)                             │
│  [📸 Camera] [🖼️ Gallery]                          │
│  [thumbnails]                                      │
│                                                    │
│              [ยกเลิก]  [✓ บันทึกและส่งกลับ Tech]  │
└────────────────────────────────────────────────────┘
```

**Rules:**
- ผลการตรวจสอบ · required (radio · default none)
- เหตุผล · required if REJECT · optional if APPROVE
- Reject reason category · dropdown · required if REJECT (predefined list · admin manageable)
- Photos · 0-5 · reuse PhotoAttachmentField (T-01)
- Submit → PATCH `/api/qa/verification/{id}/complete`
  - Stop timer · save result · save photos
  - Broadcast WebSocket to Tech

---

### 4️⃣ Tech Side · Receive QA Result

**Notification popup (small · non-blocking):**
```
┌─ QA Verification Result ────────────────────┐
│  RBL103 · NBKPMY1000                        │
│  ✅ APPROVED by QA1 (12:34 min)             │
│  → คุณสามารถเริ่มผลิตต่อได้                 │
│                            [OK] [ดูรายละเอียด]│
└─────────────────────────────────────────────┘
```

**If REJECTED:**
```
┌─ QA Verification Result ────────────────────┐
│  RBL103 · NBKPMY1000                        │
│  ❌ REJECTED by QA1 (12:34 min)             │
│                                             │
│  เหตุผล: Mold alignment ผิด                 │
│  Detail: ระยะ 0.3mm เกิน spec · redo align  │
│  📷 Photos (2) [thumbnail] [thumbnail]      │
│                                             │
│  → กรุณาแก้ไข Setup + ขอ QA verify ใหม่     │
│                            [OK] [ปรับปรุง]  │
└─────────────────────────────────────────────┘
```

**Behavior:**
- APPROVED → unlock "SETUP เสร็จสิ้น" button · Tech ปิด setup ได้
- REJECTED → Tech ต้อง add new activity เพื่อแก้ไข · Q activity ทำใหม่
- "ปรับปรุง" button → open Setup modal directly

---

## 🗄️ Data Model Changes

### Table: `qa_verification` (NEW)

```sql
CREATE TABLE qa_verification (
  verification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  
  -- Link to Setup TimeLog (the "Q" activity)
  setup_time_log_id BIGINT NOT NULL,     -- FK to setup_time_log
  machine_id VARCHAR(20) NOT NULL,
  sub_lot_id BIGINT NULL,                -- FK to sub_lot
  product_code VARCHAR(50) NULL,
  
  -- Requester (Tech)
  requester_id VARCHAR(50) NOT NULL,     -- Tech username
  request_note VARCHAR(1000) NULL,       -- Optional message from Tech
  request_photo_ids JSON NULL,           -- Photos from Tech side (reuse attachment_photo)
  
  -- QA Response
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / ACCEPTED / APPROVED / REJECTED / CANCELLED
  assigned_qa VARCHAR(50) NULL,          -- QA username after accept
  
  -- Timing (measure QA response + verification time)
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  accepted_at TIMESTAMP NULL,            -- When QA accepts
  completed_at TIMESTAMP NULL,           -- When QA submits result
  duration_seconds INT NULL,             -- accepted_at → completed_at
  
  -- Result
  result VARCHAR(20) NULL,               -- APPROVE / REJECT
  qa_note VARCHAR(1000) NULL,            -- QA reason/detail
  reject_reason_category VARCHAR(50) NULL,  -- If reject: MOLD_ALIGN / DIMENSION / SURFACE / OTHER
  qa_photo_ids JSON NULL,                -- Photos from QA (reuse attachment_photo)
  
  CONSTRAINT fk_qv_stl FOREIGN KEY (setup_time_log_id) REFERENCES setup_time_log(id),
  CONSTRAINT chk_qv_status CHECK (status IN ('PENDING','ACCEPTED','APPROVED','REJECTED','CANCELLED')),
  CONSTRAINT chk_qv_result CHECK (result IS NULL OR result IN ('APPROVE','REJECT')),
  INDEX idx_qv_status (status),
  INDEX idx_qv_machine (machine_id, status),
  INDEX idx_qv_assigned (assigned_qa, status),
  INDEX idx_qv_requested (requested_at)
);
```

### Table: `qa_reject_reason_master` (NEW)

Admin-managed dropdown for reject categories:
```sql
CREATE TABLE qa_reject_reason_master (
  code VARCHAR(50) PRIMARY KEY,
  name_th VARCHAR(200) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  category VARCHAR(50) NOT NULL,         -- e.g. 'SETUP_QUALITY'
  sort_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE
);

-- Seed data
INSERT INTO qa_reject_reason_master (code, name_th, name_en, category, sort_order) VALUES
  ('MOLD_ALIGN',   'Mold alignment ผิด',       'Mold alignment incorrect',    'SETUP_QUALITY', 1),
  ('DIMENSION',    'Dimension out of spec',    'Dimension out of specification','SETUP_QUALITY', 2),
  ('SURFACE',      'Surface defect',           'Surface defect',              'SETUP_QUALITY', 3),
  ('COLOR',        'สีไม่ตรง',                 'Color mismatch',              'SETUP_QUALITY', 4),
  ('WEIGHT',       'น้ำหนักไม่ได้',            'Weight not in tolerance',     'SETUP_QUALITY', 5),
  ('LABEL',        'Label/marking ผิด',        'Label/marking incorrect',     'SETUP_QUALITY', 6),
  ('OTHER',        'อื่นๆ (ระบุใน note)',      'Other (specify in note)',     'SETUP_QUALITY', 99);
```

### Modify `activity_code_master` (or seed data)

Add Q activity code:
```sql
INSERT INTO activity_code_master (code, name_th, name_en, description, category) VALUES
  ('Q', 'ขอ QA ตรวจสอบ', 'Request QA Verification', 
   'Auto-triggers QA verification workflow after setup complete', 'QA_CHECK');
```

### Add `QA_VERIFICATION` to attachment_photo (from T-01)

```sql
-- Update constraint to allow QA_VERIFICATION ref_type
ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type 
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST', 'QA_VERIFICATION'));
```

---

## 🔌 API Changes

### New REST Endpoints

**POST `/api/qa/verification`** (called from Tech · when Q activity ends)
```json
{
  "setupTimeLogId": 12345,
  "machineId": "RBL103",
  "subLotId": 6789,
  "productCode": "NBKPMY1000",
  "requesterId": "tech1",
  "requestNote": "เปลี่ยน mold ใหม่ · adjust temp · ready check",
  "requestPhotoIds": [123, 124]
}
```
Returns: `{ verificationId, status: 'PENDING' }` · triggers WebSocket to QA

**POST `/api/qa/verification/{id}/accept`**
```json
{ "qaId": "qa1" }
```
Returns: `{ status: 'ACCEPTED', acceptedAt: "..." }` · Timer starts

**POST `/api/qa/verification/{id}/complete`**
```json
{
  "result": "APPROVE",              // or "REJECT"
  "qaNote": "OK · first article ตรงตาม spec",
  "rejectReasonCategory": null,     // required if REJECT
  "qaPhotoIds": [125, 126]
}
```
Returns: `{ verificationId, duration_seconds: 754 }` · triggers WebSocket to Tech · unlock/lock setup completion

**POST `/api/qa/verification/{id}/cancel`** (edge case)
```json
{ "actorId": "tech1", "reason": "Setup ยกเลิก" }
```

**GET `/api/qa/verification/queue?qaId=X&status=PENDING,ACCEPTED`**
- Return QA's queue

**GET `/api/qa/verification/{id}`**
- Full detail · photos · timing · used in Tech-side result popup

**GET `/api/qa/stats?qaId=X&period=today`**
- Return: total, avg accept time, avg verify duration, approve/reject ratio

**GET `/api/qa/reject-reasons?category=SETUP_QUALITY&active=true`**
- Return list of active reject reasons for dropdown

### WebSocket Events (reuse T-03 infra)

**Broadcast to QA users:**
- `QA_VERIFICATION_NEW` — new request
- `QA_VERIFICATION_UPDATE` — snooze/cancel

**Broadcast to Tech users:**
- `QA_VERIFICATION_RESULT` — QA completed (approve/reject)

---

## 🖥️ Frontend Changes (React)

### Component tree:
```
src/components/QA/
  ├── QaDashboard.tsx              (NEW · QA landing page)
  ├── QaVerificationPopup.tsx      (NEW · alert popup · reuse T-03 pattern)
  ├── QaVerificationQueue.tsx      (NEW · panel · list pending/accepted)
  ├── ActiveVerificationCard.tsx   (NEW · post-accept card)
  ├── VerificationResultModal.tsx  (NEW · approve/reject + reason + photos)
  └── VerificationHistory.tsx      (NEW · history view)

src/components/Setup/
  └── SetupTimeLogModal.tsx        (MODIFY · handle Q activity · lock complete button)

src/components/shared/
  ├── PhotoAttachmentField.tsx     (existing · from T-01 · reuse)
  └── NotificationBell.tsx         (existing · from T-03 · extend for QA)

src/hooks/
  ├── useQaVerifications.ts        (NEW · realtime queue state)
  └── useSetupQaLock.ts            (NEW · Tech-side · lock/unlock complete based on QA status)
```

### Integration points:
- **Tech Setup Modal:** When Q activity ends → auto POST `/api/qa/verification` · disable "SETUP เสร็จสิ้น" button · show badge "⏳ รอ QA"
- **QA Dashboard:** landing page shows queue + stats · bell + popup for realtime
- **Global provider:** WebSocket subscribes to `QA_VERIFICATION_*` events

---

## ✅ Acceptance Criteria

### Tech Side
- [ ] Activity code "Q" มี dropdown · label "ขอ QA ตรวจสอบ"
- [ ] เลือก Q + start timer → เป็น normal activity
- [ ] เมื่อ Q ends → auto-create qa_verification record (status PENDING)
- [ ] Setup Job แสดง badge `⏳ รอ QA ตรวจสอบ`
- [ ] "SETUP เสร็จสิ้น" button ถูก disable จนกว่า QA approve
- [ ] รับ notification realtime เมื่อ QA complete (APPROVE/REJECT)
- [ ] REJECTED → แสดง reason + photos · unlock "SETUP เสร็จสิ้น" ยัง · Tech ต้องทำ Q ใหม่
- [ ] APPROVED → unlock "SETUP เสร็จสิ้น" · แสดง result summary

### QA Side
- [ ] NotificationBell + badge count · realtime update
- [ ] Popup alert เมื่อ new request · sound + vibrate
- [ ] Accept → เริ่มจับเวลา · Active Card แสดง
- [ ] Snooze 5 min · re-notify
- [ ] Result Modal · APPROVE/REJECT radio · reason (required if REJECT) · category dropdown · photos
- [ ] Submit → stop timer · save duration · notify Tech
- [ ] Queue Panel · filter · stats · history view

### Data
- [ ] `qa_verification` table with all timing fields
- [ ] `qa_reject_reason_master` seeded with default reasons
- [ ] Activity code "Q" in `activity_code_master`
- [ ] `attachment_photo` constraint updated to include `QA_VERIFICATION`
- [ ] Photos accessible via signed URL

### Real-time / WebSocket
- [ ] Message delivered within 3 seconds
- [ ] Auto-reconnect on disconnect
- [ ] Tech + QA receive correct-role messages

### Testing
- [ ] Unit test: qa_verification service (accept · complete · cancel)
- [ ] Integration test: Tech Q activity → QA notification → QA accept → complete → Tech notification
- [ ] E2E test: full flow with photos
- [ ] Test บน Samsung Tab Active 5 Pro · both Tech and QA screens
- [ ] Test edge case: QA declines · Tech reject scenario · redo cycle

---

## 🚀 Migration Plan

**V24:** New Flyway migration
```sql
-- V24__qa_setup_verification.sql

CREATE TABLE qa_verification (
  verification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  setup_time_log_id BIGINT NOT NULL,
  machine_id VARCHAR(20) NOT NULL,
  sub_lot_id BIGINT NULL,
  product_code VARCHAR(50) NULL,
  requester_id VARCHAR(50) NOT NULL,
  request_note VARCHAR(1000) NULL,
  request_photo_ids JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  assigned_qa VARCHAR(50) NULL,
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  accepted_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  duration_seconds INT NULL,
  result VARCHAR(20) NULL,
  qa_note VARCHAR(1000) NULL,
  reject_reason_category VARCHAR(50) NULL,
  qa_photo_ids JSON NULL,
  CONSTRAINT fk_qv_stl FOREIGN KEY (setup_time_log_id) REFERENCES setup_time_log(id),
  CONSTRAINT chk_qv_status CHECK (status IN ('PENDING','ACCEPTED','APPROVED','REJECTED','CANCELLED')),
  CONSTRAINT chk_qv_result CHECK (result IS NULL OR result IN ('APPROVE','REJECT'))
);
CREATE INDEX idx_qv_status ON qa_verification(status);
CREATE INDEX idx_qv_machine ON qa_verification(machine_id, status);
CREATE INDEX idx_qv_assigned ON qa_verification(assigned_qa, status);
CREATE INDEX idx_qv_requested ON qa_verification(requested_at);

CREATE TABLE qa_reject_reason_master (
  code VARCHAR(50) PRIMARY KEY,
  name_th VARCHAR(200) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  category VARCHAR(50) NOT NULL,
  sort_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO qa_reject_reason_master (code, name_th, name_en, category, sort_order) VALUES
  ('MOLD_ALIGN', 'Mold alignment ผิด', 'Mold alignment incorrect', 'SETUP_QUALITY', 1),
  ('DIMENSION', 'Dimension out of spec', 'Dimension out of specification', 'SETUP_QUALITY', 2),
  ('SURFACE', 'Surface defect', 'Surface defect', 'SETUP_QUALITY', 3),
  ('COLOR', 'สีไม่ตรง', 'Color mismatch', 'SETUP_QUALITY', 4),
  ('WEIGHT', 'น้ำหนักไม่ได้', 'Weight not in tolerance', 'SETUP_QUALITY', 5),
  ('LABEL', 'Label/marking ผิด', 'Label/marking incorrect', 'SETUP_QUALITY', 6),
  ('OTHER', 'อื่นๆ (ระบุใน note)', 'Other (specify in note)', 'SETUP_QUALITY', 99);

INSERT INTO activity_code_master (code, name_th, name_en, description, category) VALUES
  ('Q', 'ขอ QA ตรวจสอบ', 'Request QA Verification',
   'Auto-triggers QA verification workflow after setup', 'QA_CHECK');

ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST', 'QA_VERIFICATION'));
```

---

## 🔗 Related Issues

- **T-01** · Setup Activity + Photo · `Q` is a new activity code · photos reuse `attachment_photo`
- **T-03** · WebSocket notification infrastructure · reuse for QA popup
- **O-10** · Similar notification+response pattern (Tech help request · shares WebSocket infra)
- **DA-01** · Master data with photos · similar admin UI pattern (for qa_reject_reason_master)

---

## 📝 Notes / Open Questions

- [ ] Auto-trigger vs Manual: Q activity code = auto-trigger · หรือมี button "🔔 ขอ QA verify" แยก?
- [ ] Concurrent Q requests: หลาย machines · หลาย Tech · QA อาจ overload · assign strategy?
- [ ] QA offline: fallback ยังไง (Telegram? · escalate to QA supervisor?)
- [ ] Reject → Redo cycle: track how many round-trips · KPI Tech quality
- [ ] Time SLA: target QA response < 5 min · verification < 30 min?
- [ ] Multi-QA: assign to specific QA on shift · หรือ broadcast?
- [ ] Approve without inspecting: prevent QA "rubber-stamp" · require min duration (e.g. > 2 min)?
- [ ] Reject reason master · admin manage · เชื่อม DA-01?

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V24 migration + seed data | 0.5 day |
| Backend · QaVerification entity + Repo + Service | 1 day |
| Backend · REST endpoints (create, accept, complete, cancel, queue) | 1.5 days |
| Backend · WebSocket broadcast integration (reuse T-03 infra) | 0.5 day |
| Backend · Auto-trigger logic on Q activity end (hook Setup service) | 0.5 day |
| Backend · Reject reason master admin CRUD | 0.5 day |
| Frontend · QaDashboard + Queue panel | 1 day |
| Frontend · VerificationResultModal (radio + reason + photos) | 1 day |
| Frontend · ActiveVerificationCard + timer | 0.5 day |
| Frontend · Tech-side Q activity handling + lock/unlock | 0.75 day |
| Frontend · Tech notification popup (result) | 0.5 day |
| Testing + integration | 1.25 days |
| **Total** | **~8.5 days (L)** |

---

## 🎯 Design Decision Log

**Q: Q activity code = auto-trigger · หรือ manual button?**
A: **Auto-trigger** เพราะ:
- Simpler for Tech · ไม่มีขั้นตอนพลาด
- Uniform: Q code = QA verify · ชัดเจน
- Manual button add clutter to UI

**Q: Reject reason free-text · หรือ dropdown?**
A: **Both** · dropdown category + free-text detail
- Category: enforce structure · reporting/analytics
- Detail: specifics · context

**Q: หลัง Reject · Tech ทำอะไรต่อ?**
A: **Add new activity to fix** · then Q activity ใหม่
- Setup timelog เก็บ history ทุก round
- Track "Setup redo count" · KPI Tech quality

**Q: QA เห็น Setup detail ก่อน Accept ไหม?**
A: **Yes** · popup แสดง full detail (machine · Tech · SubLot · request note · photos)
- QA เตรียมตัวก่อนไปหน้างาน
- Decision: accept หรือ ให้ QA อื่น (via snooze/decline)
