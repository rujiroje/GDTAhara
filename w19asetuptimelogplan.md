# W19a · Setup TimeLog Enhancement — Development Plan

**Status**: ✅ **Design LOCKED · 2026-07-31** — 5 open questions answered · ready to implement
**Target**: Phase 1 hot-fix · **priority BEFORE Pallet (W19)**
**Owner**: Dev · with input from **Technician** (form usage) + PC (summary reporting) + QA (cavity check)
**References**:
- Reference form: `RBL102QP350A set2 setup.pdf` (physical form used today · handwritten)
- Replaces (or extends): existing `MachineSetupJob` + `setup_checklist_templates`
- Complements: W17.1 UAT + existing Technician panel

---

## 🔒 Locked-in Decisions (2026-07-31)

| # | Question | ✅ Decision |
|---|---|---|
| **Q1** | แทน Checklist template เดิม หรือ 2 modes? | **แทนของเดิม** — TimeLog เป็น mode เดียว · Checklist template ถูก deprecate · ข้อมูลเก่า read-only |
| **Q2** | Custom codes — Tech เพิ่มได้ไหม? | **เฉพาะที่กำหนดให้เท่านั้น** · ไม่มี "OTHER"/custom · Admin จัดการ master upfront · Tech เลือก dropdown ตายตัว |
| **Q3** | Photo compression? | **ลดขนาดก่อนเก็บ** · server-side resize max 1024px width + JPEG quality 75 · target < 300 KB / รูป |
| **Q4** | Photo storage location? | **On-premise Application Server** · `${APP_STORAGE_ROOT}/setup-photos/{yyyy}/{mm}/{setupJobId}/{uuid}.jpg` |
| **Q5** | Cavity check ใครทำ? | **⏭️ ข้าม / ไม่ทำใน W19a** — ปัจจุบันไม่มีการบันทึกจริง · เลื่อนไป Phase 2 |
| **Q6** | Overlap entries — บล็อกหรืออนุญาต? | **อนุญาต** · ไม่ warn ด้วย · setup มี parallel activities จริง (heating + QA พร้อมกัน) |

**Locked-in (2026-07-31 · รอบ 2-3)**:
- **Q8** · Gantt view → ✅ **YES ทำเลย** · แสดงไว้สรุปเพื่อวิเคราะห์งานต่อ · **ไม่ optional แล้ว**
- **Q9** · Auto-end IN_PROGRESS → ❌ **NO · User กด Complete เอง** ทุก entry เพื่อป้องกันการบันทึกล่าช้า
- **Q10** · Mold Code validation → ❌ **ไม่ทำ** · ยังไม่มี Mold master · defer Phase 2

**🎯 Status: 10/10 questions LOCKED · ready to implement**

- **Q7** · CT / ปริ footer → ⏭️ **SKIP · ข้อมูลไม่ได้บันทึกแยก field** (user 2026-07-31 · รอบ 3) · Tech จดกระดาษต่อไป · ไม่มี field ใน DB

---

## ⚠️ ผลกระทบจาก decisions

### จาก Q1 · แทน Checklist:
- **`setup_checklist_templates` + `setup_job_steps` tables → deprecate** (ยังคงอยู่ · แต่ไม่มี code ใหม่ใช้)
- Existing Setup Jobs ที่กรอก Checklist ไปแล้ว → **read-only historical view** · เห็นแต่แก้ไม่ได้
- ทีมต้องทำ **UAT retrain** ก่อน pilot · แจ้ง Tech ว่า UI เปลี่ยน

### จาก Q2 · Fixed codes only:
- **Admin ต้องเพิ่ม code `P` (Purge) + code อื่นที่ทีมใช้** ในระยะ config ก่อน pilot
- ควร**สัมภาษณ์ Tech senior** เอา codes ทั้งหมดมาก่อน (ไม่ใช่แค่ 10 จากฟอร์ม)
- ถ้า Tech เจอกิจกรรมที่ไม่มี code → **ต้องหยุดแจ้ง Admin** เพิ่ม code ก่อนบันทึกได้
- **ควรเพิ่ม field `is_active`** ให้ Admin ปิด/เปิด code (แทนที่จะลบ) · เก็บ audit
- **UI dropdown ต้องมี search** เพราะ code เยอะ (>15) จะหายาก

### จาก Q3 · Photo compression:
- Client อาจอัปโหลด 5 MB · server compress เหลือ ~200-300 KB
- ลด storage · ลด bandwidth · ลด load time
- **Trade-off**: รูปละเอียดน้อยลง (สำหรับ evidence ยังพอ · ไม่เหมาะ audit ระดับ pixel)

### จาก Q4 · On-premise storage:
- ต้องประเมิน **disk capacity ของ app server**
- คาดการณ์: ~20 Setup Jobs/day × ~5 photos/job × 250 KB = **~25 MB/day → ~9 GB/year**
- ต้อง **backup รวมกับ DB backup** (per Phase 2)
- **File path DB column** เก็บ **relative path** เท่านั้น (portable ถ้าย้าย server)

### จาก Q6 · Allow overlap:
- Validation `ck_stl_duration` เดิม (end > start) **ยังคงมี** · เฉพาะ overlap ระหว่าง entries ไม่บล็อก
- Gantt view (ถ้าทำ) จะเห็น overlap ชัด — stack vertically

### จาก Q5 · Skip Cavity check:
- **ลบ `setup_cavity_check` table ออกจาก V19** · ทั้ง service + controller + frontend grid + tests
- ประหยัด **~0.5 MD dev time** (จาก 3.5 → 3.0 MD)
- ในฟอร์มกระดาษ (top-right) ยังคงเขียนมือต่อได้ · จะดิจิไทซ์ใน Phase 2 พร้อม QA IPQC/FQC form
- ถ้าอนาคตต้องการ · schema พร้อมสร้างในภายหลัง (backward compatible)

### จาก Q8 · Gantt view = REQUIRED:
- Gantt view เดิม optional → **บังคับทำ** เพื่อให้ทีมใช้วิเคราะห์
- แสดง horizontal bars ต่อ code + สี · เห็น overlap ชัด · เห็นช่วงเวลาที่ setup ใช้แต่ละกิจกรรม
- **เพิ่ม 0.25 MD** เทียบกับ v2.0 · net dev = **3.25 MD**
- ควรมี **print-friendly CSS** เพื่อ export A4 landscape แนบรายงาน mgmt

### จาก Q9 · User กด Complete เอง (no auto-end):
- **Complete Setup Job** ถูก block ถ้ามี entry status = IN_PROGRESS ค้างอยู่
- Alert: "ยังมี N entries ที่ยังไม่ได้กด End · ต้อง end ก่อน complete"
- Tech ต้อง scroll ไปกด "END NOW" ทีละ entry (หรือมีปุ่ม "End All Now")
- **Prevent data loss** — บังคับให้ user รับผิดชอบเวลาจริง
- Trade-off: อาจมี friction ถ้า Tech ลืม → ตอน Complete จะเจอ block · แก้ง่าย (กด end all)

### จาก Q10 · No Mold Code validation:
- ลบ **warning "อย่าลืมกรอก Mold Code"** ที่เดิม recommend
- Mold master ยังไม่มี · ไม่มีข้อมูลอ้างอิงว่าอะไร valid
- Phase 2 เมื่อสร้าง Mold master แล้ว ค่อยเปิดใช้ validation

---

## 📎 Form Header Linkage — ตอบคำถามจากคุณ (2026-07-31)

> **คำถาม**: "การบันทึกส่วนหัวของเอกสาร คือ เครื่องจักร และวันที่ ส่วนนี้จะเชื่อมโยงกับ Job set up ที่ Tech เข้าไปทำงาน ถูกต้องหรือไม่"

### ✅ **ถูกต้อง 100%** — auto-populate จาก MachineSetupJob

Header ในฟอร์มกระดาษ **ทุก field** มี source อยู่ใน `machine_setup_job` table แล้ว **ไม่ต้องกรอกซ้ำ**:

| Field บนฟอร์ม | ตัวอย่างจากรูป | Source ในระบบ | เห็นได้จาก |
|---|---|---|---|
| **Blow M/C No.** | `102` | `machine_setup_job.machine → machines.machine_number` | Auto-fill · read-only |
| **ผลิตภัณฑ์ ก่อน** | `QP350` | `machine_setup_job.from_product → products.product_code` | Auto-fill · read-only |
| **ผลิตภัณฑ์ หลัง** | `QP350 M` | `machine_setup_job.to_product → products.product_code` | Auto-fill · read-only |
| **วันที่** | `30-7-26` | `machine_setup_job.plan_date` | Auto-fill · read-only |
| **Technician (ผู้ทำ)** | (ไม่มีในฟอร์ม) | `machine_setup_job.assigned_to → users.username` | Auto-fill จาก Login |

### 🔗 Data flow

```
PC import Excel Plan
  ↓
ระบบ auto-create machine_setup_job
  พร้อม: machine + fromProduct + toProduct + planDate + assignedTo
  ↓
Tech Login → เห็น setup job ใน zone "วันนี้"
  ↓
Tech คลิก setup job → เปิด SetupJobPanel
  ↓
Header แสดงข้อมูลทั้งหมด (auto-populate จาก setup_job.*)
  ↓
Tech กด "+ Add Entry"
  ↓
เพิ่ม setup_time_log row · FK setup_job_id = <จาก header>
  (ระบบใส่ FK ให้เอง · Tech ไม่ต้อง select machine/date ซ้ำ)
```

### 💡 UI Header — ตัวอย่างที่เห็นจริง

```
┌─ Setup Job #123 ───────────────────────────────────────┐
│ 🏭 Machine: 102 (TAHARA-102)                            │
│ 📦 QP350 → QP350 M                                       │
│ 📅 วันที่: 30/07/2026                                     │
│ 👷 Technician: somchai.k (auto จาก login)                │
│ Status: 🟡 IN_PROGRESS                                   │
│                                                          │
│ [+ Add TimeLog Entry] [🛑 End All] [✅ Complete Setup]   │
├─────────────────────────────────────────────────────────┤
│ (TimeLog table below)                                   │
└─────────────────────────────────────────────────────────┘
```

### 🎯 ประโยชน์ที่ได้

- ✅ **ไม่ต้องกรอก header ซ้ำ** — ลดเวลา + ลด typo
- ✅ **ไม่มี mismatch** — machine + date ที่ Tech เห็น = เดียวกับที่ PC วางแผน
- ✅ **1 Setup Job = 1 Header** — ไม่มีการสร้าง TimeLog แบบ "ลอย" ไม่มี FK
- ✅ **Trace ครบ** — ทุก TimeLog entry ⇐ SetupJob ⇐ ProductionPlan ⇐ WO ⇐ Excel Plan Import

### ⚠️ Edge case ที่คุมด้วย DB
- `setup_time_log.setup_job_id` = NOT NULL FK
- CASCADE DELETE — ถ้าลบ SetupJob (ไม่ควรเกิด) · TimeLog ทั้งหมดหายตาม
- Header ไม่มีเลย → Tech เปิด SetupJobPanel ไม่ได้ · ไม่มีที่ให้ add entry

---

## 🔁 Continuous Recording Mode — ตอบคำถามจากคุณ (2026-07-31)

> **คำถาม (1)**: "การกรอกข้อมูลการ setup จะเห็นว่าบันทึกเป็นช่วง ๆ ไม่ได้บันทึกทีเดียวเสร็จ ระบบออกแบบให้บันทึกแบบต่อเนื่อง หรือบันทึกเข้ามาเรื่อยๆ ได้"
>
> **คำถาม (2)**: "หมายถึงทำเสร็จ 1 หัวข้อ ก็เข้ามาบันทึก แล้วออกไปทำงาน แล้วกลับมากดที่การ setup เดิม แล้วเพิ่มบันทึกเข้าไปเรื่อยๆ จนกว่าจะเสร็จ หรือกดปิดงาน แบบนี้ถูกต้องหรือไม่"

### ✅ **ถูกต้อง 100% ทั้ง 2 ข้อ · นี่คือ core workflow ที่ผมออกแบบไว้เลย**

### 🎯 Workflow ที่คุณอธิบาย (ตรงเป๊ะ)

```
👷 Tech ทำ activity 1 (เช่น Heat Up)
   ↓ เสร็จ
📱 เข้าระบบ · เปิด Setup Job เดิม · กด "+ Add Entry"
   → Code: H · Start: 05:00 · End: 06:30 · กด Save
   → ปิด browser ได้เลย
   ↓
👷 กลับไปทำ activity 2 (เช่น Purge)
   ↓ เสร็จ
📱 เข้าระบบอีก · **เปิด Setup Job เดิม** (ค้างอยู่ · status = IN_PROGRESS)
   → เห็น entry ที่บันทึกไปแล้ว 1 อัน
   → กด "+ Add Entry" อีก · Code: P · Start: 06:30 · End: 13:00 · Save
   ↓
👷 ทำต่อไปเรื่อย ๆ · แต่ละกิจกรรมกลับมาบันทึก
   ↓ (30 กิจกรรม · ใช้เวลา 20+ ชั่วโมง · อาจข้ามกะ)
   ↓
✅ Setup เสร็จหมดแล้ว · Tech กด "Complete Setup Job"
   → ระบบเช็ค: มี IN_PROGRESS ค้างไหม? (Q9)
   → ถ้ามี → alert ให้ end ก่อน
   → ถ้าไม่มี → status = COMPLETED · Operator เริ่มผลิตได้
```

### 🎨 ตอบตรง ๆ ทีละข้อ

| คำถาม | คำตอบ | เพราะอะไร |
|---|---|---|
| ทำเสร็จ 1 หัวข้อ เข้ามาบันทึกทีเดียว? | ✅ **ได้** | 1 activity = 1 entry (POST 1 ครั้ง) |
| ออกไปทำงาน · ปิด browser ได้? | ✅ **ได้** | Data อยู่ใน DB · ไม่มี session state |
| กลับมากด Setup เดิม? | ✅ **ได้** | SetupJob ยัง IN_PROGRESS · เปิดต่อจากเดิม |
| เพิ่มบันทึกเรื่อย ๆ? | ✅ **ได้** | Unlimited entries (append) · ไม่จำกัดจำนวน |
| จนกว่าจะเสร็จ? | ✅ **ได้** | ไม่มี time limit · อยู่ได้ข้ามกะ · ข้ามวัน |
| กดปิดงาน = จบ? | ✅ **ใช่** | กด "Complete Setup Job" · status → COMPLETED · lock ไม่ให้ Tech แก้อีก |

### 🏭 Design principles (technical)

จากฟอร์มกระดาษที่คุณส่งมา · Setup 1 job ใช้เวลา **23+ ชั่วโมง** (03:00 ถึง 02:00 อีกวัน · ~30 entries) — Tech ไม่มีทางจำมาเขียนทีเดียวปลายกะ · **ต้องบันทึกทันทีที่ทำ**

### 📐 Design principles

1. **SetupJob อายุยาว** · status = `IN_PROGRESS` ค้างอยู่ได้ **หลายชั่วโมง / หลายวัน / ข้ามกะ**
2. **แต่ละ entry บันทึกทีละ 1 rec** · POST 1 request ต่อ entry · ไม่มี "save all" batch
3. **Auto-save เพื่อเลี่ยง data loss** · กด Save/OK ใน form → บันทึกลง DB ทันที · ไม่มี draft ค้าง browser
4. **หลาย session ต่อ 1 Setup Job** · Tech ปิด browser · กลับมาเปิดใหม่ · อ่านต่อได้ (state อยู่ใน DB · ไม่ใช่ session)
5. **หลาย device** · Tech ใช้ tablet ที่เครื่อง · PC ในออฟฟิศ · มือถือ · เห็นข้อมูลเดียวกัน (สลับได้)
6. **หลาย user ต่อ Setup Job** · shift handoff — Tech กะเช้าเริ่ม · Tech กะดึกทำต่อได้

### 🎬 Scenario จริง — Timeline หลาย session

```
📅 30/07/2026

⏰ 06:30 · Tech A (กะเช้า) เปิด SetupJobPanel
  → เห็น 3 entries ที่ Tech ก่อนหน้าบันทึกไว้ตั้งแต่ตี 3 (จากกะดึก)
  → เพิ่ม entry: H · 05:00-06:30 (90 min) · Heat Up
  → กด Save · DB บันทึก · ปิด browser

⏰ 08:00 · Tech A ไปทำงานที่เครื่อง (leave desktop)

⏰ 10:00 · Tech A เปิด tablet ที่ข้างเครื่อง
  → SetupJobPanel เปิดขึ้นมา · เห็น 4 entries ที่มีแล้ว
  → เริ่มทำ Setup S · กด "+ Add Entry" · กรอก Start=10:00 · End = ยังไม่รู้ · toggle "⏸ Still going"
  → กด Save · entry บันทึก status=IN_PROGRESS · end_time=NULL

⏰ 10:45 · Tech A เสร็จ Setup ครั้งนี้
  → เปิด tablet อีกครั้ง · หา entry S ที่ยัง IN_PROGRESS · กด "END NOW"
  → end_time = 10:45 · duration auto = 45 min

⏰ 14:00 · Tech A พักกลางวัน · เพิ่ม entry B · 12:00-13:00 (60 min · retroactive)

⏰ 18:00 · Tech A จบกะ · Setup ยังไม่เสร็จ · ไม่กด Complete
  → status ของ SetupJob ยัง IN_PROGRESS
  → เข้ากะดึก Tech B เห็นใน "ทำอยู่" zone

⏰ 22:00 · Tech B เปิดต่อ · เพิ่ม entries อีก
  → เห็นครบทุก entries ของ Tech A + ที่ตัวเองเพิ่ม

⏰ 02:00 · Tech B เสร็จ Setup · เจอ IN_PROGRESS entry ค้าง 1 อัน (Q9)
  → ระบบ block Complete · list เตือน
  → Tech B กด "End All Now" · entry จบด้วย end_time=02:00
  → Tech B กด "Complete Setup Job" · status → COMPLETED
```

### 🛠️ Technical design

**Backend (สนับสนุน continuous):**
- `POST /api/technician/setup-jobs/{id}/time-logs` — **stateless** · ไม่มี session · ใช้ JWT auth
- ทุก entry บันทึกใน DB transaction เดี่ยว (atomic)
- Return updated `TimeLogDto` · frontend อัปเดต table ทันที
- **Optimistic UI** — frontend ปรับ table ก่อน · ถ้า API fail → rollback + toast error

**Frontend (สนับสนุน continuous):**
- **Auto-refresh table** ทุก 60 วินาที (ถ้ามีคนอื่นเพิ่ม entry ในเครื่องอื่น จะเห็น)
- **On focus refresh** — เปิด tab กลับมา → refresh
- **Optimistic lock** — ก่อน update/delete · ส่ง `updated_at` ล่าสุดที่รู้จัก · server เช็คว่าตรงกับ DB ไหม
  - ถ้าไม่ตรง → 409 Conflict "someone else edited this · refresh please"
- **Session ไม่มี** — ทุกอย่างอยู่ที่ DB · ปิด browser ไม่มี data loss

**Concurrent editing:**
- 2 Tech เพิ่ม entry พร้อมกัน → ทั้งสองสำเร็จ · sequence_no auto-assign (next max + 1)
- 2 Tech แก้ entry เดียวกัน → คนที่ save ทีหลังจะเจอ 409 Conflict
- 2 Tech ลบ entry เดียวกัน → คนที่ 2 เจอ 404

### 📊 UI Indicators สำหรับ continuous mode

**On SetupJobPanel header:**
```
Setup Job #123 · Status: 🟡 IN_PROGRESS (opened 06:30 · 15 hrs ago)
Last activity: 22:15 by tech_b (5 min ago)
🔄 Auto-refresh every 60s · Last checked: 22:20
```

**On TimeLog table (per entry):**
- IN_PROGRESS entries → highlight สีเหลือง · แสดง "running for 45 min"
- Entries ที่เพิ่งเพิ่ม (< 5 min) → highlight สีฟ้าจาง ๆ + "just now" badge
- Entries ของคนอื่น (ไม่ใช่ current user) → แสดง avatar + tooltip "by tech_a at 10:00"

### ⚠️ Data safety points

| Scenario | Behavior |
|---|---|
| ปิด browser ระหว่างกรอก form (ยังไม่ save) | ⚠️ **entry หาย** — no draft persist · Tech ต้องกด Save ทุกครั้ง |
| Network หลุดตอนกด Save | ⚠️ Toast "Save failed · try again" · form ยังเปิด · retry ได้ |
| กด Save แล้ว network หลุดก่อน response | ⚠️ อาจเกิด duplicate ถ้ากด save 2 ครั้ง · **mitigation**: idempotency key (UUID per form open) |
| Tablet ปิดจอเอง (screen sleep) | ✅ Data ใน DB ปลอดภัย · เปิดจอ · re-fetch OK |
| Server restart ระหว่าง save | ⚠️ ถ้า transaction commit แล้ว → OK · ก่อน commit → entry หาย · Tech ต้องบันทึกใหม่ |
| Concurrent edit conflict | ✅ 409 Conflict · แจ้ง Tech "refresh + ลองใหม่" |

### 🔧 Config options ที่เพิ่มใน `application.properties`

```properties
# Continuous mode support
app.setup.autorefresh.interval-sec=60         # Frontend polling interval
app.setup.optimistic-lock.enabled=true        # Enable ETag/updated_at check
app.setup.idempotency-key.enabled=true        # Prevent duplicate on retry
app.setup.max-in-progress-per-tech=5          # Warning threshold (not hard block)
```

---

## 1. Objective

**"Replace fixed checklist with a flexible time-log — Technician records EVERY activity during setup as (code + start–end time + description + photos), with automatic per-code time summary. Match the paper form `Blow M/C (Set-up)` exactly."**

### Business outcome
- ✅ ทดแทนกระดาษ Blow M/C Set-up ที่จดมือ (ปัจจุบันเวลาเขียนแนวนอนบน grid time bar)
- ✅ **สรุปเวลา per code อัตโนมัติ** — เห็นทันทีว่า Setup กะนี้ใช้เวลาไปกับ Q (30 QA) 500 นาที · S (Setup) 300 นาที · etc.
- ✅ **Real-time capture** — Technician บันทึกทันทีที่ทำ ไม่ใช่จำมาเขียนตอนเลิกงาน
- ✅ **Photo evidence** ต่อ activity — Mold change, defect, adjustment · trace ย้อนได้
- ✅ **PC/Mgmt เห็นสถิติ** — เปรียบเทียบ Setup time ระหว่างเครื่อง/product/technician
- ✅ **Match ฟอร์มเดิม** — ทีมคุ้นเคยกับ code letters (M/S/Q/A/C/Z/R/H/B/X + custom)

---

## 2. Business Context — Reference Form Analysis

จากไฟล์แนบ `RBL102QP350A set2 setup.pdf` (Blow M/C No. 102 · QP350 → QP350M · 30/07/26):

### Header
| Field | ตัวอย่าง | ที่มา |
|---|---|---|
| Blow M/C No. | `102` | machine number |
| ผลิตภัณฑ์ ก่อน | `QP350` | fromProduct |
| ผลิตภัณฑ์ หลัง | `QP350 M` | toProduct |
| วันที่ | `30-7-26` | date |

### รหัส (Code) Legend — 10 codes ในฟอร์ม (บาง code ใน highlight = สำคัญ/พบบ่อย)

| Code | Description (TH) | Description (EN) | Category |
|---|---|---|---|
| **M** | Mold Change | เปลี่ยนแม่พิมพ์ | **Mold** |
| **A** | เปลี่ยนวัตถุดิบ | Material Change | Material |
| **S** | Setup | Setup activity | **Setup** |
| **C** | เคลียร์ไลน์ | Clear Line | Cleanup |
| **Q** | 30 QA | QA check | **Quality** |
| **Z** | คนไม่พอ | Not enough people | **Waiting** |
| **R** | ผลิต | Production run | Production |
| **H** | 30 Heat Up | Heat Up | Heating |
| **B** | เวลาหยุดพัก | Break time | Break |
| **X** | เครื่องจักรเสีย | Machine breakdown | **Breakdown** |

**Custom codes ที่เห็นในตัวอย่างจริง (ไม่อยู่ใน legend)**:
- **P** = Purge (`P | 6:30-13:00 = 390 นาที · Purge ไว้ถูกกัน`)
- → **ระบบต้องรองรับ codes เพิ่มโดย Admin หรือ freeform "other"**

### Time Bar (top ruler)
- Column headers = ชั่วโมง `3, 4, 5, ..., 24, 1, 2, 3` (24 hrs across midnight)
- ใต้ header = tick marks / รหัสอักษร วางบน timeline
- **ยากใน digital**: อาจแทนด้วย **Gantt-style timeline** ที่ auto-generate จาก activities table

### Main Activities Table (3 columns)

| ลำดับรหัส | Time | รายละเอียดงาน |
|---|---|---|
| Z | 03:00 – 05:00 = 120 | ปั๊มไม่มีคน |
| H | 05:00 – 06:30 = 90 | เปิดเครื่อง Heat up ไว้ |
| P | 06:30 – 13:00 = 390 | Purge ไว้ถูกกัน |
| S | 13:00 – 13:50 = 50 | เป่าปรับเผ้าลาย และ APP จุดกัน จุดเสียง เทียบตัวอย่าง QA 10 shot |
| Q | 13:50 – 14:50 = 60 | ส่ง QA - QA แจ้ง บอกเป็น เกรดฟัน บ้างจอ ไม่รอน ปริมาณตก ทุก cav |
| S | 14:50 – 15:00 = 10 | ปรับ cutting จาก 9.20-8.90 มม + Prelift จาก 6.0-4.0 เกินงานส่ง QA 10 shot |
| ... | ... | ~30 rows total ตลอด 24 ชม |

**Observations**:
- Same code หลาย row (S พบ ~10 ครั้ง · Q พบ ~7 ครั้ง)
- Duration = end - start (minutes) — ควร auto-calc
- Description มีตัวเลข technical (cutting 9.20→8.90, Prelift 6.0→4.0) — free text ที่มี jargon
- Notes ท้ายฟอร์ม: **"Setup (รายละเอียดย่อย) ต้องลงข้อมูลว่าอะไรคือปัญหาและปรับแก้ไขยังไง จุดนี้สำคัญ"**

### Cavity Check Table (top-right)
- Grid 5 stations × 5 cavities (Cav.1 – Cav.5)
- Digital equivalent: **checkbox matrix** with pass/fail per station×cavity
- ตอนนี้ยังเขียนมือ · Phase 1 W19a จะ digitize

### Bottom footer (ตัวอย่างเห็น)
- `CT: 7803` (Cycle Time?)
- `ปริ 651 5.24` (Volume?)

---

## 3. Impact Analysis

### Tables affected
| Table | Change |
|---|---|
| `machine_setup_job` | Keep · Checklist template deprecated (read-only historical) |
| `setup_checklist_templates` | **Deprecated** · read-only for historical Setup Jobs · no new writes |
| `setup_job_steps` | **Deprecated** · same as above |
| **NEW: `setup_activity_code`** | Master data — 10 default codes · Admin-managed only (no user custom) |
| **NEW: `setup_time_log`** | Time-tracked entries (many per setup_job) |
| **NEW: `setup_time_log_photo`** | Photos attached to time log entries (many per entry) · compressed on save |
| ~~`setup_cavity_check`~~ | ⏭️ **Skipped** · Phase 2 (no current recording) |

### Services affected
- `MachineSetupJobService` — keep · **REPLACE** Complete dialog logic (drop Checklist writes · redirect to TimeLog)
- **New: `SetupTimeLogService`** — add/edit/delete time log entries + summary aggregation + photo compression
- **New: `SetupActivityCodeService`** — CRUD master codes (Admin only)
- **New: `ImageResizeService`** — server-side compression (util shared)
- ~~`SetupCavityCheckService`~~ — skipped (Q5)

### Controllers
- `MachineSetupJobController` — add nested endpoints for time logs, photos, summary
- **New: `SetupActivityCodeController`** — Admin only
- ~~cavity endpoints~~ — skipped

### Frontend
- **Modify**: `SetupJobPanel.jsx` — **REPLACE** Checklist UI entirely with TimeLog · no mode toggle
- **New**: `SetupTimeLogTable.jsx` — inline editable table
- **New**: `SetupTimeLogEntryForm.jsx` — Add/Edit modal with dropdown + time picker + photo upload
- **New**: `SetupTimeSummary.jsx` — per-code summary + chart
- **New**: `SetupActivityCodeManager.jsx` — Admin panel (add/edit/deactivate codes)
- ~~`SetupCavityCheckGrid.jsx`~~ — skipped
- **Optional**: `SetupTimelineGantt.jsx` — visual Gantt bar (skip if tight)
- **Read-only**: legacy checklist view for old Setup Jobs (small `HistoricalChecklistView.jsx`)

### Roles / permissions
- **Technician**: create/edit own time logs · upload photos
- **PC / Mgmt**: read-only + view summary + export + cross-setup analytics
- **Admin**: manage `setup_activity_code` master (add/edit/activate/deactivate)
- **NO user-created codes** — Admin-only master maintenance

---

## 4. Database Schema

### 4.1 Flyway migration: `V19__Create_setup_timelog_tables.sql`

```sql
-- Master data: activity codes (Admin CRUD)
CREATE TABLE setup_activity_code (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,          -- M, S, Q, A, C, Z, R, H, B, X, P (custom), ...
    description_th NVARCHAR(200) NOT NULL,
    description_en NVARCHAR(200) NULL,
    category VARCHAR(30) NOT NULL,             -- Mold · Material · Setup · Cleanup · Quality · Waiting · Production · Heating · Break · Breakdown · Other
    color_hex VARCHAR(7) NULL,                 -- Optional color for Gantt visualization
    is_active BIT NOT NULL DEFAULT 1,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    created_by BIGINT NOT NULL,

    CONSTRAINT fk_sac_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX ix_sac_active ON setup_activity_code(is_active, display_order);

-- Seed 10 default codes from paper form
INSERT INTO setup_activity_code (code, description_th, description_en, category, color_hex, display_order, created_by) VALUES
  ('M', N'เปลี่ยนแม่พิมพ์', 'Mold Change', 'Mold', '#8B5CF6', 10, 1),
  ('A', N'เปลี่ยนวัตถุดิบ', 'Material Change', 'Material', '#EC4899', 20, 1),
  ('S', N'Setup', 'Setup activity', 'Setup', '#0EA5E9', 30, 1),
  ('C', N'เคลียร์ไลน์', 'Clear Line', 'Cleanup', '#F59E0B', 40, 1),
  ('Q', N'30 QA', 'QA check', 'Quality', '#10B981', 50, 1),
  ('Z', N'คนไม่พอ', 'Not enough people', 'Waiting', '#6B7280', 60, 1),
  ('R', N'ผลิต', 'Production run', 'Production', '#22C55E', 70, 1),
  ('H', N'30 Heat Up', 'Heat Up', 'Heating', '#F97316', 80, 1),
  ('B', N'เวลาหยุดพัก', 'Break time', 'Break', '#94A3B8', 90, 1),
  ('X', N'เครื่องจักรเสีย', 'Machine breakdown', 'Breakdown', '#EF4444', 100, 1);

-- Time log entries (many per setup_job)
CREATE TABLE setup_time_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    setup_job_id BIGINT NOT NULL,
    activity_code_id BIGINT NOT NULL,
    code_snapshot VARCHAR(10) NOT NULL,                -- Denormalized code at insert-time (audit)

    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NULL,                           -- NULL = still in progress
    duration_min INT NULL,                             -- Computed: end - start (only when end_time set)

    description NVARCHAR(1000) NULL,                   -- The "รายละเอียดงาน" column
    sequence_no INT NOT NULL,                          -- Row order within setup (1, 2, 3, ...)

    created_by BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NULL,
    updated_by BIGINT NULL,

    CONSTRAINT fk_stl_job FOREIGN KEY (setup_job_id) REFERENCES machine_setup_job(id) ON DELETE CASCADE,
    CONSTRAINT fk_stl_code FOREIGN KEY (activity_code_id) REFERENCES setup_activity_code(id),
    CONSTRAINT fk_stl_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_stl_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT ck_stl_duration CHECK (end_time IS NULL OR end_time > start_time)
);

CREATE INDEX ix_stl_job_seq ON setup_time_log(setup_job_id, sequence_no);
CREATE INDEX ix_stl_code ON setup_time_log(activity_code_id);
CREATE INDEX ix_stl_start ON setup_time_log(start_time);

-- Photos attached to time log entries (0..N per entry)
CREATE TABLE setup_time_log_photo (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    time_log_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,               -- Relative path from storage root
    original_filename VARCHAR(255) NULL,
    file_size_bytes BIGINT NULL,
    mime_type VARCHAR(50) NULL,
    caption NVARCHAR(500) NULL,                        -- Optional caption

    uploaded_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    uploaded_by BIGINT NOT NULL,

    CONSTRAINT fk_stlp_log FOREIGN KEY (time_log_id) REFERENCES setup_time_log(id) ON DELETE CASCADE,
    CONSTRAINT fk_stlp_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE INDEX ix_stlp_log ON setup_time_log_photo(time_log_id);

-- Cavity check table: SKIPPED in W19a per Q5 decision · will add in Phase 2
```

### 4.2 Rollback: `V19__Create_setup_timelog_tables_rollback.sql`
```sql
DROP TABLE IF EXISTS setup_time_log_photo;
DROP TABLE IF EXISTS setup_time_log;
DROP TABLE IF EXISTS setup_activity_code;
```

---

## 5. Backend Design

### 5.1 Entities

**`SetupActivityCode.java`**
```java
@Entity @Table(name = "setup_activity_code")
@Data @NoArgsConstructor @AllArgsConstructor
public class SetupActivityCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "description_th", nullable = false, length = 200)
    private String descriptionTh;

    @Column(name = "description_en", length = 200)
    private String descriptionEn;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
```

**`SetupTimeLog.java`**
```java
@Entity @Table(name = "setup_time_log")
@Data @NoArgsConstructor @AllArgsConstructor
public class SetupTimeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setup_job_id", nullable = false)
    private MachineSetupJob setupJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_code_id", nullable = false)
    private SetupActivityCode activityCode;

    @Column(name = "code_snapshot", nullable = false, length = 10)
    private String codeSnapshot;                     // Denormalized

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_min")
    private Integer durationMin;                     // Auto-computed

    @Column(length = 1000)
    private String description;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "timeLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SetupTimeLogPhoto> photos = new ArrayList<>();

    @PreUpdate
    public void computeDuration() {
        if (endTime != null && startTime != null) {
            this.durationMin = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }
}
```

**`SetupTimeLogPhoto.java`** and **`SetupCavityCheck.java`** — analog · see schema

### 5.2 Services

**`SetupTimeLogService.java`** (new)
```java
public interface SetupTimeLogService {
    TimeLogDto addEntry(Long setupJobId, TimeLogRequest req, String username);
    TimeLogDto updateEntry(Long entryId, TimeLogRequest req, String username);
    void deleteEntry(Long entryId, String username);
    TimeLogDto endInProgressEntry(Long entryId, LocalDateTime endTime, String username);

    List<TimeLogDto> listBySetupJob(Long setupJobId);
    List<TimeSummaryDto> summarizeByCode(Long setupJobId);

    // Photo management
    PhotoDto uploadPhoto(Long timeLogId, MultipartFile file, String caption, String username);
    void deletePhoto(Long photoId, String username);

    // Reporting
    List<TimeSummaryDto> summarizeByCodeForMachine(Long machineId, LocalDate from, LocalDate to);
    List<TimeSummaryDto> summarizeByCodeForTechnician(Long userId, LocalDate from, LocalDate to);
}
```

**`ImageResizeService.java`** (new · shared util per Q3)
```java
public interface ImageResizeService {
    /**
     * Resize and compress image before persisting.
     * - Max width 1024px (keep aspect ratio)
     * - JPEG quality 75
     * - Target size < 300 KB per photo
     * - Strip EXIF for privacy
     */
    byte[] resizeForStorage(MultipartFile source) throws IOException;

    /** Return path where photo should be stored per Q4 on-premise scheme */
    Path resolveStoragePath(Long setupJobId, String uuid);
}
```

Implementation uses `javax.imageio` (JDK built-in · no new dep) — sample:
```java
BufferedImage src = ImageIO.read(source.getInputStream());
int targetWidth = Math.min(1024, src.getWidth());
double scale = (double) targetWidth / src.getWidth();
int targetHeight = (int) (src.getHeight() * scale);
BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
scaled.getGraphics().drawImage(src.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
// Write as JPEG q=0.75
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
ImageWriteParam param = writer.getDefaultWriteParam();
param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
param.setCompressionQuality(0.75f);
writer.setOutput(ImageIO.createImageOutputStream(baos));
writer.write(null, new IIOImage(scaled, null, null), param);
return baos.toByteArray();
```

~~`SetupCavityCheckService.java`~~ — **skipped per Q5**

### 5.3 DTOs

**`TimeLogRequest.java`**
```java
Long activityCodeId
LocalDateTime startTime
LocalDateTime endTime      // null = in progress
String description
Integer sequenceNo         // optional · auto if null
```

**`TimeLogDto.java`** — response
```java
Long id · Long setupJobId
Long activityCodeId · String code · String descriptionTh · String category · String colorHex
LocalDateTime startTime · LocalDateTime endTime · Integer durationMin
String description · Integer sequenceNo
String createdByName · LocalDateTime createdAt
String updatedByName · LocalDateTime updatedAt
List<PhotoDto> photos
```

**`TimeSummaryDto.java`** — per-code summary
```java
String code · String descriptionTh · String category · String colorHex
Integer entryCount              // number of time log entries with this code
Integer totalDurationMin        // sum of duration_min
Double percentageOfTotal        // vs total setup time
LocalDateTime firstStart        // when did this code first appear
LocalDateTime lastEnd           // when did this code last end
```

~~`CavityCheckDto.java`~~ — **skipped per Q5**

### 5.4 Controllers

**`MachineSetupJobController.java`** — add:
```
GET    /api/technician/setup-jobs/{id}/time-logs
POST   /api/technician/setup-jobs/{id}/time-logs
PUT    /api/technician/setup-jobs/{id}/time-logs/{entryId}
DELETE /api/technician/setup-jobs/{id}/time-logs/{entryId}
POST   /api/technician/setup-jobs/{id}/time-logs/{entryId}/end     — set endTime for in-progress
POST   /api/technician/setup-jobs/{id}/time-logs/{entryId}/photos  — multipart upload
DELETE /api/technician/setup-jobs/{id}/time-logs/{entryId}/photos/{photoId}
GET    /api/technician/setup-jobs/{id}/summary                     — TimeSummaryDto[] grouped by code

// Cavity check: SKIPPED per Q5 · Phase 2

GET    /api/pc/setup-summary?machineId=&from=&to=                  — cross-setup aggregation
GET    /api/pc/setup-summary?technicianId=&from=&to=

GET    /api/setup-codes                 — dropdown source (any auth · returns active codes)
```

**`SetupActivityCodeController.java`** (new · Admin-only per Q2 · read for all)
```
GET    /api/setup-codes                 — list active (any auth) · used by dropdown
GET    /api/admin/setup-codes           — list all incl inactive (Admin)
POST   /api/admin/setup-codes           — create · Admin only per Q2
PUT    /api/admin/setup-codes/{id}      — update description/color/order
PUT    /api/admin/setup-codes/{id}/toggle-active  — activate/deactivate (soft)
```

**❌ No user-facing create endpoint** — Tech ไม่มีสิทธิ์เพิ่ม/แก้ code · เจอ activity ที่ไม่มี code → หยุด · แจ้ง Admin

---

## 6. Frontend Design

### 6.1 Modify `SetupJobPanel.jsx`

**Add "Start TimeLog" mode** — Instead of / alongside the existing checklist:

```
┌─ Setup Job — TAHARA 102 · QP350 → QP350M · 30/07/2026 ─┐
│                                                         │
│ [📋 Checklist mode (existing)] [⏱️ TimeLog mode (new)]   │
│                                                         │
│ ── TimeLog Table ────────────────────────────────────── │
│                                                         │
│ #  Code  Time            Duration  Description       📷  │
│ 1  Z     03:00–05:00     120 min   ปั๊มไม่มีคน               │
│ 2  H     05:00–06:30      90 min   เปิดเครื่อง Heat up ไว้  │
│ 3  P     06:30–13:00     390 min   Purge ไว้ถูกกัน           │
│ 4  S     13:00–13:50      50 min   เป่าปรับเผ้าลาย... 📎(2)  │
│ 5  Q     13:50–14:50      60 min   ส่ง QA - QA แจ้ง...       │
│ ...                                                     │
│ [+ เพิ่ม Entry]                    [🕐 Start Entry ตอนนี้]  │
│                                                         │
│ ── Summary per code ─────────────────────────────────── │
│ Code                    Count  Total min    %          │
│ S · Setup                10    300 min      25%   ▓▓▓░ │
│ Q · 30 QA                 7    280 min      23%   ▓▓▓░ │
│ P · Purge (custom)        1    390 min      32%   ▓▓▓▓ │
│ Z · Not enough people     2    150 min      12%   ▓░░░ │
│ H · Heat Up               1     90 min       8%   ░░░░ │
│ ...                                                     │
│ Total setup: 1,210 min = 20.2 hours                     │
│                                                         │
│ ── Cavity Check (5×5) ───────────────────────────────── │
│         Cav.1  Cav.2  Cav.3  Cav.4  Cav.5              │
│ St 1     ✅     ✅     ✅     ❌     ✅                    │
│ St 2     ✅     ✅     ✅     ✅     ✅                    │
│ St 3     ✅     ❌     ✅     ✅     ✅                    │
│ St 4     ✅     ✅     ✅     ✅     ✅                    │
│ St 5     ✅     ✅     ✅     ✅     ✅                    │
│                                Pass: 23  Fail: 2       │
│                                                         │
│ [Cancel]                       [Complete Setup Job]     │
└─────────────────────────────────────────────────────────┘
```

### 6.2 New: `SetupTimeLogEntryForm.jsx` (Add/Edit Modal)

```
┌─ ⏱️ Add Time Log Entry ──────────────────────────────────┐
│                                                          │
│ Code *:  [S · Setup ▼]                                    │
│           (dropdown shows: M · A · S · C · Q · Z · R ·    │
│                            H · B · X · + Custom)          │
│                                                          │
│ Start *: [30/07/2026] [13:00] [🕐 Now]                    │
│ End:     [30/07/2026] [13:50] [🕐 Now] [⏸ Still going]    │
│ Duration: 50 minutes (auto)                              │
│                                                          │
│ Description: [เป่าปรับเผ้าลาย และ APP จุดกัน จุดเสียง          │
│               เทียบตัวอย่าง QA 10 shot        _________] │
│                                                          │
│ Photos (0/5):                                            │
│   [📷 Add photo]  Camera or File                          │
│   (previews appear here as thumbnails)                   │
│                                                          │
│ [Cancel]                                    [💾 Save]     │
└──────────────────────────────────────────────────────────┘
```

**Interactions**:
- **🕐 Now** button — fills in current time (for start or end)
- **⏸ Still going** — leaves end null · entry appears in table with "IN PROGRESS" badge · user can click "End Entry" later
- **Duration auto** — calc as user types end time
- **Overlap warning** — if start conflicts with existing entry, warn but allow (setup can have parallel activities logically · e.g., machine heating while operator does QA)

### 6.3 New: `SetupTimeSummary.jsx`

- Table: Code · Count · Total min · % (with bar)
- Bar chart (horizontal) using CSS or lightweight lib
- Color = `activity_code.color_hex`
- Sort desc by total min

### 6.4 New: `SetupCavityCheckGrid.jsx`

- 5×5 grid
- Each cell: click to toggle PASS → FAIL → NA → PASS
- Or tap to open notes modal
- Bottom summary: Pass count · Fail count · NA count

### 6.5 (Optional) `SetupTimelineGantt.jsx`

- Horizontal bar chart showing time entries on a timeline
- x-axis = hours (matches paper form's 3-24, 1-3)
- Each entry = colored bar (by code) at its start-end position
- Overlaps stack vertically

---

## 7. API Endpoints Summary

| Method | Path | Role | Body | Response |
|---|---|---|---|---|
| GET | `/api/technician/setup-jobs/{id}/time-logs` | Tech/PC | — | `List<TimeLogDto>` |
| POST | `/api/technician/setup-jobs/{id}/time-logs` | Tech | `TimeLogRequest` | `TimeLogDto` |
| PUT | `/api/technician/setup-jobs/{id}/time-logs/{entryId}` | Tech (own) | `TimeLogRequest` | `TimeLogDto` |
| DELETE | `/api/technician/setup-jobs/{id}/time-logs/{entryId}` | Tech (own) | — | 204 |
| POST | `/api/technician/setup-jobs/{id}/time-logs/{entryId}/end` | Tech | `{endTime}` | `TimeLogDto` |
| POST | `/api/technician/setup-jobs/{id}/time-logs/{entryId}/photos` | Tech | multipart | `PhotoDto` |
| DELETE | `/api/technician/setup-jobs/{id}/time-logs/{entryId}/photos/{photoId}` | Tech | — | 204 |
| GET | `/api/technician/setup-jobs/{id}/summary` | Tech/PC | — | `List<TimeSummaryDto>` |
| GET | `/api/pc/setup-summary` | PC/Mgmt | `?machineId&technicianId&from&to` | `List<TimeSummaryDto>` |
| GET | `/api/setup-codes` | any auth | — | `List<CodeDto>` (active only · for dropdown) |
| GET | `/api/admin/setup-codes` | Admin | — | `List<CodeDto>` (all inc inactive) |
| POST | `/api/admin/setup-codes` | **Admin only** | `CodeDto` | `CodeDto` |
| PUT | `/api/admin/setup-codes/{id}` | Admin | `CodeDto` | `CodeDto` |
| PUT | `/api/admin/setup-codes/{id}/toggle-active` | Admin | — | `CodeDto` |
| ~~cavity-check endpoints~~ | — | — | **Phase 2** |

---

## 8. Development Phases (~3 MD estimate · Q5 skip saved 0.5 MD)

### W19a.1 — DB + Master Codes + Admin UI (0.75 MD)
- Flyway V19 migration (H2 + SQL Server test) · **no cavity table per Q5**
- Seed 10 default codes (Admin ต้องเพิ่ม P + อื่นๆก่อน pilot)
- `SetupActivityCode` entity + repository
- `SetupActivityCodeService` — CRUD **Admin-only** (no user creation per Q2)
- `SetupActivityCodeController` — separate `/api/setup-codes` (read all auth) vs `/api/admin/setup-codes` (write)
- `SetupActivityCodeManager.jsx` admin panel
- Unit tests

### W19a.2 — TimeLog Backend + Image Resize (1 MD)
- `SetupTimeLog` + `SetupTimeLogPhoto` entities
- `SetupTimeLogService` with all CRUD + summary
- `ImageResizeService` per Q3 · javax.imageio · 1024px + q=0.75 · target <300 KB
- File storage per Q4: `${APP_STORAGE_ROOT}/setup-photos/{yyyy}/{mm}/{setupJobId}/{uuid}.jpg`
- Environment property: `app.storage.root=/var/gdtahara/storage` (default) · overrideable
- Photo upload endpoint with MultipartFile
- Unit tests: 12+ scenarios · include image resize verification
- **No overlap validation** per Q6

### W19a.3 — Frontend TimeLog UI (1 MD)
- **Replace** `SetupJobPanel.jsx` Complete dialog with TimeLog UI (no mode toggle per Q1)
- `HistoricalChecklistView.jsx` — read-only for old Setup Jobs (backward compat)
- `SetupTimeLogTable.jsx` — inline list with edit/delete
- `SetupTimeLogEntryForm.jsx` — dropdown (fixed codes) · time pickers · photo upload
- `SetupTimeSummary.jsx` — table + horizontal bars
- Client-side pre-resize (optional · saves upload bandwidth)
- ~~`SetupCavityCheckGrid.jsx`~~ — skipped per Q5

### W19a.4 — Gantt View + Complete guard (0.5 MD · Q8+Q9)
- **Q8**: `SetupTimelineGantt.jsx` — visual timeline · color-coded bars per code · overlaps stack
- **Q8**: Print-friendly CSS (A4 landscape · for management report attach)
- **Q9**: Complete Setup Job guard:
  - `MachineSetupJobService.complete()` — check if any time_log has `end_time IS NULL`
  - If yes → throw ValidationException "N IN_PROGRESS entries · end them first"
  - Frontend: Complete button disabled with tooltip · list IN_PROGRESS at top with "END NOW"
  - Add "🛑 End All Now" convenience button (sets end_time = now for all IN_PROGRESS)

### W19a.5 — Reporting + Training + UAT prep (0.25 MD)
- Cross-setup summary endpoint (per machine / per technician / date range)
- Update `02 Technician training doc` — **rewrite Setup section** (TimeLog · no more Checklist)
- Update `00 System Flow diagram` — Technician flow · reflect TimeLog + Q9 complete rule
- Add short **"หน้าจอเปลี่ยน"** notice for existing Tech users (change management)

---

## 9. File Structure

### Backend
```
src/main/java/com/gdtahara/gdtaharabackend/
├── model/SetupActivityCode.java              (NEW)
├── model/SetupTimeLog.java                    (NEW)
├── model/SetupTimeLogPhoto.java               (NEW)
├── repository/SetupActivityCodeRepository.java (NEW)
├── repository/SetupTimeLogRepository.java     (NEW)
├── repository/SetupTimeLogPhotoRepository.java (NEW)
├── service/SetupActivityCodeService.java      (NEW)
├── service/SetupTimeLogService.java           (NEW · interface)
├── service/SetupTimeLogServiceImpl.java       (NEW)
├── service/ImageResizeService.java            (NEW · javax.imageio · Q3)
├── service/FileStorageService.java            (NEW · shared · setup photos)
├── controller/MachineSetupJobController.java  (MODIFY: add nested endpoints)
├── controller/SetupActivityCodeController.java (NEW · admin CRUD)
├── dto/TimeLogDto.java                        (NEW)
├── dto/TimeLogRequest.java                    (NEW)
├── dto/TimeSummaryDto.java                    (NEW)
├── dto/PhotoDto.java                          (NEW)
├── dto/CodeDto.java                           (NEW)

src/main/resources/db/migration/
└── V19__Create_setup_timelog_tables.sql       (NEW)

src/test/java/com/gdtahara/gdtaharabackend/service/
├── SetupTimeLogServiceTest.java               (NEW · 12+ tests)
└── ImageResizeServiceTest.java                (NEW · verify < 300 KB output)

src/test/java/com/gdtahara/gdtaharabackend/controller/
└── SetupTimeLogControllerIT.java              (NEW)
```

### Frontend
```
gdtahara-frontend/src/
├── api/phase1Api.js                           (MODIFY: add timelog + cavity + code endpoints)
├── components/technician/
│   ├── SetupJobPanel.jsx                      (MODIFY: **REPLACE** Checklist with TimeLog per Q1 · +Q9 Complete guard)
│   ├── SetupTimeLogTable.jsx                  (NEW · IN_PROGRESS highlight + End Now button)
│   ├── SetupTimeLogEntryForm.jsx              (NEW · dropdown fixed codes per Q2)
│   ├── SetupTimeSummary.jsx                   (NEW)
│   ├── SetupTimelineGantt.jsx                 (NEW · **required per Q8**)
│   ├── HistoricalChecklistView.jsx            (NEW · read-only legacy)
│   └── EndAllInProgressDialog.jsx             (NEW · Q9 · convenience end-all)
├── components/admin/
│   └── SetupActivityCodeManager.jsx           (NEW · Admin-only per Q2)
├── styles/
│   └── gantt-print.css                        (NEW · Q8 · A4 landscape)
└── utils/
    └── timeUtils.js                            (NEW · duration calc helpers)
```

### Docs
```
docs/
├── phase1/w19a-setup-timelog-plan.md          (THIS FILE)
├── training/02-technician-setup-flow.md       (MODIFY: add TimeLog section)
└── training/00-system-flow-by-role.md         (MODIFY: update Tech flow)
```

---

## 10. Testing Plan

### Unit tests (SetupTimeLogServiceTest)
1. ✅ addEntry — happy path with end_time → duration auto-computed
2. ✅ addEntry — in-progress entry (end_time = null) → status IN_PROGRESS
3. ✅ endInProgressEntry — sets end_time · computes duration
4. ❌ addEntry — end < start → validation error
5. ❌ addEntry — sequence_no clash → auto-renumber
6. ✅ updateEntry — same user allowed · updates fields · recomputes duration
7. ❌ updateEntry — different user → IDOR error (unless PC/Admin)
8. ✅ deleteEntry — soft delete? No, hard delete · cascade photos
9. ✅ summarizeByCode — 10 entries · 4 codes · returns 4 rows · correct sums + %
10. ✅ summarizeByCode — empty setup → returns []
11. ✅ Photo upload — file saved · DB row created · returns url
12. ❌ Photo upload — >5MB → validation error
13. ✅ Photo delete — file removed · DB row deleted

### ~~Cavity check tests~~ — SKIPPED per Q5

### Image resize tests (ImageResizeServiceTest)
14. ✅ resize 5 MB input → output < 300 KB
15. ✅ preserve aspect ratio · width capped at 1024px
16. ✅ EXIF stripped
17. ❌ non-image file → validation error

### Cross-setup summary tests
18. ✅ summarizeByCodeForMachine — 3 setups · aggregate correct per code
19. ✅ summarizeByCodeForTechnician — 5 setups by 1 user · per code sum

### Master code guard tests (Q2)
20. ❌ Non-admin POST /api/admin/setup-codes → 403
21. ✅ Admin toggle-active → code disappears from `/api/setup-codes` (active list)
22. ✅ Historical entries with deactivated code → still readable

### Complete Setup guard tests (Q9)
23. ❌ Complete Setup Job while 2 IN_PROGRESS entries exist → ValidationException "2 entries not ended"
24. ✅ Complete after ending all IN_PROGRESS → success
25. ✅ "End All Now" convenience — sets end_time = now for all IN_PROGRESS · then Complete succeeds

### Gantt render tests (Q8)
26. ✅ 10 entries no overlap → 10 bars in sequence
27. ✅ 3 overlapping entries → stack vertically (row 1/2/3)
28. ✅ Print CSS → A4 landscape · bars fit within page width

### Continuous recording tests
29. ✅ Add entry morning · close browser · reopen 4 hours later → entry still visible
30. ✅ Tech A adds entry · Tech B on different device fetches same SetupJob → sees Tech A's entry
31. ✅ Same entry edited by 2 users concurrently → 2nd save gets 409 Conflict
32. ✅ Idempotency key on POST — retry after network fail → no duplicate
33. ✅ SetupJob spans multiple shifts (Tech A + Tech B contribute entries) → all accounted in summary
34. ✅ 5+ IN_PROGRESS entries per Tech → warning shown but not blocked (config)

### E2E test
- Create SetupJob → add 10 time entries · 3 with photos → verify summary + all data persisted + photos resized

### Manual QA scenarios
1. Add entry, set start = 03:00, end = 05:00 → duration should be 120 min
2. Add in-progress entry → click "End" 2 hours later → duration = 120 min
3. Upload 3 photos (each 3 MB) → verify all < 300 KB after resize · previews show · delete 1 → file removed
4. Tech opens dropdown · sees only 10 (or admin-updated) codes · NO "Add custom" option
5. Add 2 overlapping entries (S · 13:00-14:00 · Q · 13:30-14:00) — both saved · no warning
6. Compare screen output side-by-side with paper form → verify match
7. **(Q9)** Try Complete Setup with 3 IN_PROGRESS → blocked · alert lists 3 · click "End All Now" → all ended · Complete succeeds
8. **(Q8)** Open Gantt view · verify bars colored per code · overlaps stack · print preview A4 landscape ครบทั้ง 24 ชม

---

## 11. Timeline (Estimated · updated after Q5-Q10 lock)

| Day | Task | MD |
|---|---|---|
| Mon | DB migration + master codes + Admin CRUD + Admin UI | 0.75 |
| Tue | TimeLog entities + service + ImageResizeService + tests | 1.0 |
| Wed | Photo upload + storage + summary + Complete guard (Q9) | 0.5 |
| Thu | Frontend REPLACE SetupJobPanel + TimeLog table + form | 0.75 |
| Fri AM | Gantt view + print CSS (Q8) | 0.5 |
| Fri PM | Summary + HistoricalChecklistView + training doc rewrite | 0.5 |
| Mon+ | UAT with 2 Tech + PC (change management!) | — |

**Total dev: ~4.0 MD**
- Base: 3.5 MD
- −0.5 MD from Q5 cavity skip
- +0.5 MD from Q8 Gantt (now required)
- +0.25 MD from Q9 Complete guard
- +0.25 MD from Gantt print CSS
- **Net = ~4.0 MD**

**Ready for UAT: end of week · Gantt เสร็จพร้อม TimeLog หลัก**

**⚠️ Pre-work needed (before Mon)**:
- **Sit with Tech senior** · list all activity codes ทีมใช้จริง (10 default + P + others)
- Admin จะได้ seed migration ที่ถูก · Tech ไม่ต้องรอ Admin เพิ่ม code ระหว่าง pilot

---

## 12. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Technician ลืมกด End entry → in-progress ค้าง | **High** — Q9 บล็อก Complete ไม่ได้ | UI warning ทุก 30 นาที ถ้ามี IN_PROGRESS · ปุ่ม "End All Now" ตอน Complete · alert PC ถ้าค้างข้ามกะ |
| Q9 · Tech ลืม end หลายกะ → Complete ไม่ได้ | Med — friction แรก ๆ | Training ย้ำ · ปุ่ม End All Now ช่วย · เตือนตั้งแต่ก่อนกด Complete |
| Q8 · Gantt render ช้าถ้า entries เยอะ (100+) | Low — 30-40 ต่อ setup ปกติ | ใช้ CSS pure (no chart lib) · virtualize ถ้าเกิน 50 rows |
| Overlap entries (2 activities พร้อมกัน) | Low — สับสน | Allow with warning · Gantt view จะเห็นชัด |
| Photo upload ช้าเพราะ tablet network | Med — Tech รอ | Upload async · queue if network drop · retry |
| ต้องแปลง old paper record มา digital ก่อน pilot | Low — historical | ไม่จำเป็น · เริ่ม log ใหม่ตั้งแต่วัน pilot |
| **Fixed codes only** (Q2) · Tech เจอ activity ที่ไม่มี code → หยุดงาน | **High** — block work | **Pre-work**: สัมภาษณ์ Tech senior · list codes ทั้งหมด ทำ seed migration ก่อน pilot · Admin standby หา code missing during pilot |
| ผู้ใช้เก่าติดกับ Checklist mode (Q1 · replace) | High — change management | **UAT retrain session** · แจ้งล่วงหน้า · doc training ต้อง update ทันที · หัวหน้ากะ demo ให้ Tech ก่อน |
| Cycle Time / Volume (CT: 7803) — ไม่ได้อยู่ในแผนนี้ | Low | รอ requirement ชัดเจน · เพิ่ม field ทีหลัง |
| Time bar ruler (3-24, 1-3) — สร้าง Gantt ยาก | Low | Optional · deprioritize · text table อ่านง่ายพอ |

---

## 13. Open Questions (ต้องยืนยันกับทีม)

1. **แทนที่ Checklist template เดิม หรือให้เลือก mode?**
   - **Recommend**: Two modes toggle · Tech เลือก ("Checklist" for simple / "TimeLog" for real-time)
   - **Alternative**: Deprecate Checklist · TimeLog only (ระวังกระทบ ที่มีข้อมูลเก่า)

2. **Custom codes — Tech เพิ่มได้เอง หรือ Admin only?**
   - **Recommend**: Tech เพิ่ม "OTHER" ครั้งเดียวใน entry (free-text) · Admin ค่อย formalize เป็น code ใหม่จากรายการนั้น

3. **Photo compression?**
   - Camera phone ถ่าย 3-5 MB / รูป → Backend resize เป็น max 1024px width
   - **Recommend**: server-side resize · store both original + thumbnail

4. **Photo storage location?**
   - **Recommend**: On-prem `storage/setup-photos/{yyyy}/{mm}/{setupJobId}/{uuid}.jpg`
   - Phase 2 อาจย้ายไป MinIO/S3

5. **Cavity check — Tech ทำเอง หรือ QA ทำ?**
   - **Recommend**: Tech ทำแรก (initial check) · QA อาจ re-verify · 2 columns "tech_checked" / "qa_verified"
   - **Simplify Phase 1**: 1 result column · both roles สามารถ update ได้

6. **Overlap entries — บล็อกหรืออนุญาต?**
   - **Recommend**: อนุญาต with warning (real setup มี parallel เช่น heating + QA)

7. **CT (Cycle Time) และ ปริ (Volume) footer ในฟอร์ม — ต้องเก็บไหม?**
   - **Recommend**: **Phase 1 ไม่เก็บ** · รอ Tech/PC บอกว่าใช้ทำอะไร · phase 2 อาจเพิ่ม

8. **Timeline Gantt view — สำคัญไหม?**
   - **Recommend**: **Optional** · nice-to-have · ถ้าเวลาเหลือทำ · ไม่ block release

9. **Setup Job status "COMPLETED" — เมื่อไหร่ mark?**
   - **Recommend**: Tech กด Complete Setup Job (ปุ่มเดิม) · ระบบ auto-end entries ที่ IN_PROGRESS ด้วย end_time = now

10. **มี validation ให้ code M (Mold Change) ต้องมาคู่กับ Mold Code from/to ที่ SetupJob เดิมไหม?**
    - **Recommend**: Yes · ถ้ามี entry code=M → เตือน "อย่าลืมกรอก Mold Code ที่ SetupJob header"

---

## 14. Sample SQL

### 14.1 Summary per code for a setup
```sql
SELECT
  sac.code,
  sac.description_th,
  sac.color_hex,
  COUNT(stl.id) AS entry_count,
  SUM(stl.duration_min) AS total_min,
  ROUND(SUM(stl.duration_min) * 100.0 / SUM(SUM(stl.duration_min)) OVER (), 1) AS pct
FROM setup_time_log stl
JOIN setup_activity_code sac ON stl.activity_code_id = sac.id
WHERE stl.setup_job_id = ?
  AND stl.duration_min IS NOT NULL
GROUP BY sac.id, sac.code, sac.description_th, sac.color_hex
ORDER BY total_min DESC;
```

### 14.2 In-progress entries alert
```sql
SELECT stl.id, stl.setup_job_id, sac.code, stl.start_time,
       DATEDIFF(MINUTE, stl.start_time, SYSDATETIME()) AS running_min,
       u.username AS technician
FROM setup_time_log stl
JOIN setup_activity_code sac ON stl.activity_code_id = sac.id
JOIN users u ON stl.created_by = u.id
WHERE stl.end_time IS NULL
  AND stl.start_time < DATEADD(MINUTE, -30, SYSDATETIME());   -- Running > 30 min
```

### 14.3 Cross-setup avg per code per machine (for PC/Mgmt)
```sql
SELECT
  m.machine_name,
  sac.code,
  sac.description_th,
  COUNT(DISTINCT stl.setup_job_id) AS setup_count,
  AVG(stl.duration_min) AS avg_min_per_entry,
  SUM(stl.duration_min) AS total_min
FROM setup_time_log stl
JOIN setup_activity_code sac ON stl.activity_code_id = sac.id
JOIN machine_setup_job msj ON stl.setup_job_id = msj.id
JOIN machines m ON msj.machine_id = m.id
WHERE msj.plan_date BETWEEN ? AND ?
GROUP BY m.machine_name, sac.code, sac.description_th
ORDER BY m.machine_name, total_min DESC;
```

---

## 15. VSCode Prompts (Ready to Paste)

### Prompt 1: DB + Master Codes + Admin (0.75 MD)
```
Implement W19a.1 per docs/phase1/w19a-setup-timelog-plan.md §4:

CONSTRAINTS (locked decisions):
- Q2: Codes are Admin-managed ONLY. Tech has NO create endpoint.
- Q5: DO NOT include setup_cavity_check table.

1. Add Flyway V19__Create_setup_timelog_tables.sql (setup_activity_code + setup_time_log + setup_time_log_photo · NO cavity table)
2. Seed 10 default activity codes (M A S C Q Z R H B X) per §4.1
   NOTE: Custom code "P" (Purge) and others will be added by Admin post-deploy after interviewing Tech senior. Do NOT hardcode P.
3. Create SetupActivityCode entity + SetupActivityCodeRepository
4. Create SetupActivityCodeService with CRUD (Admin auth guard)
5. Create SetupActivityCodeController with two paths:
   - GET /api/setup-codes (any auth) — active codes only, for Tech dropdown
   - Admin CRUD under /api/admin/setup-codes (POST/PUT/PUT toggle-active)
6. Create SetupActivityCodeManager.jsx admin panel (add/edit/toggle-active · inline table)
7. Add menu entry in AdminDashboard: "🎨 Manage Setup Codes"
8. Unit tests § items 20-22 (Q2 guard tests)
9. Verify: GET /api/setup-codes returns 10 · Admin adds "P" via UI · re-fetch returns 11
```

### Prompt 2: TimeLog Backend + Image Resize (1 MD)
```
Implement W19a.2 per docs/phase1/w19a-setup-timelog-plan.md §5.1-5.4, §10:

CONSTRAINTS:
- Q3: Server-side image resize to 1024px + JPEG q=0.75 · target < 300 KB
- Q4: Storage path = ${app.storage.root}/setup-photos/{yyyy}/{mm}/{setupJobId}/{uuid}.jpg
  - Add property app.storage.root=/var/gdtahara/storage (default) in application.properties
- Q6: NO overlap validation between entries. Only ck_stl_duration (end > start) applies.

1. Create SetupTimeLog + SetupTimeLogPhoto entities per §5.1
2. Create repositories with:
   - findBySetupJobIdOrderBySequenceNo
   - summarizeByCode (JPQL native aggregation)
3. Create ImageResizeService per §5.2 — javax.imageio · 1024px + q=0.75 · strip EXIF
4. Create FileStorageService with resolveStoragePath(setupJobId, uuid) → Q4 path
5. Create SetupTimeLogService with:
   - addEntry (auto-compute duration if end_time set)
   - updateEntry (IDOR: only creator or PC; no overlap check)
   - deleteEntry (IDOR)
   - endInProgressEntry (POST /end)
   - summarizeByCode aggregation with %
   - uploadPhoto: resize → save file → DB row
   - deletePhoto: rm file + DB row
6. Add all endpoints to MachineSetupJobController per §7 (skip cavity)
7. Unit tests §10 items 1-17 (skip Q5 cavity items)
8. IT tests for photo upload multipart + resize verification
9. Verify: upload 3 MB photo → stored < 300 KB · path matches Q4 pattern
```

### Prompt 3: Cross-setup Reporting (0.25 MD · was Prompt 3)
```
Implement cross-setup summary per docs/phase1/w19a-setup-timelog-plan.md §7:

1. Add endpoint GET /api/pc/setup-summary?machineId=&technicianId=&from=&to=
   - Aggregate per code across multiple Setup Jobs in date range
   - Returns List<TimeSummaryDto>
2. Reuse SetupTimeLogService.summarizeByCode with extra filter params
3. Add controller test with 3 Setup Jobs on different machines
4. Postman collection update
```

### Prompt 4: Frontend TimeLog UI (1 MD)
```
Implement W19a.3 per docs/phase1/w19a-setup-timelog-plan.md §6:

CONSTRAINTS:
- Q1: REPLACE existing Checklist UI entirely. No mode toggle.
- Q2: Code dropdown is fixed list from GET /api/setup-codes. NO "Add custom" button.
- Q3: Optional client-side pre-resize (backend also resizes, so this is only for bandwidth)
- Q6: No overlap warning UI.

1. Modify SetupJobPanel.jsx:
   - Remove existing Checklist rendering entirely
   - Render TimeLog components in Complete dialog
   - Add read-only mode for old Setup Jobs that have checklist data (HistoricalChecklistView)
2. Create HistoricalChecklistView.jsx (for backward compat)
   - Displays legacy setup_job_steps data as read-only table
   - Shown when time_log entries = 0 AND checklist exists
3. Create SetupTimeLogTable.jsx per §6.1:
   - Show # · Code · Time · Duration · Description · 📷 badge
   - Row hover: Edit / Delete
   - IN_PROGRESS rows highlighted with "END NOW"
4. Create SetupTimeLogEntryForm.jsx per §6.2:
   - Code dropdown (from GET /api/setup-codes · with search)
   - Date + Start time pickers (🕐 Now buttons)
   - End time picker + "⏸ Still going" toggle
   - Auto-compute duration display
   - Description textarea
   - Photo upload (max 5 · client-side resize optional to 1024px)
5. Wire API calls through phase1Api.js
6. Real-time table updates after each save
7. Test with 10 entries + photos on H2 backend
```

### Prompt 5: Gantt view + Complete guard (0.75 MD · Q8+Q9)
```
Implement W19a.4 per docs/phase1/w19a-setup-timelog-plan.md §6.5, §8:

CONSTRAINTS:
- Q8: Gantt is REQUIRED (not optional) · for analysis by mgmt
- Q9: Complete Setup MUST BLOCK if IN_PROGRESS entries exist · user must end each manually

1. Backend: MachineSetupJobService.completeSetupJob():
   - Query time_log where setup_job_id=X AND end_time IS NULL
   - If count > 0 → throw ValidationException with detail: "{count} entries in progress: [id1, id2, ...]"
   - HTTP 400 response with entry IDs
   - Add unit tests §10 items 23-25

2. Frontend Complete guard:
   - Disable Complete button + tooltip "N in-progress" if any IN_PROGRESS
   - IN_PROGRESS section at top of TimeLog table (red border)
   - "END NOW" per row (existing) + "🛑 End All Now" bulk action
   - EndAllInProgressDialog.jsx: confirms count · sets end_time=now for all · then user retry Complete

3. SetupTimelineGantt.jsx per §6.5:
   - Horizontal bars · x-axis = time (auto-scale from setup earliest→latest)
   - Each entry = colored bar (color from activity_code.colorHex)
   - Overlapping entries stack vertically (row 2, 3, ...)
   - Hover tooltip: code · description · duration
   - IN_PROGRESS entries = striped pattern
   - Toggle button in SetupJobPanel: [📋 Table | 📊 Gantt]

4. Print-friendly CSS (styles/gantt-print.css):
   - @page { size: A4 landscape; margin: 10mm; }
   - Hide non-Gantt UI elements
   - Force bar labels visible

5. Test §10 items 23-28 pass (Complete guard + Gantt render)
```

### Prompt 6: Summary + Training Update (0.5 MD)
```
Implement W19a.5 per docs/phase1/w19a-setup-timelog-plan.md §6.3, §8:

1. Create SetupTimeSummary.jsx:
   - Table: Code · Description · Count · Total min · %
   - Horizontal bar chart (CSS-based · width = percentage · color = colorHex)
   - Total row at bottom
2. Embed SetupTimeSummary in SetupJobPanel below TimeLog table (always visible)
3. Update docs/training/02-technician-setup-flow.md:
   - REWRITE "Setup Job" section entirely — no more Checklist
   - New sections: "TimeLog · เริ่มต้น" / "การเพิ่ม entry" / "การแนบรูป" / "การดู summary" / "การ Complete (Q9)"
   - Add ⚠️ "หน้าจอเปลี่ยน" notice at top
   - Add ⚠️ Q9 warning: "ต้อง end ทุก entry ก่อน Complete"
4. Update docs/training/00-system-flow-by-role.md:
   - Rewrite Technician Mermaid flow
   - Show Q9 gate before Complete
5. E2E test full flow: create setup → add 10 entries → 3 IN_PROGRESS → try Complete (blocked) → End All → summary + Gantt render correct
6. Commit + push to feature/phase1-w11-backend
```

---

## Appendix A — Physical Form Reference

**Form**: `Blow M/C No. ___ (Set-up)` · from `RBL102QP350A set2 setup.pdf`
**Machine**: 102 · Product: QP350 → QP350M · Date: 30/07/2026
**File location**: `/root/.claude/uploads/b61dfbab-7115-5009-a046-a39750678a4e/3d799b1b-BarcodeTahara.pdf` (image #1 shared 2026-07-31)

**Sections**:
- Header (M/C · products · date · cavity check table)
- รหัส legend (10 codes)
- Time bar ruler (24h)
- Main activities table (Code · Time · Description) · ~30 rows for a full setup
- Footer notes: **"Setup (รายละเอียดย่อย) ต้องลงข้อมูลว่าอะไรคือปัญหาและปรับแก้ไขยังไง จุดนี้สำคัญ"**

---

## Appendix B — Related Documents

- `docs/phase1/w19-pallet-assembly-plan.md` — W19 Pallet (do AFTER this W19a)
- `docs/phase1/setup-job-logic.md` — existing setup job logic
- `docs/training/02-technician-setup-flow.md` — will be updated
- `docs/training/00-system-flow-by-role.md` — Technician flow needs update

---

**Document version**: v2.4 · 31 July 2026 · **Locked 10/10** ✅ + Continuous Recording confirmed
**Written for**: GDTAHARA Phase 1 W19a · Setup TimeLog Enhancement
**Priority**: 🔴 **High · do before W19 Pallet** (per user request)
**Status**: ✅ **Design FULLY LOCKED · ready for VSCode dev**

### Changelog
- **v2.2** (2026-07-31): Q7 locked as SKIP · added Form Header Linkage section
  - Q7 · ⏭️ SKIP — ข้อมูลไม่ได้บันทึกแยก field · Tech จดกระดาษต่อไป
  - Added §"Form Header Linkage" — clarifies machine/date auto-populate from SetupJob (per user question)
- **v2.1** (2026-07-31): Additional locks Q6, Q8, Q9, Q10
  - Q6 · ✅ Allow overlap · no warning (re-confirmed)
  - Q8 · ✅ Gantt view REQUIRED (not optional) · for analysis
  - Q9 · ❌ NO auto-end IN_PROGRESS · User must complete each entry manually · prevents late recording
  - Q10 · ❌ No Mold Code validation (Mold master not yet exists)
- **v2.0** (2026-07-31): Locked 5 open questions (Q1-Q5)
  - Q1 · REPLACE Checklist (single mode · no toggle)
  - Q2 · Fixed codes only · Admin-managed · NO user creation
  - Q3 · Server-side image resize (1024px + q=0.75 · < 300 KB)
  - Q4 · On-premise storage: `${app.storage.root}/setup-photos/{yyyy}/{mm}/{setupJobId}/{uuid}.jpg`
  - Q5 · ⏭️ SKIP Cavity check (Phase 2)
- **v1.0** (2026-07-31): Initial design with open questions
