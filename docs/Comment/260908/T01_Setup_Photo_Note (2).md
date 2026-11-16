# T-01 · Photo + Note Attachment · Setup TimeLog + Machine Downtime

**Role:** Technician
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M-L (Medium-Large · ~7-9 dev days · covers 2 screens)
**Source:** Pilot Feedback 8 Sep 2026 · Comment sheet R2 + follow-up

---

## 📋 Summary · สรุป

เพิ่ม feature **แนบรูปจากกล้องหรือ gallery** ให้กับ 2 หน้าจอที่ Technician ใช้:

### Screen 1 · Setup TimeLog (งาน Setup เครื่องจักร)
1. **แนบรูปได้ตอนเริ่ม Activity** — จากกล้อง หรือ gallery
2. **ปุ่มหยุด Activity เด่นชัดขึ้น** — ปัจจุบันเป็นไอคอนแดงเล็ก
3. **แนบรูป + หมายเหตุตอนจบ Activity** — เพิ่ม form after stop

### Screen 2 · Machine Downtime Recording (บันทึกเครื่องจักรหยุดการผลิต) 🆕
Menu path: `บันทึกข้อมูลการผลิต → machine → บันทึกเครื่องจักรหยุดการผลิต`
1. **แนบรูปได้ตอนบันทึก Downtime start** — จากกล้อง หรือ gallery
2. **แนบรูป + หมายเหตุตอนสิ้นสุด Downtime** — same pattern เหมือน Setup

**หลักการ:** ใช้ **PhotoAttachmentField component เดียวกัน** ทั้ง 2 หน้าจอ · ลด duplicate code

---

## 🎯 User Stories

### Story 1 · Setup TimeLog
**As a Technician,**
เมื่อทำ Setup เครื่องจักร (เช่น เปลี่ยนแม่พิมพ์ · Change Mold),
I want to:
- แนบรูปสภาพเครื่องก่อนเริ่ม (เช่น mold เก่า/ใหม่ · sticker · label)
- เห็นปุ่มหยุด Activity ชัดเจน · ไม่ต้องเพ่งหา
- บันทึกหมายเหตุ + แนบรูปตอนจบ Activity (เช่น mold ใหม่หลังติดตั้ง · ปัญหาที่เจอ)

### Story 2 · Machine Downtime
**As a Technician,**
เมื่อบันทึกเครื่องจักรหยุดการผลิต (Machine Downtime),
I want to:
- แนบรูปสภาพเครื่อง / ป้าย alarm code / จุดที่เสีย ตอนเริ่มบันทึก
- บันทึกหมายเหตุ + แนบรูปตอนสิ้นสุด Downtime (สภาพหลังซ่อม · เอกสาร maintenance)

**So that (both):**
- มีหลักฐานการทำงาน · ตรวจสอบย้อนหลังได้
- ลด error การกดปุ่มพลาด
- Documentation ครบถ้วน · QA + Maintenance manager มั่นใจได้
- Support root-cause analysis หา pattern การหยุดเครื่อง

---

## 📸 Current State (Reference)

**Screen: `งาน Setup เครื่องจักร`** (Machine Setup Work)
URL: `https://10.1.53.30/MES/` (Tech1 login · Technician role)

### 3-step existing flow:

**Step 1 · Job List**
- แสดง Setup jobs ที่รอ (RBL103, RBL102, RBL104, RBL202)
- Status: ท่าอยู่ (In Progress) · เลยกำหนด (Overdue)
- Actions: `บันทึก TIMELOG` · `ข้าม` · `เริ่ม SETUP`

**Step 2 · Add Activity Modal (Timer NOT started)**
- Header: RBL103 · TAHARA · NBKPMY1000
- Input: Activity Code (dropdown) · หมายเหตุ (optional text) · `เริ่มจับเวลา` button
- Bottom: `หมายเหตุ Setup (รวม)` · `SETUP เสร็จสิ้น` button

**Step 3 · Timer Running**
- Shows: `กำลังทำ 1 Activity` badge · `จบทั้งหมด` button
- Activity row: `M เปลี่ยนแม่พิมพ์ · เริ่ม 08:17` + small red stop button (icon)
- Bottom: `แผนภูมิเวลา (Gantt)` · `หมายเหตุ Setup (รวม)` · `SETUP เสร็จสิ้น` (disabled)

---

## 🎨 UI/UX Requirements · New Features

### 1️⃣ Photo Attachment on Activity Start

**Location:** Modal Step 2 (before pressing `เริ่มจับเวลา`)

**UI Design:**
```
┌─ เพิ่ม Activity ──────────────────────────────────┐
│ Activity Code *          หมายเหตุ (ถ้ามี)          │
│ [M — เปลี่ยนแม่พิมพ์ ▾]  [___________________]     │
│                                                    │
│ 📷 แนบรูป (ถ้ามี)                                  │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│ │  📸 Camera│ │ 🖼️ Gallery│ │ ➕ Add   │            │
│ └──────────┘ └──────────┘ └──────────┘            │
│                                                    │
│ [Thumbnail preview if photo attached · x to remove]│
│                                                    │
│                              [▶ เริ่มจับเวลา]      │
└────────────────────────────────────────────────────┘
```

**Behavior:**
- Camera button → open native camera (mobile: capture photo)
- Gallery button → open file picker (accept: `image/*` · multiple: true)
- Max 3 photos per activity start · max 5MB each
- Preview: thumbnails 80×80px with × button to remove
- Photos uploaded to backend BEFORE timer starts (async · show upload progress)

---

### 2️⃣ Prominent Stop Button on Active Activity

**Current problem:** ปุ่มหยุดเป็น icon สี่เหลี่ยมแดงเล็กมาก + button `จบ Activity นี้` แสดงเมื่อ hover เท่านั้น

**New Design:**
```
┌─ Active Activity Row ──────────────────────────────┐
│ 🔴 M · เปลี่ยนแม่พิมพ์                              │
│    เริ่ม 08:17  |  ⏱️ 00:12:34 (running)          │
│                                                    │
│                          [🛑 จบ Activity นี้] ←ใหญ่│
└────────────────────────────────────────────────────┘
```

**Spec:**
- Button size: **min 48×160px** (industrial tablet · glove-friendly)
- Color: `#EF4444` (red) background · white text · `bold 16px`
- Icon: 🛑 (stop) หรือ ⏹️ (square)
- Text: "จบ Activity นี้" · always visible (no hover-only)
- Position: right-aligned in the activity row
- Show elapsed time real-time (mm:ss format)

**Delete button (trash icon):**
- Keep as-is (small · at far right) · but move away from Stop button
- Add confirm dialog: "ลบ Activity นี้? · ข้อมูลจะหายทั้งหมด"

---

### 3️⃣ Note + Photo Modal on Activity Stop

**Trigger:** After clicking `🛑 จบ Activity นี้`

**UI Design:**
```
┌─ จบ Activity: M · เปลี่ยนแม่พิมพ์ ───────────────┐
│                                                    │
│ เริ่ม: 08:17 | จบ: 08:29 | ระยะเวลา: 12:34         │
│                                                    │
│ 📝 หมายเหตุ / รายละเอียด                            │
│ ┌────────────────────────────────────────────────┐│
│ │ [multiline textarea · 3 rows · placeholder:    ]││
│ │ [ปัญหา · การแก้ไข · เครื่องมือที่ใช้]           ]││
│ └────────────────────────────────────────────────┘│
│                                                    │
│ 📷 แนบรูปประกอบ (ถ้ามี)                             │
│ ┌──────────┐ ┌──────────┐                          │
│ │ 📸 Camera│ │ 🖼️ Gallery│                          │
│ └──────────┘ └──────────┘                          │
│                                                    │
│ [Thumbnails]                                       │
│                                                    │
│              [ยกเลิก]  [✓ บันทึกและจบ]              │
└────────────────────────────────────────────────────┘
```

**Behavior:**
- Modal opens immediately after Stop pressed
- Note field: optional · max 500 chars
- Photo: max 5 photos · max 5MB each
- `บันทึกและจบ` button:
  - Uploads photos (if any)
  - PATCH activity record with `stop_time`, `note`, `photo_ids[]`
  - Close modal · return to Activity list
- `ยกเลิก` button: keeps activity stopped (already stopped when this modal opened) · just closes modal without note/photo

**Alternative:** Auto-open modal · OR give option "จบทันที" (stop without note) — configure per user preference?

---

## 🗄️ Data Model Changes

### Existing Tables (add columns)

**Table: `setup_time_log` (V21 · existing)**
```sql
ALTER TABLE setup_time_log
  ADD COLUMN start_note VARCHAR(500) NULL,
  ADD COLUMN stop_note VARCHAR(500) NULL,
  ADD COLUMN start_photo_ids JSON NULL,
  ADD COLUMN stop_photo_ids JSON NULL;
```

**Table: `machine_downtime_log` (existing · check exact name)**
```sql
ALTER TABLE machine_downtime_log
  ADD COLUMN start_note VARCHAR(500) NULL,
  ADD COLUMN stop_note VARCHAR(500) NULL,
  ADD COLUMN start_photo_ids JSON NULL,
  ADD COLUMN stop_photo_ids JSON NULL;
```
*Note: หากชื่อ table ต่างจากนี้ให้ปรับตาม (อาจเป็น `production_downtime`, `machine_stop_log`, ฯลฯ)*

### Table: `attachment_photo` (NEW · shared for multiple entities)

Design: **ใช้ table เดียวสำหรับรูปแนบทุกประเภท** · polymorphic association

```sql
CREATE TABLE attachment_photo (
  photo_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ref_type VARCHAR(30) NOT NULL,         -- 'SETUP_TIME_LOG' | 'MACHINE_DOWNTIME'
  ref_id BIGINT NOT NULL,                -- FK to owning record
  attach_phase VARCHAR(10) NOT NULL,     -- 'START' | 'STOP'
  file_url VARCHAR(500) NOT NULL,        -- S3 or cloud storage URL
  file_name VARCHAR(255) NOT NULL,
  file_size_kb INT NOT NULL,
  mime_type VARCHAR(50) NOT NULL,
  uploaded_by VARCHAR(50) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_ref_type CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME')),
  CONSTRAINT chk_attach_phase CHECK (attach_phase IN ('START', 'STOP'))
);
CREATE INDEX idx_photo_ref ON attachment_photo(ref_type, ref_id);
CREATE INDEX idx_photo_upload_time ON attachment_photo(uploaded_at);
```

**Advantages ของ shared table:**
- ✅ ใช้ component เดียว · storage service เดียว
- ✅ Scale ง่าย · future: NG photos, Setup approval photos ก็ใช้ table นี้ได้
- ✅ Query stats: photos per user/machine/type ทำได้ที่เดียว

**Storage plan:**
- Local dev: `/var/gdtahara/uploads/{ref_type_lower}/{yyyy}/{mm}/{ref_id}/{photo_uuid}.jpg`
- Prod: S3 bucket `gdtahara-attachments` · path: same structure
- Serve via signed URL (expire 1 hour) · not direct public link

---

## 🔌 API Changes

### Existing endpoints (modify):

**POST `/api/setup/{setupJobId}/activity/start`**
```json
{
  "activityCode": "M",
  "startNote": "mold เก่า NBKPMY1000 · เตรียมเปลี่ยน",
  "startPhotoIds": [123, 124]  // photo_ids from upload endpoint
}
```

**PATCH `/api/setup/activity/{activityId}/stop`**
```json
{
  "stopNote": "ติดตั้งเรียบร้อย · ทดสอบ mold ใหม่ผ่าน",
  "stopPhotoIds": [125, 126, 127]
}
```

### New endpoints (shared for both Setup + Downtime):

**POST `/api/attachment/photo/upload`**
- Multipart form · fields: `file`, `refType`, `refId` (optional · can attach later)
- Returns: `{ "photoId": 123, "url": "https://..." }`
- Validate: mime type (jpg/png/webp) · size ≤ 5MB
- Compress: resize to max 1920px · quality 85%
- `refType`: `SETUP_TIME_LOG` | `MACHINE_DOWNTIME`

**DELETE `/api/attachment/photo/{photoId}`**
- Delete photo (only if owning record not yet committed/locked)
- Return 204

**GET `/api/attachment/photos?refType=X&refId=Y&phase=Z`**
- Return list of photos with signed URLs
- Query params: `refType`, `refId`, optional `phase` (START/STOP)

### Machine Downtime endpoints (modify existing):

**POST `/api/downtime/start`**
```json
{
  "machineId": "RBL103",
  "downtimeCategory": "MAINTENANCE",
  "downtimeCode": "M02",
  "startNote": "hydraulic pump ผิดปกติ · alarm A045",
  "startPhotoIds": [123, 124]
}
```

**PATCH `/api/downtime/{downtimeId}/stop`**
```json
{
  "stopNote": "เปลี่ยน seal ใหม่ · test ผ่าน",
  "stopPhotoIds": [125, 126]
}
```

---

## 🖥️ Frontend Changes (React)

### Component tree:
```
src/components/shared/
  ├── PhotoAttachmentField.tsx   (NEW · reusable · shared 2 screens)
  └── PhotoStopModal.tsx         (NEW · reusable · note+photo on stop)

src/components/Setup/
  ├── SetupJobList.tsx           (existing · no change)
  ├── SetupTimeLogModal.tsx      (existing · modify · use PhotoAttachmentField)
  └── ActivityRow.tsx            (existing · modify · prominent Stop button + PhotoStopModal)

src/components/Downtime/
  ├── DowntimeStartModal.tsx     (existing · modify · use PhotoAttachmentField)
  └── DowntimeStopModal.tsx      (existing · modify · use PhotoStopModal)
```

### Key libraries:
- `react-webcam` — for camera capture on mobile browsers
- `browser-image-compression` — client-side resize before upload
- Existing UI kit (Material-UI · per README)

### Files to modify:
**Setup screens:**
- `SetupTimeLogModal.tsx` · add `<PhotoAttachmentField>` before start button
- `ActivityRow.tsx` · replace icon stop with big button + PhotoStopModal

**Downtime screens (บันทึกข้อมูลการผลิต → machine → บันทึกเครื่องจักรหยุดการผลิต):**
- `DowntimeStartModal.tsx` · add `<PhotoAttachmentField>` before start button
- `DowntimeStopModal.tsx` · integrate `<PhotoStopModal>` for note + photo

**Shared components (NEW):**
- `PhotoAttachmentField.tsx` · camera + gallery picker · thumbnails · reusable
- `PhotoStopModal.tsx` · duration display + note + photos on stop

### PhotoAttachmentField API (reusable):
```tsx
<PhotoAttachmentField
  refType="SETUP_TIME_LOG"          // or "MACHINE_DOWNTIME"
  refId={activityId}                 // owning record ID (optional at start)
  phase="START"                      // or "STOP"
  maxPhotos={3}                      // default 3 for start, 5 for stop
  onChange={(photoIds) => {...}}
  disabled={false}
/>
```

---

## ✅ Acceptance Criteria

### Setup TimeLog Screen

**Photo on Start:**
- [ ] Tech สามารถแนบรูป 0-3 ใบจาก camera หรือ gallery ตอน start Activity
- [ ] Preview thumbnail แสดง · ลบได้ก่อน start
- [ ] Photos upload สำเร็จ · linked กับ activity record
- [ ] Photos ดูได้ตอน review Setup history

**Stop Button:**
- [ ] ปุ่ม `🛑 จบ Activity นี้` แสดงตลอด (ไม่ต้อง hover) · ขนาด ≥ 48×160px
- [ ] สี red-500 · text bold 16px · glove-friendly
- [ ] แสดง elapsed time realtime (mm:ss)

**Note + Photo on Stop:**
- [ ] Modal เปิดหลัง Stop · แสดง start-stop time + duration
- [ ] หมายเหตุ optional · max 500 chars
- [ ] แนบรูป 0-5 ใบ
- [ ] `บันทึกและจบ` → PATCH activity · save note + photos
- [ ] `ยกเลิก` → activity ยัง stopped · แค่ไม่มี note/photo

### Machine Downtime Screen 🆕

**Photo on Downtime Start:**
- [ ] Tech สามารถแนบรูป 0-3 ใบตอนเริ่มบันทึก Downtime
- [ ] Photos linked กับ downtime record via `attachment_photo` (ref_type=MACHINE_DOWNTIME)
- [ ] แสดงร่วมกับ alarm code / downtime category ใน history

**Note + Photo on Downtime Stop:**
- [ ] Modal เปิดหลัง Stop · แสดง duration + downtime category
- [ ] หมายเหตุ · max 500 chars · optional
- [ ] แนบรูป 0-5 ใบ (photo หลังซ่อม · maintenance report)
- [ ] Save → PATCH downtime · update note + stop_photo_ids

### Shared

**Data:**
- [ ] `setup_time_log` และ `machine_downtime_log` มี columns ใหม่: start_note, stop_note, start_photo_ids, stop_photo_ids
- [ ] `attachment_photo` table สร้าง · shared สำหรับ 2 ref_type · index ครบ
- [ ] Photos เก็บที่ storage · แสดงผ่าน signed URL

**Testing:**
- [ ] Unit test: PhotoAttachmentField component (reusable · test ทั้ง 2 refType)
- [ ] Integration test: upload → attach → activity/downtime save
- [ ] E2E test: full flow ทั้ง 2 screens (start with photo → stop with note+photo)
- [ ] Test บน Samsung Tab Active 5 Pro (10.1") · ใส่ถุงมือ · กด Stop button ติดทั้ง 2 หน้าจอ

---

## 🚀 Migration Plan

**V22:** New Flyway migration · covers both Setup + Downtime
```sql
-- V22__attachment_photo_and_notes.sql

-- 1. Setup TimeLog · add note + photo IDs columns
ALTER TABLE setup_time_log
  ADD COLUMN start_note VARCHAR(500) NULL,
  ADD COLUMN stop_note VARCHAR(500) NULL,
  ADD COLUMN start_photo_ids JSON NULL,
  ADD COLUMN stop_photo_ids JSON NULL;

-- 2. Machine Downtime Log · add same columns
-- Note: adjust table name if actual name differs (production_downtime · machine_stop_log · ฯลฯ)
ALTER TABLE machine_downtime_log
  ADD COLUMN start_note VARCHAR(500) NULL,
  ADD COLUMN stop_note VARCHAR(500) NULL,
  ADD COLUMN start_photo_ids JSON NULL,
  ADD COLUMN stop_photo_ids JSON NULL;

-- 3. Shared attachment_photo table (polymorphic)
CREATE TABLE attachment_photo (
  photo_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ref_type VARCHAR(30) NOT NULL,
  ref_id BIGINT NOT NULL,
  attach_phase VARCHAR(10) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_size_kb INT NOT NULL,
  mime_type VARCHAR(50) NOT NULL,
  uploaded_by VARCHAR(50) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_ref_type CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME')),
  CONSTRAINT chk_attach_phase CHECK (attach_phase IN ('START', 'STOP'))
);
CREATE INDEX idx_photo_ref ON attachment_photo(ref_type, ref_id);
CREATE INDEX idx_photo_upload_time ON attachment_photo(uploaded_at);
```

---

## 📝 Notes / Open Questions

- [ ] Confirm max photo count per activity: 3 (start) + 5 (stop) OK?
- [ ] Compression: resize to 1920px OK? (or need bigger for detail work?)
- [ ] Auto-open Stop Modal · or configurable per user?
- [ ] Photo retention policy? (e.g., delete after 6 months? · keep forever?)
- [ ] Storage cost estimate: ~50KB compressed × 5 photos × 30 setups/day × 15 machines = ~112 MB/day ≈ 41 GB/year

---

## 🎯 Effort Estimate (Updated · covers 2 screens)

| Task | Effort |
|---|---|
| Flyway V22 migration (Setup + Downtime + attachment_photo) | 0.5 day |
| Backend · AttachmentPhoto entity + Repo + Service (shared) | 1 day |
| Backend · Upload endpoint + storage config | 1 day |
| Backend · Update setup activity start/stop APIs | 0.5 day |
| **Backend · Update downtime start/stop APIs** 🆕 | 0.5 day |
| Frontend · PhotoAttachmentField component (shared reusable) | 1 day |
| Frontend · PhotoStopModal component (shared reusable) | 0.5 day |
| Frontend · Update SetupTimeLogModal + ActivityRow | 1 day |
| Frontend · Prominent Stop button redesign (Setup) | 0.25 day |
| **Frontend · Update DowntimeStartModal + DowntimeStopModal** 🆕 | 1 day |
| Testing + integration (both screens) | 1.5 days |
| **Total** | **~8.75 days (M-L)** |

**เพิ่มจาก scope เดิม +2 days** สำหรับ Downtime screen (backend + frontend + test)

---

## 🔗 Related Issues

- **T-02** · Setup detail ยืนยันกับการทำงานจริง (Process alignment) · may affect activity code list
- **Q-01** · QA workflow after Tech setup (uses same setup_time_log data + Setup Modal integration)
- **T-03** · Tech notification system (WebSocket infra used for QA result notification)
- **DA-01** · Master data with photo (similar photo pattern · reuse component)

---

## 🔀 Integration with Q-01 · QA Workflow (Tech-side touch points)

**Setup Modal (SetupTimeLogModal.tsx) มีการปรับปรุงเพิ่มเติม เพื่อรองรับ Q-01 workflow:**

### 1. Q Activity Awareness

- Activity Code dropdown มี option "Q — ขอ QA ตรวจสอบ" (from Q-01 seed data)
- เมื่อ Tech เลือก Q + `เริ่มจับเวลา` → Activity เริ่มปกติ (เหมือน M, A, C)
- เมื่อ Tech กด `จบ Activity นี้` บน Q activity:
  - Normal stop flow (with Photo+Note modal per T-01)
  - **PLUS:** ระบบ auto-call `POST /api/qa/verification` (Q-01) หลัง commit stop
  - Attach the same Photo+Note จาก stop modal ให้ QA เห็นเป็น "request context"

### 2. Lock "SETUP เสร็จสิ้น" Button

**Rule:** Setup Job มี pending QA verification → button `SETUP เสร็จสิ้น` = disabled

**UI:**
```
[SETUP เสร็จสิ้น]  → disabled
Tooltip: "⏳ รอผลการตรวจสอบจาก QA (Q-01)"
Badge below: "⏳ รอ QA ตรวจสอบ · ส่งเมื่อ 08:29 (2 นาทีที่แล้ว)"
```

**When unlocked:** 
- After QA APPROVE → button enabled + badge เปลี่ยนเป็น "✅ QA Approved"
- After QA REJECT → button ยัง disabled · badge "❌ QA Rejected · Redo Setup" + result popup

### 3. QA Result Notification Popup (Tech-side)

**Trigger:** WebSocket receive `QA_VERIFICATION_RESULT` event (broadcast from Q-01)

**Small non-blocking popup (top-right toast):**

**APPROVED:**
```
┌─ ✅ QA Approved ────────────────────────┐
│  RBL103 · NBKPMY1000                    │
│  QA1 · 12:34 min                        │
│  → คุณสามารถกด "SETUP เสร็จสิ้น" ได้    │
│                              [OK] [View]│
└─────────────────────────────────────────┘
```

**REJECTED:**
```
┌─ ❌ QA Rejected ────────────────────────┐
│  RBL103 · NBKPMY1000                    │
│  QA1 · 12:34 min                        │
│  เหตุผล: Mold alignment ผิด             │
│  Detail: ระยะ 0.3mm เกิน spec           │
│  📷 Photos (2) [thumb] [thumb]          │
│  → กรุณาแก้ไข + ขอ QA verify ใหม่       │
│                     [OK] [ปรับปรุง Setup]│
└─────────────────────────────────────────┘
```

**Behavior:**
- Sound alert (short beep · configurable)
- Non-blocking (Tech ทำงานอื่นต่อได้)
- "View" → open full QA result detail modal
- "ปรับปรุง Setup" → reopen Setup Modal สำหรับเครื่องนั้น · Tech สามารถ add new activity เพื่อแก้ไข · แล้วขอ Q ใหม่

### 4. Setup Job List Badge Update

**On Setup Job Card (SetupJobList.tsx):**

```
┌─ RBL103 · TAHARA ────────────────────────┐
│  วันที่: 2026-07-31                        │
│  ต้องเสร็จก่อน 07:00:00                    │
│                                            │
│  [🟢 IN_PROGRESS]  [⏳ รอ QA ตรวจสอบ]     │  ← เพิ่ม QA status badge
│                                            │
│  [บันทึก TIMELOG] [ข้าม]                   │
└────────────────────────────────────────────┘
```

**Badge states:**
- `⏳ รอ QA ตรวจสอบ` (PENDING) — orange
- `🔍 QA กำลังตรวจ` (ACCEPTED) — blue
- `✅ QA Approved` (APPROVED) — green · ปิด Setup ได้
- `❌ QA Rejected` (REJECTED) — red · ต้องทำใหม่

---

## 📊 Effort Adjustment · With Q-01 Integration

**เพิ่มจาก 8.75 → 9.75 days** (~+1 day):

| Additional Task (Q-01 integration) | Effort |
|---|---|
| Frontend · WebSocket listener for QA_VERIFICATION_RESULT | 0.25 day |
| Frontend · QA result popup (approved/rejected variants) | 0.5 day |
| Frontend · Lock/Unlock "SETUP เสร็จสิ้น" button + badge | 0.25 day |
| Frontend · Setup Job List badge update | 0.25 day |
| Backend · Hook auto-call QA verification on Q activity stop | (0.25 day - already in Q-01 scope) |
| Testing integration Tech ↔ QA | 0.25 day |
| **T-01 Total (Updated)** | **~9.75 days (L)** |
