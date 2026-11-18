# 02 — Technician: Setup Job + TimeLog (W19a ✨ ใหม่)

**สำหรับ**: ช่าง Technician (คน Setup เครื่อง/แม่พิมพ์)
**ใช้เวลาเรียน**: ~25 นาที
**ต้องมีก่อน**: บัญชี user + role = Technician
**⚠️ สำคัญ**: **หน้าจอเปลี่ยนใหม่ทั้งหมด** ตั้งแต่ W19a (สค. 2026) — Checklist เดิมยกเลิก · ใช้ **TimeLog** แทน

---

## 🆕 อะไรเปลี่ยนใน W19a

| หัวข้อ | ของเดิม (Checklist) | ✨ ใหม่ (TimeLog) |
|---|---|---|
| การบันทึก | Tick ✅ ทีละขั้นตอน · กรอกครั้งเดียวตอน Complete | **บันทึกทีละกิจกรรม · ทำเสร็จข้อไหน เข้ามาบันทึก · ปิดไป · กลับมาเปิดต่อ** |
| กิจกรรม | Fixed template ตาม machine type | **10 รหัสกิจกรรม** (M/A/S/C/Q/Z/R/H/B/X) · dropdown เลือก |
| เวลา | ไม่มี · จับเวลาไม่ได้ | Start–End time · **duration auto-calc** · summary ต่อรหัส |
| รูปประกอบ | 1 รูป/ขั้น | Multi-photo/entry · **compress อัตโนมัติ** < 300 KB |
| Complete | กดปุ่มเดียวจบ | **บล็อกถ้ามี IN_PROGRESS** · ต้อง end ทุก entry ก่อน |
| ทำงานข้ามกะ | ไม่รองรับ | ✅ Tech A เริ่ม · Tech B มาต่อ · เห็น entries เดียวกัน |

---

## ภาพรวม — งาน Technician 1 Setup Job

```
📥 PC สร้าง WO → ระบบ auto Setup Job (M: mold change)
  ↓
🔧 Tech เปิด SetupJobPanel → เห็น 3 zones (ทำอยู่/เลยกำหนด/วันนี้)
  ↓
🎬 เริ่มงานจริง
  ↓
  ทำ Heat Up (05:00-06:30) → กด "+ เพิ่ม Activity" → บันทึก H (90 นาที)
  ↓ ปิด browser · ไปทำงานต่อ
  ↓
  ทำ Purge (06:30-13:00) → กลับมาเปิด Setup เดิม → บันทึก S (390 นาที)
  ↓ ปิดอีก
  ↓
  ... (ทำเรื่อยๆ ทั้งกะ · อาจข้ามกะ · Tech อื่นมาต่อได้)
  ↓
✅ ครบทุกกิจกรรม → กด "Complete Setup Job"
  ↓ ระบบเช็ค: มี IN_PROGRESS ค้างไหม?
  ↓ ถ้ามี → alert · กด "End All Now" หรือ end ทีละอัน
  ↓
🎉 status = COMPLETED · Operator เริ่มผลิตได้
```

---

## 1️⃣ Login + เลือก Setup Job

### ขั้นตอน
1. Login → เข้า **TechnicianDashboard** (มี tabs · เลือก "Setup Job")
2. เห็น 3 zones แบ่งตามความสำคัญ:
   - 🔵 **ทำอยู่ (IN_PROGRESS)** — งานที่เริ่มไปแล้ว
   - 🔴 **เลยกำหนด** — Plan เริ่มแล้ว ยังไม่ setup
   - 🟡 **วันนี้** — ตามคิว
3. คลิก card งานที่จะทำ → เปิด **SetupTimeLogDialog**

### ⚠️ ถ้าเห็น zone "เลยกำหนด" สีแดง
- Operator กำลังรอ · ทำก่อนอันอื่น
- ถ้าค้างข้ามกะ → หัวหน้ากะได้รับ escalate

---

## 2️⃣ หน้า SetupTimeLogDialog — ส่วนประกอบ

หน้าต่างที่เปิดมามี 3 ส่วน:

### 🎯 Header (auto-populate จาก SetupJob)
- **เครื่อง**: TAHARA-102 (จาก machine)
- **สินค้า**: QP350 → QP350 M (from → to product)
- **วันที่**: 30/07/2026 (plan date)
- **Technician**: ชื่อคุณ (auto จาก login)
- **Status badge**: 🟡 IN_PROGRESS / ✅ COMPLETED

### ➕ Add Activity — form กรอก entry ใหม่
- **Code (dropdown)**: 10 กิจกรรมมาตรฐาน
- **เริ่ม / จบ**: DateTime picker พร้อมปุ่ม "🕐 Now"
- **ระยะเวลา**: auto-calc จาก end - start (นาที)
- **รายละเอียด**: textarea 500 chars
- **📷 รูป**: ไม่บังคับ · max 5 รูป · compress อัตโนมัติ

### 📋 Completed entries table
คอลัมน์: **รหัส · รายละเอียด · เริ่ม · จบ · นาที · [ลบ]**
- Sort ตาม start_time ล่าสุด
- IN_PROGRESS entries — highlight สีเหลือง + ปุ่ม "End Now"
- Total row ท้ายตาราง

### 📊 Summary per code
- Table: **Code · จำนวน entries · รวม min · %**
- Bar chart แนวนอน · สีตาม code (M แดง / A ส้ม / etc.)

### 📝 หมายเหตุ Setup (รวม)
- Field เดียว · เก็บ notes ทั้ง setup job

---

## 3️⃣ 10 รหัสกิจกรรม (Activity Codes)

| Code | ภาษาไทย | สี | ใช้เมื่อ |
|---|---|---|---|
| **M** | เปลี่ยนแม่พิมพ์ | 🔴 แดง | Mold change · Mechanical |
| **A** | ปรับอุณหภูมิ | 🟠 ส้ม | Temperature adjustment |
| **S** | ปรับความเร็ว/Stroke | 🟡 เหลือง | Speed/Stroke |
| **C** | ตรวจสอบ Cycle Time | 🔵 ฟ้า | Cycle time check |
| **Q** | ตรวจคุณภาพ | 🟣 ม่วง | Quality inspection |
| **Z** | ล้างทำความสะอาด | 🟢 เขียวอ่อน | Cleaning · Maintenance |
| **R** | ซ่อมเล็กน้อย | 🟤 น้ำตาล | Minor repair |
| **H** | รอ Heat-up | 🟠 ส้มเข้ม | Heat-up wait |
| **B** | รอวัตถุดิบ | ⚫ เทา-น้ำเงิน | Material wait |
| **X** | อื่นๆ | ⚪ เทา | Other |

**⚠️ Admin เท่านั้นเพิ่ม/แก้ code ได้** — Tech เจอกิจกรรมที่ไม่มี code → ใช้ X (อื่นๆ) พร้อมเขียนใน "รายละเอียด" · แจ้ง Admin เพิ่มภายหลัง

---

## 4️⃣ เพิ่ม Activity ทีละอัน (Continuous Mode)

### หลักการ · บันทึกทันทีที่ทำ · ไม่ต้องบันทึกครั้งเดียว

```
🕐 05:00 · เริ่ม Heat Up
  → (ทำงานที่เครื่อง · ไม่ต้องเปิดจอ)
🕐 06:30 · Heat Up จบ
  → เปิด SetupJobPanel → คลิก card เดิม
  → + Add Activity: Code=H · Start=05:00 · End=06:30 · Save
  → ปิด browser · ไปทำงานต่อ

🕐 06:30 · เริ่ม Purge
  → (ทำงาน)
🕐 13:00 · Purge จบ
  → เปิด SetupJobPanel อีก · card เดิม
  → + Add Activity: Code=S · Start=06:30 · End=13:00 · Save
```

### วิธีเพิ่ม entry
1. กด **"+ เพิ่ม Activity"** ใน dialog
2. เลือก **Code** จาก dropdown
3. กรอก **เริ่ม** (start time) · กด "🕐 Now" ถ้าเพิ่งเริ่ม
4. กรอก **จบ** (end time) · หรือ toggle **"⏸ ยังไม่จบ"** ถ้ากิจกรรมยัง run อยู่
5. **ระยะเวลา** แสดง auto (min)
6. กรอก **รายละเอียด** (เช่น "ปรับ cutting 9.20→8.90 mm · Prelift 6.0→4.0")
7. เพิ่ม **รูป** (ถ้ามี · เลือกจากเครื่อง หรือถ่ายจากมือถือ)
8. กด **Save**

### เมื่อ activity ยังไม่จบ (IN_PROGRESS)
- Toggle "⏸ ยังไม่จบ" → บันทึกโดยไม่มี end_time
- Entry แสดงในตารางสีเหลือง + ปุ่ม **"End Now"**
- กลับมากด "End Now" ตอนกิจกรรมจริงจบ → เติม end_time = ปัจจุบัน

---

## 5️⃣ แนบรูปประกอบ activity

### เมื่อไหร่ควรถ่าย
- **เปลี่ยน mold / blow pin** — ถ่ายก่อน-หลัง (evidence)
- **ปรับ setting** สำคัญ — ถ่ายหน้าจอ HMI
- **เจอปัญหา** — ถ่าย defect · component ที่เสีย
- **QA request** — ถ่ายชิ้นงานตัวอย่าง

### ขั้นตอน
1. ใน form กด **📷** เพิ่มรูป
2. เลือก:
   - **ถ่ายทันที** (มือถือ/tablet — เปิดกล้อง native)
   - **เลือกจากเครื่อง** (upload file)
3. Preview แสดงในการ์ด · ระบบ **resize เหลือ < 300 KB** อัตโนมัติ (server-side)
4. ถ้าถ่ายผิด → กด 🗑️ ลบก่อน Save

### ⚠️ ข้อควรระวัง
- รูปถูก upload ตอน **กด Save** เท่านั้น
- ถ้า network หลุด → toast "Save failed · retry"
- Compress server-side: ไม่ต้อง resize เอง

---

## 6️⃣ จบ activity ที่ยัง IN_PROGRESS

### วิธีที่ 1 · End ทีละ entry
1. หา row ในตารางที่สีเหลือง (IN_PROGRESS)
2. กด **"End Now"** — end_time = ปัจจุบัน · duration auto

### วิธีที่ 2 · End All Now (bulk)
1. กด **"🛑 End All Now"** (ปุ่มบน)
2. Confirm dialog: "ปิด N entries ที่ยังไม่จบ?"
3. ทุก IN_PROGRESS → end_time = ปัจจุบัน

**เมื่อไหร่ใช้ End All Now**: ก่อน Complete Setup Job · ประหยัดเวลาคลิกทีละอัน

---

## 7️⃣ ✅ Complete Setup Job

### หลังทำงานครบทุก activity
1. กด **"Complete Setup"** (สีเขียว ในหน้าต่าง)
2. ระบบเช็ค:
   - ถ้า **มี IN_PROGRESS ค้าง** → 🚫 **บล็อก** · แสดง alert
     - "มี N entries ยังไม่จบ · กด End All Now หรือ end ทีละอันก่อน"
     - ปุ่ม End All Now โผล่ขึ้นมาให้กดง่าย
   - ถ้า **ครบทุก entry** → ✅ status = COMPLETED
3. Dialog ปิดอัตโนมัติ · card หายจาก zone "ทำอยู่"
4. Operator สามารถเริ่มผลิตได้ทันที (Phase 1 ยังไม่มี QA gate)

### ⚠️ ทำไมต้อง block?
เพื่อป้องกัน **"ลืมกด End"** ทำให้ summary ผิด — Tech ทุกคนต้องรับผิดชอบเวลาจริง

---

## 8️⃣ Skip Setup Job

หากไม่สามารถ setup ได้ (เครื่องพัง / ยกเลิก / setting เดิมใช้ได้):
1. กดปุ่ม **"Skip"** ใน card
2. Dialog: ขอ **"เหตุผลที่ข้าม *"** — บังคับกรอก
3. Save → status = SKIPPED · card หาย
4. แจ้ง PC ถ้าต้องสร้างงานใหม่

---

## 9️⃣ กลับมาทำงานต่อ (Multi-session)

### ระหว่างกะเดียวกัน
- ปิด browser · ไปทำงานหน้าเครื่อง
- กลับมาเปิด SetupJobPanel — เห็น Setup Job เดิม ใน zone "ทำอยู่"
- คลิก · เห็น entries ที่เพิ่มไปแล้วครบ
- เพิ่ม entry ใหม่ต่อได้เลย

### ข้ามกะ (Tech คนอื่นมาต่อ)
- Tech A จบกะ · ไม่กด Complete → Setup ยัง IN_PROGRESS
- Tech B เข้ากะ · เปิด SetupJobPanel · เห็น setup เดียวกันใน "ทำอยู่"
- Tech B คลิก → เห็น entries ของ Tech A ทั้งหมด + เพิ่มของตัวเอง
- Tech B สามารถ Complete ได้ (ถ้าเสร็จ)

### หลายอุปกรณ์ (Multi-device)
- Tech ใช้ **tablet ที่เครื่อง** เพิ่ม entry · ใช้ **PC ที่ office** เปิดดู
- ข้อมูลเห็นเดียวกัน · auto-refresh 60 วิ

---

## 🆘 เมื่อมีปัญหา — Quick Fix

| อาการ | ทำก่อน | ถ้ายังไม่หาย |
|---|---|---|
| ไม่เห็น Setup Job ที่ควรมี | Refresh · เช็ค role Technician | PC ยัง import plan ไม่เสร็จ / plan เครื่องอื่น |
| Dropdown code ไม่มีที่ต้องการ | ใช้ X (อื่นๆ) + เขียนใน description | แจ้ง Admin เพิ่ม code ใหม่ |
| กด Complete ไม่ได้ | เช็ค IN_PROGRESS ค้าง (สีเหลือง) | กด End All Now |
| อัปโหลดรูปช้า/fail | เช็ค network · ลด size manual | Screenshot + แจ้ง IT |
| กด Save แล้ว error | ตรวจ start < end · Code ไม่ null | Screenshot + แจ้ง IT |
| ลืม end entry ข้ามกะ | End Now ตอนกลับมา · duration จะยาว | อธิบายในหมายเหตุรวม |

---

## ⛔ ข้อห้าม

1. ❌ **อย่า Complete ทั้งที่ยังไม่ทำจริง** — audit ย้อนได้
2. ❌ **อย่าลืม End entry** — summary ผิด · Complete ไม่ได้
3. ❌ **อย่าใช้ account คนอื่น** — audit หา owner ไม่เจอ
4. ❌ **อย่าลบ entry ของคนอื่น** — IDOR block · แจ้ง PC/Admin แทน
5. ❌ **อย่ากรอกเวลาปลอม** — start/end ต้องเป็นเวลาจริง

---

## 📞 ช่องทางแจ้งปัญหา

- **LINE Group**: GDTahara Pilot
- **P0 (Save fail ทั้ง dialog)**: โทร IT + PC
- **ทั่วไป**: LINE + screenshot + เลข Setup Job ID

```
🐛 [แจ้งปัญหา Setup TimeLog]
Role: Technician
เครื่อง: TAHARA-___
Setup Job ID: ______
Code ที่พยายามบันทึก: ______
อาการ: ...
Screenshot: (แนบรูป)
```

---

## ✅ ผ่าน Training นี้ควรทำได้

- [ ] Login + เห็น 3 zones ของ Setup Job
- [ ] เปิด SetupTimeLogDialog + เข้าใจ header + form + table + summary
- [ ] เพิ่ม Activity ทีละอัน (code + start/end + description + รูป)
- [ ] เข้าใจ 10 codes + ใช้ X ถ้าไม่มี code ที่ต้องการ
- [ ] IN_PROGRESS entry + End Now / End All Now
- [ ] แนบรูป · ระบบ compress อัตโนมัติ
- [ ] Multi-session · ปิด browser · กลับมาทำต่อ
- [ ] Complete Setup Job (พร้อม guard เช็ค IN_PROGRESS)
- [ ] Skip พร้อมเหตุผล
- [ ] แจ้งปัญหาด้วย template

---

**เอกสารเวอร์ชัน**: v2.0 · 3 สิงหาคม 2026 (W19a rewrite)
**ระบบเวอร์ชัน**: Phase 1 · commit `571bfb0`
**เขียนสำหรับ**: GDTAHARA MES Pilot · Toyo Seikan (Thailand)
