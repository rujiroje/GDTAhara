# GDTAHARA MES — UAT Script Phase 1
**เวอร์ชัน:** 1.0  
**วันที่จัดทำ:** 2026-06-02  
**สาขา:** `feature/phase1-w11-backend`  
**ผู้จัดทำ:** IT / Development Team  

---

## สารบัญ

1. [ภาพรวมและ Pre-requisites](#1-ภาพรวมและ-pre-requisites)
2. [UAT-01 PC: Excel Plan Import](#uat-01-pc-excel-plan-import)
3. [UAT-02 PC: Plan CRUD + Shift-Split](#uat-02-pc-plan-crud--shift-split)
4. [UAT-03 Operator: Run Card v2](#uat-03-operator-run-card-v2)
5. [UAT-04 Operator: Track-Out (Barcode Scan)](#uat-04-operator-track-out-barcode-scan)
6. [UAT-05 Label / Barcode Preview](#uat-05-label--barcode-preview)
7. [UAT-06 Technician: Setup Job](#uat-06-technician-setup-job)
8. [UAT-07 Security & Role Control](#uat-07-security--role-control)
9. [Sign-off](#9-sign-off)
10. [Known Limitations](#10-known-limitations)

---

## 1. ภาพรวมและ Pre-requisites

### 1.1 Environment

| รายการ | ค่า |
|--------|-----|
| URL Frontend | `http://<server>:5173` (dev) หรือ `http://<server>/` (prod) |
| URL Backend API | `http://<server>:8080/api` |
| Browser ที่แนะนำ | Chrome 120+ / Edge 120+ |
| ขนาดจอขั้นต่ำ | 1366 × 768 px |

### 1.2 Test Accounts

| Username | Password | Role | ใช้ใน Scenario |
|----------|----------|------|----------------|
| `pc_test` | `Test1234!` | Production Control | UAT-01, UAT-02, UAT-05, UAT-07 |
| `op_test` | `Test1234!` | Operator | UAT-03, UAT-04, UAT-07 |
| `tech_test` | `Test1234!` | Technician | UAT-06 |
| `wrong_test` | `Test1234!` | QA | UAT-07 (ตรวจ 403) |

> ⚠️ **กรุณาสร้าง account ข้างต้นก่อน UAT** — ผ่านหน้า Admin หรือให้ IT seed ด้วย SQL

### 1.3 Master Data ที่ต้องมีก่อนเริ่ม

| ข้อมูล | ค่าตัวอย่าง | วิธีตรวจสอบ |
|--------|-------------|--------------|
| Machine | `RBL101` (machine_code) | Admin → Machines |
| Product | `P-12345` (product_code), qty_per_box = 600 | Admin → Products |
| User accounts | ตามตาราง 1.2 | Admin → Users |
| Factory Config (สำหรับ Excel import) | `factoryCode = TAHARA` | ดู Known Limitations ข้อ 2 |

### 1.4 ไฟล์ทดสอบที่ต้องเตรียม

| ไฟล์ | วัตถุประสงค์ |
|------|--------------|
| `Plan_TAHARA_June2026.xlsx` | UAT-01 — ไฟล์ plan จริงจาก PC (format matrix วันคือคอลัมน์, machine คือแถว) |
| `Plan_TAHARA_June2026_update.xlsx` | UAT-01 — ไฟล์เดิมที่แก้ target บางวัน (ทดสอบ rowsUpdated) |

### 1.5 วิธี Login

1. เปิด browser ไปที่ URL Frontend
2. ใส่ **Username** และ **Password** ตามตารางบัญชีทดสอบ
3. กด **เข้าสู่ระบบ**
4. ตรวจสอบว่าหน้าจอ Dashboard ของ Role ที่ถูกต้องปรากฏขึ้น
5. มุมขวาบนต้องแสดงชื่อผู้ใช้และ Role

---

## UAT-01 PC: Excel Plan Import

**Role:** Production Control (`pc_test`)  
**วัตถุประสงค์:** ตรวจสอบการอัปโหลดไฟล์ Excel plan, ผลสรุปการนำเข้า, และประวัติ

### UAT-01A — นำเข้าไฟล์ครั้งแรก (Import ใหม่)

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | Login ด้วย `pc_test` | Dashboard Production Control ปรากฏ | | |
| 2 | กดปุ่ม **📥 นำเข้าแผน Excel** (มุมขวาบนของหน้า dashboard) | หน้า **PlanImportPanel** เปิดขึ้น, มีช่องเลือกไฟล์และช่อง Factory Code | | |
| 3 | กด **เลือกไฟล์ .xlsx** → เลือก `Plan_TAHARA_June2026.xlsx` | ชื่อไฟล์ปรากฏบนปุ่ม | | |
| 4 | ใส่ Factory Code = `TAHARA` ในช่อง (ไม่บังคับถ้า detect ได้เอง) | ช่อง Factory Code แสดงค่า `TAHARA` | | |
| 5 | กดปุ่ม **Import** | ปุ่มเปลี่ยนเป็น "กำลังนำเข้า…" + spinner หมุน | | |
| 6 | รอจนเสร็จ (≤ 10 วินาที) | การ์ดผลลัพธ์ปรากฏ: `status = SUCCESS` + chip สีเขียว | | |
| 7 | ตรวจสอบ **เพิ่มใหม่** (rowsAdded) | ตัวเลขตรงกับจำนวนแถวใน Excel ที่วันยังไม่ผ่านหรือยังไม่มีใน DB | | ระบุค่าที่คาดจากไฟล์ทดสอบ: `___` แถว |
| 8 | ตรวจสอบ **อัปเดต** (rowsUpdated) = 0 | ครั้งแรก ไม่มี update | | |
| 9 | ตรวจสอบ **ข้ามเพราะผ่านแล้ว** (rowsSkippedPast) | มีค่า ≥ 0 (วันที่ผ่านมาแล้ว) | | |
| 10 | กดแท็บ **ประวัติการนำเข้า** หรือเลื่อนดู import history ด้านล่าง | มี 1 รายการในตาราง: ชื่อไฟล์, TAHARA, จำนวน rows, เวลา, username `pc_test` | | |

### UAT-01B — อัปโหลดซ้ำ (Update existing)

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | กด **เลือกไฟล์ .xlsx** → เลือก `Plan_TAHARA_June2026_update.xlsx` (ไฟล์ที่แก้ target บางวัน) | ชื่อไฟล์ใหม่ปรากฏ | | |
| 2 | กด **Import** | Spinner หมุน ระหว่างอัปโหลด | | |
| 3 | ดูผล rowsUpdated | ค่า > 0 (แถวที่แก้ target) | | |
| 4 | ดูผล rowsAdded | ค่า = 0 หรือน้อยมาก (ไม่มีแถวใหม่) | | |
| 5 | กดไปหน้า **Dashboard** → เลือกวันที่ที่แก้ target | Plan แสดง targetQty ใหม่ตรงกับไฟล์ | | |

### UAT-01C — ไฟล์ผิดรูปแบบ / ไม่ระบุ Factory Code

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | เลือกไฟล์ .xlsx ที่ไม่ใช่ format plan (เช่น ไฟล์ข้อมูลอื่น) โดยไม่ใส่ Factory Code | ระบบคืน **422** — Alert สีแดง "กรุณาระบุ Factory Code หรือตรวจสอบไฟล์ Excel ให้ถูกต้อง" | | |
| 2 | ช่อง input กลับมา focus พร้อมรับไฟล์ใหม่ (ไม่ค้างสถานะ) | ช่อง input ปกติ ไม่มี spinner | | |

---

## UAT-02 PC: Plan CRUD + Shift-Split

**Role:** Production Control (`pc_test`)  
**วัตถุประสงค์:** ตรวจสอบการสร้าง/แก้ plan ผ่าน UI (manual) และการคำนวณ shift-split

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | ไปที่ **Production Control Dashboard → Manage Production Orders** | หน้าจัดการ Order ปรากฏ | | |
| 2 | กด **+ สร้างใบสั่งผลิตใหม่** (หรือ Create Plan) | Form สร้าง plan เปิดขึ้น | | |
| 3 | เลือก Machine = `RBL101`, Product = `P-12345`, วันที่ = วันพรุ่งนี้, Target = `1200`, Source = `manual` | ช่องกรอกข้อมูลได้ปกติ | | |
| 4 | กด **บันทึก** | รายการ plan ใหม่ปรากฏในรายการ, status = `draft` | | |
| 5 | กด plan ที่เพิ่งสร้าง → ดู **Shift Split** | แสดง dayTarget + nightTarget รวม = 1200 (ค่าเริ่มต้น 50/50 = 600/600) | | |
| 6 | แก้ Manpower Day Ratio = `0.60`, Manpower Night Ratio = `0.40` → บันทึก | dayTarget = 720, nightTarget = 480 | | |
| 7 | กลับไปหน้า Plan List → ดูปฏิทิน **ตารางแผนการผลิต** (Machine Schedule Calendar) | Plan วันพรุ่งนี้ปรากฏในช่องของ `RBL101` | | |
| 8 | ลอง Login ด้วย `wrong_test` (role = QA) → เข้า Manage Production Orders | ไม่เห็นปุ่ม "สร้าง" / redirect หรือ 403 | | Role control |

---

## UAT-03 Operator: Run Card v2

**Role:** Operator (`op_test`)  
**วัตถุประสงค์:** ตรวจสอบการเลือกใบสั่งผลิต, ดู target, ยืนยันกล่อง, และเห็น SubLot ใน tree

> **Pre-req:** มีใบสั่งผลิต status = `IN_PROGRESS` สำหรับ Machine `RBL101` วันนี้ (สร้างโดย PC ก่อน)

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | Login ด้วย `op_test` | Operator Dashboard ปรากฏ, เห็นรายการใบสั่งผลิต active | | |
| 2 | กด **เริ่มทำงาน** ของใบสั่ง `RBL101` | หน้า task choice ปรากฏ | | |
| 3 | กด **Run Card v2** | Run Card แสดง target, actual (= 0), เครื่อง RBL101 | | |
| 4 | ดูส่วน **ตรวจสอบ Sub-Lot** (SubLotTree) — ยังไม่มีกล่อง | ข้อความ "ยังไม่มีกล่อง" หรือ tree ว่าง | | |
| 5 | กรอก **Lot Number** = `LOT-0602-001` ใน Run Card | ช่อง lot lock ได้, ระบบแสดง Box No. = `1` | | |
| 6 | กด **ยืนยัน 1 กล่อง** (Confirm Box) | Box No. เพิ่มเป็น `2`, actual count เพิ่ม | | |
| 7 | กด **ยืนยัน 1 กล่อง** อีกครั้ง | Box No. เพิ่มเป็น `3` | | |
| 8 | ดู SubLotTree ด้านล่าง Run Card | เห็น 2 node: `…-B0001` และ `…-B0002`, แต่ละ node แสดง boxQty | | |
| 9 | กดที่ node `…-B0001` ใน SubLotTree | รายละเอียดกล่อง expand หรือ modal เปิด: subLotNumber, status, weight, pallet | | |
| 10 | ดูตัวเลข actual บน Run Card gauge/progress | actual = 2 × qty_per_box (= 1200 ชิ้น หาก qty_per_box = 600) | | |

---

## UAT-04 Operator: Track-Out (Barcode Scan)

**Role:** Operator (`op_test`)  
**วัตถุประสงค์:** ตรวจสอบการสแกน/พิมพ์เลขกล่อง, ดูรายละเอียด, กด Print Label

> **Pre-req:** มี sub-lot อย่างน้อย 1 กล่องจาก UAT-03 (เลขกล่อง `…-B0001`)

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | ไปที่ Operator Dashboard | เห็นปุ่ม **📦 Track-Out / สแกน Barcode** บริเวณมุมขวาบน | | |
| 2 | กดปุ่ม Track-Out | หน้า TrackOut Panel เปิด, TextField `autoFocus` (cursor อยู่ใน input ทันที) | | |
| 3 | พิมพ์เลขกล่อง `…-B0001` ที่ได้จาก UAT-03 แล้วกด **Enter** | การ์ดรายละเอียดกล่องปรากฏ: subLotNumber, status, boxQuantity, weightKg, pallet | | |
| 4 | ตรวจว่า input ถูก clear หลัง Enter | ช่อง input ว่าง, cursor กลับมา focus ที่ input (พร้อมสแกนต่อ) | | |
| 5 | ดู **ประวัติการสแกน** ด้านล่าง | มี 1 entry: เลขกล่อง + Chip สีเขียว "พบ: …" + เวลา | | |
| 6 | กดปุ่ม **Print Label** | ระบบส่งคำสั่งพิมพ์ → Snackbar สีเขียว "ส่งงานพิมพ์สำเร็จ" | | ⚠️ physical print รอ hardware (ดู Known Limitations) |
| 7 | ดู input หลัง Print | input ว่าง, cursor กลับมา focus ทันที (พร้อมสแกนกล่องถัดไป) | | |
| 8 | พิมพ์เลขกล่อง **ที่ไม่มีในระบบ** เช่น `XXXXXX` แล้วกด Enter | Snackbar สีแดง "ไม่พบกล่อง 'XXXXXX'" ปรากฏ ≤ 2 วินาที | | |
| 9 | ดูประวัติการสแกนหลังสแกนไม่เจอ | entry ที่ 2 เพิ่มขึ้น: เลขกล่อง + Chip สีแดง "ไม่พบ" | | |
| 10 | สแกนกล่อง `…-B0001` อีกครั้ง → กด **Mark Labeled** | Snackbar "บันทึก Labeled สำเร็จ", status ในการ์ดเปลี่ยนเป็น `LABELED` (ถ้า refresh) | | |
| 11 | สแกนเลขกล่อง `…-B0001` อีกครั้งหลัง Mark | รายละเอียดกล่องปรากฏ, status = `labeled` | | |

---

## UAT-05 Label / Barcode Preview

**Role:** Operator หรือ Production Control  
**วัตถุประสงค์:** ตรวจสอบ barcode image และ ZPL label ว่าแสดงข้อมูลถูกต้อง

> **Pre-req:** มี sub-lot อย่างน้อย 1 กล่องจาก UAT-03

### UAT-05A — Barcode Image

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | เข้า SubLotTree จาก Run Card v2 → คลิกที่กล่อง `…-B0001` | รายละเอียดกล่องปรากฏ | | |
| 2 | กดปุ่ม **ดู Barcode** หรือ icon barcode | รูปภาพ barcode (Code128) ปรากฏ ไม่ขาวเปล่า | | |
| 3 | ใช้สมาร์ทโฟนสแกน barcode บนจอ | สแกนได้ ค่าที่ได้ตรงกับ subLotNumber | | Optional: ขึ้นอยู่กับ device |

### UAT-05B — ZPL Label Preview

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | เข้า SubLotTree → กล่อง `…-B0001` → กดปุ่ม **ZPL** หรือ **ดู Label** | ข้อความ ZPL code ปรากฏในกล่อง text / modal | | |
| 2 | ตรวจว่า ZPL ขึ้นต้นด้วย `^XA` | บรรทัดแรกของ ZPL = `^XA` | | |
| 3 | Copy ZPL ทั้งหมด → เปิด [labelary.com/viewer.html](http://labelary.com/viewer.html) → Paste แล้วกด Render | รูป label ปรากฏ: subLotNumber, barcode, product name, qty, วันที่ | | ต้องใช้ internet |
| 4 | ตรวจ field บน label ที่ render: Product code/name | ตรงกับข้อมูลจริงใน DB | | |
| 5 | ตรวจ field: จำนวนชิ้น (qty) | ตรงกับ boxQuantity × 1 หรือ qty_per_box | | |

---

## UAT-06 Technician: Setup Job

**Role:** Technician (`tech_test`)  
**วัตถุประสงค์:** ตรวจสอบว่าการเปลี่ยน Product บนเครื่องสร้าง SetupJob และ DowntimeEvent อย่างถูกต้อง

> **Pre-req:** มีใบสั่งผลิต IN_PROGRESS บน RBL101 (product = `P-12345`)

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | Login ด้วย `tech_test` | Technician Dashboard ปรากฏ | | |
| 2 | เลือกเครื่อง `RBL101` | หน้า dashboard เครื่องปรากฏ, แสดงสถานะและ product ปัจจุบัน | | |
| 3 | ไปที่เมนู **Setup Job** / **เปลี่ยน Recipe/Product** | ฟอร์ม setup job ปรากฏ | | |
| 4 | เลือก Product ใหม่ (product อื่นที่ไม่ใช่ `P-12345`) | ระบบสร้าง Setup Job ใหม่ / แจ้งเตือน "กรุณา setup เครื่อง" | | |
| 5 | กด **เริ่ม Setup** (Start Setup) | สถานะเครื่อง = `SETUP`, Downtime Event ประเภท Setup เริ่มต้น | | |
| 6 | รอ 2–3 นาที (ไม่ต้องรอนาน — เพียงเพื่อบันทึก duration) | — | | |
| 7 | กด **Setup เสร็จแล้ว / Complete Setup** | Setup Job status = `COMPLETED`, Downtime Event มี endTime | | |
| 8 | ไปที่ Production Control Dashboard → ดู OEE ของ RBL101 วันนี้ | ช่วงเวลา Setup ปรากฏใน Downtime Breakdown, Availability ลดตาม | | |
| 9 | ตรวจสอบ Parameter Record — กด **บันทึก Parameter** ใน Technician Dashboard | Parameter บันทึกได้ปกติ, ปรากฏในประวัติ | | |

---

## UAT-07 Security & Role Control

**วัตถุประสงค์:** ตรวจสอบว่า role ผิดเข้าหน้าไม่ได้ และ session หมดอายุ redirect กลับ login

| # | การกระทำ | ผลที่คาดหวัง | Pass / Fail | หมายเหตุ |
|---|---------|-------------|-------------|---------|
| 1 | ไม่ login → เปิด URL `http://<server>/api/production-plans/date/2026-06-02` โดยตรง | ระบบคืน **401 Unauthorized** `{"error":"Authentication failed"}` | | |
| 2 | Login ด้วย `wrong_test` (role = QA) → พยายามไปหน้า Plan Import | ปุ่ม "นำเข้าแผน Excel" ไม่ปรากฏ (ซ่อนโดย role check) | | Front-end guard |
| 3 | Login ด้วย `wrong_test` → เรียก API `POST /api/production-plans/` โดยตรง (เช่น ผ่าน DevTools) | **403 Forbidden** | | Back-end guard |
| 4 | Login ด้วย `wrong_test` → เรียก `POST /api/sub-lots/confirm-box` โดยตรง | **403 Forbidden** | | |
| 5 | Login ด้วย `op_test` (role = Operator) → เรียก `GET /api/production-plans/date/2026-06-02` | **200 OK** (Operator มีสิทธิ์อ่าน plan) | | |
| 6 | Login ด้วย `op_test` → เรียก `POST /api/production-plans/` | **403 Forbidden** (Operator ไม่มีสิทธิ์สร้าง plan) | | |
| 7 | แก้ JWT token ใน localStorage ให้หมดอายุ (หรือรอ session หมด) → refresh หน้า | หน้า Login ปรากฏ / redirect อัตโนมัติ | | |

---

## 9. Sign-off

### 9.1 ผลสรุปการทดสอบ

| Scenario | จำนวน Step | Pass | Fail | Blocker | หมายเหตุ |
|----------|-----------|------|------|---------|---------|
| UAT-01 Excel Plan Import | 13 | | | | |
| UAT-02 Plan CRUD | 8 | | | | |
| UAT-03 Run Card v2 | 10 | | | | |
| UAT-04 Track-Out | 11 | | | | |
| UAT-05 Label/Barcode | 8 | | | | |
| UAT-06 Technician Setup Job | 9 | | | | |
| UAT-07 Security | 7 | | | | |
| **รวม** | **66** | | | | |

**สรุป:**
- ✅ Pass ทั้งหมด: _____ step
- ❌ Fail: _____ step
- 🔴 Blocker (ใช้งานไม่ได้ระดับ critical): _____ รายการ

**ความเห็นโดยรวม (Overall Opinion):**

☐ ผ่าน UAT — พร้อม pilot  
☐ ผ่าน UAT มีเงื่อนไข — แก้ไข Fail ที่ระบุแล้วค่อย pilot  
☐ ไม่ผ่าน UAT — ต้องแก้ Blocker ก่อน

---

### 9.2 ลายเซ็นผู้ทดสอบ

| บทบาท | ชื่อ | วันที่ | ลายเซ็น |
|-------|------|--------|---------|
| Production Control | | | |
| Production Control | | | |
| Operator | | | |
| Technician | | | |

### 9.3 ลายเซ็นผู้อนุมัติ

| บทบาท | ชื่อ-นามสกุล | ตำแหน่ง | วันที่ | ลายเซ็น |
|-------|-------------|---------|--------|---------|
| Production / PD Manager | | | | |
| IT / Project Lead | | | | |

---

## 10. Known Limitations

ข้อจำกัดที่ทราบในเวอร์ชันนี้ — **ไม่ถือเป็น Fail** ใน UAT ที่ระบุ (*):

### 10.1 Physical Label Print รอ Hardware (W18)

| รายการ | รายละเอียด |
|--------|-----------|
| **สถานะ** | 🟡 Pending — รอติดตั้ง hardware |
| **Scenario ที่กระทบ** | UAT-04 Step 6 (Print Label) |
| **พฤติกรรมปัจจุบัน** | ระบบส่ง POST `/api/labels/sub-lots/{id}/print` สำเร็จ (200) แต่ physical printer ยังไม่รับงาน |
| **เหตุผล** | Label Printer driver/TCP mode จะพัฒนาใน **W18** (`printerTarget` mode = `tcp://printer-host:9100`) |
| **Workaround** | ใช้ ZPL Preview + [labelary.com](http://labelary.com) ตรวจสอบ layout แทน |

### 10.2 Plan Sheet Template — Factory L1 Detection

| รายการ | รายละเอียด |
|--------|-----------|
| **สถานะ** | 🟡 Pending — ต้อง seed factory config |
| **Scenario ที่กระทบ** | UAT-01 Step 4 (Excel Import โดยไม่ใส่ Factory Code) |
| **พฤติกรรมปัจจุบัน** | ถ้าไม่ระบุ `factoryCode` และ file header ไม่มีชื่อ factory ชัดเจน → 422 |
| **เหตุผล** | `plan_sheet_template` table (สร้างโดย Flyway migration) ต้องมีข้อมูล `factory_code = TAHARA` และ `sheet_name_pattern` ก่อน |
| **Workaround** | ใส่ `factoryCode = TAHARA` ในช่อง Factory Code ทุกครั้งระหว่าง UAT |

### 10.3 diff_qty ใน Production Report

| รายการ | รายละเอียด |
|--------|-----------|
| **สถานะ** | 🟡 Display only |
| **Scenario ที่กระทบ** | UAT-02, UAT-03 (ส่วนแสดงผลรายงาน) |
| **พฤติกรรมปัจจุบัน** | `diff_qty` อาจแสดง `null` หรือ 0 ในบาง environment ที่ไม่ใช่ SQL Server (computed column) |
| **Workaround** | คำนวณด้วยตนเอง: `diff_qty = target_qty - actual_qty` |

### 10.4 OEE Calculation ต้องมี MachineStatusLog + Recipe

| รายการ | รายละเอียด |
|--------|-----------|
| **สถานะ** | 🔵 Dependency |
| **Scenario ที่กระทบ** | UAT-06 Step 8 |
| **พฤติกรรมปัจจุบัน** | OEE แสดง `"-"` ถ้าไม่มีข้อมูล MachineStatusLog หรือยังไม่ตั้ง Recipe |
| **วิธีแก้** | Technician ต้องบันทึกสถานะเครื่อง และ PC ต้องตั้ง Recipe (ideal_cycle_time) ก่อนดู OEE |

---

*เอกสารนี้จัดทำสำหรับ UAT รอบแรก (Pilot Phase 1) — ปรับปรุงหลัง UAT เสร็จสิ้น*
