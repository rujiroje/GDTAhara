# C-04 — BOM-to-Tank Role Linking

**Status:** Design approved, pending migration  
**Author:** SA/ARCH  
**Date:** 2026-11-17  
**Branch:** pending → `feature/phase1-c04-bom-tank-role`

---

## 1. ที่มาและเป้าหมาย

ปัจจุบัน `machine_tank.default_material_code` เป็น free string ทั้งหมดเป็น `TBD`  
Operator ต้องรู้ว่าควรเติมวัตถุดิบอะไรในแต่ละถังด้วยตัวเอง ไม่มีระบบช่วย

**เป้าหมาย:** ระบบแนะนำวัตถุดิบที่ถูกต้องต่อถังโดยอัตโนมัติ โดยดึงจาก BOM ของ production plan ที่กำลังรันอยู่

---

## 2. โครงสร้างถัง (4 ชนิดคงที่ทุกเครื่อง)

| tank_role | ชื่อถัง | ตัวอย่าง material |
|-----------|---------|-----------------|
| `RESIN_100` | ถังเม็ด 100% (Virgin) | NPP16, NPP18 |
| `EVOH` | ถัง EVOH | EVOH-FL, EVOH-XL |
| `ADMER` | ถัง ADMER / Adhesive | ADS-ADM02, ADS-ADM03 |
| `BLEND` | ถังเปิดผสม (Mix/บดผสม) | BPP16GF31NPA, เม็ดบด |

---

## 3. การออกแบบ DB

### 3.1 เพิ่ม `tank_role` ใน `materials` table

```sql
-- V49
ALTER TABLE materials ADD tank_role NVARCHAR(30) NULL
  CONSTRAINT CK_mat_tank_role CHECK (
    tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL
  );

-- Auto-seed จาก material_name pattern (ต้อง review หลัง migrate)
UPDATE materials SET tank_role = 'EVOH'      WHERE material_name LIKE '%EVOH%'
                                             OR material_code  LIKE '%EVOH%';
UPDATE materials SET tank_role = 'ADMER'     WHERE material_name LIKE '%Admer%'
                                             OR material_name LIKE '%ADMER%'
                                             OR material_code  LIKE '%ADM%';
UPDATE materials SET tank_role = 'RESIN_100' WHERE material_name LIKE '%Virgin%'
                                             OR material_name LIKE '%virgin%'
                                             OR material_name LIKE '%100%%';
UPDATE materials SET tank_role = 'BLEND'     WHERE material_name LIKE '%Mix%'
                                             OR material_name LIKE '%บด%'
                                             OR material_name LIKE '%ผสม%';
```

> ⚠️ **หลัง migrate Admin ต้อง review** ที่ `GET /admin/materials?untagged=true` และ tag ที่เหลือ

### 3.2 เพิ่ม `tank_role` ใน `machine_tank` table

```sql
-- V50
ALTER TABLE machine_tank ADD tank_role NVARCHAR(30) NULL
  CONSTRAINT CK_mt_tank_role CHECK (
    tank_role IN ('RESIN_100','EVOH','ADMER','BLEND') OR tank_role IS NULL
  );

-- Seed ตามลำดับถัง A/B/C/D (ทุกเครื่องเหมือนกัน)
UPDATE machine_tank SET tank_role = 'RESIN_100' WHERE tank_code = 'A';
UPDATE machine_tank SET tank_role = 'EVOH'      WHERE tank_code = 'B';
UPDATE machine_tank SET tank_role = 'ADMER'     WHERE tank_code = 'C';
UPDATE machine_tank SET tank_role = 'BLEND'     WHERE tank_code = 'D';
```

> ⚠️ **ต้องยืนยัน** ว่าทุกเครื่องใช้ A=100%, B=EVOH, C=ADMER, D=BLEND เหมือนกัน

---

## 4. BOM Reverse Lookup Query

```sql
-- สำหรับ machineId X กะ Y วันที่ Z
-- คืน: แต่ละถังควรมี material อะไรตาม BOM
SELECT
    mt.tank_id,
    mt.tank_role,
    mt.tank_code,
    m.material_code  AS bom_suggested_material,
    m.material_name
FROM machine_tank mt
JOIN production_plan pp
    ON  pp.machine_id  = mt.machine_id
    AND pp.shift_date  = :shiftDate
    AND pp.shift_code  = :shiftCode
JOIN products p  ON p.id = pp.product_id
JOIN bill_of_materials bom ON bom.fg_code = p.product_code
    AND bom.alternative_number = 1
JOIN bom_item bi  ON bi.bom_id = bom.id
JOIN materials m  ON m.material_code = bi.rm_code
    AND m.tank_role = mt.tank_role       -- ← key join
WHERE mt.machine_id = :machineId
  AND mt.is_active  = 1
ORDER BY mt.tank_code;
```

---

## 5. API ที่ต้องเพิ่ม/แก้

### 5.1 แก้ `TankStatusDto` — เพิ่ม field

```java
private String bomSuggestedMaterial;   // rm_code จาก BOM (null ถ้าหา plan ไม่เจอ)
private String bomSuggestedMaterialName;
```

### 5.2 `GET /cm/machines/{machineId}/tanks?shiftDate=&shiftCode=`

ปัจจุบัน return tank status — เพิ่ม `bomSuggestedMaterial` ต่อ tank

### 5.3 Admin API (ใหม่)

| Endpoint | Description |
|----------|-------------|
| `GET /admin/materials?untagged=true` | วัตถุดิบที่ยัง `tank_role = NULL` |
| `PATCH /admin/materials/{code}/tank-role` | Admin tag tank_role ให้ material |

---

## 6. FE — แสดงผลใน Tank Card (C-01)

```
┌─────────────────────────────┐
│ ถัง B — EVOH                │
│ BOM แนะนำ: EVOH-FL  ✅      │  ← ตรงกัน = แสดงปกติ
│ เติมล่าสุด: 90 kg           │
└─────────────────────────────┘

┌─────────────────────────────┐
│ ถัง C — ADMER               │
│ ⚠️ BOM: ADS-ADM03           │  ← ต่างจากที่เติม = warning
│ เติมล่าสุด: ADS-ADM05       │
└─────────────────────────────┘
```

---

## 7. Prerequisites ก่อน implement

- [ ] ยืนยัน tank_code A/B/C/D ของทุกเครื่องตรงกับ role ที่กำหนด
- [ ] Admin review material tag หลัง V49 migrate
- [ ] ตรวจสอบ production_plan.machine_id join กับ machine_tank.machine_id ถูกต้อง (ปัจจุบัน loose string join)

---

## 8. ความสัมพันธ์กับฟีเจอร์อื่น

- **C-01 Meter**: แสดง `bomSuggestedMaterial` ใน TankCard component
- **C-03 Stock**: `material_reorder_config` สามารถ reference `tank_role` เพื่อ alert per tank type ได้ในอนาคต
- **StockAlertScheduler**: ไม่กระทบ — alert ยังใช้ `material_code` ตรงๆ
