# API Contract: C-02 Waste Weight Recording
**SA:** System Analyst · **Date:** 2026-11-16 · **Status:** Retroactive (เพิ่มหลัง implementation)
**Auth Base:** `hasAnyRole('CM Operator', 'DataAdmin', 'Document')`

---

## GET /api/cm/waste/types
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')

### Response Body (Array)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| code | String | No | "STARTUP_SCRAP" |
| nameTh | String | No | "เศษเริ่มเดิน" |
| nameEn | String | Yes | "Startup Scrap" |

---

## POST /api/cm/waste
**Auth:** hasAnyRole('CM Operator')

### Request Body (WasteWeightRecord via @RequestBody)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| machineId | String | @NotBlank | ✅ Yes |
| woNumber | String | none | ❌ No |
| wasteTypeCode | String | @NotBlank | ✅ Yes |
| weightKg | BigDecimal | @NotNull, @DecimalMin("0.001") | ✅ Yes |
| note | String | none | ❌ No |

### Response: 201 Created (void)

---

## GET /api/cm/waste/my-records
**Auth:** hasAnyRole('CM Operator')
**Description:** Records ของ current user ในกะปัจจุบัน

### Response Body (Array of WasteWeightRecordDto)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| id | Long | No | 1 |
| machineId | String | No | "RBL201" |
| woNumber | String | Yes | "WO-001" |
| wasteTypeCode | String | No | "STARTUP_SCRAP" |
| weightKg | BigDecimal | No | 2.500 |
| note | String | Yes | "หมายเหตุ" |
| recordedBy | String | No | "operator01" |
| recordedAt | LocalDateTime | No | "2026-11-16T08:30:00" |
| pendingRequestStatus | String | Yes | "PENDING" / "APPROVED" / "REJECTED" / null |

---

## PATCH /api/cm/waste/{id}/self-edit
**Auth:** hasAnyRole('CM Operator')
**Constraint:** เฉพาะ record ของตัวเอง ในกะปัจจุบันเท่านั้น

### Request Body (SelfEditDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| machineId | String | @NotBlank | ✅ Yes |
| woNumber | String | none | ❌ No |
| wasteTypeCode | String | @NotBlank | ✅ Yes |
| weightKg | BigDecimal | @NotNull, @DecimalMin("0.001") | ✅ Yes |
| note | String | none | ❌ No |
| reason | String | @NotBlank | ✅ Yes |

### Response: 200 OK (void)

> ⚠️ **Phase 1 bug:** FE ส่งโดยไม่มี `machineId` และ `reason` → HTTP 400 — แก้แล้ว commit 4935420

---

## POST /api/cm/waste/{id}/edit-request
**Auth:** hasAnyRole('CM Operator')
**Description:** ขอแก้ไข record ข้ามกะ (ต้องรอ Doc อนุมัติ)

### Request Body (EditRequestDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| requestedMachineId | String | none | ❌ No |
| requestedWoNumber | String | none | ❌ No |
| requestedWasteTypeCode | String | none | ❌ No |
| requestedWeightKg | BigDecimal | @DecimalMin("0.001") | ❌ No |
| requestedNote | String | none | ❌ No |
| reason | String | @NotBlank | ✅ Yes |
| requestPhotoIds | String | none | ❌ No |

### Response: 201 Created (void)

> ⚠️ **Phase 1 bug (fixed):** `requestedWoNumber` เดิม @NotBlank แต่ WO เป็น optional field → ลบ @NotBlank แล้ว commit 4935420

---

## GET /api/doc/waste-requests/pending
**Auth:** hasAnyRole('Document', 'DataAdmin')
**Description:** คำขอที่รอ Document role อนุมัติ

### Response Body (Array of WasteEditRequestDto)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| id | Long | No | 1 |
| requesterId | String | No | "operator01" |
| reason | String | No | "แก้ไขน้ำหนักผิด" |
| requestedWasteTypeCode | String | Yes | "STARTUP_SCRAP" |
| requestedWeightKg | BigDecimal | Yes | 3.000 |
| requestedWoNumber | String | Yes | "WO-002" |
| requestedNote | String | Yes | null |
| status | String | No | "PENDING" |
| requestedAt | LocalDateTime | No | "2026-11-16T10:00:00" |

---

## POST /api/doc/waste-requests/{id}/respond
**Auth:** hasAnyRole('Document', 'DataAdmin')

### Request Body (DocResponseDto)
| Field | Type | Validation | Required |
|-------|------|-----------|----------|
| decision | String | @NotBlank, @Pattern("APPROVED\|REJECTED") | ✅ Yes |
| docNote | String | none | ❌ No |

### Response: 200 OK (void)

> ⚠️ **Phase 1 bug (fixed):** FE ส่ง `{approved: boolean, note}` แทน `{decision: "APPROVED"/"REJECTED", docNote}` → HTTP 400 — แก้แล้ว commit 4935420

---

**Sign-off (Retroactive):**
- [x] BE dev: ตรวจสอบกับ WasteWeightController.java, DocEditRequestController.java แล้ว
- [x] FE dev: ตรวจสอบกับ cmWasteApi.js, SelfEditModal, EditRequestModal, DocWasteRequestDashboard แล้ว
