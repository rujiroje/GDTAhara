# 01 — Operator: เริ่มกะ ถึงจบกะ (5 Tasks)

**สำหรับ**: พนักงานประจำเครื่อง (Operator)
**ใช้เวลาเรียน**: ~30 นาที
**ต้องมีก่อน**: บัญชี user + role = Operator
**⚠️ อัปเดต 3 ส.ค. 2026**: OperatorDashboard = **2-level menu** · เลือก WO → เลือก 1 ใน **5 tasks**

---

## 🗂️ ภาพรวม OperatorDashboard = 2-level menu

```
🌅 Login → เข้าหน้า Operator
     ↓
📋 Level 1 · Select Report (WO ที่ active) — เลือก WO เครื่องตัวเอง
     ↓
🎯 Level 2 · Select Task — 5 ปุ่ม:
     ├─ ❌ บันทึกของเสีย (NG Recording)
     ├─ 📦 บันทึกการบรรจุ (Packaging) — RunCard + Confirm Box
     ├─ 🔁 พิมพ์ซ้ำ Label (Reprint Barcode)
     ├─ 🔦 Track-Out (สแกน mark labeled)
     └─ 🎁 ประกอบ Pallet (Pallet Assembly) ✨ W19 ใหม่
```

**Header หลังเลือก WO**: แสดง `เครื่อง · สินค้า` + ปุ่ม **← กลับ** เพื่อเปลี่ยน WO

---

## 1️⃣ Login + Level 1 · Select Report

### ขั้นตอน
1. เปิด browser · ไปที่ URL ระบบ · Login
2. เข้าสู่ **OperatorDashboard** — เห็น **grid ของ WO ที่กำลังผลิต** (color blocks)
3. คลิก card WO ที่ตัวเองประจำ → เข้า Level 2 (select_task)

### ⚠️ ถ้าไม่เห็น WO ที่ควรมี
- **PC ยังไม่ import plan** → แจ้ง PC
- **Tech ยังไม่ Complete Setup Job** → รอ Tech (หรือทวง)
- **WO ผิดกะ** → เช็ค plan_date + shift ที่ PC

---

## 2️⃣ Level 2 · Select Task (5 ปุ่ม)

หลังคลิก WO เห็น 5 tasks:

| # | Task | ใช้เมื่อ |
|---|---|---|
| 1 | ❌ **บันทึกของเสีย (NG Recording)** | ระหว่างผลิต · เจอชิ้นเสีย |
| 2 | 📦 **บันทึกการบรรจุ (Packaging)** | ครบ 1 กล่อง → confirm + สร้าง sub-lot |
| 3 | 🔁 **พิมพ์ซ้ำ Label (Reprint)** | Label เดิมหาย/ฉีก |
| 4 | 🔦 **Track-Out** | สแกน barcode label · mark labeled |
| 5 | 🎁 **ประกอบ Pallet** ✨ W19 | รวม sub-lots เป็น pallet · print label QP-PD-002-F046 |

**สลับ tasks ได้ตลอด** — กด "← กลับ" เพื่อเปลี่ยน task หรือเปลี่ยน WO

---

## 3️⃣ ❌ Task: บันทึกของเสีย (NG Recording)

**ใช้เมื่อ**: ระหว่างผลิต · พบชิ้นเสีย (defect) · ต้องบันทึกทันที

### ขั้นตอน
1. เลือก task **"บันทึกของเสีย"** → เข้าหน้า NgRecording
2. เห็น **grid ปุ่ม NG types** (จาก master data · อาจมี 10-20 types)
   - แต่ละปุ่มมีสีตาม category
   - เช่น: `เกรดฟัน` · `บ้างจอ` · `ไม่รอน` · `ปริมาณตก` · `Bubble` · `Contamination` · ...
3. เจอของเสีย → **แตะปุ่ม NG type ที่ตรง**
4. Modal เปิด · กรอก **จำนวน (ชิ้น)**
5. กด **บันทึก** → toast "บันทึกของเสียสำเร็จ"

### 🅾 "Other" NG (ถ้าไม่มี type ที่ตรง)
1. กดปุ่ม **"Other"** (ถ้ามี)
2. Dialog: `บันทึกของเสีย — Other`
3. กรอก:
   - **รายละเอียดปัญหา ***
   - **จำนวน (ชิ้น) ***
4. กด บันทึก

### 🚨 แจ้งเครื่องจักรหยุด (Alert dialog)
1. กดปุ่ม **"แจ้งเครื่องหยุด"** (ในหน้า NG)
2. Dialog เปิด · กรอก **เหตุผล** (reason)
3. กด **แจ้ง** → LD (Shift Leader) เห็น notification ทันที
4. LD กด "รับทราบ" · ส่งช่างมาแก้

### ⚠️ ข้อควรระวัง
- **บันทึกทันทีที่เจอ** — ห้ามรวมนับก้อนแล้วบันทึกทีเดียว
- **จำนวนต้อง > 0**
- Source ใน DB = **"Operator"** (แยกจาก QA_Process / Technician / ShiftLeader)
- PC เห็นใน BlowDailyReport Section 5 (group by source)

---

## 4️⃣ 📦 Task: บันทึกการบรรจุ (Packaging) — Lot + Label

**ใช้เมื่อ**: ผลิตครบ 1 กล่อง · ต้องบันทึก + พิมพ์ Tag Barcode ติดกล่อง

### ขั้นตอน
1. เลือก task **"บันทึกการบรรจุ"** → เข้าหน้า **PackagingRecording**
2. หน้าจอแสดง:
   - **Lot Number** (auto-fill จากเครื่องจักร · รูปแบบ `yymmdd-{เครื่อง}` เช่น `260814-RBL101`)
   - **Box No.** (ตัวเลขกล่องถัดไป · แสดงเพื่อดูเท่านั้น)
   - ปุ่ม **"บันทึก 1 กล่อง"** สีน้ำเงิน

### บันทึก 1 กล่อง
1. ตรวจ **Lot Number** ให้ถูกต้อง (auto-fill จากเครื่อง · แก้ได้ถ้าจำเป็น)
2. กด **"บันทึก 1 กล่อง"**
3. ระบบ:
   - Server สร้าง **Box No.** อัตโนมัติ (เรียงลำดับ · ป้องกันซ้ำกัน)
   - Dialog **Preview Label** เปิดขึ้น → เห็น Tag Barcode ก่อนพิมพ์
4. ตรวจ label ใน preview · กด **"พิมพ์ / Save as PDF"**
5. Label ออกมาเป็น PDF → ส่งพิมพ์ติดกล่อง

### 🏷️ รูปแบบ Tag Barcode

| สินค้า | ขนาด Tag | รายละเอียด |
|---|---|---|
| ทั่วไป (Standard) | **4" × 4"** | Logo · Barcode · ข้อมูลกล่อง · มอก. |
| ส่งออกต่างประเทศ (Export) | **4" × 8"** | ส่วน Standard + ส่วน **GOODS** ต่อด้านล่าง |

**Barcode value**: `{รหัสสินค้า}-{Lot Number}-{เลขกล่อง 3 หลัก}`
ตัวอย่าง: `EBAJMY0130A-260814-RBL101-005`

**ส่วน GOODS (เฉพาะ Export)**:
1. Supplier name: TOYO SEIKAN (THAILAND) CO.,LTD
2. Quantity: {จำนวน/กล่อง} pcs/case
3. Loading date: ......... (กรอกมือหลังพิมพ์)
4. Origin: Thailand

### Lot Number
- **อย่าแก้ Lot Number** หลังเริ่มบันทึกแล้ว (กด "แก้ไข" ได้ แต่ต้องแน่ใจ)
- ถ้าเปลี่ยน Lot → กล่องใหม่จะเริ่มนับ Box No. จาก 1 ใหม่สำหรับ Lot นั้น
- Lot เดิมยังอยู่ใน DB — ไม่หาย

### ⚠️ ข้อควรระวัง
- **Server กำหนด Box No.** — ห้ามแก้เลขกล่องเอง
- ถ้า 2 คนบรรจุจากเครื่องเดียวกัน (2 สายพาน) → กด พร้อมกันได้ · ระบบแจก Box No. ต่างกันอัตโนมัติ
- **พิมพ์ label ทันที** หลัง preview — อย่าปิด dialog ก่อนพิมพ์ (ต้องใช้ Reprint ถ้าพลาด)

---

## 5️⃣ 🔁 Task: พิมพ์ซ้ำ Label (Reprint Barcode)

**ใช้เมื่อ**: Label หาย/ฉีก · ต้องพิมพ์ใหม่

### ขั้นตอน
1. เลือก task **"พิมพ์ซ้ำ Label"** → เข้าหน้า ReprintBarcode
2. **Filter by Lot Number** — dropdown เลือก Lot ที่มี sub-lots
3. เห็นตาราง: `กล่องที่ · เวลาบรรจุ · ผู้บรรจุ`
4. คลิก sub-lot ที่ต้องการพิมพ์
5. **Dialog: ตัวอย่าง Label** — preview
6. กด **พิมพ์** → ส่ง ZPL ไป printer
7. Audit log บันทึก reprint (print_count += 1)

### ⚠️ ข้อควรระวัง
- PC ตรวจย้อนได้ว่ากล่องไหน reprint กี่ครั้ง
- ถ้า reprint เยอะผิดปกติ → PC/QA มาตรวจ

---

## 6️⃣ 🔦 Task: Track-Out (สแกน mark labeled)

**Track-Out = ยืนยัน "กล่องนี้ติด label แล้ว พร้อมออกจากไลน์"**

### ขั้นตอน
1. เลือก task **"Track-Out"** → เข้าหน้า TrackOutPanel
2. Cursor อยู่ที่ช่อง **"เลขกล่อง / Sub-Lot Number"** (placeholder: "สแกนหรือพิมพ์แล้วกด Enter…")
3. **2 mode**:
   - 🔦 **มี scanner (post-pilot)**: ยิงบาร์โค้ดที่ label → auto-fill
   - ⌨️ **ไม่มี scanner (pilot ตอนนี้)**: **พิมพ์เลข 12 หลักด้วยมือ** → กด Enter
4. ระบบตรวจ **Mod-10 check digit** — ผิด 1 หลัก → error ทันที
5. หน้าจอแสดง Sub-Lot info: จำนวน · น้ำหนัก · Pallet · Lot
6. กด **"Mark Labeled"** → status `open` → `labeled` ✅
7. กล่องนี้พร้อม scan เข้า pallet (task 5)

### ⚠️ ข้อควรระวัง
- **ต้อง Track-Out ก่อน Pallet Assembly** · pallet scan จะ reject กล่อง status = open
- Mark Labeled ซ้ำได้ (idempotent · ไม่เปลี่ยนสถานะแล้ว)

---

## 7️⃣ 🎁 Task: ประกอบ Pallet (Pallet Assembly) ✨ W19

**ดูรายละเอียดใน [08-operator-pallet-assembly.md](./08-operator-pallet-assembly.md)** — เอกสารแยกเพราะเนื้อหาเยอะ

### สรุปสั้น
1. เลือก task **"ประกอบ Pallet"** → เข้า PalletAssemblyPanel
2. **สร้าง Pallet ใหม่** (เลข P001) · หรือเลือก pallet ที่เปิดอยู่
3. **สแกน barcode** กล่องเข้า pallet (ต้อง Track-Out ก่อน)
4. **ปิด Pallet** (Close) → lock
5. **พิมพ์ Label** QP-PD-002-F046 → ติดบน pallet
6. **Rearrange** ได้ (แก้ pallet เก่า → สร้างใหม่ P001A)

---

## 🌊 Flow ประจำวัน — Timeline ตัวอย่าง

```
🌅 08:00 · เริ่มกะ · Login → Select WO (เครื่อง TAHARA-101)
   ↓
🏭 08:00-10:00 · ผลิตเรื่อย ๆ
   • ครบ 1 กล่อง → Task 📦 Packaging → Confirm Box
   • ครบ 1 กล่อง → Task 📦 Packaging → Confirm Box
   • เจอเสีย 3 ชิ้น → Task ❌ NG → tap type → บันทึก 3
   • ครบ 1 กล่อง → Task 📦 Packaging → Confirm Box
   ↓
📦 10:30 · ครบ 5 กล่อง (Sub-Lot สร้างแล้วทั้ง 5)
   → Task 🔦 Track-Out → พิมพ์เลข 12 หลัก × 5 กล่อง → Mark Labeled ✅✅✅✅✅
   ↓
🎁 11:00 · กล่องพอทำ pallet
   → Task 🎁 Pallet Assembly → สร้าง P001 → สแกน 5 กล่อง → ปิด → พิมพ์ label
   ↓
🏭 11:30-15:00 · ผลิตต่อ · repeat cycle
   ↓
🚨 15:30 · เครื่องหยุด (breakdown)
   → Task ❌ NG → กด "แจ้งเครื่องหยุด" → กรอกเหตุ → แจ้ง LD
   ↓
🔧 16:00 · ช่างซ่อมเสร็จ · เดินเครื่องต่อ
   ↓
🌆 17:00 · จบกะ · ปิด browser (data ปลอดภัยใน DB)
```

---

## 🆘 Quick Fix

| อาการ | ทำก่อน | ถ้ายังไม่หาย |
|---|---|---|
| Login ไม่ผ่าน | ตรวจ Caps Lock · password ถูก | แจ้งหัวหน้ากะ |
| ไม่เห็น WO ที่ควรมี | Refresh · เช็ค role Operator | PC/Tech ยังไม่พร้อม |
| กด "บันทึก 1 กล่อง" แล้ว error | ตรวจ Lot Number ไม่ว่าง | Screenshot + แจ้ง IT |
| label popup ไม่เปิด / รูปไม่ขึ้น | อนุญาต popup ในบราวเซอร์ · Refresh | แจ้ง IT |
| พิมพ์ label แล้วขนาดผิด | เลือก "Actual size" ใน print dialog (ไม่ใช่ Fit) | ตั้งค่า printer ใหม่ |
| NG type ไม่มีที่ต้องการ | ใช้ "Other" + description | แจ้ง Admin เพิ่ม type |
| Track-Out พิมพ์เลขแล้ว "not found" | เช็คว่าเลข 12 หลักครบ | เช็ค Sub-Lot Tree ก่อนว่ามีจริง |
| Pallet scan reject | เช็คว่า Track-Out แล้ว | ต้อง Mark Labeled ก่อน |
| หน้าจอค้าง / โหลดช้า | Refresh (F5) · ปิดแท็บอื่น | ปิด browser · เปิดใหม่ |

---

## ⛔ ข้อห้าม

1. ❌ **อย่าใช้ account คนอื่น** — audit ผิด
2. ❌ **อย่ารวม NG นับก้อน** — timestamp ผิด
3. ❌ **อย่าบันทึกกล่องซ้ำ** — เช็ค Sub-Lot Tree ก่อน
4. ❌ **อย่า Track-Out ก่อนติด label** — ระบบเชื่อว่าติดแล้ว
5. ❌ **อย่า mix product ใน pallet เดียว** — ระบบ block

---

## 📞 ช่องทางแจ้งปัญหา

- **LINE Group**: GDTahara Pilot
- **P0 (login ไม่ได้ / บันทึกไม่ได้ทั้งระบบ)**: โทร IT + PC ทันที

```
🐛 [แจ้งปัญหา Operator]
Role: Operator
เครื่อง: TAHARA-___
กะ: D / N   วันที่: __/__/____
Task ที่ใช้อยู่: (NG / Packaging / Reprint / Track-Out / Pallet)
เลข WO / Sub-Lot / Pallet: ______
อาการ: ...
Screenshot: (แนบรูป)
```

---

## ✅ ผ่าน Training นี้ควรทำได้

- [ ] Login → Select WO (Level 1)
- [ ] เข้าใจ 5 tasks (Level 2) และเลือกใช้ให้ตรง
- [ ] ❌ บันทึก NG · แตะปุ่ม type · Other · แจ้งเครื่องหยุด
- [ ] 📦 Packaging · กด "บันทึก 1 กล่อง" → preview label → พิมพ์ PDF ติดกล่อง
- [ ] รู้จัก Lot Number format (`yymmdd-{เครื่อง}`) และ Barcode format
- [ ] รู้ว่า Export product มี GOODS section เพิ่มด้านล่าง (4"×8")
- [ ] 🔁 Reprint Label
- [ ] 🔦 Track-Out · สแกน หรือ พิมพ์เลข · Mark Labeled
- [ ] 🎁 Pallet Assembly (ดู 08 ต่อ)
- [ ] เข้าใจ flow: Confirm Box → Track-Out → Pallet
- [ ] แจ้งปัญหาด้วย template

---

## 🔗 เอกสารที่เกี่ยวข้อง

- **08 Pallet Assembly** — task 🎁 รายละเอียดเต็ม
- **07 Track-Out cross-role** — cross-role handoff
- **00 System Flow** — flow diagrams ทุก role

---

**เอกสารเวอร์ชัน**: v2.1 · 14 สิงหาคม 2026 (อัปเดต Task Packaging — label format, ขนาด 4"×4"/4"×8", GOODS section Export)
**ระบบเวอร์ชัน**: Phase 1 · W11
**เขียนสำหรับ**: GDTAHARA MES Pilot · Toyo Seikan (Thailand)
