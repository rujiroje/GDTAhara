# DA-01 · NG Type Master · Reference Photos + OP Preview

**Role:** Data Admin (upload) · Operator (view reference)
**Priority:** 🟡 Medium
**Target:** Phase 1 (before M7 Mass Rollout · Jan 2027)
**Effort:** S-M (Small-Medium · ~3-4 dev days)
**Source:** Pilot Feedback 8 Sep 2026 · Comment R46 + follow-up 10 Sep 2026
**Related:**
- **NG-WS-01** (ng_type_master · 6 quick buttons + Others grid)
- **T-01** (attachment_photo pattern · reuse)
- **OP-WS-01** (photo storage · Signed URL)

---

## 📋 Summary · สรุป

Data Admin ต้องการแนบรูปตัวอย่าง (reference photo) ต่อ NG type ใน Master Data · เพื่อให้ Operator ดูเทียบขณะบันทึก NG ได้ · เพิ่มความแม่นยำ

**Current state (per screenshot):** Master Data > ประเภทของเสีย · มีแค่ text · NG Code · Description · Category · Machine Type

**Enhancement:** เพิ่มรูปประกอบ · Admin CRUD · OP ดู reference ได้ในขณะบันทึก NG

---

## 🎯 Design Decisions

| # | Topic | Decision |
|---|---|---|
| 1 | Data storage | Reuse `attachment_photo` (T-01) · add ref_type='NG_TYPE' |
| 2 | Multiple photos | Yes · 0-5 per NG type · 1 primary |
| 3 | Photo display in OP screen | "ℹ️ info" button on NG buttons · click shows photos |
| 4 | Master data list | Thumbnail column (primary photo) |
| 5 | Upload UX | Reuse PhotoAttachmentField (T-01) · camera + gallery |
| 6 | Access control | Admin only for upload/delete · OP read-only |

---

## 🎨 UI/UX Design

### 1️⃣ Master Data List · Enhanced with Photo Thumbnail

```
┌─ Master Data > ประเภทของเสีย ─────────────────────────────┐
│                                                              │
│  [🔍 Search]  [+ เพิ่มข้อมูลใหม่]                            │
│                                                              │
│  ┌────┬──────┬──────────┬──────────┬───────┬───────────┬──┐│
│  │ ID │ Code │ Photo    │ Descript.│ Categ.│ MachineTyp│Ac││
│  ├────┼──────┼──────────┼──────────┼───────┼───────────┼──┤│
│  │ 1  │ Q1   │ [🖼️ ×2] │ จุดดำ    │ Op    │ TAHARA    │e/│
│  │ 2  │ Q2   │ [🖼️ ×3] │ จุดใส    │ Op    │ TAHARA    │  ││
│  │ 3  │ Q3   │ [🖼️ ×1] │ จุดเหลือง│ Op    │ TAHARA    │  ││
│  │ 4  │ Q4   │ [+ Add] │ คออออกครีบ│ Op   │ TAHARA    │  ││
│  │ 5  │ Q5   │ [🖼️ ×2] │ เศษปากขวด│ Op    │ TAHARA    │  ││
│  │ 6  │ Q6   │ [+ Add] │ ลายเส้น  │ Op    │ TAHARA    │  ││
│  └────┴──────┴──────────┴──────────┴───────┴───────────┴──┘│
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Photo column:**
- Thumbnail of primary photo (40×40 · rounded corner)
- Badge count if multiple photos ("×2", "×3")
- "[+ Add]" if no photos yet
- Click → open Photo Manager modal

---

### 2️⃣ Edit NG Type · Include Photo Management

```
┌─ แก้ไข NG Type · Q1 · จุดดำ ─────────────────────────────┐
│                                                            │
│  NG Code:       [Q1________]                              │
│  Description:   [จุดดำ_____________________]              │
│  Category:      [Operator ▾]                              │
│  Machine Type:  [TAHARA ▾]                                │
│                                                            │
│  ─────────────────────────────────────────                │
│                                                            │
│  📷 Reference Photos (0-5)                                 │
│  ┌────────────────────────────────────────────────────┐  │
│  │  [Photo 1 ⭐] [Photo 2] [+ Add]                    │  │
│  │  primary                                            │  │
│  │                                                      │  │
│  │  [📸 Camera] [🖼️ Gallery]                          │  │
│  │                                                      │  │
│  │  Actions per photo:                                  │  │
│  │  ⭐ Set as primary · 🗑️ Delete · ✏️ Caption        │  │
│  └────────────────────────────────────────────────────┘  │
│                                                            │
│                       [ยกเลิก]  [✓ บันทึก]                │
└────────────────────────────────────────────────────────────┘
```

**Photo management:**
- Add up to 5 photos per NG type
- Set 1 as "primary" (shown in list thumbnail · OP first view)
- Optional caption per photo ("ตัวอย่างมุม A" · "close-up")
- Reorder by drag
- Delete individual photo

---

### 3️⃣ OP Screen · Reference Photo Preview

**From NG-WS-01 · 6 quick buttons:**

```
┌─ บันทึก NG · RBL103 · 🅛 LEFT · OP1 ─────────────────────┐
│                                                              │
│  ⭐ ปุ่มด่วน · Top 6                                        │
│                                                              │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐              │
│  │  🩹  ℹ️   │ │  ⚫  ℹ️   │ │  ✂️  ℹ️   │  ← "info" icon │
│  │            │ │            │ │            │              │
│  │  จุดดำ    │ │  จุดใส     │ │  ลายเส้น   │              │
│  │  Q1 · 15  │ │  Q2 · 8    │ │  Q6 · 3    │              │
│  └────────────┘ └────────────┘ └────────────┘              │
│  ...                                                          │
└──────────────────────────────────────────────────────────────┘
```

**Click ℹ️ icon → Reference photo modal:**

```
┌─ Q1 · จุดดำ · Reference Photos ─────────────────┐
│                                                    │
│  [Large photo view · swipeable]                    │
│                                                    │
│  ●○○  (2 more photos)                              │
│                                                    │
│  Caption: "ตัวอย่างจุดดำมุมด้านบน"                │
│                                                    │
│  Category: Operator · Machine: TAHARA              │
│                                                    │
│              [ปิด]  [เลือกเป็น NG นี้]             │
└────────────────────────────────────────────────────┘
```

**Alternative: Long-press on button → quick preview** (small popup with primary photo)

---

## 🗄️ Data Model Changes (V32 Migration)

**Minimal · reuse existing:**

```sql
-- V32__ng_type_reference_photos.sql

-- ============================================
-- 1. Add photo_ids + primary_photo_id to ng_type_master
-- ============================================

ALTER TABLE ng_type_master
  ADD COLUMN photo_ids JSON NULL,           -- array of attachment_photo.photo_id
  ADD COLUMN primary_photo_id BIGINT NULL;

-- ============================================
-- 2. Extend attachment_photo (from T-01) for NG_TYPE
-- ============================================

ALTER TABLE attachment_photo DROP CONSTRAINT chk_ref_type;
ALTER TABLE attachment_photo ADD CONSTRAINT chk_ref_type
  CHECK (ref_type IN ('SETUP_TIME_LOG', 'MACHINE_DOWNTIME', 'HELP_REQUEST',
                       'QA_VERIFICATION', 'BOX_REPRINT', 'TANK_REFILL',
                       'WASTE_WEIGHT', 'WASTE_EDIT_REQUEST', 'NG_TYPE'));

-- ============================================
-- 3. Optional · Add caption column to attachment_photo (used by NG_TYPE)
-- ============================================

ALTER TABLE attachment_photo
  ADD COLUMN caption VARCHAR(200) NULL,
  ADD COLUMN display_order INT DEFAULT 0;
```

**That's it · minimal footprint** · NG_TYPE just uses existing polymorphic photo table

---

## 🔌 API Endpoints

### Photo Management (per NG type)
- `POST /api/admin/ng-type/{code}/photo/upload` — upload new photo · caption optional
- `GET /api/ng-type/{code}/photos` — list all photos with signed URLs
- `PATCH /api/admin/ng-type/{code}/photo/{photoId}/primary` — set as primary
- `PATCH /api/admin/ng-type/{code}/photo/{photoId}/caption` — update caption
- `PATCH /api/admin/ng-type/{code}/photos/reorder` — reorder list
- `DELETE /api/admin/ng-type/{code}/photo/{photoId}` — remove

### Existing NG Type (extended)
- `GET /api/ng-type-master` — now includes `primary_photo_url` + `photo_count`
- `PATCH /api/admin/ng-type/{code}` — update text fields (existing)

---

## 🖥️ Frontend Components (React)

```
src/components/Admin/
  ├── NGTypeMasterList.tsx           (MODIFY · add photo column with thumbnail)
  ├── NGTypeEditModal.tsx            (MODIFY · integrate photo section)
  └── NGTypePhotoManager.tsx         (NEW · gallery + primary + caption)

src/components/OP/
  └── NGButtonReferenceModal.tsx     (NEW · info icon → reference photo view)

src/components/NG/
  └── NGRecordScreen.tsx             (MODIFY from NG-WS-01 · add ℹ️ icons)

src/components/shared/
  └── PhotoAttachmentField.tsx       (existing from T-01 · reuse)

src/hooks/
  └── useNGTypePhotos.ts             (NEW · fetch photos with signed URLs)
```

---

## ✅ Acceptance Criteria

### Admin · Photo Management
- [ ] Admin sees photo count + thumbnail in Master Data list
- [ ] Edit modal has Photo section
- [ ] Upload from camera or gallery (reuse PhotoAttachmentField from T-01)
- [ ] Max 5 photos per NG type
- [ ] Set 1 as primary · shown in list thumbnail
- [ ] Optional caption per photo (max 200 chars)
- [ ] Reorder photos by drag
- [ ] Delete individual photo (soft delete · keep attachment)

### OP · Reference View
- [ ] ℹ️ info icon on each NG button (from NG-WS-01)
- [ ] Click ℹ️ → modal shows photos (swipeable)
- [ ] Show caption if provided
- [ ] "เลือกเป็น NG นี้" button in modal (quick select)
- [ ] Reference works for both quick buttons AND "อื่นๆ" grid

### Data & Storage
- [ ] Photos stored via `attachment_photo` with ref_type='NG_TYPE'
- [ ] `ng_type_master.photo_ids` JSON array for order
- [ ] `ng_type_master.primary_photo_id` FK for list display
- [ ] Signed URLs (1-hour expiry) · not direct

### Performance
- [ ] Master Data list · thumbnail loads lazy
- [ ] Photo modal loads within 1 sec
- [ ] Compressed images (max 800px · quality 85%)

### Testing
- [ ] Unit: photo_ids array manipulation
- [ ] Integration: upload → attach → display · full flow
- [ ] E2E: Admin edit → add 3 photos → set primary → OP sees reference
- [ ] Edge: delete primary photo → auto-promote next
- [ ] Edge: NG type with no photos · gracefully handle
- [ ] Test on Samsung Tab Active 5 Pro · touch reorder works

---

## 🚀 Migration Plan

**V32** — see SQL above

**Deploy strategy:**
- Deploy V32 · no data migration needed (photo_ids default NULL)
- Backend + frontend deploy together
- Admin can start uploading reference photos immediately
- OP sees ℹ️ icon on next login

**Data seeding (optional):**
- Data Admin bulk uploads reference photos for existing 20 NG types
- Batch tool: CSV mapping (ng_code, photo_path) for initial setup

---

## 🎯 Effort Estimate

| Task | Effort |
|---|---|
| Flyway V32 migration | 0.25 day |
| Backend · NGTypePhoto endpoints (CRUD) | 0.75 day |
| Backend · Extend ng-type-master query with photo URLs | 0.25 day |
| Frontend · Master Data list enhancement (thumbnail + count) | 0.5 day |
| Frontend · Edit modal · photo section integration | 0.75 day |
| Frontend · Photo Manager component (primary + caption + reorder) | 0.75 day |
| Frontend · OP Reference Modal (swipeable) | 0.5 day |
| Frontend · Add ℹ️ icons to NG buttons (from NG-WS-01) | 0.25 day |
| Testing + integration | 0.5 day |
| **Total** | **~4.5 days (S-M)** |

---

## 🔗 Dependencies

- **T-01 (V22)** · attachment_photo table · PhotoAttachmentField component
- **NG-WS-01 (V26)** · ng_type_master · NG record screen with 6 buttons
- Signed URL infrastructure (from T-01)

---

## 📝 Design Decision Log

| # | Decision | Rationale |
|---|---|---|
| 1 | Reuse attachment_photo pattern | Consistent · minimal new tables |
| 2 | Max 5 photos per type | Sufficient · not overwhelming |
| 3 | 1 primary photo for list display | Fast overview · single thumbnail |
| 4 | Caption optional | Flexibility · not mandatory |
| 5 | ℹ️ info icon on OP buttons | Discoverable · not intrusive |
| 6 | Modal (not tooltip) for photos | Better on tablets · touch-friendly |
| 7 | Swipe navigation in modal | Natural mobile UX |
| 8 | Admin-only edit | Data quality · avoid inconsistency |

---

## 🔮 Future Enhancements

- Video reference (short clip · e.g. 5-10 sec) · Phase 2
- Annotated photos (arrows · circles · text overlay) · Phase 2
- Similarity search (upload photo · find matching NG type via AI) · Phase 3
- Auto-classification (OP takes photo · AI suggests NG type) · Phase 3 Vision
- Multi-language captions (TH + EN) · Phase 2
- Approval workflow for photo upload (QA approves before published) · Phase 2

---

## 🎯 Integration Points

- **NG-WS-01** · ng_type_master used by 6 quick buttons + Others grid
- **T-01** · same photo upload pattern · same storage
- **DA-01 (this)** · adds reference photos as "training material" for OP

**Also affects:**
- Reports (future) can include NG type photos for defect analysis
- Training materials for new OP (photos serve as guide)
- Quality control audits (visual documentation)
