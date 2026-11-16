# O-12 · Packaging Material Auto-Deduction with OP Selection

**Role:** Operator
**Priority:** 🟠 High
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** M (Medium · ~5-6 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment R21 + follow-up 10 Sep 2026
**Related:** OP-WS-01 (box_label + handheld_session + box save flow) · C-01 (ROH/SCAP via CM · separate scope)

---

## 📋 Summary · สรุป

เมื่อ OP ปิด Box · ระบบตัดวัตถุดิบ **packaging** (HIBE · UNBW · VERP) อัตโนมัติตาม BOM · แต่ BOM มี alternate items (เก่า/ใหม่) · OP ต้องเลือกว่าใช้ตัวไหนเพื่อไม่ให้ตัด double

**Key rule:**
- **OP ต้องเลือกทุก login (session start)** · ไม่จำ last choice
- Alternate group **auto-detect จาก BOM** (naming pattern)
- Single-member groups: **แสดงให้เห็น** (info only) · ไม่ต้องเลือก
- ROH/SCAP: **ไม่อยู่ในระบบนี้** (C-01 · CM handles)

---

## 🎯 Scope

**IN scope (auto-deduct via handheld session):**
- **HIBE** — แผ่นกั้น · ถุง · ของบรรจุใหม่
- **UNBW** — ของบรรจุ reused (แผ่นกั้นใช้ซ้ำ ฯลฯ)
- **VERP** — Box กระดาษ

**OUT of scope (handled elsewhere):**
- **ROH** — เม็ดพลาสติก · raw material (→ C-01 · CM Material Meter)
- **SCAP** — เศษ / scrap (→ C-01)
- Non-packaging materials

---

## 🎯 User Story

**As an Operator,**
เมื่อ login handheld + เลือก machine/position · ก่อน pack box แรกในกะ,
I want to:
- เห็น modal แสดงว่า packaging อะไรบ้างที่จะโดนตัด
- เลือก variant ที่ใช้จริง (ใหม่/reused) สำหรับ group ที่มีทางเลือก
- เริ่ม pack box · ระบบตัดตามที่เลือกไว้ · ทุก box จนจบ session
- เปลี่ยน selection ได้กลางกะถ้าจำเป็น (rare)

**As an Admin,**
I want to:
- ระบบ auto-detect groups จาก BOM (ไม่ต้อง config ทีละ product)
- Override groups ผ่าน screen เดียว
- เห็น audit log · deduction ที่เกิดขึ้น · ตรวจย้อนหลังได้

---

## 🎨 UI/UX Design

### 1️⃣ Session Start · Packaging Selection Modal

**Trigger:** หลัง handheld login + เลือก machine + position (per OP-WS-01) · ก่อนหน้า Packing screen

**Behavior rules:**
- ถ้า BOM ไม่มี HIBE/UNBW/VERP items · **skip modal** (rare)
- ถ้ามี packaging items · **แสดงเสมอ** (ยืนยันว่ารู้ว่าจะตัดอะไร)
- Multi-member groups (2+) · **radio required**
- Single-member groups · **info only** (แสดง · ไม่ต้องเลือก)

```
┌─ เริ่ม Packing Session · RBL103 · 🅛 LEFT · OP2 ────────────┐
│                                                                │
│  Product: DBAJMY0130                                           │
│  Lot: ABC-2026-09-10-01                                        │
│                                                                │
│  📦 System พบ Packaging Materials · โปรดยืนยัน:              │
│  ─────────────────────────────────────────────────────────    │
│                                                                │
│  🔀 กลุ่ม 1 · Separated Sheet 1100×1100×3mm                   │
│  ┌──────────────────────┐  ┌──────────────────────┐          │
│  │ 🆕 ใหม่ (HIBE)        │  │ ♻️ Reused (UNBW)    │          │
│  │ SSPP110A              │  │ #SSPP110A            │          │
│  │ 0.062 PCS ต่อ FG      │  │ 0.001 PCS ต่อ FG    │          │
│  │       [ ○ ]           │  │        [ ○ ]         │          │
│  └──────────────────────┘  └──────────────────────┘          │
│  ⚠️ ต้องเลือก 1 อัน (required)                                │
│                                                                │
│  📌 กลุ่ม 2 · กล่องกระดาษ No.20 (450×600×480 mm)              │
│  ┌────────────────────────────────────────────┐              │
│  │ ✅ CAPP20 · VERP                             │              │
│  │    1.0 PCS ต่อ FG · auto-select              │              │
│  │    (มีตัวเลือกเดียว)                          │              │
│  └────────────────────────────────────────────┘              │
│                                                                │
│  ✨ No other packaging groups                                  │
│                                                                │
│  ─────────────────────────────────────────────────────────    │
│  🔍 ดู BOM ทั้งหมด (11 items)  [ ▾ Expand ]                  │
│                                                                │
│              [ยกเลิก]     [✓ ยืนยัน · เริ่ม Packing]           │
└────────────────────────────────────────────────────────────────┘
```

**Validation:**
- ถ้ามี multi-member group ที่ยังไม่เลือก · disable "ยืนยัน" + highlight red
- ยืนยัน → บันทึก `handheld_session_packaging_choice` · redirect to Packing screen

---

### 2️⃣ Packing Screen · Header Shows Current Selection

```
┌─ Packing · RBL103 · 🅛 LEFT · OP2 · Lot ABC-2026-09-10-01 ─┐
│                                                              │
│ 📦 Packaging: 🆕 SSPP110A (new) · CAPP20                    │
│                                       [✏️ เปลี่ยน Packaging] │
│ ─────────────────────────────────────────────────────────  │
│                                                              │
│  Current Box: #45 · 12 / 100 pcs                            │
│  Started: 08:15                                              │
│                                                              │
│  [+1] [+10] [+50] [Custom qty]                              │
│                                                              │
│  [ปิด Box · Print Label]                                     │
└──────────────────────────────────────────────────────────────┘
```

**Header info:**
- Current selection · compact display · icon 🆕 or ♻️
- "เปลี่ยน Packaging" button → open selection modal again

---

### 3️⃣ Change Selection (Mid-shift · rare)

**Trigger:** Click "✏️ เปลี่ยน Packaging"

**Rules:**
- อนุญาตเฉพาะ **ระหว่าง box** (ไม่ใช่ในระหว่าง pack)
- ถ้ากำลัง pack box อยู่ (มี pcs > 0) → warn "ปิด box ปัจจุบันก่อน หรือยกเลิก box"
- เปิด selection modal · บันทึกใหม่ · log change

```
┌─ เปลี่ยน Packaging Selection ─────────────────────────┐
│                                                        │
│  ⚠️ การเปลี่ยนจะ apply กับ Box ต่อไปเท่านั้น          │
│                                                        │
│  Group 1: Separated Sheet 1100                        │
│  ปัจจุบัน: 🆕 SSPP110A (new)                          │
│  เปลี่ยนเป็น: ( ) 🆕 SSPP110A   ( ) ♻️ #SSPP110A     │
│                                                        │
│  เหตุผลการเปลี่ยน (optional):                          │
│  [__________________________]                          │
│                                                        │
│              [ยกเลิก]  [✓ บันทึกการเปลี่ยน]           │
└────────────────────────────────────────────────────────┘
```

---

### 4️⃣ Admin · BOM Alt Group Review Screen

**เมื่อ Product ใหม่มี BOM ใหม่:**
- System auto-detect groups เมื่อ BOM upload/update
- Admin สามารถ review + override

```
┌─ Admin · BOM Alt Group Review · Product DBAJMY0130 ────────┐
│                                                              │
│  Detected Groups:                                           │
│  ─────────────────────────────────────────────────────────  │
│  ┌────────────────────────────────────────────────────┐   │
│  │ 🔀 Group: SEP_SHEET_1100 (auto-detected)             │   │
│  │  base name: SSPP110A                                  │   │
│  │  Members: 2                                           │   │
│  │    • SSPP110A · HIBE · new · 0.062 PCS/FG            │   │
│  │    • #SSPP110A · UNBW · reused · 0.001 PCS/FG        │   │
│  │  Status: OP will pick                                 │   │
│  │  [Edit] [Merge with another] [Split]                 │   │
│  └────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌────────────────────────────────────────────────────┐   │
│  │ 📌 Group: BOX_NO20 (auto-detected)                    │   │
│  │  base name: CAPP20                                    │   │
│  │  Members: 1                                           │   │
│  │    • CAPP20 · VERP · 1.0 PCS/FG                       │   │
│  │  Status: Auto-deduct (info-only shown to OP)          │   │
│  │  [Edit] [Add alternate item]                          │   │
│  └────────────────────────────────────────────────────┘   │
│                                                              │
│  [+ Manual: Create custom group]                            │
│  [ Bulk import from CSV]                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 🧠 Auto-Detection Logic

**Algorithm (runs on BOM upload/refresh):**

```
Step 1: Filter items where category IN ('HIBE', 'UNBW', 'VERP')
Step 2: Extract "base name" per item:
  - Strip leading '#' (marker for reused)
  - Strip trailing suffixes (e.g. "-REUSED", "-NEW")
  - Normalize case
Step 3: Group by base name:
  Example base name "SSPP110A":
    Members = [SSPP110A (HIBE), #SSPP110A (UNBW)]
Step 4: Register groups + members in DB
Step 5: If admin has already overridden previously · preserve override
```

**Detection rules (in priority order):**

| Priority | Rule | Example |
|---|---|---|
| 1 | Explicit `alt_group_code` in material_master (admin pre-set) | Best case |
| 2 | Naming convention: `#` prefix → reused variant | `#SSPP110A` |
| 3 | Naming convention: "REUSED" in name | "Reused Separated Sheet" |
| 4 | Category pair: (HIBE + UNBW) with same base name | SSPP110A + #SSPP110A |
| 5 | Fallback: single-member group | Standalone item |

**Naming convention examples this handles:**
- ✅ `SSPP110A` (HIBE) + `#SSPP110A` (UNBW) → group `SSPP110A`
- ✅ `BOX-A5-NEW` (HIBE) + `BOX-A5-REUSED` (UNBW) → group `BOX-A5`
- ⚠️ Complex names may need admin override (e.g. different code entirely)

---

## 🎯 Deduction Flow

```
OP กด "ปิด Box · Print Label" · Box #45 · Product DBAJMY0130
  ↓
Backend logic:
  1. Load BOM for DBAJMY0130 (11 items)
  2. Filter items by category:
     - HIBE items → in scope
     - UNBW items → in scope
     - VERP items → in scope
     - ROH items → SKIP (CM scope)
     - SCAP items → SKIP (CM scope · not deducted anyway)
  3. Get session choices for each group:
     SEP_SHEET_1100 → OP chose SSPP110A (HIBE new)
     BOX_NO20 → auto (only 1 member · CAPP20)
  4. Deduct actual selection:
     ✓ SSPP110A · deduct 0.062 × 1 = 0.062 PCS
     ✗ #SSPP110A · SKIP (not chosen · in same group)
     ✓ CAPP20 · deduct 1.0 × 1 = 1.0 PCS
  5. Log each deduction:
     packaging_deduction_log:
       - box_label_id: 45
       - bom_item_code: SSPP110A, actual_deducted: SSPP110A, chosen=TRUE
       - bom_item_code: #SSPP110A, actual_deducted: NULL, chosen=FALSE
       - bom_item_code: CAPP20, actual_deducted: CAPP20, chosen=TRUE (single member)
  6. Update material stock (for HIBE/UNBW/VERP only)
  7. ROH items: log for CM's C-01 workstream (or defer to CM's own tracking)
  8. Print box label (via OP-WS-01 printer routing)
```

---

## 🗄️ Data Model Changes (V28 Migration)

```sql
-- V28__packaging_alternate_group.sql

-- ============================================
-- 1. Alternate Group Master (auto + manual)
-- ============================================

CREATE TABLE packaging_alternate_group (
  group_code VARCHAR(50) PRIMARY KEY,
  group_name_th VARCHAR(200),
  group_name_en VARCHAR(200),
  base_item_code VARCHAR(50),                 -- e.g. "SSPP110A" (canonical)
  auto_detected BOOLEAN DEFAULT TRUE,
  admin_override BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE
);

-- ============================================
-- 2. Group Members
-- ============================================

CREATE TABLE packaging_alternate_item (
  item_code VARCHAR(50) NOT NULL,
  group_code VARCHAR(50) NOT NULL,
  variant_type VARCHAR(20) NOT NULL,          -- 'NEW' | 'REUSED' | 'STANDARD'
  category VARCHAR(20) NOT NULL,              -- 'HIBE' | 'UNBW' | 'VERP'
  display_priority INT DEFAULT 0,             -- lower = show first
  auto_assigned BOOLEAN DEFAULT TRUE,
  PRIMARY KEY (item_code, group_code),
  CONSTRAINT chk_variant CHECK (variant_type IN ('NEW','REUSED','STANDARD')),
  CONSTRAINT chk_category CHECK (category IN ('HIBE','UNBW','VERP')),
  INDEX idx_pai_group (group_code)
);

-- ============================================
-- 3. Session Selection (extends handheld_session · OP-WS-01)
-- ============================================

CREATE TABLE handheld_session_packaging_choice (
  session_id BIGINT NOT NULL,
  group_code VARCHAR(50) NOT NULL,
  selected_item_code VARCHAR(50) NOT NULL,
  auto_selected BOOLEAN DEFAULT FALSE,        -- true if single-member (no user pick)
  selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id, group_code),
  CONSTRAINT fk_hspc_session FOREIGN KEY (session_id) REFERENCES handheld_session(session_id),
  CONSTRAINT fk_hspc_group FOREIGN KEY (group_code) REFERENCES packaging_alternate_group(group_code)
);

-- ============================================
-- 4. Change Log (audit for mid-session change)
-- ============================================

CREATE TABLE packaging_choice_change_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  group_code VARCHAR(50) NOT NULL,
  from_item_code VARCHAR(50) NOT NULL,
  to_item_code VARCHAR(50) NOT NULL,
  changed_by VARCHAR(50) NOT NULL,
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reason VARCHAR(500),
  INDEX idx_pccl_session (session_id, changed_at)
);

-- ============================================
-- 5. Deduction Log (per box · full audit)
-- ============================================

CREATE TABLE packaging_deduction_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  box_label_id BIGINT NOT NULL,
  bom_item_code VARCHAR(50) NOT NULL,           -- what BOM listed
  is_in_alt_group BOOLEAN DEFAULT FALSE,
  alt_group_code VARCHAR(50) NULL,
  was_chosen BOOLEAN DEFAULT FALSE,             -- true if actually deducted
  actual_deducted_code VARCHAR(50) NULL,        -- NULL if skipped
  deducted_qty DECIMAL(12,4),
  material_category VARCHAR(20),
  session_id BIGINT NULL,
  deducted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pdl_box FOREIGN KEY (box_label_id) REFERENCES box_label(id),
  INDEX idx_pdl_box (box_label_id),
  INDEX idx_pdl_time (deducted_at),
  INDEX idx_pdl_item (bom_item_code, deducted_at)
);

-- ============================================
-- 6. Optional: pre-set alt group in material_master
-- ============================================

ALTER TABLE material_master
  ADD COLUMN alt_group_code VARCHAR(50) NULL,
  ADD COLUMN variant_type VARCHAR(20) NULL;
CREATE INDEX idx_mm_alt_group ON material_master(alt_group_code);
```

---

## 🔌 API Endpoints

### BOM Analysis
- `POST /api/bom/analyze-alt-groups?productCode=X` — trigger auto-detect (or refresh)
- `GET /api/bom/{productCode}/packaging-groups` — return groups for OP UI

### Admin Group Management
- `GET /api/admin/alt-group` — list all groups
- `POST /api/admin/alt-group` — manual create
- `PATCH /api/admin/alt-group/{code}` — edit
- `POST /api/admin/alt-group/{code}/merge` — merge with another
- `POST /api/admin/alt-group/{code}/split` — split items to separate groups

### Session Selection
- `POST /api/handheld/session/{id}/packaging-choice` — save OP's selections
- `GET /api/handheld/session/{id}/packaging-choice` — retrieve current
- `PATCH /api/handheld/session/{id}/packaging-choice` — mid-session change

### Deduction (called internally on box save)
- (invoked by OP-WS-01 box save endpoint · not standalone)
- `GET /api/box/{id}/deduction-log` — view what was deducted (audit)

---

## 🖥️ Frontend Components (React)

```
src/components/OP/
  ├── PackagingSelectionModal.tsx        (NEW · session start · multi-member picker)
  ├── PackagingHeaderDisplay.tsx         (NEW · header current selection)
  ├── PackagingChangeModal.tsx           (NEW · mid-session change)
  └── PackingScreen.tsx                  (MODIFY · integrate above)

src/components/Admin/
  ├── AltGroupReview.tsx                 (NEW · per-product review)
  ├── AltGroupList.tsx                   (NEW · all groups list)
  └── AltGroupEditor.tsx                 (NEW · CRUD one group)

src/hooks/
  ├── usePackagingSession.ts             (NEW · manage session choice state)
  └── useBomAltGroups.ts                 (NEW · fetch groups per product)
```

---

## ✅ Acceptance Criteria

### Auto-Detection
- [ ] BOM upload triggers auto-detect for HIBE/UNBW/VERP items
- [ ] Naming convention `#` prefix + variant marker recognized
- [ ] Groups persist · admin override preserved on re-detect
- [ ] Admin can review/edit/merge/split groups

### Session Selection
- [ ] After handheld login + session start · **modal shown always** (if packaging exists)
- [ ] Multi-member groups: radio required
- [ ] Single-member groups: shown info-only · no interaction needed
- [ ] Cannot proceed to packing until all multi-member groups selected
- [ ] Choice saved · persistent for session

### Packing Screen
- [ ] Header shows current packaging selection compact
- [ ] "เปลี่ยน Packaging" button available
- [ ] Change only allowed between boxes · block during in-progress packing

### Deduction
- [ ] On box close · deduct only chosen items per group
- [ ] Non-chosen alternates skipped (not deducted)
- [ ] Single-member items auto-deducted
- [ ] ROH/SCAP not touched (out of scope · CM handles via C-01)
- [ ] Deduction log records all items (chosen + skipped) for audit

### Change Log
- [ ] Every session choice + mid-session change logged
- [ ] Log includes reason field (optional)

### Testing
- [ ] Unit: auto-detection with various naming patterns
- [ ] Unit: single vs multi-member group logic
- [ ] Integration: BOM with mix of HIBE/UNBW/VERP/ROH · correct filter
- [ ] E2E: full flow · login → select → pack 3 boxes → verify deduction
- [ ] Test บน Samsung XCover 7 Pro (handheld) · glove-friendly
- [ ] Edge case: BOM with no packaging → skip modal
- [ ] Edge case: All single-member groups → still show modal (info-only)

---

## 🚀 Migration Plan

**V28** — see SQL above

**Deploy order:**
1. Deploy V28 · new tables + columns
2. Run BOM auto-detect job on all existing products (batch)
3. Admin review/adjust groups (manual QA)
4. Deploy frontend · Pilot machine only (1 machine · shadow mode 1 week)
5. Enforce on all machines after validation

**Data seeding:**
- Initial run: analyze all active BOMs · create groups
- Admin dashboard shows: "N groups detected, X need review"

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V28 migration | 0.5 day |
| Backend · Auto-detect service (naming pattern + rules) | 1 day |
| Backend · Group CRUD + session choice storage | 0.75 day |
| Backend · Deduction engine (integrate with OP-WS-01 box save) | 1 day |
| Backend · Audit log + reporting endpoints | 0.5 day |
| Frontend · Packaging Selection Modal (session start) | 1 day |
| Frontend · Header display + Change modal | 0.75 day |
| Frontend · Admin Alt Group Review + Editor | 1 day |
| Testing + integration (with OP-WS-01) | 1 day |
| **Total** | **~7.5 days (M)** |

---

## 🔗 Dependencies

- **OP-WS-01 (V25):** handheld_session · box_label · packing flow · printer routing
- **BOM table** (existing) · material_master · category field
- **C-01 (upcoming):** ROH/SCAP will be handled there · this workstream skips them

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Auto-detect groups from BOM | Reduce admin config · scales to many products |
| 2 | Force selection every login | Accuracy · handoff between shifts safe |
| 3 | Single-member shown info-only | Transparency without extra tap |
| 4 | ROH/SCAP excluded (→ CM) | Separation of concerns · C-01 handles |
| 5 | Report deferred | Focus on Daily Report first (later phase) |
| 6 | Change allowed mid-shift · logged | Flexibility for real-world edge cases |
| 7 | Naming convention detection | Handles common `#` prefix pattern |
| 8 | Session-level choice (not per-box) | Reduce clicks · matches reality |

---

## 🔮 Future Enhancements

- Analytics dashboard: HIBE vs UNBW ratio per product/month (deferred per user)
- Cost impact analysis (using UNBW saves X THB/box)
- Auto-suggest based on stock: prefer whichever has more stock (predictive)
- Barcode scan · scan actual packaging used (highest accuracy · slower)
- Integration with SAP BOM alternates (Phase 3)

---

## 🎯 Report Integration

**Daily Production Report** (per user's priority · focus here first):
- Include packaging usage summary per shift/day
- Show: which alternates were used · count boxes per selection
- Total packaging cost per shift (once material cost data available)
- Format: TBD when Daily Report designed
