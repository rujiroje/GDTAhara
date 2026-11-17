# Phase 1 — W11 Session Log

**Branch:** `feature/phase1-w11-backend`  
**Last commit:** `cd68ba9`  
**Date:** 2026-11-17

---

## สิ่งที่ทำเสร็จ W11

### C-01 CM Material Meter Tracking

| รายการ | สถานะ |
|--------|-------|
| WasteEntryForm: เปลี่ยน machineId TextField → Dropdown จาก `/api/cm/machines` | ✅ |
| CmWasteHub: แสดง Chip กะปัจจุบัน (shiftCode · date · startTime–endTime) | ✅ |
| CmWasteHub: ส่ง `currentUsername` ให้ WasteHistoryTable (เดิม missing) | ✅ |
| cmMeterApi: ลบ `getCurrentShift` ซ้ำ (original line 4 ถูกต้อง) | ✅ |

### C-02 Waste Weight Edit Workflow

| รายการ | สถานะ |
|--------|-------|
| DocHub.jsx (ใหม่): Tab 0 ภาพรวม + Tab 1 คำขอแก้ไขของเสีย | ✅ |
| Dashboard.jsx: Document role routing → `<DocHub />` แทน `<DocMgmtDashboard />` | ✅ |

> **ก่อนหน้า:** Document role ไม่มีทางอนุมัติ waste edit request เลย

### C-03 CM Stock Dashboard

| รายการ | สถานะ |
|--------|-------|
| ReorderConfigPanel.jsx (ใหม่): DataAdmin เพิ่ม/แก้ material_reorder_config inline | ✅ |
| CmStockDashboard: Tab 3 "ตั้งค่า Reorder" (แสดงเฉพาะ DataAdmin) | ✅ |
| cmStockApi: `getReorderConfig()` + `updateReorderConfig(materialCode, patch)` | ✅ |

> **ผลกระทบ:** StockAlertScheduler ทำงานได้ถูกต้อง — ก่อนหน้า `material_reorder_config` ว่างทั้งหมด ไม่มี alert เลย

### API Contracts (Retroactive)

| ไฟล์ | ครอบคลุม |
|------|---------|
| `docs/api-contracts/CONTRACT-C01-cm-meter.md` | GET /machines, /tanks, POST /meter, /refill, /end-of-shift, /correction |
| `docs/api-contracts/CONTRACT-C02-cm-waste.md` | GET /types, POST /waste, PATCH /self-edit, POST /edit-request, /doc/respond |
| `docs/api-contracts/CONTRACT-C03-cm-stock.md` | GET /overview, /stock-card, /reorder-config, PATCH /reorder-config |

### Standards

- เพิ่ม **§15 Contract-First BE-FE Development Standard** ใน `Agent-Team-Standard.md`
  - Phase A: SA เขียน contract ก่อน code เสมอ
  - Phase B: BE/FE implement ตาม contract เท่านั้น
  - Phase C: 3-agent review (SA + CR + QA) ก่อน push

---

## Commits W11

| Hash | เนื้อหา |
|------|---------|
| `d99bf52` | DocHub, WasteEntryForm dropdown, CONTRACT files, QA-TEST-GUIDE |
| `cd68ba9` | ReorderConfigPanel, CmStockDashboard Tab 3, shift indicator, duplicate fix |

---

## สิ่งที่ยังค้างอยู่ (Phase 1b)

| รายการ | เหตุผลที่ค้าง |
|--------|--------------|
| `default_material_code` 27 ถัง ยังเป็น TBD | รอ material code จริงจาก user |
| Excel/PDF export C-03 | ต้องเพิ่ม Apache POI + iText ใน pom.xml |
| Telegram notification | TODO stub ใน StockAlertScheduler + WasteWeightService |
| **BOM-Tank Role linking (C04)** | ออกแบบแล้ว รอ approve → ดู [C04-BOM-Tank-Role-Spec.md](C04-BOM-Tank-Role-Spec.md) |

---

## Bug ที่แก้ใน W11

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| Doc role ไม่มีหน้า approve waste | Dashboard routing ชี้ผิด component | DocHub.jsx + routing fix |
| machineId free-text error-prone | TextField ไม่มี validation | Dropdown จาก API |
| WasteHistoryTable ไม่กรองตาม user | `currentUsername` ไม่ถูก pass | useAuth() + prop pass |
| Shift indicator หายไปหลัง refactor | getCurrentShift ถูกลบแล้วเพิ่มซ้ำผิดที่ | ลบ duplicate ออก |
| StockAlert ไม่ทำงาน | material_reorder_config ว่าง | ReorderConfigPanel สำหรับ DataAdmin |
