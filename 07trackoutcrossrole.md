# 07 — Track-Out: End-to-End Cross-Role Flow

**สำหรับ**: **ทุกคน** — Operator · Technician · CM · QA · LD · Doc · PC · MK · Mgmt
**ใช้เวลาเรียน**: ~15 นาที
**เมื่อไหร่ควรอ่าน**: **หลังผ่าน training role ของตัวเองแล้ว** — เพื่อเห็นภาพรวมว่างานของเราส่งต่อใคร

---

## 🎯 Track-Out คืออะไร (ในคำเดียว)

**Track-Out = "กล่องนี้ติด label เรียบร้อยแล้ว พร้อมออกจากไลน์ผลิต"**

- ไม่ใช่ "จบการผลิต" (ผลิตจบตั้งแต่กรอกกล่องใน RunCard)
- ไม่ใช่ "ส่งลูกค้า" (มีอีกหลายขั้นก่อนถึงลูกค้า)
- **คือจุดที่ระบบมั่นใจว่ากล่องนี้พร้อมส่งต่อคลัง**

---

## 🌊 ภาพรวม End-to-End Flow — 8 Steps

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│   1️⃣  PC        →  Import แผน Excel                                │
│                    ↓ (auto: WO + Setup Job)                        │
│   2️⃣  Tech      →  Setup เครื่อง (checklist + Mold code)           │
│                    ↓ (Complete → Operator เริ่มได้ทันที)            │
│   3️⃣  LD/Doc    →  รับวัตถุดิบเข้า Stock (Lot + Qty)               │
│                    ↓                                               │
│   4️⃣  CM        →  Stock-out ตาม BOM (ROH manual + AUTO)          │
│                    ↓                                               │
│   5️⃣  Operator  →  RunCard: ผลิต + Confirm Box (สร้าง Sub-Lot)     │
│                    ↓ (ระบบ gen barcode 12 หลัก + Mod-10)           │
│   6️⃣  Operator  →  พิมพ์/จด label + ติดกล่อง                       │
│                    ↓                                               │
│   7️⃣  Operator  →  ⭐ TRACK-OUT ⭐ (scan/พิมพ์ → Mark Labeled)     │
│                    ↓                                               │
│   8️⃣  PC        →  Blow Daily Report เห็นทุก sub-lot ที่ track-out │
│                                                                    │
│   (ระหว่างทาง — QA สุ่ม NG · Tech ชั่ง scrap · LD ตรวจ alert)     │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🔗 Handoff Points — จุดที่งานส่งต่อระหว่าง Role

| ลำดับ | จาก | → | ถึง | สิ่งที่ส่งต่อ | ระบบเห็นอะไร |
|---|---|---|---|---|---|
| H1 | PC | → | Tech | Setup Job | Panel "งาน Setup" · zone "วันนี้" |
| H2 | Tech | → | Operator | Setup Complete | RunCard พร้อมใช้ · Machine active |
| H3 | LD/Doc | → | CM | Stock IN | CM เห็น Lot ใน dropdown ROH |
| H4 | CM | → | Operator | (ผ่านเวลา — mix เข้าเครื่อง) | ไม่เห็นในระบบ · physical เท่านั้น |
| H5 | Operator | → | Operator | Confirm Box → Sub-Lot | Sub-Lot Tree มี entry ใหม่ |
| H6 | Operator (RunCard) | → | Operator (Track-Out) | สร้าง Sub-Lot ✅ | Status = `open` |
| H7 | Operator (Track-Out) | → | PC | Mark Labeled | Status = `labeled` · Blow Report update |
| H8 | Operator | → | QA | บันทึกกล่อง | QA เห็น WO ใน active list |

**ถ้า handoff ไหนหลุด — โดยเฉพาะ H3 (LD→CM) และ H2 (Tech→Op) — ไลน์หยุดทันที**

---

## ⭐ Track-Out Step-by-Step

### ก่อน Track-Out (prerequisites)
- ✅ Operator ได้ **Confirm Box** ใน RunCard แล้ว (Sub-Lot ถูกสร้างในระบบ)
- ✅ Label ถูกพิมพ์/จด/ติดกล่องแล้ว (Pilot: กระดาษกาว A4)

### ขั้นตอน (Operator ทำ)
1. เมนู **Track-Out** ทางซ้าย
2. ช่อง **"เลขกล่อง / Sub-Lot Number"** — placeholder `สแกนหรือพิมพ์แล้วกด Enter…`
3. **2 mode:**
   - 🔦 **มี scanner (post-pilot)**: ยิงบาร์โค้ดที่ label → auto-fill
   - ⌨️ **ไม่มี scanner (pilot ตอนนี้)**: พิมพ์เลข 12 หลักด้วยมือ → กด Enter หรือปุ่ม **"ค้นหา"** (`handleScan`)
4. ระบบตรวจ **Mod-10 check digit** — ถ้าพิมพ์ผิด 1 หลัก → error ทันที ไม่มีทาง mark ผิดกล่อง
5. หน้าจอแสดง Sub-Lot info:
   - Status badge (⚪ open / 🟢 labeled)
   - จำนวน · น้ำหนัก · Pallet · Lot
6. กดปุ่ม:
   - **"พิมพ์ (Print)"** (`handlePrint`) — พิมพ์ label ซ้ำ ถ้ายังไม่ได้พิมพ์
   - **"Mark Labeled"** (`handleMarkLabeled`) — ยืนยันว่าติด label แล้ว
7. Status เปลี่ยน ⚪ → 🟢 · เรียบร้อย

---

## 🎨 Status Journey ของ Sub-Lot

```
    ผลิต RunCard         Track-Out           (Future: Shipping)
       ↓                      ↓                       ↓
   [⚪ open]  ──────→   [🟢 labeled]  ──────→   [📦 shipped]
                                                (Phase 2/3)
```

| Status | ความหมาย | ใครทำให้เกิด |
|---|---|---|
| ⚪ **open** | สร้างแล้ว แต่ยังไม่ track-out | Operator (RunCard confirm box) |
| 🟢 **labeled** | ยืนยันติด label + Track-Out แล้ว | Operator (Track-Out mark) |
| 📦 **shipped** | *(Phase 2/3)* ส่งลูกค้า/คลังแล้ว | — |

---

## 🔄 Reprint Barcode (Fall-back)

### เมื่อไหร่ใช้
- Label ที่กล่องหาย/ฉีก/พิมพ์เลือน
- ต้องการพิมพ์ใหม่หลาย ๆ กล่อง (bulk)
- QA ต้องเก็บ label copy สำหรับ audit

### วิธี
1. เมนู **Reprint Barcode** (Operator dashboard)
2. **filter by Lot Number** — เห็น Sub-Lot ทั้งหมดของ Lot นั้น
3. ตาราง:
   - กล่องที่ · เวลาบรรจุ · ผู้บรรจุ
4. คลิก **ตัวอย่าง Label** → preview ก่อนพิมพ์
5. กด **พิมพ์**
6. Audit log บันทึก reprint ทุกครั้ง (PC ตรวจย้อนหลังได้)

---

## 🆘 Common Cross-Role Issues (ที่ทำให้ Flow ล้ม)

| ปัญหา | หา root cause ที่ role ไหน | วิธีแก้ |
|---|---|---|
| Operator เห็น RunCard ว่างเปล่า | PC (H1) — plan ไม่ import หรือ Tech (H2) — Setup ยังไม่ complete | PC ตรวจ import log · Tech ตรวจ zone "ทำอยู่" |
| CM หา Lot ROH ไม่เจอ | LD/Doc (H3) — ยังไม่บันทึก Stock IN | LD/Doc รับของเข้าให้ครบ |
| Track-Out พิมพ์เลขแล้ว "not found" | Operator (H5) — ยังไม่ confirm box | เช็ค Sub-Lot Tree ใน RunCard ก่อน |
| Mark Labeled ซ้ำได้เรื่อย ๆ | ไม่ควรเกิด — ถ้าเกิดแจ้ง IT | Bug report |
| Blow Daily Report NG count ไม่ตรง | ตรวจ source: Operator vs QA vs LD บันทึกซ้อนกัน | PC ดู Section 5 grouped by source |
| ยอด Material variance ผิด | LD/Doc IN ผิด · หรือ CM OUT ผิด | ดู Stock Card timeline · แจ้ง PC |

---

## 📊 Track-Out ปรากฏที่ไหนบ้าง

| หน้า | เห็นอะไร | ใครดู |
|---|---|---|
| **Sub-Lot Tree (RunCard)** | Status ✅ labeled | Operator |
| **Track-Out Panel** | ประวัติ mark labeled ตัวเอง | Operator |
| **Historical Reports** | Count sub-lot per WO | PC · Doc |
| **Blow Daily Report** | Sub-lot list + timestamp | PC |
| **Variance Report** | (ทางอ้อม) actual FG = count labeled | PC · Mgmt |
| **Audit Log** | ทุก mark/reprint event | Admin |

---

## 🧭 Cheat Sheet — "ใครทำอะไรก่อน-หลัง"

### พร้อมเริ่มกะแล้ว → **ต้องมี 4 อย่างนี้**
1. ✅ PC: Import plan สัปดาห์นี้แล้ว
2. ✅ Tech: Complete Setup Job สำหรับกะนี้
3. ✅ LD/Doc: ROH ทุกตัวใน BOM มี Stock IN
4. ✅ CM: พร้อมเริ่ม mix

### ระหว่างเดินเครื่อง — **แต่ละคนทำวนของตัวเอง**
- Operator: Confirm Box ทุกกล่อง + Track-Out
- CM: Stock-out ต่อ WO (ทำครั้งเดียวจบ)
- QA: สุ่มตรวจ + บันทึก NG
- Tech: ⚖️ ชั่ง scrap (ถ้ามี)
- LD: 🚨 ตอบ alert เครื่องหยุด · IN ของใหม่ถ้ามี

### จบกะ / จบ WO — **PC สรุป**
- Blow Daily Report ต่อกะ ต่อ WO
- Variance Report เทียบ planned vs actual
- Historical ดูย้อนหลัง / drill-down

---

## ⛔ กฎทองของ Cross-Role

1. ❌ **อย่าข้าม handoff** — เช่น Operator start RunCard ก่อน Tech complete Setup = data ไม่ครบ
2. ❌ **อย่าเดา** — Lot Number, จำนวน, Sub-Lot ให้เลือกจากระบบเสมอ
3. ❌ **อย่าแก้ข้อมูลของ role อื่น** — IDOR block · แจ้ง role นั้นแก้เอง
4. ✅ **แจ้งใน LINE เมื่อ handoff หลุด** — role ต่อไปทำงานต่อไม่ได้
5. ✅ **Track-Out ทุกกล่อง** — กล่องที่ไม่ track-out = หายจากรายงาน = KPI เพี้ยน

---

## 🎓 คำศัพท์ Cross-Role ที่ต้องเข้าใจตรงกัน

| ศัพท์ | ความหมายที่ตกลงกัน |
|---|---|
| **Weekly Plan** | Excel ที่ PC import ทำ WO ทั้งสัปดาห์ |
| **WO / Production Order** | 1 batch ผลิต 1 product 1 เครื่อง (สร้างจาก plan) |
| **Setup Job** | งาน Tech ต้องทำก่อนเริ่ม WO |
| **Parent Lot** | Lot ระดับกะ (per machine + day + shift) |
| **Sub-Lot** | 1 กล่อง = 1 sub-lot · เลข 12 หลัก + Mod-10 |
| **Track-Out** | ยืนยันติด label + พร้อมออกจากไลน์ |
| **Stock-IN** | LD/Doc รับวัตถุดิบเข้า |
| **Stock-OUT** | CM ตัดวัตถุดิบไปใช้ |
| **Source** | ที่มาของ NG log · QA_Process / Operator / ShiftLeader |
| **Variance** | ส่วนต่าง planned (BOM×target) vs actual (CM stock-out) |
| **canManage** | Role ที่แก้ WO/BOM ได้ (PC + Admin) |

---

## 📞 ช่องทางแจ้งปัญหา Cross-Role

- **LINE Group**: GDTahara Pilot
- **P0 (Flow ทั้งไลน์หยุด)**: โทร IT + PC + หัวหน้ากะ พร้อมกัน
- **Handoff ล้ม** (H1-H8 ข้างบน): แจ้งใน LINE + tag role ที่หายไป

### Template แจ้ง handoff หลุด
```
🔗 [Handoff ล้ม]
จาก role: ______   →   ถึง role: ______
Handoff #: ______ (H1-H8)
คำอธิบาย: (เช่น "LD ยังไม่ IN Lot XYZ · CM ทำต่อไม่ได้")
เครื่อง / WO: ______
Screenshot: (แนบรูป)
```

---

## ✅ ผ่าน Training นี้แล้ว คุณควรเข้าใจ

- [ ] End-to-end flow 8 steps (PC → Tech → LD/Doc → CM → Op → Track-Out → PC)
- [ ] Handoff points 8 จุด (H1-H8) — เห็นว่างานตัวเองส่งต่อใคร
- [ ] Track-Out เป็นอะไร · ไม่ใช่อะไร (ไม่ใช่ "จบผลิต" · ไม่ใช่ "ส่งลูกค้า")
- [ ] Status journey ⚪ open → 🟢 labeled → 📦 shipped (Phase 2/3)
- [ ] Mod-10 check digit ป้องกัน typo ตอน manual entry
- [ ] Reprint Barcode ใช้เมื่อ label หาย/ฉีก
- [ ] Common cross-role issues + วิธี debug (root cause อยู่ role ไหน)
- [ ] กฎทอง 5 ข้อ + template แจ้ง handoff หลุด

---

## 🎯 คำถามสำหรับตัวเอง (ก่อนเริ่ม Pilot)

- ถ้าไม่เห็น RunCard คุณจะถามใครก่อน?
- ถ้า Mark Labeled แล้วสถานะไม่เปลี่ยน คุณจะทำอะไร?
- ถ้าเจอ Lot ในระบบที่ไม่มีของจริง คุณจะแจ้งใคร?
- Track-Out กล่อง กะเช้าลืม 3 กล่อง · กะบ่ายมาเห็น จะทำยังไง?
- (ตอบใน LINE group ก่อน pilot เริ่ม)

---

## 🔗 ลิงก์ทุก workflow (สำหรับอ่านต่อ)

- [01 Operator — daily flow](./01-operator-daily-flow.md)
- [02 Technician — Setup Job](./02-technician-setup-flow.md)
- [03 CM Operator — Stock-out + Scrap](./03-cm-operator-flow.md)
- [04 QA — บันทึก NG](./04-qa-inspection-flow.md)
- [05 PC — Plan · BOM · Report](./05-pc-workflow.md)
- [06 LD/Doc — Stock IN + Card](./06-ld-doc-stock-flow.md)

---

**เอกสารเวอร์ชัน**: v1.0 · 31 กรกฎาคม 2026
**ระบบเวอร์ชัน**: Phase 1 · commit `bb6da49` (W18)
**เขียนสำหรับ**: GDTAHARA MES Pilot · Toyo Seikan (Thailand)

---

## 🎉 จบชุด Training

Training 01–07 ครอบคลุมทุก role และ workflow ที่ต้องใช้ในช่วง Pilot แล้ว

**สิ่งที่ยัง missing (จะเพิ่มก่อน Pilot เริ่ม)**:
- ⏳ Video training 3-5 module หลัก
- ⏳ Cheat sheet 1 หน้า ต่อ role (พิมพ์ติดข้างเครื่อง)
- ⏳ Bilingual TH+JA version (สำหรับหัวหน้ากะญี่ปุ่น)

**หลัง Pilot เดือนแรก จะรวบรวม feedback เพื่ออัปเดต v2** — pilot user ทุกคนช่วยจดสิ่งที่ **สับสน / ผิดจากจริง / ควรเพิ่ม** ใน LINE group
