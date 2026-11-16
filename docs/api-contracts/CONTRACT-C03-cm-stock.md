# API Contract: C-03 CM Stock Card Dashboard
**SA:** System Analyst · **Date:** 2026-11-16 · **Status:** Retroactive
**Auth Base:** `hasAnyRole('CM Operator', 'DataAdmin')`

---

## GET /api/cm/stock/overview
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')

### Response Body (StockOverviewDto)
| Field | Type | Nullable | Example |
|-------|------|---------|---------|
| materials | Array\<StockOverviewItemDto\> | No | [...] |
| totalBalanceKg | BigDecimal | Yes | 1500.000 |
| totalActiveLots | Integer | Yes | 8 |
| alertCount | Integer | No | 2 |

**StockOverviewItemDto:**
| Field | Type | Nullable |
|-------|------|---------|
| materialCode | String | No |
| materialName | String | Yes |
| balanceKg | BigDecimal | Yes |
| totalInKg | BigDecimal | Yes |
| totalOutKg | BigDecimal | Yes |
| lotCount | Integer | No |
| lastMovementAt | LocalDateTime | Yes |
| reorderPointKg | BigDecimal | Yes |
| safetyStockKg | BigDecimal | Yes |
| stockStatus | String | No | "NORMAL" / "LOW" / "CRITICAL" / "UNKNOWN" |
| hasActiveAlert | Boolean | No |

---

## GET /api/cm/stock/{materialCode}
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')
**Path:** materialCode (String, URL-encoded)

### Response Body (MaterialDetailDto)
| Field | Type | Nullable |
|-------|------|---------|
| materialCode | String | No |
| materialName | String | Yes |
| balanceKg | BigDecimal | No |
| reorderPointKg | BigDecimal | Yes |
| safetyStockKg | BigDecimal | Yes |
| dailyAvgUsageKg | BigDecimal | Yes |
| estDaysRemaining | Integer | Yes |
| stockStatus | String | No |
| lots | Array\<LotBalanceDto\> | No |

**LotBalanceDto:**
| Field | Type | Nullable |
|-------|------|---------|
| lotRef | String | Yes |
| balanceKg | BigDecimal | No |
| receivedAt | LocalDateTime | Yes |
| lastActivityAt | LocalDateTime | Yes |

---

## GET /api/cm/stock/{materialCode}/stock-card
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')
**Path:** materialCode (URL-encoded)
**Query:**
| Param | Type | Required |
|-------|------|---------|
| from | LocalDate (ISO) | ❌ No |
| to | LocalDate (ISO) | ❌ No |
| machineId | String | ❌ No |
| lotRef | String | ❌ No |

### Response Body (StockCardResponseDto)
| Field | Type | Nullable |
|-------|------|---------|
| materialCode | String | No |
| materialName | String | Yes |
| movements | Array\<StockCardItemDto\> | No |
| totalInKg | BigDecimal | No |
| totalOutKg | BigDecimal | No |
| totalCorrectionKg | BigDecimal | No |
| openingBalance | BigDecimal | No |
| closingBalance | BigDecimal | No |

**StockCardItemDto:**
| Field | Type | Nullable |
|-------|------|---------|
| movementType | String | No | "IN" / "OUT" / "CORRECTION" |
| materialCode | String | No |
| lotRef | String | Yes |
| qtyKg | BigDecimal | Yes |
| negativeQty | BigDecimal | Yes |
| movementAt | LocalDateTime | No |
| actor | String | Yes |
| machineId | String | Yes |
| tankId | String | Yes |
| note | String | Yes |
| isCorrection | Boolean | No |
| sourceId | Long | Yes |
| sourceTable | String | Yes |
| runningBalance | BigDecimal | Yes |

---

## GET /api/cm/stock/multi-material-summary
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')
**Query:** days (int, default=7)

### Response Body (Array of MultiMaterialSummaryItemDto)
| Field | Type | Nullable |
|-------|------|---------|
| materialCode | String | No |
| materialName | String | Yes |
| balanceKg | BigDecimal | No |
| usedKg | BigDecimal | No |
| receivedKg | BigDecimal | No |
| changeKg | BigDecimal | No |
| trend | String | No | "UP" / "DOWN" / "STABLE" |
| stockStatus | String | No |

---

## GET /api/cm/stock/low-stock
**Auth:** hasAnyRole('CM Operator', 'DataAdmin')

### Response Body (Array of LowStockAlertDto)
| Field | Type | Nullable |
|-------|------|---------|
| id | Long | No |
| materialCode | String | No |
| balanceAtAlert | BigDecimal | No |
| reorderPoint | BigDecimal | No |
| estDaysRemaining | BigDecimal | Yes |
| alertDate | LocalDate | No |
| telegramSent | Boolean | No |
| telegramSentAt | LocalDateTime | Yes |
| resolvedAt | LocalDateTime | Yes |

---

## GET /api/cm/stock/reorder-config
**Auth:** hasRole('DataAdmin') only

### Response Body (Array of MaterialReorderConfig — Entity)
| Field | Type | Nullable |
|-------|------|---------|
| materialCode | String | No (PK) |
| reorderPointKg | BigDecimal | Yes |
| safetyStockKg | BigDecimal | Yes |
| reorderQtyKg | BigDecimal | Yes |
| leadTimeDays | Integer | Yes |
| isActive | Boolean | No |

---

## PATCH /api/cm/stock/reorder-config/{materialCode}
**Auth:** hasRole('DataAdmin') only

### Request Body (Partial patch — Map)
| Field | Type | Required |
|-------|------|---------|
| reorderPointKg | String (numeric) | ❌ No |
| safetyStockKg | String (numeric) | ❌ No |
| reorderQtyKg | String (numeric) | ❌ No |
| leadTimeDays | String (integer) | ❌ No |
| isActive | String ("true"/"false") | ❌ No |

### Response: 200 OK (MaterialReorderConfig entity)

---

**Scheduler (StockAlertScheduler):**
- Runs every 15 minutes (`fixedDelay=900_000`)
- Queries `v_material_balance` vs `material_reorder_config`
- Creates `low_stock_alert` rows when balance < reorderPointKg
- Resolves (sets `resolvedAt`) when balance recovers
- TODO: Send Telegram notification (not yet implemented)

---

**Sign-off (Retroactive):**
- [x] BE dev: ตรวจสอบกับ CmStockController.java, StockAlertScheduler.java แล้ว
- [x] FE dev: ตรวจสอบกับ cmStockApi.js, CmStockDashboard.jsx, StockCardTable.jsx แล้ว
