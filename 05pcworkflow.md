# 05 — PC: Production Control (Plan · BOM · Report)

**สำหรับ**: Production Control (PC) และ Doc/Management ที่ต้องเข้าถึงข้อมูลผลิต
**ใช้เวลาเรียน**: ~40 นาที (ยาวสุดในชุด · ฟังก์ชันเยอะ)
**ต้องมีก่อน**: บัญชี user + role = Production Control / Management / Document

---

## ⚠️ Scope — สิ่งที่ PC ทำได้ vs ยังไม่ได้

### ✅ ทำได้ในตอนนี้ (Phase 1)
- ดู Dashboard เห็นทุกเครื่องที่เดินอยู่ (real-time gauge)
- Import แผน Excel รายสัปดาห์ (Weekly Plan) + สร้าง WO อัตโนมัติ
- จัดการใบสั่งผลิต (WO) manual
- ดู/นำเข้า BOM · คำนวณ Material Requirement · Variance Report
- ดูรายงานย้อนหลัง (Historical) · drill-down รายวัน/รายกะ
- ดู Blow Daily Report ครบส่วน (Section 1-5 + NG summary + Excel export)

### ⏳ Phase 2 จะเพิ่ม
- Production Planning UI (สร้างแผนในระบบ ไม่ต้อง Excel)
- Freeze period + APS
- Real-time Inventory Monitoring (FG/Semi)
- OEE / KPI Dashboard

---

## ภาพรวม — Role ของ PC ใน 1 สัปดาห์

```
📅 ต้นสัปดาห์
  ↓
  1. รับแผนผลิตจากลูกค้า/mkt → กรอกลง Excel
  2. Import Excel → ระบบสร้าง WO + Setup Job อัตโนมัติ
  3. ตรวจ Variance สัปดาห์ที่แล้ว
  ↓
🌅 ประจำวัน (D, N shift)
  ↓
  4. ดู Dashboard เห็นทุกเครื่องกำลังทำอะไร ผลิตเท่าไหร่
  5. เจอเครื่อง idle/breakdown → ตาม EN
  6. สิ้นกะ: ตรวจ BlowDailyReport ต่อเครื่อง
  ↓
📊 ประจำเดือน
  ↓
  7. คำนวณ Material Requirement จาก BOM (Q ถัดไป)
  8. Import BOM ใหม่ (SAP export monthly)
  9. ทำรายงาน Variance ส่ง Management
```

---

# Section A · Dashboard + Overview

## A1. หน้า Dashboard หลัก

### ขั้นตอน
1. Login → เข้าสู่หน้า `pc-dashboard`
2. เห็น **หัวข้อ** "Production Overview"
3. **2 Menu groups** ด้านขวาบน:
   - 📋 **แผนการผลิต** (สีม่วง)
   - 🗂️ **BOM** (สีฟ้า)
4. Grid ด้านล่างแสดง **GaugeCard** — 1 การ์ดต่อ 1 WO ที่กำลังเดิน

### GaugeCard แต่ละใบแสดง
| ข้อมูล | ตัวอย่าง |
|---|---|
| Machine Name | TAHARA-101 |
| Product Name | PET-500ml-BLUE |
| Target Qty | 12,000 |
| Current Good | 8,547 |
| NG Qty | 123 |
| Status + สี | 🟢 Running / 🟡 Idle / 🔴 Breakdown / ⚪ Setup |

### วิธี drill-down
- **คลิกที่ GaugeCard** → เข้าสู่ **Detail View** ของ WO นั้น
- เห็น downtime events, material logs, NG logs, packaging, ประวัติต่าง ๆ

### ⚠️ ถ้าเห็น "ยังไม่มีเครื่องกำลังเดิน"
- ระบบยังไม่มี active WO
- ไปที่ **แผนการผลิต → จัดการใบสั่งผลิต** เพื่อสร้าง WO manual (ถ้าจำเป็น)

---

# Section B · แผนการผลิต (📋 สีม่วง)

## B1. 📥 นำเข้าแผน Excel (Plan Import)

**สำคัญที่สุดของ PC — สร้าง WO ทั้งสัปดาห์ในครั้งเดียว**

### ขั้นตอน
1. Dashboard → 📋 แผนการผลิต → **📥 นำเข้าแผน Excel**
2. หน้า **PlanImportPanel** โหลดขึ้น
3. เตรียมไฟล์ Excel ตาม template TAHARA (col B = machine + product · row 6 = date)
4. Drag & drop หรือกด **เลือกไฟล์**
5. รอ 5-30 วินาที ระบบ parse
6. เห็น **result summary**:
   - จำนวน WO ที่สร้าง
   - จำนวน error (ถ้ามี — เช่น machine ไม่รู้จัก)
7. ระบบสร้างอัตโนมัติ:
   - **Production Plans** ตามวันในแผน
   - **Work Orders** (WO) ต่อวัน/เครื่อง/product
   - **MachineSetupJob** ทุกครั้งที่ product เปลี่ยน (auto-scan)

### 📜 Import History
- Panel เดียวกันมี **history table** — เห็น import ครั้งก่อน ๆ
- คลิกเข้าไปดูรายละเอียด: filename, uploaded by, timestamp, result count
- **Rollback**: ยังไม่มีใน Phase 1 — ถ้า import ผิดต้องแก้ WO ทีละใบ

### ⚠️ ข้อควรระวัง
- **ตรวจ Excel ก่อน import ทุกครั้ง** — เครื่องที่ไม่มีใน master จะ error
- **Import 2 ครั้งซ้ำ** = อาจสร้าง WO ซ้ำ — ระบบไม่ dedupe อัตโนมัติ
- Setup Job สร้างให้ auto — ไม่ต้องกดสร้างเอง

---

## B2. ⚙️ จัดการใบสั่งผลิต (Manage WO)

### เมื่อไหร่ใช้
- แผน Excel ผิด ต้องแก้ WO
- ต้องเพิ่ม WO ระหว่างสัปดาห์
- ยกเลิก WO ที่ยังไม่เริ่ม

### ขั้นตอน
1. 📋 แผนการผลิต → **⚙️ จัดการใบสั่งผลิต**
2. เห็น list ของ WO ทั้งหมด (filter by status: all / ACTIVE / INACTIVE)
3. คลิก WO ไหน → แก้ไข target, dates, notes
4. Save

### ⚠️ ข้อควรระวัง
- **แก้ WO ที่มี actual แล้ว** = ระวัง Variance report เพี้ยน
- ยกเลิก WO ที่ Operator กำลัง run = Operator เห็น error → แจ้งก่อนทุกครั้ง

---

## B3. 📅 ตารางแผนการผลิต (Machine Schedule)

**Calendar view รายเดือน** — เห็นเครื่องไหนทำอะไรวันไหน

### ขั้นตอน
1. 📋 แผนการผลิต → **📅 ตารางแผนการผลิต**
2. เห็น **grid** — rows = เครื่อง, columns = วันในเดือน
3. เลือน **prev / next month**
4. คลิก cell → เห็น detail ของ WO วันนั้น

### ใช้เมื่อ
- เช็คว่าเครื่องไหนว่าง วันไหน
- Balance load ระหว่างเครื่อง
- นำเสนอ mgmt ในภาพรวม

---

## B4. 📊 ดูรายงานย้อนหลัง (Historical Reports)

### ขั้นตอน
1. 📋 แผนการผลิต → **📊 ดูรายงานย้อนหลัง**
2. เห็น **table รายชื่อ WO** ที่ปิดแล้ว (INACTIVE) — เรียงตามวันที่
3. Filter: machine, product, date range, status
4. คลิก WO ไหน → drill-down เห็น:
   - **Detail View** (สรุป WO)
   - **Daily Detail** (รายละเอียดรายวัน)
   - **Daily Shift Detail** (แยกกะ D/N)
   - **📄 Blow Daily Report** ปุ่มพิเศษ → เปิด BlowDailyReportPage (Section D)

---

# Section C · BOM (🗂️ สีฟ้า)

## C1. 📤 นำเข้า BOM (BOM Upload)

**ทำเดือนละครั้ง — หลังได้ SAP Export ใหม่**

### ขั้นตอน
1. Dashboard → 🗂️ BOM → **📤 นำเข้า BOM**
2. หน้า **BomUploadPage**
3. เตรียมไฟล์:
   - **FG_New.xlsx** (~5,266 rows) — BOM ของสินค้าสำเร็จรูป
   - **semi_new.xlsx** (~818 rows) — BOM ของ Semi-FG
4. Upload ไฟล์ทีละไฟล์
5. ระบบ parse + validate
6. เห็น summary: added/updated/errors

### ⚠️ ข้อควรระวัง
- **BOM ใหม่ทับของเก่า** — ถ้ามี product ที่ SAP ลบไปแล้ว มันจะ inactive แต่ไม่ลบ
- **ต้อง import ครบทั้ง 2 ไฟล์** (FG + Semi) ในรอบเดียวกัน — ไม่งั้น Semi ไม่ link
- Import ผิด → ติดต่อ Dev รอ rollback (ยังไม่มีปุ่ม rollback ตอนนี้)

---

## C2. 🔍 ดู BOM (BOM Viewer)

**อ่านสูตรผลิตของ product ตัวไหนก็ได้**

### ขั้นตอน
1. 🗂️ BOM → **🔍 ดู BOM (สูตรผลิต)**
2. Search product code / name
3. คลิก product → เห็นสูตร:
   - Material code / name
   - ประเภท (ROH / HIBE / VERP / UNBW / HALB)
   - Qty ต่อ 1 FG (Kg)
   - isScrap (ระบบซ่อนอยู่แล้ว)
4. หลาย level — expand Semi-FG ดูสูตรย่อย

### ใช้เมื่อ
- Debug ทำไม CM หา material ไม่เจอ (Master data ผิด)
- ตรวจสอบสูตรก่อน production
- คำนวณ cost ด้วยมือ (ระหว่างรอ report auto)

---

## C3. 📦 ความต้องการ RM (Material Requirement)

**คำนวณว่าต้องซื้อ / เตรียม RM เท่าไหร่ · จาก BOM × Target Qty ของ WO ทั้งหมด**

### ขั้นตอน
1. 🗂️ BOM → **📦 ความต้องการ RM**
2. หน้า **MaterialRequirementDashboard**
3. เลือก period (สัปดาห์ / เดือน)
4. ระบบคำนวณจาก **BOM × Target Qty** ของทุก active WO
5. เห็น **2 ตาราง**:

**ตาราง Summary** (per material)
| คอลัมน์ | ตัวอย่าง |
|---|---|
| Material code | PE-HDPE-001 |
| ชื่อ | HDPE Blue-natural |
| **รวมต้องการ** | 1,234.567 Kg |
| **Stock คงเหลือ** | 890.000 Kg |
| **ขาดอีก** | **344.567 Kg** ← สีแดง |

**ตาราง Detail** (per material × WO)
- เป้าผลิต WO
- qty/FG (จาก BOM)
- รวมต้องการต่อ WO

### ✨ W18 update
- **ทุกตัวเลขมี comma** แล้ว (fmt helper) — `1,234.567` แทน `1234.567`
- **คำนวณครบทุกประเภท** (ROH + HIBE + VERP + UNBW) — เพราะทุกตัวต้องมีในสต๊อก

### ใช้เมื่อ
- ตัดสินใจสั่ง RM (ทำเดือนละครั้ง)
- เช็คก่อนสัปดาห์ผลิต — RM พอไหม
- นำเสนอ mgmt "ขาด RM ทำแผนไม่ได้"

---

## C4. 📊 รายงาน Variance

**เทียบ planned vs actual — เจอ scrap / over/under-consumption**

### ขั้นตอน
1. 🗂️ BOM → **📊 รายงาน Variance**
2. หน้า **VarianceReportPage**
3. เลือก WO / period
4. เห็นตาราง:
   - Material · planned (BOM × actual FG) · actual (จาก CM stock-out) · variance (%)
   - Positive variance = ใช้เกิน BOM
   - Negative variance = ใช้ต่ำกว่า BOM (อาจ NG นับผิด)

### ใช้เมื่อ
- ตรวจว่า CM กรอกถูกไหม (variance สูงเกิน = สงสัย)
- หา room สำหรับปรับ BOM (ถ้า actual น้อยกว่า BOM ตลอด)
- Improvement KPI

---

# Section D · Blow Daily Report ✨ ใหม่ W18

## D1. เปิด Blow Daily Report

### ขั้นตอน
1. **Historical Reports** → คลิก WO → ปุ่ม **"📄 Blow Daily Report"**
2. หน้า **BlowDailyReportPage** โหลด — data มาจาก `DailyBlowReportService`
3. เห็น **5 sections**:

### Section 1: Header
- WO number · Machine · Product · Date · Shift

### Section 2: Production Summary
- Target Qty · Actual Qty · Good · NG · Yield %

### Section 3: Downtime Events
- Time start / end · duration · reason · category (planned / unplanned)

### Section 4: Material Consumption
- Material · lot (ROH) · planned · actual · variance

### Section 5: NG Summary ✨ ใหม่ (W18)
**เดิม**: แสดง 1 row ต่อ 1 NG event → ยาวมาก อ่านยาก
**ใหม่ (W18)**: **Group by ประเภท + คำอธิบาย + source · sum จำนวน · เรียงมาก→น้อย · Total row**

ตัวอย่าง:
| ประเภท | คำอธิบาย | Source | จำนวน |
|---|---|---|---|
| A | Contamination | Operator | 45 |
| B | Bubble | QA_Process | 22 |
| A | Contamination | QA_Process | 15 |
| C | Cap crack | Operator | 8 |
| — | **Total** | — | **90** |

### 📥 Export Excel
- ปุ่ม **"Export Excel"** ทำ .xlsx ตาม format เดียวกัน
- ใช้ส่ง mgmt / เก็บ archive

---

# Section E · Detail Views (Drill-down)

จาก Historical / Dashboard คลิก WO → **DailyDetail / DailyShiftDetail**

### DailyDetail
- ดูรายวัน — สรุปตามวัน (รวมทั้ง shift)
- **Downtime events table** · Material consumption · NG count

### DailyShiftDetail
- แยกกะ D / N
- นับ headcount ต่อกะ
- Supervisor ประจำกะ

### Filter + Search
- Filter by machine, product, order number, date range
- Search order number ตรง ๆ

---

## 🆘 เมื่อมีปัญหา — Quick Fix

| อาการ | ทำก่อน | ถ้ายังไม่หาย |
|---|---|---|
| Import แผน error "machine not found" | เช็ค machine code ตรงกับ master | เพิ่ม machine ใน master ก่อน |
| WO สร้างแล้ว Operator ไม่เห็น | เช็คว่าเลือกวัน + กะ ถูกต้อง | Refresh · เช็ค Setup Job สร้างครบไหม |
| BOM upload error | เช็ค column headers ตรง SAP export | Screenshot + แจ้ง Dev |
| Material Requirement ตัวเลขเพี้ยน | เช็คว่า BOM Q ล่าสุดถูก import แล้ว | ตรวจ product ที่สงสัยใน BOM Viewer |
| Blow Daily Report โหลดช้า | รอ 5-10 วิ (มี aggregation ใหญ่) | ปิด report เก่า · ลอง WO อื่น |
| Excel export ไม่ทำงาน | เช็ค pop-up blocker | ใช้ browser อื่น |

---

## ⛔ ข้อห้าม — อย่าทำ

1. ❌ **อย่า import BOM ตอน CM กำลัง stock-out** — data race
2. ❌ **อย่ายกเลิก WO ที่มี actual แล้ว** — variance report เพี้ยน
3. ❌ **อย่า import แผน 2 ครั้งซ้ำ** ในสัปดาห์เดียว — WO ซ้ำ
4. ❌ **อย่าลบ user ที่ยังมี log** — audit trail ขาด
5. ❌ **อย่าแก้ target qty ระหว่าง Operator run** — เขาจะสับสน

---

## 📞 ช่องทางแจ้งปัญหา (ระหว่าง Pilot)

- **LINE Group**: GDTahara Pilot
- **P0 (Dashboard ไม่โหลด / Import ทั้งชุด fail)**: โทร IT + Dev ทันที
- **แจ้งปัญหาทั่วไป**: LINE + screenshot + Menu path (เช่น "BOM → Viewer")

### รูปแบบแจ้งปัญหา (Copy ไปวาง)
```
🐛 [แจ้งปัญหา PC]
Role: Production Control
Menu / Page: __________ (เช่น BOM → Material Requirement)
เครื่อง / Product / WO: __________
อาการ: (สั้น ๆ ทำอะไรอยู่ แล้วเกิดอะไรขึ้น)
Screenshot: (แนบรูป)
```

---

## ✅ ผ่าน Training นี้แล้ว คุณควรทำได้

### พื้นฐาน
- [ ] Login + เข้า Dashboard เห็น gauge ทุกเครื่อง
- [ ] Drill-down คลิก gauge → Detail

### แผนการผลิต 📋
- [ ] Import แผน Excel + ตรวจ result summary
- [ ] จัดการ WO manual (เพิ่ม/แก้/ยกเลิก)
- [ ] ดูตารางเดือน (Schedule Calendar)
- [ ] เปิด Historical Reports + filter

### BOM 🗂️
- [ ] Upload BOM (FG + Semi)
- [ ] ดู BOM per product
- [ ] คำนวณ Material Requirement — อ่านตาราง Summary + Detail
- [ ] อ่าน Variance Report

### Reporting ✨
- [ ] เปิด Blow Daily Report + อ่าน 5 sections
- [ ] Export Excel

### Governance
- [ ] รู้ข้อห้าม 5 ข้อ (BOM import timing, WO cancellation, etc.)
- [ ] แจ้งปัญหาด้วย template

---

## 🎓 คำศัพท์ที่ต้องรู้

| ศัพท์ | ความหมาย |
|---|---|
| **WO** | Work Order — ใบสั่งผลิต 1 batch ต่อ 1 record |
| **Production Plan** | แผนรายวันต่อเครื่อง (parent ของ WO) |
| **Setup Job** | งาน Setup เครื่อง — ระบบ auto สร้างเมื่อ product เปลี่ยน |
| **Parent Lot** | Lot ระดับกะ (per machine + day + shift) |
| **Sub Lot** | กล่อง 1 ใบ = 1 sub lot (12 หลัก + Mod-10) |
| **BOM** | Bill of Materials — สูตรวัตถุดิบ |
| **Variance** | ส่วนต่าง planned vs actual |
| **GaugeCard** | การ์ดหน้า dashboard แสดงเครื่อง 1 ตัว |
| **canManage** | Role permission — คนแก้แผนได้ (PC + Admin) |
| **Yield %** | (Good / Target) × 100 |

---

**เอกสารเวอร์ชัน**: v1.0 · 31 กรกฎาคม 2026
**ระบบเวอร์ชัน**: Phase 1 · commit `bb6da49` (W18)
**เขียนสำหรับ**: GDTAHARA MES Pilot · Toyo Seikan (Thailand)
