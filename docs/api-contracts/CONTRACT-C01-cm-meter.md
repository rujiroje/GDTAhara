# API Contract: C-01 CM Material Meter Tracking
**SA:** System Analyst · **Date:** 2026-11-16 · **Status:** Retroactive (ทำขึ้นหลัง implementation — เพื่อ document สิ่งที่มีอยู่จริง)
**Auth Base:** `hasAnyRole('CM Operator', 'DataAdmin')`

---

## GET /api/cm/machines
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')

### Response Body (Array)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| machineId | String | No | "RBL201" |
| machineName | String | No | "RBL201" |

---

## GET /api/cm/tanks
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')
**Query Params:** `machineId` (optional String)

### Response Body (Array of TankStatusDto)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| tankId | String | No | "RBL201-T1" |
| machineId | String | No | "RBL201" |
| tankCode | String | No | "T1" |
| tankName | String | Yes | "Tank A" |
| defaultMaterialCode | String | Yes | "TBD" |
| capacityKg | BigDecimal | No | 500.000 |
| latestMeterKg | BigDecimal | Yes | 123.456 |
| latestReadingAt | LocalDateTime | Yes | "2026-11-16T14:00:00" |
| shiftStartRecorded | Boolean | No | true |
| shiftEndRecorded | Boolean | No | false |
| refillCount | Integer | No | 2 |
| totalRefillsKg | BigDecimal | No | 300.000 |

> ⚠️ **FE field mapping critical:** ใช้ `tank.defaultMaterialCode` (ไม่ใช่ `tank.material`), `tank.latestMeterKg` (ไม่ใช่ `tank.currentLevel`), คำนวณ level% เองจาก `latestMeterKg / capacityKg * 100`

---

## POST /api/cm/meter
**Auth:** hasAnyRole('CM Operator')

### Request Body (TankMeterEntryDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| tankId | String | @NotBlank | ✅ Yes |
| meterKg | BigDecimal | @NotNull, @DecimalMin("0") | ✅ Yes |
| note | String | none | ❌ No |

### Response: 200 OK (void)

---

## POST /api/cm/refill
**Auth:** hasAnyRole('CM Operator')

### Request Body (RefillRequestDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| tankId | String | @NotBlank | ✅ Yes |
| refillKg | BigDecimal | @NotNull, @DecimalMin("0.001") | ✅ Yes |
| lotRef | String | none | ❌ No |
| note | String | none | ❌ No |

### Response: 200 OK (void)

---

## POST /api/cm/end-of-shift
**Auth:** hasAnyRole('CM Operator')

### Request Body (EndOfShiftMeterRequestDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| tankId | String | @NotBlank | ✅ Yes |
| endMeterKg | BigDecimal | @NotNull, @DecimalMin("0") | ✅ Yes |
| note | String | none | ❌ No |

### Response: 200 OK (void)

---

## POST /api/cm/correction
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')

### Request Body (CorrectionRequestDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| tankId | String | @NotBlank | ✅ Yes |
| correctedKg | BigDecimal | @NotNull | ✅ Yes |
| reason | String | @NotBlank | ✅ Yes |

### Response: 200 OK (void)

---

## GET /api/cm/machines/{machineId}/refill-history
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')
**Path:** machineId (String, URL-encoded)
**Query:** limit (int, default=20)

### Response Body (Array of MachineRefillHistoryDto)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| recordedAt | LocalDateTime | No | "2026-11-16T14:00:00" |
| tankId | String | No | "RBL201-T1" |
| tankName | String | Yes | "Tank A" |
| material | String | Yes | "PE-HD" |
| quantity | BigDecimal | No | 50.000 |
| unit | String | No | "kg" |
| shiftCode | String | Yes | "A" |
| entryType | String | No | "REFILL" |

---

**Sign-off (Retroactive):**
- [x] BE dev: ตรวจสอบกับ CmMeterController.java แล้ว
- [x] FE dev: ตรวจสอบกับ CmMeterDashboard.jsx, cmMeterApi.js แล้ว
