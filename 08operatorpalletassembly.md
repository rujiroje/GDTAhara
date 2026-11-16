# 08 — Operator: Pallet Assembly (W19 ✨ ใหม่)

**สำหรับ**: Operator (คนประจำเครื่อง) · หลังจากผ่าน training 01 มาแล้ว
**ใช้เวลาเรียน**: ~20 นาที
**ต้องมีก่อน**: บัญชี user + role = Operator
**⚠️ Feature ใหม่ W19 (สค. 2026)** — แทนการเขียนป้ายกล่อง QP-PD-002-F046 ด้วยมือ

---

## 🎯 Pallet Assembly คืออะไร

**"เอา sub-lot (กล่อง) หลาย ๆ ใบมารวมเป็น 1 pallet เพื่อส่งคลัง · ระบบเก็บ trace ครบ · พิมพ์ label ตามฟอร์มเดิม"**

- **1 pallet = หลาย sub-lots** (จาก lot เดียว หรือหลาย lot ได้)
- **Sub-lot ต้อง Track-Out (labeled) ก่อน** ถึงจะ scan เข้า pallet ได้
- **Rearrange support** · เอา pallet เก่ามาแก้ เมื่อของเสีย/split shipping

---

## ภาพรวม — งานประกอบ Pallet

```
🏭 ผลิตกล่อง → Confirm Box → Track-Out (mark labeled)
  ↓ (ต้อง Track-Out ก่อน)
📦 กด "Pallet Assembly" → สร้าง Pallet ใหม่ / เลือกที่เปิดอยู่
  ↓
🔦 สแกน barcode ที่ label กล่อง → เข้า pallet
  ↓ (repeat จนครบ)
🔒 กด "ปิด Pallet" + ตรวจก่อน
  ↓
🖨️ พิมพ์ label QP-PD-002-F046 → ติดบน pallet · ส่งคลัง
```

---

## 1️⃣ เข้าเมนู Pallet Assembly

### ขั้นตอน
1. Login → OperatorDashboard
2. คลิกเมนู **"🎁 Pallet Assembly"** ทางซ้าย
3. เข้าสู่ **PalletAssemblyPanel**
4. Header แสดง: **เครื่อง** · **สินค้า** (จาก WO ที่ active)

### สิ่งที่เห็น (subView = "home")
- ปุ่ม **"+ สร้าง Pallet ใหม่"**
- List pallets วันนี้ · แสดง:
  - เลข Pallet · จำนวนกล่อง · จำนวนขวด
  - Status: OPEN 🟡 / CLOSED 🔵 / PRINTED 🟢 / CANCELLED ❌
  - สร้างโดย · timestamp

---

## 2️⃣ สร้าง Pallet ใหม่

### ขั้นตอน
1. กด **"+ สร้าง Pallet ใหม่"**
2. เข้าสู่ subView = "create" — form กรอก:
   - **สินค้า** (auto จาก WO เครื่อง)
   - **เลข Pallet** — พิมพ์เอง (เช่น `P001`, `P002`)
   - **Target Qty** (optional · auto จาก product master ถ้ามี)
3. กด **"สร้าง Pallet"**
4. Backend: `POST /api/pallets`
5. Redirect ไปหน้า scan → พร้อม scan boxes เข้า pallet

### ⚠️ ข้อควรระวัง
- **เลขซ้ำไม่ได้** ในวัน+สินค้าเดียวกัน · ระบบจะ error
- **1 pallet = 1 product** — mix product ไม่ได้

---

## 3️⃣ Scan Box เข้า Pallet

### ขั้นตอน
1. หน้า scan mode — cursor อยู่ที่ **"สแกน Barcode กล่อง (กด Enter)"**
2. ยิง scanner ที่ label กล่อง (barcode ที่พิมพ์จาก Track-Out)
3. Barcode = `{product_code}-{lot_number}-{box_no}` (เช่น `QP350M-260803-072`)
4. Backend: `POST /api/pallets/{id}/scan` — เช็ค:
   - Sub-lot มีในระบบไหม?
   - Status = labeled ไหม? (ต้อง Track-Out ก่อน)
   - อยู่ pallet อื่นแล้วหรือยัง?
   - Product ตรงกับ pallet หรือไม่?
5. ถ้าผ่านทั้งหมด → **box เข้า pallet** · counter อัปเดต

### สถิติที่เห็นบนหน้า scan
- **จำนวนกล่อง**: 5 / 20 (ถ้ามี target)
- **จำนวนขวด**: 3,600 / 14,400 (target)
- Progress bar
- List boxes ที่ scan แล้ว (พร้อมปุ่ม 🗑️ ลบ)

### 🚨 Error cases
| อาการ | แก้ยังไง |
|---|---|
| "Box not found" | เช็ค barcode ตรงกับ label · ลอง Track-Out ก่อน |
| "Box already on another pallet" | ตรวจ pallet อื่น · ต้อง remove ก่อน |
| "Product mismatch" | ทำ pallet ใหม่สำหรับ product นี้ |
| "Box status not labeled" | Track-Out box นี้ก่อน (เมนู Track-Out) |

---

## 4️⃣ ลบ Box ออกจาก Pallet

หากสแกนผิด:
1. หา row ใน list ที่ต้องการลบ
2. กด **🗑️ ลบ**
3. Backend: `DELETE /api/pallets/{id}/boxes/{subLotId}`
4. Sub-lot กลับสู่ status = labeled · scan เข้า pallet อื่นได้

---

## 5️⃣ ปิด Pallet (Close)

เมื่อรวม box ครบ (หรือหมด lot):
1. กด **"ปิด Pallet"** (สีน้ำเงิน)
2. Backend: `POST /api/pallets/{id}/close`
3. Status: OPEN → **CLOSED** 🔵
4. ห้าม scan เพิ่ม · ห้ามลบ · **lock**
5. ถ้าอยากเพิ่ม/ลบ กล่อง → ต้อง **Rearrange** (สร้าง pallet ใหม่จากอันเก่า)

---

## 6️⃣ 🖨️ พิมพ์ Label

หลัง close:
1. กด **"พิมพ์ Label"**
2. Backend: `POST /api/pallets/{id}/print` · เพิ่ม print_count
3. เปิด **PalletLabelPrint** (component แยก)
4. Layout ตาม form **QP-PD-002-F046 Rev.02**:
   - Header: บริษัท · Code · Big lot-date-range (บนซ้าย) · Big pallet number (บนขวา)
   - Matrix table: LOT NO. × BOX NO. (8 boxes/row · auto-wrap)
   - Footer: QA sign slot · SHIFT PD (Day/Night) · จำนวน (auto) · Date · Form code
5. Browser print dialog เปิด → เลือก printer → พิมพ์ A5 landscape
6. ติด label บน pallet · ส่งคลัง

### พิมพ์ซ้ำ (Reprint)
- Label หาย/ฉีก → กด "พิมพ์ Label" อีกครั้ง
- print_count เพิ่ม (audit log)
- Layout เหมือนเดิม

---

## 7️⃣ 🔄 Rearrange from Existing Pallet

**เมื่อไหร่ใช้**: 
- กล่องเสียระหว่างขนส่ง — เอา pallet เก่ามาลบกล่องเสีย
- QA reject บางกล่อง — สร้าง revision A/B/R
- Split shipping — pallet เก่าแยกเป็น 2 อัน

### ขั้นตอน
1. เปิด pallet เก่า (CLOSED) → กด **"🔄 Rearrange"**
2. เข้า subView = "rearrange" — form:
   - **เลข Pallet ใหม่** (เช่น `P001A`)
   - **เหตุผล** (ไม่บังคับ · เช่น "damage 3 boxes")
3. กด **"สร้าง Rearrange"**
4. Backend: `POST /api/pallets/{id}/rearrange` — สร้าง pallet ใหม่:
   - Status = OPEN
   - `parent_pallet_id` = pallet เก่า
   - `revision_suffix` = "A" (หรือตามที่ระบุ)
   - **Box ทั้งหมด move จาก pallet เก่า → pallet ใหม่**
5. หน้า scan mode เปิด — ลบ box ที่ไม่ต้องการ · เพิ่ม box ใหม่
6. Close + Print เหมือนปกติ

### หลักการ
- **Pallet เดิม (parent) ยังคง CLOSED · audit ครบ**
- **Pallet ใหม่ (child)** trace back ผ่าน `parent_pallet_id`
- ประวัติทั้งหมดใน `pallet_close_log` (action = REARRANGED_FROM)

---

## 8️⃣ 📄 Label Format (QP-PD-002-F046)

Label ที่พิมพ์ออกมา:

```
┌─────────────────────────────────────────────────────────┐
│ [Logo] Toyo Seikan (Thailand)      CODE: BSMH0500A       │
│                                                          │
│  260803                                       PALLET 1   │
│  260804                                                  │
│                                        (big pallet #)    │
│                                             7            │
│  LOT DATE : BOX NO.                                      │
│  ┌─────────┬─────────────────────────────────────────┐   │
│  │ LOT NO. │ BOX NO.                                 │   │
│  ├─────────┼───┬───┬───┬───┬───┬───┬───┬───┤          │   │
│  │ 260803  │ 72│ 73│ 74│ 75│ 76│ 77│ 78│ 79│         │   │
│  │         │ 80│ 81│ 82│   │   │   │   │   │         │   │
│  │ 260804  │  1│  2│  3│  4│  5│   │   │   │         │   │
│  └─────────┴───┴───┴───┴───┴───┴───┴───┴───┘          │   │
│                                                          │
│  [QA sig ______]  [SHIFT PD  Day____ Night____]         │
│                                                          │
│  จำนวน: 1,680x                    DATE: 30/07/2026       │
│                                                          │
│  <QP-PD-002-F046> Rev.02                                 │
└─────────────────────────────────────────────────────────┘
```

- **Big lot-date-range** (บนซ้าย) — เห็นชัดจากไกล
- **Big pallet number** (บนขวา) — "7" (หรือเลขที่กำหนด)
- **QA + SHIFT PD** slots — เว้นว่างให้เซ็น (paper sign · Phase 1 ยังไม่มี e-sign)
- **จำนวน + วันที่** auto-fill

---

## 🆘 เมื่อมีปัญหา — Quick Fix

| อาการ | ทำก่อน | ถ้ายังไม่หาย |
|---|---|---|
| Scan barcode แล้ว "not found" | เช็คว่ากล่องนั้น Track-Out แล้วหรือยัง | Track-Out ก่อน · แล้ว scan อีกครั้ง |
| "Box already on another pallet" | เช็คว่ากล่องไปอยู่ pallet ไหน | Remove จาก pallet เก่า → scan เข้าใหม่ |
| "Product mismatch" | เช็คว่า pallet นี้กำหนด product อะไร | สร้าง pallet ใหม่สำหรับ product นี้ |
| ปิด pallet แล้ว lock — อยากแก้ | ห้าม unlock | Rearrange สร้าง pallet ใหม่ |
| Label พิมพ์ผิด (blur / ขาด field) | ลองพิมพ์ซ้ำ (Reprint) | เช็ค printer setting A5 landscape |
| Rearrange แล้วเผลอไม่ตรง | สร้าง Rearrange อีกครั้ง (P001A → P001B) | ปรึกษาก่อน · เก็บทั้ง 2 pallet บนกระดาษ |

---

## ⛔ ข้อห้าม

1. ❌ **อย่า scan กล่องที่ยังไม่ Track-Out** — status ต้อง = labeled
2. ❌ **อย่า mix product** ใน pallet เดียว — ระบบ block
3. ❌ **อย่ากรอกเลข pallet ซ้ำ** ในวัน+สินค้าเดียวกัน — error
4. ❌ **อย่าปิด pallet ก่อนครบจำนวน** ถ้าไม่แน่ใจ — ต้อง Rearrange แก้
5. ❌ **อย่าลบ box** จาก pallet CLOSED · Rearrange แทน

---

## 📞 ช่องทางแจ้งปัญหา

- **LINE Group**: GDTahara Pilot
- **P0 (Scan fail ทุก barcode)**: โทร IT + PC
- **ทั่วไป**: LINE + screenshot + Pallet number

```
🐛 [แจ้งปัญหา Pallet Assembly]
Role: Operator
เครื่อง: TAHARA-___
Pallet Number: ______
Barcode ที่ scan (ถ้ามี): ______
อาการ: ...
Screenshot: (แนบรูป)
```

---

## ✅ ผ่าน Training นี้ควรทำได้

- [ ] เข้าเมนู Pallet Assembly + เห็น list วันนี้
- [ ] สร้าง Pallet ใหม่ (product + เลข pallet)
- [ ] Scan barcode กล่องเข้า pallet
- [ ] ลบ box ที่ scan ผิด
- [ ] ปิด pallet + เข้าใจว่า lock แก้ไม่ได้
- [ ] พิมพ์ label QP-PD-002-F046
- [ ] Reprint ถ้า label หาย/ฉีก
- [ ] Rearrange จาก pallet เก่า · เข้าใจ parent_pallet_id
- [ ] เข้าใจ 5 error messages ที่พบบ่อย
- [ ] แจ้งปัญหาด้วย template

---

## 🎓 คำศัพท์

| ศัพท์ | ความหมาย |
|---|---|
| **Pallet** | รวมกล่องหลายใบ (10-30 กล่อง/pallet) · 1 pallet = 1 product |
| **Sub-lot** | กล่อง 1 ใบ · เลข barcode 12 หลัก |
| **Track-Out** | ยืนยันว่ากล่องติด label แล้ว (status = labeled · ทำก่อน scan เข้า pallet) |
| **Close pallet** | ปิด pallet · lock · ห้ามแก้ · ต้อง Rearrange ถ้าอยากแก้ |
| **Rearrange** | สร้าง pallet ใหม่จากเก่า · parent-child relationship |
| **QP-PD-002-F046** | Form code · label ที่พิมพ์ · Rev.02 |
| **REARRANGED_FROM** | Action ใน audit log · trace pallet ใหม่มาจากไหน |

---

**เอกสารเวอร์ชัน**: v1.0 · 3 สิงหาคม 2026 (W19)
**ระบบเวอร์ชัน**: Phase 1 · commit `571bfb0`
**เขียนสำหรับ**: GDTAHARA MES Pilot · Toyo Seikan (Thailand)
