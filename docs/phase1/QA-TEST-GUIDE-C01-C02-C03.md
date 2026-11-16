# QA Test Guide: Phase 1 — C-01, C-02, C-03
**Date:** 2026-11-16 · **Tester:** Manual UAT
**Branch:** feature/phase1-w11-backend

---

## ก่อนเริ่มทดสอบ

```
1. ล็อกอินด้วย role: CM Operator (เช่น user: cm01)
2. เปิด browser DevTools > Network tab (ดู HTTP status)
3. Backend ต้อง running อยู่ที่ port 8080
4. Frontend ต้อง running อยู่ที่ port 5173
```

---

## C-01: CM Material Meter Tracking

### TC-01-01: ดูรายการถัง
- Login ด้วย CM Operator
- ไปที่ Tab "บันทึกวัตถุดิบ"
- **Expected:** เห็น dropdown เลือกเครื่อง → เลือกเครื่องใดก็ได้ → เห็นการ์ดถัง
- **Check:** level% แสดงเป็น `Math.round(latestMeterKg/capacityKg*100)` (ไม่ใช่ค่า hardcode)
- **Check:** status chip แสดง EMPTY/CRITICAL/LOW/OK ตามค่า level%

### TC-01-02: บันทึกค่ามิเตอร์
- เลือกถัง → กดปุ่มบันทึกค่ามิเตอร์ (TankMeterEntry)
- กรอก meterKg ที่ถูกต้อง
- **Expected:** HTTP 200, ค่าในการ์ดอัพเดต
- **Check:** Network request ส่งไป `POST /api/cm/meter`

### TC-01-03: บันทึกรีฟิล
- เลือกถัง → กด refill
- กรอก refillKg > 0
- **Expected:** HTTP 200, refillCount +1

### TC-01-04: ดู refill history
- เลือกเครื่อง → ดู history
- **Expected:** เห็นรายการ refill ล่าสุดไม่เกิน 20 รายการ
- **Check:** field `recordedAt`, `tankId`, `quantity`, `entryType` แสดงถูกต้อง

---

## C-02: Waste Weight Recording

### TC-02-01: บันทึกของเสีย (Happy Path)
- Login CM Operator
- ไปที่ Tab "บันทึกของเสีย"
- **Check (ใหม่):** dropdown เครื่องต้องโหลดจาก `/api/cm/machines` (ไม่ใช่ free text)
- เลือกเครื่อง → เลือกประเภทของเสีย → กรอกน้ำหนัก 1.000
- กด "บันทึก"
- **Expected:** HTTP 201, รายการปรากฏในตาราง

### TC-02-02: Self Edit (แก้ไขทันที)
- จาก TC-02-01 — กดปุ่ม Edit (ดินสอสีน้ำเงิน) บนรายการที่เพิ่งบันทึก
- **Check:** modal เปิดขึ้น มี field: ประเภทของเสีย, น้ำหนัก, หมายเหตุ, **เหตุผลการแก้ไข** (required)
- กรอกน้ำหนักใหม่ แต่ **ไม่กรอก reason**
- **Expected:** validation error "กรุณาระบุเหตุผลการแก้ไข" (ไม่ให้ submit)
- กรอก reason แล้ว submit
- **Expected:** HTTP 200, น้ำหนักอัพเดตในตาราง

### TC-02-03: Self Edit ล้มเหลวข้ามกะ
- ลองแก้ไขรายการจากกะที่แล้ว (ถ้ามี)
- **Expected:** HTTP 403 หรือ error message ว่าไม่สามารถแก้ไขข้ามกะได้

### TC-02-04: ส่งคำขอแก้ไข (Edit Request)
- กดปุ่ม History (สีส้ม) บนรายการ
- **Check:** modal เปิดขึ้น มี field: เลข WO (optional), ประเภทของเสียใหม่, น้ำหนักใหม่, หมายเหตุ, **เหตุผล** (required)
- กรอกเหตุผลและค่าใหม่ที่ต้องการเปลี่ยน → submit
- **Expected:** HTTP 201, สถานะ PENDING ปรากฏใน column "สถานะคำขอ"
- **Check:** ปุ่ม Edit/History ถูก disabled สำหรับรายการที่มี PENDING

### TC-02-05: Doc อนุมัติคำขอแก้ไข
- Logout → Login ด้วย role: Document
- **Check (ใหม่):** เห็น 2 tabs — "ภาพรวมการผลิต" และ "คำขอแก้ไขของเสีย"
- คลิก Tab "คำขอแก้ไขของเสีย"
- **Expected:** เห็นรายการ PENDING จาก TC-02-04
- กดปุ่ม "ตอบสนอง" → เลือก "อนุมัติ" → กรอก note → ยืนยัน
- **Expected:** HTTP 200, status เปลี่ยนเป็น APPROVED
- **Check:** Network request ส่ง `{ decision: "APPROVED", docNote: "..." }` (ไม่ใช่ `{ approved: true, note: "..." }`)

### TC-02-06: Doc ปฏิเสธคำขอแก้ไข
- ทำขั้นตอนเหมือน TC-02-05 แต่เลือก "ปฏิเสธ"
- **Expected:** status เปลี่ยนเป็น REJECTED

---

## C-03: Stock Card Dashboard

### TC-03-01: ดูภาพรวมสต็อก
- Login CM Operator
- ไปที่ Tab "สต็อกวัตถุดิบ"
- **Expected:** เห็นตาราง materials พร้อม balance, reorder point, status chip
- **Check:** summary cards แสดง totalBalanceKg, totalActiveLots, alertCount (ถ้ามี)

### TC-03-02: ดูรายละเอียด material
- คลิกที่แถว material ใดก็ได้
- **Expected:** เปิด detail view แสดง balance, reorderPoint, dailyAvg, estDaysRemaining
- **Check:** ตาราง lots แสดง lotRef, balanceKg, receivedAt, lastActivityAt

### TC-03-03: ดู Stock Card
- จาก TC-03-02 → กดปุ่ม "ดู Stock Card"
- **Expected:** เปิด stock card table แสดง movements
- กรอง by date range (from/to)
- **Expected:** รายการกรองตามวันที่ที่เลือก
- **Check:** runningBalance แสดงถูกต้อง (ไม่ติดลบเกินควร)

### TC-03-04: Tab เปรียบเทียบ 7 วัน
- คลิก Tab "เปรียบเทียบ 7 วัน"
- **Expected:** ตาราง multi-material summary แสดง balanceKg, usedKg, receivedKg, changeKg, trend icon

### TC-03-05: Tab แจ้งเตือน
- คลิก Tab "แจ้งเตือน"
- **Expected ถ้าไม่มี alert:** "ไม่มีการแจ้งเตือนสต็อกต่ำในขณะนี้"
- **Expected ถ้ามี alert:** cards แสดง materialCode, balanceAtAlert, reorderPoint, estDaysRemaining

### TC-03-06: Scheduler ทำงาน
- ตรวจสอบ backend log หลัง 15 นาที
- **Expected:** เห็น log "StockAlertScheduler: completed. created=X, resolved=Y alerts"
- **Check:** ถ้า balance < reorderPoint → มีแถวใน `low_stock_alert` table

---

## Regression Tests (ตรวจสอบว่า feature เก่ายังทำงานปกติ)

### TR-01: ล็อกอินทุก role ได้
- CM Operator → เห็น 4 tabs: เบิก BOM, บันทึกวัตถุดิบ, สต็อก, บันทึกของเสีย
- Document → เห็น 2 tabs: ภาพรวมการผลิต, คำขอแก้ไขของเสีย
- Production Control, Shift Leader, Operator, Technician, QA → dashboard ปกติ

### TR-02: เบิก BOM ยังทำงาน
- CM Operator → Tab "เบิก BOM" → เบิก lot ได้ตามปกติ

---

## สิ่งที่ยังไม่พร้อม (ไม่ต้องทดสอบ)

| Feature | สถานะ |
|---------|-------|
| Telegram notification | TODO stub — ยังไม่ส่ง |
| default_material_code 27 ถัง | ยังเป็น 'TBD' — รอ user กรอก |
| Excel/PDF export C-03 | Phase 1b — ยังไม่ implement |

---

## Bug Tracking

| # | Test | Expected | Actual | Status |
|---|------|---------|--------|--------|
| | | | | |

_กรอกตารางนี้ระหว่างทดสอบ_
