# W19 · Pallet Assembly + Print Label — Development Plan

**Status**: 📋 Design · pending sign-off before implementation
**Target**: Phase 1 extension · W19 · after Phase 1 UAT sign-off
**Owner**: Dev · with input from PD (form spec) + PC (workflow) + QA + MK (label design approval)
**References**:
- Reference form: `QP-PD-002-F046 Rev.02` (physical paper form used today, handwritten)
- Depends on: W10 Sub-Lot · W13 Barcode · W14 ZPL · W16.3 TrackOutPanel
- Related: existing `sub_lot.pallet_number` field (VARCHAR 50, nullable)

---

## 1. Objective

**"Digitize the pallet composition record — Operator scans finished boxes into a pallet, system tracks composition, and prints the pallet label in the exact layout of the existing paper form (QP-PD-002-F046 Rev.02). Additionally, support rearranging from an existing pallet as a starting template (for re-work, splits, or partial re-assembly)."**

### Business outcome
- ✅ ยกเลิกการเขียน pallet label ด้วยมือ (ลดข้อผิดพลาด + เวลา)
- ✅ Trace ได้ว่า pallet ไหนประกอบด้วย sub-lot อะไรบ้าง (บน DB)
- ✅ QA / Shift PD ยัง sign ปกติ (paper) แต่ข้อมูลทั้งหมด auto-fill
- ✅ ลบขั้นตอน "จดเลขกล่อง" ที่ error-prone (จากภาพจริง เห็นการขีดฆ่า/แก้)
- ✅ **Rearrange จาก pallet เดิม** — โหลด pallet เก่าเป็น template แล้วปรับ (add/remove boxes) · ของอาจไม่ครบเหมือนเดิม · original ไม่ถูกแก้ (audit intact)

---

## 1.5. Use Cases — เมื่อไหร่ใช้ Rearrange

### UC-1: Broken pallet ระหว่างขนส่ง
- Original pallet #7 → 16 กล่อง · ส่งไปคลัง
- ระหว่างขนส่ง กล่อง 3 ใบ damage (broke, wet, contaminated)
- Operator ต้อง rebuild pallet ใหม่: **โหลด pallet #7 เดิม → ลบกล่องเสีย 3 ใบ → close pallet #7A** (13 boxes)

### UC-2: QA reject partial
- Pallet #10 → 16 boxes · pending shipping
- QA random-inspect · เจอ 2 กล่องหลุด spec
- Operator **โหลด #10 → ลบ 2 กล่องเสีย → close #10A** (14 boxes) + 2 กล่องเสียย้ายไปขั้น NG

### UC-3: ลูกค้าสั่งเพิ่มเติม / split shipping
- Pallet #12 → 16 boxes · ลูกค้าขอส่งครึ่งเดียวก่อน
- Operator **โหลด #12 → ลบ 8 กล่อง → close #12A** (ส่ง 8) + **สร้าง #12B ใหม่** (8 ที่เหลือ)

### UC-4: Rebuild หลัง QA sign
- QA เซ็นแล้ว pallet #5 → detect กล่องผิด product ตอนพิมพ์ label
- ห้ามแก้ #5 (audit) → **โหลด #5 → remove กล่องผิด + add ที่ถูก → close #5R** (revision)

**หลักการ**: `original` **ไม่เคยถูกแก้** · `rearranged` = new pallet มี `parent_pallet_id` ชี้กลับ = trace ได้ครบ

---

## 2. Business Context — Reference Form Analysis

จากรูปตัวอย่าง (`3d799b1b-BarcodeTahara.pdf`) เห็นข้อมูลที่ต้องอยู่บน label:

### Header row
| Field | ตัวอย่าง | ที่มา |
|---|---|---|
| Company logo + name | Toyo Seikan (Thailand) Co.,Ltd. | static |
| CODE | `BSMH0500A` | product code |
| **19-20** (handwritten big) | lot date range (July 19-20) | derived from lot dates on this pallet |
| **7** (handwritten big, top-right) | pallet number in sequence | user-entered on close |
| PALLET | `1` | pallet number (small, in field) |
| LOT DATE: BOX NO. | table header | fixed |

### Main table — LOT NO. × BOX NO. matrix
- 1 row per LOT · up to **8 box numbers per row** (matrix grid, 8 columns)
- Auto-wrap to new row when > 8 boxes for same lot
- Boxes ที่ยกเลิก → ขีดฆ่า (ไม่เกิดใน digital version — สแกนเฉพาะ valid boxes)

จากภาพ:
| LOT NO. | BOX NOs |
|---|---|
| `260719` | 072, 073, 074, 075, 076, 077, 078, 079 |
| `260719` (cont.) | 080, 081, 082 |
| `260720` | 001, 002, 003, 004, 005 |

= **16 boxes across 2 lots** on this pallet

### Footer row
| Field | ตัวอย่าง | ที่มา |
|---|---|---|
| QA | signature + `19-07-26` | manual (พิมพ์ blank slot ให้ sign) |
| SHIFT PD (DAY / NIGHT) | ลายเซ็น under DAY | manual |
| จำนวน (Quantity) | `1680x` | **auto**: sum of boxQuantity of all sub-lots on this pallet |
| DATE | `'20 / 7 / 26` (26/07/2020?) | auto (close date) |
| Form footer | `<QP-PD-002-F046> Rev.02` | static |

### Sub-lot number decoding
- Physical label = 6-digit lot date + 3-digit box number = **9 digits**
- Example: `260719-072` = Lot ที่ผลิต **26 July 2019**, กล่องที่ **72**
- Wait — actually looking at date `19-07-26` in QA field = **Y-M-D format**, so `260719` = **19 July 2026** (YY-MM-DD reversed for form: 26/07/26 = 26 Jul 2026)
- **Our system's 12-digit sub-lot** already encodes date + sequence + Mod-10 → need mapping OR extract `lotDate` from ProductionReport

**Recommend**: Use our 12-digit sub-lot as internal barcode · display 9-digit derived format on the printed label (`YYMMDD-NNN` where NNN = boxSequence within that ParentLot)

---

## 3. Impact Analysis

### Tables affected
| Table | Change |
|---|---|
| `sub_lot` | **Add FK**: `pallet_id BIGINT NULL` |
| `sub_lot` | Add index on `pallet_id` for grouping |
| `sub_lot.status` | Add new status: `packed` (between `labeled` and `shipped`) |
| **New: `pallet`** | Master table for pallet records |
| **New: `pallet_close_log`** | Audit log of pallet close events (who/when/why) |

### Services affected
- `SubLotService` — add `assignToPallet()` · verify status = labeled first
- **New: `PalletService`** — CRUD + close + validate
- **New: `PalletLabelService`** — generate print-ready HTML/PDF matching form layout

### Controllers
- `SubLotController` — expose `POST /sub-lots/{id}/assign-pallet`
- **New: `PalletController`** — full REST

### Frontend
- **New**: `gdtahara-frontend/src/components/operator/PalletAssemblyPanel.jsx`
- Add menu entry in `OperatorDashboard` → "🎁 ประกอบ Pallet"

### Roles / permissions
- **Operator**: create · scan · close · print
- **PC / LD**: view all pallets · print re-copy
- **QA**: view (for physical signing)

---

## 4. Database Schema

### 4.1 New Flyway migration: `V18__Create_pallet_tables.sql`

```sql
-- Pallet master table
CREATE TABLE pallet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pallet_number VARCHAR(50) NOT NULL,   -- User-entered "7" from top-right
    product_id BIGINT NOT NULL,           -- FK products
    product_code VARCHAR(50) NOT NULL,    -- Denormalized for label print
    target_qty INT NULL,                  -- Optional target (from product master)
    actual_qty INT NOT NULL DEFAULT 0,    -- Sum of boxQuantity of assigned sub-lots
    box_count INT NOT NULL DEFAULT 0,     -- Count of sub-lots assigned

    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN · CLOSED · PRINTED · CANCELLED
    pallet_date DATE NOT NULL,            -- Date of pallet creation (for grouping)
    lot_date_min DATE NULL,               -- Earliest lot date of sub-lots on this pallet
    lot_date_max DATE NULL,               -- Latest lot date (for "19-20" big number)

    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    created_by_user_id BIGINT NOT NULL,
    closed_at DATETIME2 NULL,
    closed_by_user_id BIGINT NULL,
    printed_at DATETIME2 NULL,
    print_count INT NOT NULL DEFAULT 0,

    notes NVARCHAR(500) NULL,

    -- Rearrange support (§1.5 use cases)
    parent_pallet_id BIGINT NULL,                    -- FK to source pallet if this is a rearrange
    rearrange_reason NVARCHAR(500) NULL,             -- Why rearranged (Damage · QA reject · Split · Revision)
    revision_suffix VARCHAR(10) NULL,                -- e.g. "A", "B", "R" for #7A #7B #5R

    CONSTRAINT fk_pallet_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_pallet_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_pallet_closed_by FOREIGN KEY (closed_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_pallet_parent FOREIGN KEY (parent_pallet_id) REFERENCES pallet(id),
    CONSTRAINT uk_pallet_number_date_product_rev
        UNIQUE (pallet_number, pallet_date, product_id, revision_suffix)
);

CREATE INDEX ix_pallet_status ON pallet(status);
CREATE INDEX ix_pallet_date ON pallet(pallet_date DESC);
CREATE INDEX ix_pallet_product ON pallet(product_id);
CREATE INDEX ix_pallet_parent ON pallet(parent_pallet_id) WHERE parent_pallet_id IS NOT NULL;

-- Extend sub_lot with pallet FK
ALTER TABLE sub_lot ADD pallet_id BIGINT NULL;
ALTER TABLE sub_lot ADD CONSTRAINT fk_sub_lot_pallet
    FOREIGN KEY (pallet_id) REFERENCES pallet(id);
CREATE INDEX ix_sub_lot_pallet ON sub_lot(pallet_id) WHERE pallet_id IS NOT NULL;

-- Audit log
CREATE TABLE pallet_close_log (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pallet_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,          -- CREATED · BOX_ADDED · BOX_REMOVED · CLOSED · PRINTED · REOPENED · REARRANGED_FROM
    sub_lot_id BIGINT NULL,               -- If action relates to specific box
    ref_pallet_id BIGINT NULL,            -- If action = REARRANGED_FROM · points to source pallet
    performed_by BIGINT NOT NULL,
    performed_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    reason NVARCHAR(500) NULL,

    CONSTRAINT fk_pcl_pallet FOREIGN KEY (pallet_id) REFERENCES pallet(id),
    CONSTRAINT fk_pcl_sub_lot FOREIGN KEY (sub_lot_id) REFERENCES sub_lot(id),
    CONSTRAINT fk_pcl_user FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE INDEX ix_pcl_pallet ON pallet_close_log(pallet_id, performed_at DESC);
```

### 4.2 Rollback: `V18__Create_pallet_tables_rollback.sql`
```sql
DROP TABLE IF EXISTS pallet_close_log;
ALTER TABLE sub_lot DROP CONSTRAINT fk_sub_lot_pallet;
ALTER TABLE sub_lot DROP COLUMN pallet_id;
DROP TABLE IF EXISTS pallet;
```

---

## 5. Backend Design

### 5.1 Entities

**`Pallet.java`**
```java
@Entity @Table(name = "pallet")
@Data @NoArgsConstructor @AllArgsConstructor
public class Pallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pallet_number", nullable = false, length = 50)
    private String palletNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "target_qty")
    private Integer targetQty;

    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty = 0;

    @Column(name = "box_count", nullable = false)
    private Integer boxCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";   // OPEN · CLOSED · PRINTED · CANCELLED

    @Column(name = "pallet_date", nullable = false)
    private LocalDate palletDate;

    @Column(name = "lot_date_min")
    private LocalDate lotDateMin;

    @Column(name = "lot_date_max")
    private LocalDate lotDateMax;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "closed_at") private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private User closedBy;

    @Column(name = "printed_at") private LocalDateTime printedAt;
    @Column(name = "print_count", nullable = false) private Integer printCount = 0;

    @Column(length = 500) private String notes;

    // Rearrange support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_pallet_id")
    private Pallet parentPallet;

    @Column(name = "rearrange_reason", length = 500)
    private String rearrangeReason;

    @Column(name = "revision_suffix", length = 10)
    private String revisionSuffix;     // "A", "B", "R"

    @OneToMany(mappedBy = "parentPallet", fetch = FetchType.LAZY)
    private List<Pallet> derivedPallets = new ArrayList<>();

    @OneToMany(mappedBy = "pallet", fetch = FetchType.LAZY)
    private List<SubLot> subLots = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (palletDate == null) palletDate = LocalDate.now();
    }
}
```

**Extend `SubLot.java`**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "pallet_id")
private Pallet pallet;
```

### 5.2 Services

**`PalletService.java`** (new)
```java
public interface PalletService {
    PalletDto createOpen(Long productId, String palletNumber, LocalDate palletDate, String username);
    PalletDto scanBox(Long palletId, String subLotNumber, String username);   // adds sub-lot
    PalletDto removeBox(Long palletId, Long subLotId, String username);
    PalletDto close(Long palletId, String username);
    PalletDto reopen(Long palletId, String username, String reason);          // admin only
    PalletDto cancel(Long palletId, String username, String reason);
    PalletDto getById(Long palletId);
    List<PalletDto> listOpenByUser(String username);
    List<PalletDto> listByDate(LocalDate date, String productCode);
    PalletLabelDto renderLabel(Long palletId);  // for print

    // ── Rearrange (§1.5) ──────────────────────────────────────────────────
    /**
     * Create a NEW pallet using an existing pallet as template.
     * - Copies composition: adds all sub-lots from source that are still eligible
     *   (status = packed AND on source pallet)
     * - Source pallet remains untouched (audit intact)
     * - New pallet gets parent_pallet_id = sourcePalletId
     * - New pallet_number = user-provided (typically source + suffix like "7A")
     * - Sub-lots move: their pallet_id updates to new pallet, source pallet's actual_qty/box_count reduces
     *
     * Note on "moving" boxes: since sub-lot can only be on 1 pallet at a time,
     * boxes are physically moved from source → new pallet. Source becomes historical
     * record via pallet_close_log entries (REARRANGED_FROM → new pallet).
     *
     * If user wants source unchanged, they must first remove specific boxes from source
     * (source stays CLOSED · boxes go back to status = labeled · then reassign to new pallet).
     */
    PalletDto rearrangeFrom(Long sourcePalletId, String newPalletNumber,
                             String revisionSuffix, String reason,
                             List<Long> subLotIdsToInclude,  // null = all eligible
                             String username);

    /**
     * List all pallets derived from source (via parent_pallet_id chain).
     */
    List<PalletDto> getDerivedPallets(Long sourcePalletId);

    /**
     * Get lineage: source → derived → derived-of-derived · trace full history.
     */
    List<PalletDto> getPalletLineage(Long palletId);
}
```

### 5.2.1 `rearrangeFrom` — Business Rules

1. **Source pallet must be CLOSED or PRINTED** (not OPEN — no sense rearranging what isn't done)
2. **New pallet number required** — recommend suffix pattern (7 → 7A, 7A → 7B, 10 → 10R)
3. **`subLotIdsToInclude`**:
   - `null` or empty → copy ALL sub-lots from source (full move)
   - Provided list → only copy specified boxes (partial move · source retains the rest)
4. **Same product required** — cannot mix products across rearrange
5. **On success**:
   - New pallet created with status = OPEN
   - Each included sub-lot: `pallet_id` → new pallet
   - Source pallet: `actual_qty` and `box_count` reduced by moved boxes
   - Source pallet: **stays CLOSED** (not reopened — history intact)
   - `pallet_close_log`: 3 entries per moved box (REARRANGED_FROM on new · BOX_REMOVED on source · BOX_ADDED on new)
6. **Rollback**: if new pallet is CANCELLED, sub-lots return to source (if source still exists) OR revert to status=labeled (if source deleted)
7. **Cannot chain > 5 levels deep** — prevents infinite loops (config: max lineage depth)

**Key validation rules in `scanBox`**:
1. Sub-lot must exist (by subLotNumber, with Mod-10 check)
2. Sub-lot.status must be `labeled` (from Track-Out)
3. Sub-lot.pallet_id must be NULL (not already on a pallet)
4. Sub-lot.product must match pallet.product (no mixed products)
5. If different lot from existing pallet contents → warn user (allowed but flagged)
6. On success: set sub_lot.pallet_id, sub_lot.status = 'packed', pallet.actual_qty += boxQty, pallet.box_count += 1, update lot_date_min/max

### 5.3 DTOs

**`PalletDto.java`**
```java
Long id · String palletNumber · String productCode · String productName
Integer actualQty · Integer targetQty · Integer boxCount
String status · LocalDate palletDate · LocalDate lotDateMin · LocalDate lotDateMax
LocalDateTime createdAt · String createdByName
LocalDateTime closedAt · String closedByName
// Rearrange lineage
Long parentPalletId · String parentPalletNumber · String revisionSuffix · String rearrangeReason
Integer derivedCount    // count of pallets that used this as source
List<PalletBoxDto> boxes
```

**`RearrangePalletRequest.java`** (new)
```java
String newPalletNumber        // e.g. "7A"
String revisionSuffix         // e.g. "A", "B", "R"
String reason                 // one of: DAMAGE · QA_REJECT · SPLIT · REVISION · OTHER
List<Long> subLotIdsToInclude // null = all · specified = partial
```

**`PalletBoxDto.java`**
```java
Long subLotId · String subLotNumber · String displaySubLotNumber (9-digit form: YYMMDD-NNN)
LocalDate lotDate · Integer boxSequence · Integer boxQuantity · BigDecimal weightKg
```

**`PalletLabelDto.java`** — for label rendering
```java
String palletNumber · String productCode
LocalDate lotDateMin · LocalDate lotDateMax
Integer totalQty · LocalDate closeDate
Map<LocalDate, List<Integer>> lotBoxMatrix  // key = lot date, value = box numbers list
```

### 5.4 Controller

**`PalletController.java`** (new · under `@RequestMapping("/api/operator/pallets")`)
```java
POST   /api/operator/pallets                — create open pallet
POST   /api/operator/pallets/{id}/scan      — scan box (body: {subLotNumber})
DELETE /api/operator/pallets/{id}/boxes/{subLotId}  — remove box
POST   /api/operator/pallets/{id}/close     — close pallet
POST   /api/operator/pallets/{id}/print     — mark printed + return HTML
POST   /api/operator/pallets/{id}/reopen    — admin only
POST   /api/operator/pallets/{id}/rearrange — ✨ create new pallet from this one as template
                                              body: RearrangePalletRequest
GET    /api/operator/pallets/{id}           — get details
GET    /api/operator/pallets/{id}/derived   — ✨ list pallets rearranged FROM this
GET    /api/operator/pallets/{id}/lineage   — ✨ full ancestry chain
GET    /api/operator/pallets?status=OPEN&user=me  — list mine
GET    /api/operator/pallets?includeHistorical=true&product=X&date=Y  — for rearrange picker
GET    /api/operator/pallets/{id}/label     — get render data (JSON)
```

---

## 6. Frontend Design

### 6.1 New component: `PalletAssemblyPanel.jsx`

**Location**: `gdtahara-frontend/src/components/operator/PalletAssemblyPanel.jsx`

**Screen zones**:
```
┌─ Header ─────────────────────────────────────────────────────────┐
│  🎁 ประกอบ Pallet    [+ สร้างใหม่] [🔄 Rearrange จาก pallet เดิม] │
│  [Pallets เปิดอยู่: 2]                                           │
├─ Active Pallet Selector (if multiple open) ──────────────────────┤
│  [Product BSMH0500A · Pallet #7 · 12/16 boxes ▼]                │
│  ↪ Rearranged from #7 · reason: DAMAGE  (if applicable, shown)   │
├─ Scan Input ─────────────────────────────────────────────────────┤
│  [เลข Sub-Lot: __________]  [สแกน / เพิ่ม]                       │
│  Placeholder: "สแกน barcode ที่ label หรือพิมพ์ Enter"            │
├─ Progress ───────────────────────────────────────────────────────┤
│  Current: 12 boxes · 1,260 pcs · [━━━━━━━━░░] 75%                │
│  Target: 16 boxes · 1,680 pcs (may be incomplete if rearranged)  │
├─ Boxes Table (grouped by lot) ───────────────────────────────────┤
│  Lot 260719 (11 boxes): 072 073 074 075 076 077 078 079         │
│                          080 081 082  [❌ remove]                  │
│  Lot 260720 (5 boxes):  001 002 003 004 005                       │
├─ Actions ─────────────────────────────────────────────────────────┤
│  [❌ ยกเลิก Pallet]  [🔒 ปิด Pallet + พิมพ์]                       │
└───────────────────────────────────────────────────────────────────┘
```

**Rearrange Modal** (opened via "🔄 Rearrange จาก pallet เดิม" button):
```
┌─ 🔄 Rearrange from existing pallet ──────────────────────────────┐
│  Step 1: Search pallet                                            │
│    Product: [BSMH0500A ▼]   Date: [26/07/2026 ▼]                  │
│    Pallet #: [7]     [🔍 ค้นหา]                                    │
│                                                                   │
│  Step 2: Select source pallet                                     │
│    ● Pallet #7 · 16 boxes · 1,680 pcs · Closed 26/07/26 · 📄       │
│      [👁 ดูรายละเอียด]                                             │
│                                                                   │
│  Step 3: New pallet info                                          │
│    New pallet #: [7A_____]    Revision: [A ▼]                     │
│    Reason: (● DAMAGE ○ QA_REJECT ○ SPLIT ○ REVISION ○ OTHER)       │
│    Notes: [________________________________________]              │
│                                                                   │
│  Step 4: Select boxes to include                                  │
│    ☑ ทั้งหมด (16)  หรือเลือกทีละกล่อง:                             │
│    ☑ 260719-072  ☑ 260719-073  ☑ 260719-074  ☒ 260719-075 (skip)  │
│    ☑ 260719-076  ...                                              │
│                                                                   │
│  Preview: จะสร้าง Pallet #7A · 15 boxes · 1,575 pcs               │
│  Source #7 จะเหลือ: 1 box · 105 pcs (stays CLOSED)                │
│                                                                   │
│  [ยกเลิก]                                    [✅ Rearrange]        │
└───────────────────────────────────────────────────────────────────┘
```

**Interactions (Rearrange flow)**:
- Search by product + date + pallet number → autocomplete list
- Preview shows real-time count updates as user checks/unchecks boxes
- Warning if partial rearrange leaves source with 0 boxes: "Source pallet will be empty — mark as CANCELLED?"
- On success → open new Pallet in scan mode (Operator can add more boxes if wanted)
- Lineage indicator visible in header: "↪ Rearranged from #7"

**Interactions**:
- **สแกน**: Enter/scanner input → validate Mod-10 → call `POST /scan` → update table
- **Duplicate scan**: alert "กล่องนี้อยู่ pallet อื่นแล้ว"
- **Wrong product**: alert "product ไม่ตรงกับ pallet นี้"
- **Wrong status** (not labeled): alert "กล่องนี้ยังไม่ Track-Out — ต้อง Track-Out ก่อน"
- **Close pallet**:
  - Confirm dialog: "ปิด pallet? Total N boxes / M pcs"
  - Enter pallet number (big "7" from form)
  - After close → open print dialog

### 6.2 Print component: `PalletLabelPrint.jsx`

**Layout** (HTML → `window.print()` · A5 landscape · fits paper form):
```html
<div class="pallet-label" style="width: 210mm; height: 148mm;">
  <header>
    <img src="/toyo-logo.png" />
    <span>Toyo Seikan (Thailand) Co.,Ltd.</span>
    <span>CODE: BSMH0500A</span>
  </header>
  <div class="big-numbers">
    <span class="big-lot-range">19-20</span>
    <span class="big-pallet-no">7</span>
  </div>
  <div class="pallet-field">PALLET: 1</div>
  <div class="lot-date-header">LOT DATE : BOX NO.</div>
  <table class="lot-matrix">
    <thead>
      <tr><th>LOT NO.</th><th colspan="8">BOX NO.</th></tr>
    </thead>
    <tbody>
      <tr><td>260719</td><td>072</td><td>073</td>…<td>079</td></tr>
      <tr><td></td>       <td>080</td><td>081</td><td>082</td>…</tr>
      <tr><td>260720</td> <td>001</td><td>002</td>…<td>005</td></tr>
    </tbody>
  </table>
  <footer>
    <div class="qa-box">
      <label>QA</label>
      <div class="signature-slot"></div>
    </div>
    <div class="shift-box">
      <label>SHIFT PD</label>
      <table><tr><th>DAY</th><th>NIGHT</th></tr>
             <tr><td></td>   <td></td></tr></table>
    </div>
    <div class="qty-box">
      <label>จำนวน</label>
      <span>1,680x</span>
    </div>
    <div class="date-box">DATE: 20 / 07 / 26</div>
    <div class="form-code">&lt;QP-PD-002-F046&gt; Rev.02</div>
  </footer>
</div>
```

**CSS considerations**:
- Print-only stylesheet with `@page { size: A5 landscape; margin: 5mm; }`
- Force 8 columns for BOX NO. table (pad empty cells)
- Big numbers use handwritten-style font (or blue color to mimic marker)
- Signature slot = empty box height 30mm

### 6.3 Menu integration

Update `gdtahara-frontend/src/components/operator/OperatorDashboard.jsx`:
```jsx
<Button onClick={() => setView('pallet-assembly')}>
  🎁 ประกอบ Pallet
</Button>
```

---

## 7. API Endpoints Summary

| Method | Path | Role | Body | Response |
|---|---|---|---|---|
| POST | `/api/operator/pallets` | Operator | `{productId, palletNumber, palletDate}` | `PalletDto` |
| POST | `/api/operator/pallets/{id}/scan` | Operator | `{subLotNumber}` | `PalletDto` |
| DELETE | `/api/operator/pallets/{id}/boxes/{subLotId}` | Operator (own) | — | `PalletDto` |
| POST | `/api/operator/pallets/{id}/close` | Operator | `{palletNumber?}` | `PalletDto` |
| POST | `/api/operator/pallets/{id}/print` | Operator/PC | — | `{htmlOrPdfUrl}` |
| POST | `/api/operator/pallets/{id}/reopen` | Admin | `{reason}` | `PalletDto` |
| GET | `/api/operator/pallets/{id}` | Operator/PC | — | `PalletDto` |
| GET | `/api/operator/pallets` | Operator/PC | `?status=&product=&date=` | `List<PalletDto>` |
| GET | `/api/operator/pallets/{id}/label` | Operator/PC | — | `PalletLabelDto` |

---

## 8. Development Phases (3.5 MD estimate · +1 MD for Rearrange)

### W19.1 — DB + Backend Core (1 MD)
- Flyway V18 migration (test on H2 + SQL Server) · includes `parent_pallet_id` FK
- `Pallet` entity + repository
- Extend `SubLot` with FK
- `PalletService` with core methods: create, scanBox, close, getById
- Unit tests (8+): validate rules (Mod-10, status = labeled, no cross-product)

### W19.2 — REST + DTO (0.5 MD)
- `PalletController` all endpoints (including rearrange · derived · lineage)
- DTOs: `PalletDto`, `PalletBoxDto`, `PalletLabelDto`, `RearrangePalletRequest`
- Global exception handling for validation errors
- Postman collection

### W19.3 — Frontend Scan Panel (0.5 MD)
- `PalletAssemblyPanel.jsx` — create, scan, remove, close
- Menu entry in `OperatorDashboard`
- Real-time progress bar

### W19.4 — Print Label (0.5 MD)
- `PalletLabelPrint.jsx` — HTML matching form layout
- Print CSS (A5 landscape · signature slots · handwriting font)
- Print button in `PalletAssemblyPanel`
- Reprint from history

### W19.5 — ✨ Rearrange feature (1 MD · NEW)
- Backend: `PalletService.rearrangeFrom()` · unit tests 6+ scenarios (§10)
- REST: `POST /api/operator/pallets/{id}/rearrange` + list/lineage endpoints
- Frontend: `PalletRearrangeDialog.jsx` — 4-step modal (search · select source · new pallet info · box selection)
- Update `PalletAssemblyPanel` header to show lineage indicator
- E2E: rearrange scenario with partial move + verify lineage

### W19.6 — Testing + Training doc (0.5 MD)
- E2E test: scan → close → print → verify on PDF
- E2E test: create → close → rearrange to #A → verify source unchanged + new pallet correct
- Add `08-operator-pallet-assembly.md` to `docs/training/` (include Rearrange section)
- Update `01-operator-daily-flow.md` (mention step 8 = pallet + optional rearrange)

---

## 9. File Structure — Full List

### Backend (new)
```
src/main/java/com/gdtahara/gdtaharabackend/
├── model/Pallet.java                        (NEW)
├── model/SubLot.java                        (MODIFY: add pallet FK)
├── repository/PalletRepository.java         (NEW)
├── repository/PalletCloseLogRepository.java (NEW)
├── service/PalletService.java               (NEW · interface)
├── service/PalletServiceImpl.java           (NEW · impl)
├── service/PalletLabelService.java          (NEW · HTML generator)
├── controller/PalletController.java         (NEW)
├── dto/PalletDto.java                       (NEW)
├── dto/PalletBoxDto.java                    (NEW)
├── dto/PalletLabelDto.java                  (NEW)
├── dto/CreatePalletRequest.java             (NEW)
├── dto/ScanBoxRequest.java                  (NEW)

src/main/resources/db/migration/
└── V18__Create_pallet_tables.sql            (NEW)

src/test/java/com/gdtahara/gdtaharabackend/service/
└── PalletServiceTest.java                   (NEW · 8+ tests)

src/test/java/com/gdtahara/gdtaharabackend/controller/
└── PalletControllerIT.java                  (NEW · IT tests)

src/test/java/com/gdtahara/gdtaharabackend/e2e/
└── PalletFlowE2ETest.java                   (NEW · full flow)
```

### Frontend (new)
```
gdtahara-frontend/src/
├── api/phase1Api.js                         (MODIFY: add pallet + rearrange endpoints)
├── components/operator/
│   ├── PalletAssemblyPanel.jsx              (NEW · main scan UI + lineage indicator)
│   ├── PalletLabelPrint.jsx                 (NEW · print component)
│   ├── PalletRearrangeDialog.jsx            (NEW · ✨ 4-step modal for rearrange)
│   ├── PalletLineageBadge.jsx               (NEW · ✨ shows "↪ from #7")
│   └── OperatorDashboard.jsx                (MODIFY: add menu entry)
└── styles/
    └── pallet-label-print.css               (NEW · print-specific CSS)
```

### Docs (new)
```
docs/
├── phase1/w19-pallet-assembly-plan.md       (THIS FILE)
└── training/08-operator-pallet-assembly.md  (NEW · training)
```

---

## 10. Testing Plan

### Unit tests (PalletServiceTest)
1. ✅ createOpen — success creates OPEN status
2. ✅ scanBox — happy path, adds to pallet, updates counters
3. ❌ scanBox — Mod-10 fails → validation error
4. ❌ scanBox — sub-lot status = open (not labeled) → error
5. ❌ scanBox — sub-lot already on another pallet → error
6. ❌ scanBox — product mismatch → error
7. ✅ scanBox — different lot same product → allowed (with warning)
8. ✅ removeBox — removes and updates counters correctly
9. ✅ close — sets status CLOSED, closed_at, closedBy
10. ❌ close — empty pallet → error "no boxes"
11. ✅ reopen — admin can reopen CLOSED pallet
12. ❌ scanBox after close → error "pallet is closed"

### Rearrange tests (PalletRearrangeTest · ✨ new)
13. ✅ rearrangeFrom — full move (subLotIds=null) copies ALL boxes to new pallet · source qty=0
14. ✅ rearrangeFrom — partial move (5 of 16 boxes) · source retains 11 · new has 5
15. ✅ rearrangeFrom — sub-lots move (pallet_id updated) · source pallet stays CLOSED
16. ❌ rearrangeFrom — source pallet OPEN (not closed) → error "close source first"
17. ❌ rearrangeFrom — sub-lot not on source anymore (was moved earlier) → skip with warning
18. ❌ rearrangeFrom — max lineage depth exceeded (>5 levels) → error
19. ✅ rearrangeFrom — audit log has 3 entries per moved box (REARRANGED_FROM · BOX_REMOVED · BOX_ADDED)
20. ✅ getDerivedPallets — returns pallets in lineage chain
21. ✅ getPalletLineage — full ancestry chain (traces parent → parent → ...)
22. ✅ cancel derived pallet — sub-lots return to source (if source still has room)

### Integration tests
- Full POST/GET cycle via MockMvc
- Verify audit_logs entries created

### E2E test (H2)
- Setup: create WO → confirm 5 boxes (RunCard) → Track-Out all → create pallet → scan all → close → verify HTML contains all 5 box numbers

### Manual QA scenarios
1. Scan 16 boxes from 2 lots → verify matrix renders 8 boxes/row
2. Try to add 17th box exceeding target → warn but allow
3. Print, sign QA + shift PD, verify layout matches paper form side-by-side
4. Reprint after signature — verify print_count += 1
5. ✨ Rearrange full: pallet #7 (16 boxes) → #7A · verify #7 now 0 boxes CLOSED · #7A has 16
6. ✨ Rearrange partial: #10 (16) → #10A (14 selected) · verify #10 has 2 remaining · #10A has 14
7. ✨ Rearrange after QA reject: exclude 2 damage boxes · verify those 2 revert to status=labeled (available for new pallet)
8. ✨ Chain rearrange: #5 → #5R → #5R2 → verify lineage endpoint returns all 3
9. ✨ Cancel #7A → verify source #7 receives boxes back OR sub-lots revert to labeled

---

## 11. Timeline (Estimated)

| Week | Task | MD |
|---|---|---|
| W19 Mon-Tue | Backend core (DB + Service + REST + basic tests) | 1.5 |
| W19 Wed | Frontend scan panel | 0.5 |
| W19 Thu | Print label + CSS + reprint | 0.5 |
| W19 Fri | ✨ Rearrange backend (service + REST + tests) | 0.5 |
| W20 Mon | ✨ Rearrange frontend (4-step modal + lineage badge) | 0.5 |
| W20 Tue | Training doc + UAT integration | 0.5 |
| W20 Wed | UAT execution (Operators + PD + QA + PC for rearrange) | — |
| W20 Thu | Fix + sign-off | — |

**Total dev: 3.5 MD · Ready for pilot: end of W20**

---

## 12. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Print layout ไม่ตรงกับฟอร์มกระดาษ (fit page ยาก) | High — QA reject | Preview page ทำก่อน · เทียบ side-by-side กับกระดาษจริงก่อน sign-off |
| Operator สแกนผิด pallet | Med — mixed products | Product validation ก่อน scan · alert รุนแรง |
| Sub-lot ยังไม่ track-out แต่ต้องการเข้า pallet | Med — workflow ผิด | Force status = `labeled` · error message ชัด |
| Multiple pallets เปิดพร้อมกันของ operator เดียว | Low — สับสน | UI selector ชัด · list ที่ header |
| ต้องพิมพ์ซ้ำเพราะกระดาษเสีย | Low | Reprint endpoint · print_count log |
| Barcode display format (9-digit vs 12-digit) confuse Operator | Med | Show both in UI: `260719-072 (260719 072 8)` |
| Cross-shift pallet (start day, close night) | Low | Allow multi-day open · display `pallet_date` = created day |
| ✨ Rearrange loop (A rearranges from B rearranges from A) | Med — data corruption | Enforce `parent_pallet_id` DAG · max depth 5 · unit test 18 |
| ✨ Rearrange partial leaves source empty but CLOSED | Low — confusion | Warning modal · offer to cancel source pallet |
| ✨ Sub-lots "moved" but user expected "copied" | High — misunderstanding | Explicit UI text: "จะย้ายกล่องจาก #7 มา #7A · #7 จะเหลือน้อยลง" |
| ✨ Rearrange after label already printed + shipped | High — physical/system out of sync | Only allow rearrange if `printed_at IS NULL` OR require explicit "override" flag with reason |

---

## 13. Open Questions (ต้องยืนยันกับทีม)

1. **Pallet number**: manual entry (จากเดิม "7" เขียนมือ) หรือให้ระบบ auto-increment?
   - **Recommend**: Manual (ทีมคุ้นเคยกับตัวเลขเดิม)

2. **Target qty**: ต่อ product? หรือ manual ทุกครั้ง?
   - **Recommend**: เก็บใน `products.pallet_target_qty` (add column · nullable) · แสดงเป็น hint ไม่ hard-block

3. **1 Operator เปิดหลาย pallets ได้พร้อมกันไหม?**
   - **Recommend**: ได้ · list ที่ header · เลือกก่อน scan

4. **QA / Shift PD sign — digital signature หรือ paper สแกนเก็บ?**
   - **Phase 1 W19**: paper sign (ระบบพิมพ์ blank slot ให้เซ็น)
   - **Phase 2**: อาจเพิ่ม e-sign

5. **Pallet label = ฟอร์มเดียวกันเป๊ะ หรือ redesign ให้อ่านง่ายกว่า?**
   - **Recommend**: match ฟอร์มเดิม 100% (เพื่อ audit + ทีมคุ้นเคย) · phase 2 ค่อย redesign

6. **แสดง lot date format**: `260719` (YYMMDD) หรือ `19/07/26` (DD/MM/YY)?
   - **Recommend**: match ฟอร์มเดิม = `YYMMDD` (6 หลัก)

7. **หลัง close แล้ว scan เพิ่มได้ไหม?**
   - **Recommend**: ไม่ได้ · ต้อง reopen (admin only) พร้อม reason
   - **Alternative**: ใช้ Rearrange สร้าง pallet ใหม่ (#7 → #7A) แทน — ดีกว่า reopen เพราะไม่ทำลาย history

8. **✨ Rearrange — ย้าย vs copy กล่อง?**
   - **Recommend**: MOVE (sub-lot goes to new pallet, source loses it)
   - **Rationale**: sub-lot อยู่ 1 pallet ต่อครั้งเท่านั้น (physical + system)
   - ถ้าอยากได้ pallet ใหม่แบบ "clone" ที่ยังมีกล่องเดิม — ไม่รองรับ (ไม่มี physical basis)

9. **✨ Rearrange — source ที่เหลือ 0 boxes ทำอะไร?**
   - **Recommend**: Warning + option ให้ CANCEL source pallet
   - Source ที่ยัง CLOSED แต่ 0 boxes จะดูสับสนในรายงาน

10. **✨ Rearrange — restrict role อย่างไร?**
    - **Recommend**: Operator + PC ทำได้ · แต่ **ต้องระบุ reason**
    - Admin เท่านั้นทำได้ถ้า source `printed_at IS NOT NULL` (label พิมพ์แล้ว = อาจถูกส่งไปแล้ว)

11. **✨ Rearrange — revision suffix pattern**
    - **Recommend**: single letter A/B/C/... สำหรับ minor · R สำหรับ revision · R2/R3 สำหรับหลาย revision
    - Auto-suggest next available: #7 → #7A · #7A → #7B (ระบบเสนอ · user แก้ได้)

12. **✨ Rearrange — จำกัด lineage depth?**
    - **Recommend**: max 5 levels · เกินแล้ว error "สร้าง pallet ใหม่ตั้งแต่ต้นแทน"
    - เกิดขึ้นน้อยมากในทางปฏิบัติ · แต่ป้องกัน infinite loop

---

## 14. Sample SQL — Verify + Query

### 14.1 Find all sub-lots ready to pallet (status = labeled, no pallet)
```sql
SELECT sl.sub_lot_number, sl.box_quantity, pr.product_id, p.product_code
FROM sub_lot sl
JOIN production_report pr ON sl.production_report_id = pr.id
JOIN products p ON pr.product_id = p.id
WHERE sl.status = 'labeled'
  AND sl.pallet_id IS NULL
ORDER BY sl.confirmed_at DESC;
```

### 14.2 Get pallet composition (for label rendering)
```sql
SELECT
  DATE(pr.report_date) AS lot_date,
  sl.sub_lot_number,
  sl.box_quantity,
  ROW_NUMBER() OVER (PARTITION BY DATE(pr.report_date) ORDER BY sl.confirmed_at) AS box_seq
FROM sub_lot sl
JOIN production_report pr ON sl.production_report_id = pr.id
WHERE sl.pallet_id = ?
ORDER BY lot_date, box_seq;
```

### 14.3 Verify pallet totals
```sql
SELECT
  p.id, p.pallet_number, p.actual_qty, p.box_count,
  COUNT(sl.id) AS actual_box_count,
  SUM(sl.box_quantity) AS actual_pcs
FROM pallet p
LEFT JOIN sub_lot sl ON sl.pallet_id = p.id
WHERE p.id = ?
GROUP BY p.id, p.pallet_number, p.actual_qty, p.box_count;
-- box_count should = actual_box_count · actual_qty should = actual_pcs
```

---

## 15. VSCode Prompts (Ready to Paste)

### Prompt 1: Backend scaffold
```
Implement W19.1 Pallet Assembly backend:

1. Add Flyway V18__Create_pallet_tables.sql per docs/phase1/w19-pallet-assembly-plan.md §4
2. Create Pallet entity (§5.1)
3. Extend SubLot entity with pallet_id FK
4. Create PalletRepository, PalletCloseLogRepository
5. Create PalletService interface + PalletServiceImpl per §5.2
6. Enforce validation rules in scanBox (Mod-10, status=labeled, no pallet, product match)
7. Emit audit_logs for all state transitions
8. Add unit tests in PalletServiceTest — cover all 12 scenarios in §10
9. Run mvn test · confirm all green
```

### Prompt 2: REST + DTO
```
Implement W19.2 Pallet REST layer per docs/phase1/w19-pallet-assembly-plan.md §5.3, §5.4, §7:

1. Create PalletDto, PalletBoxDto, PalletLabelDto, CreatePalletRequest, ScanBoxRequest
2. Create PalletController with all 9 endpoints
3. Wire OpenAPI/Swagger annotations
4. Add PalletControllerIT with MockMvc for POST /scan happy path + 2 error paths
5. Add Postman collection entry
6. Test with existing Bearer token flow
```

### Prompt 3: Frontend scan panel
```
Implement W19.3 PalletAssemblyPanel per docs/phase1/w19-pallet-assembly-plan.md §6:

1. Create gdtahara-frontend/src/components/operator/PalletAssemblyPanel.jsx
2. Layout: header + selector + scan input + progress bar + boxes table + actions
3. Real-time updates after each scan
4. Handle errors: Mod-10 fail, not labeled, wrong product, already on pallet
5. Add menu entry "🎁 ประกอบ Pallet" in OperatorDashboard
6. Add API calls to phase1Api.js (createPallet, scanBox, removeBox, closePallet, listPallets)
```

### Prompt 4: Print label
```
Implement W19.4 PalletLabelPrint per docs/phase1/w19-pallet-assembly-plan.md §6.2:

1. Create PalletLabelPrint.jsx with HTML layout matching QP-PD-002-F046 Rev.02 form
2. Create styles/pallet-label-print.css with @media print rules (A5 landscape)
3. Big "lot-date-range" number top-left · big "pallet-number" top-right
4. Lot × Box matrix table (8 cols) with auto-wrap
5. Signature slots: QA · Shift PD (Day/Night)
6. Auto-fill: quantity, close date, form code
7. Trigger via window.print() after PalletService.print() returns
8. Test print preview matches physical form side-by-side
```

### Prompt 5: ✨ Rearrange (backend + frontend)
```
Implement W19.5 Pallet Rearrange per docs/phase1/w19-pallet-assembly-plan.md §1.5, §5.2.1, §6:

1. Backend:
   - Add PalletService.rearrangeFrom(sourceId, newNumber, revisionSuffix, reason, subLotIds, username)
   - Follow business rules §5.2.1: source must be CLOSED/PRINTED · same product · move (not copy) · audit 3 entries per box · max lineage depth 5
   - Add getDerivedPallets() and getPalletLineage()
   - Extend PalletDto with parentPalletId, revisionSuffix, rearrangeReason, derivedCount
   - Add RearrangePalletRequest DTO
   - Unit tests §10 items 13-22 (10 rearrange scenarios)
   - REST: POST /api/operator/pallets/{id}/rearrange · GET /derived · GET /lineage

2. Frontend:
   - Create PalletRearrangeDialog.jsx — 4-step modal per §6.1 rearrange mockup:
     Step 1: Search source (product + date + pallet #)
     Step 2: Select from result list
     Step 3: Enter new pallet number + revision + reason
     Step 4: Checkbox tree of boxes to include (default all checked)
   - Create PalletLineageBadge.jsx — small chip "↪ from #7 · DAMAGE"
   - Update PalletAssemblyPanel header:
     - "🔄 Rearrange จาก pallet เดิม" button (opens dialog)
     - Show PalletLineageBadge if current pallet is rearranged
   - Preview counts update real-time as boxes are checked/unchecked
   - Warning modal if source will have 0 boxes after rearrange

3. E2E: PalletRearrangeE2ETest.java — create #7 with 16 boxes · close · rearrange 14 to #7A · verify #7 CLOSED with 2 boxes · #7A OPEN with 14
```

### Prompt 6: E2E + Training
```
Complete W19.6 per docs/phase1/w19-pallet-assembly-plan.md §8, §10:

1. Create PalletFlowE2ETest.java — full H2 flow:
   plan → confirm 5 boxes → track-out all → create pallet → scan 5 → close → print → assert HTML contains all box numbers
2. Add docs/training/08-operator-pallet-assembly.md — Thai training doc matching format of 01-07 (include Rearrange section)
3. Update docs/training/01-operator-daily-flow.md — add step 8 "ประกอบ Pallet" after Track-Out (mention rearrange option)
4. Update docs/training/README.md — add row 08
5. Commit + push to feature/phase1-w11-backend
```

---

## Appendix A — Physical Form Reference

**Form code**: `QP-PD-002-F046 Rev.02`
**Format**: A5 landscape (or half-A4)
**Fields**: See §2 Business Context

**Photo reference**: `/root/.claude/uploads/b61dfbab-7115-5009-a046-a39750678a4e/3d799b1b-BarcodeTahara.pdf`

---

## Appendix B — Related Documents

- `docs/phase1/lot-hierarchy-er.md` — SubLot / ProductionReport / ParentLot
- `docs/phase1/sub-lot-numbering.md` — 12-digit sub-lot + Mod-10
- `docs/training/01-operator-daily-flow.md` — where step 8 will be added
- `docs/phase1/uat-script.md` — will need W19 addition

---

**Document version**: v1.0 · 31 July 2026
**Written for**: GDTAHARA Phase 1 W19 · Pallet Assembly feature
**Status**: 📋 Design · pending sign-off before implementation
