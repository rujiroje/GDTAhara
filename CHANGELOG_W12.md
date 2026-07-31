# GDTahara Backend — Changelog W12 (2026-07-31)

## 1. CM Operator — Auto Stock-Out (HIBE / VERP / UNBW)

### หลักการ
| ประเภทวัตถุดิบ | วิธีตัด |
|---|---|
| ROH | Manual — CM เลือก Lot + กรอกปริมาณ |
| HIBE, VERP, UNBW | AUTO — ตาม BOM × ยอดจริง ไม่ต้อง check stock balance |
| isScrap = true | ข้าม (ไม่แสดง) |

### ไฟล์ที่แก้ไข

**`src/.../dto/CmBomItemDto.java`**
- เพิ่ม field `private boolean autoDeduct;`

**`src/.../dto/StockOutRequestDto.java`**
- เพิ่ม field `private boolean autoDeduct;` (ถ้า true → ข้าม stock balance check)

**`src/.../service/CmOperatorService.java`**
- เพิ่ม `private static final Set<String> AUTO_DEDUCT_TYPES = Set.of("HIBE", "VERP", "UNBW");`
- `getBomItemsForReport()`: ลบบรรทัด `if ("UNBW"...) continue;`, ใช้ `autoDeduct` flag แทน
- `recordStockOut()`: ห่อ stock balance check ด้วย `if (!request.isAutoDeduct()) { ... }`

**`gdtahara-frontend/.../cm-operator/CmOperatorDashboard.jsx`**
- `mkRowState()`: ถ้า `isAuto` → pre-fill quantity จาก BOM, enabled=true
- `handleSubmit()`: ส่ง `autoDeduct` flag + `lotNumber: null` สำหรับ auto rows
- Table rows: AUTO badge (น้ำเงิน), แถวสีฟ้า, "ตัดอัตโนมัติ" แทน lot dropdown

---

## 2. CM Operator — ชั่งน้ำหนักของเสีย

### หลักการ
CM สามารถบันทึกน้ำหนักของเสียได้เหมือน Technician ข้อมูลบันทึกลง `scrap_weight_logs` table เดียวกัน

### ไฟล์ที่แก้ไข

**`src/.../controller/TechnicianController.java`**
- endpoint `POST /api/technician/reports/{reportId}/scrap-weight`
- เพิ่ม `'CM Operator'` เข้า `@PreAuthorize`

**`gdtahara-frontend/.../cm-operator/CmOperatorDashboard.jsx`**
- เพิ่ม `scrapOptions[]` array (8 ตัวเลือก เหมือน Tech)
- เพิ่ม state: `isScrapModalOpen`, `scrapData`, `scrapSubmitting`, `scrapMsg`
- เพิ่ม `handleScrapSubmit()` → `POST /api/technician/reports/{id}/scrap-weight`
- เพิ่มปุ่ม ⚖️ ชั่งน้ำหนักของเสีย ใน header ของ BomStockOutView
- เพิ่ม modal ที่มี dropdown ประเภทของเสีย + ช่องน้ำหนัก

---

## 3. BlowDailyReport — NG Summary (Section 5)

### หลักการ
เปลี่ยนจากแสดงทีละแถว (row per event) → Summary จำนวนรวมต่อประเภท

### ไฟล์ที่แก้ไข

**`gdtahara-frontend/.../pc/BlowDailyReportPage.jsx`**
- Section 5 UI: Group `ngDetails` by `{ngType, ngDescription, source}`, sum quantity, sort ใหญ่→น้อย, แสดงแถว Total
- Excel export: ใช้ summary เดียวกัน (ไม่มี timestamp รายบรรทัดอีกต่อไป)

---

## 4. Material Requirement Dashboard — Number Formatting

### ไฟล์ที่แก้ไข

**`gdtahara-frontend/.../pc/MaterialRequirementDashboard.jsx`**
- เพิ่ม helper: `const fmt = (n, dp=3) => Number(n).toLocaleString('en-US', {...})`
- ตาราง Summary: `รวมต้องการ`, `Stock คงเหลือ`, `ขาดอีก` → มี comma แล้ว
- ตาราง Detail: `เป้าผลิต`, `qty/FG`, `รวมต้องการ` → มี comma แล้ว
- หมายเหตุ: คำนวณ **ทุกประเภท** (ROH, HIBE, VERP, UNBW) ถูกต้องแล้ว เพราะทุกประเภทต้องมีใน Stock

---

## 5. Stock Management — Performance Optimization

### ปัญหาเดิม
80–250+ SQL queries ต่อ 1 page load (N+1 + duplicate query + EAGER fetch)

### ผลหลังแก้
2 SQL queries ต่อ page load (initial), history โหลด on-demand

### ไฟล์ที่แก้ไข

**`src/.../model/MaterialStockTransaction.java`**
- เปลี่ยน `material`, `user`, `productionReport` จาก default EAGER → `FetchType.LAZY`

**`src/.../repository/MaterialStockTransactionRepository.java`**
- เพิ่ม `findBalancesForAllMaterials()` — 1 JPQL aggregate query สำหรับ balance ทุก material
- เพิ่ม `findByMaterialIdWithDetailsOrderByTimestampDesc()` — JOIN FETCH user + productionReport + product + machine (ไม่มี N+1)

**`src/.../dto/StockSummaryDto.java`** *(ไฟล์ใหม่)*
- Fields: `materialId, materialCode, materialName, unit, currentBalance`

**`src/.../service/ShiftLeaderService.java`**
- เพิ่ม `getStockSummaries()` — 2 SQL queries รวม (findAll + findBalancesForAllMaterials)
- เพิ่ม `getStockCardForMaterial(materialId)` — ใช้ JOIN FETCH, ไม่มี N+1
- แก้ `getMaterialStocks()` — คำนวณ balance ใน-memory จาก list ที่โหลดแล้ว (ลบ duplicate query)

**`src/.../controller/ShiftLeaderController.java`**
- เพิ่ม `GET /api/shift-leader/stock-summary` — lightweight, authorized: Shift Leader, DataAdmin, Production Control, Document
- เพิ่ม `GET /api/shift-leader/stock-history/{materialId}` — on-demand history, same roles

**`gdtahara-frontend/.../common/MaterialStockManager.jsx`** *(rewrite)*
- Initial load: เรียก `/stock-summary` เท่านั้น (ไม่โหลด history)
- `/master-data/materials` โหลดเฉพาะเมื่อเปิด modal รับของ (lazy)
- "ดูประวัติ": เรียก `/stock-history/{materialId}` on-demand
- เพิ่ม search filter รหัส/ชื่อวัตถุดิบ
- ยอดติดลบ highlight สีแดง

---

## 6. Doc Role — Stock Management Access

### ปัญหาเดิม
`Document` role ถูก route ไปที่ `ProductionControlDashboard` ซึ่งไม่มีหน้าจัดการ Stock

### ไฟล์ที่แก้ไข

**`gdtahara-frontend/src/pages/Dashboard.jsx`**
- เปลี่ยน `case "Document":` → render `<DocMgmtDashboard />` (เดิมเป็น `<ProductionControlDashboard />`)
- `DocMgmtDashboard` มีครบ: ภาพรวมการผลิต + ดูรายงานย้อนหลัง + ความต้องการวัตถุดิบ + **จัดการสต็อกวัตถุดิบ**
- `Management` role ยังคง route ไป `ProductionControlDashboard` เหมือนเดิม

---

## สรุป Flow การทำงาน Stock วัตถุดิบ (ครบวงจร)

```
Logistic รับของ
     ↓
LD (Shift Leader) / Doc (Document)
  → บันทึก Stock-IN ผ่าน MaterialStockManager
  → POST /api/shift-leader/stock-transactions  { transactionType: "IN" }
     ↓
CM Operator
  → เลือก WO → ดู BOM
  → ROH: เลือก Lot + กรอกปริมาณ (Manual)
  → HIBE/VERP/UNBW: AUTO (pre-fill จาก BOM × ยอดจริง)
  → POST /api/cm-operator/stock-out/batch
     ↓
scrap_weight_logs
  → CM หรือ Tech บันทึกน้ำหนักของเสีย
  → POST /api/technician/reports/{id}/scrap-weight
```

---

## API Endpoints ที่เพิ่มในเซสชันนี้

| Method | Path | Role | หมายเหตุ |
|---|---|---|---|
| GET | `/api/shift-leader/stock-summary` | LD, Doc, PC, Admin | Lightweight — 2 SQL |
| GET | `/api/shift-leader/stock-history/{materialId}` | LD, Doc, PC, Admin | On-demand history |
| POST | `/api/cm-operator/stock-out/batch` | CM Operator | มี `autoDeduct` flag |
| POST | `/api/technician/reports/{id}/scrap-weight` | Tech, PC, Admin, **CM** | CM เพิ่งเพิ่ม |
