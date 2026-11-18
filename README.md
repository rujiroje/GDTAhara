# คู่มือการใช้งาน GDTAHARA — Training Docs

คู่มือสำหรับผู้ใช้งานจริงในโรงงาน · แยกตาม Workflow ที่ใช้ในแต่ละวัน
เนื้อหาปรับตามระบบเวอร์ชัน `feature/phase1-w11-backend @ 571bfb0` (W19 Pallet + W19a Setup TimeLog)

---

## 📚 สารบัญ

### Workflow หลัก (Phase 1)

| ลำดับ | Workflow | ใครใช้ | สถานะเอกสาร |
|---|---|---|---|
| **00** | [**🎨 System Flow by Role (Mermaid)**](./00-system-flow-by-role.md) | **ทุกคน · อ่านก่อน** | ✅ พร้อมใช้ |
| 01 | [Operator — เริ่มกะ ถึงจบกะ](./01-operator-daily-flow.md) | Operator (คนเยอะสุด) | ✅ พร้อมใช้ |
| 02 | [Technician — Setup TimeLog ✨ NEW](./02-technician-setup-flow.md) | Technician | ✅ **v2 · W19a rewrite** |
| 03 | [CM Operator — Stock-out + ชั่งของเสีย](./03-cm-operator-flow.md) | CM (Mixing) | ✅ พร้อมใช้ |
| 04 | [QA — บันทึกของเสียจากการสุ่มตรวจ](./04-qa-inspection-flow.md) | QA | ✅ พร้อมใช้ |
| 05 | [PC — Plan · BOM · Report](./05-pc-workflow.md) | Production Control | ✅ พร้อมใช้ |
| 06 | [LD/Doc — Material Stock IN + Card](./06-ld-doc-stock-flow.md) | Shift Leader / Document | ✅ พร้อมใช้ |
| 07 | [Track-Out (cross-role End-to-End)](./07-track-out-cross-role.md) | **ทุก role** | ✅ พร้อมใช้ |
| **08** | [**🎁 Pallet Assembly ✨ NEW**](./08-operator-pallet-assembly.md) | Operator | ✅ **W19 new** |

---

## 🎉 สถานะชุดเอกสาร
**Training 00–08 ครบทุก role + ทุก workflow** · Phase 1 = 100% pilot-ready

### 🆕 เพิ่ม/อัปเดตล่าสุด (3 สค. 2026)
- ✏️ **02 Technician v2** — rewrite ทั้งไฟล์ (Setup TimeLog · 10 codes · continuous · Q9 Complete guard)
- 🆕 **08 Pallet Assembly** — ไฟล์ใหม่ · scan barcode + rearrange + print label QP-PD-002-F046

### 📈 สถิติชุดเอกสาร (9 ไฟล์)
| File | Role | หัวข้อ |
|---|---|---|
| **00** | ทุกคน — Flow diagrams | 9 Mermaid diagrams |
| 01 | Operator | 6 workflow |
| **02** ✏️ | Technician (W19a rewrite) | 9 workflow |
| 03 | CM Operator | 6 workflow |
| 04 | QA | 6 workflow |
| 05 | PC | 5 workflow |
| 06 | LD/Doc | 7 workflow |
| 07 | Cross-role | End-to-end |
| **08** 🆕 | Pallet Assembly (W19) | 8 workflow |

### 📖 แนะนำลำดับอ่าน
1. **`00-system-flow-by-role.md`** ก่อน → เห็นภาพรวม + flow ของ role ตัวเอง
2. ดู training role ตัวเอง (01-06)
3. **`08 Pallet Assembly`** — ถ้าเป็น Operator (feature ใหม่ล่าสุด)
4. **`07 Track-Out cross-role`** — ทุก role อ่านสุดท้าย

---

## 🎯 วิธีใช้คู่มือนี้

1. **เปิดตามหัวข้อ workflow ที่ตรงกับหน้าที่คุณ** — ไม่ต้องอ่านทุกไฟล์
2. แต่ละ workflow มี **ขั้นตอน 1-2-3** ทำตามได้ทันที
3. ถ้าเจอปัญหา ดู **"เมื่อมีปัญหา"** ที่ท้ายเอกสารแต่ละไฟล์
4. ยังไม่หาย → **แจ้งใน LINE group "GDTahara Pilot"** พร้อม screenshot

---

## 📱 ระบบทำงานบนอะไรได้บ้าง

| อุปกรณ์ | ใช้ได้ | หมายเหตุ |
|---|---|---|
| คอมพิวเตอร์ (Windows / Mac) | ✅ | Chrome / Edge เท่านั้น |
| Tablet (Android / iPad) | ✅ | Chrome / Safari · แนะนำ 10" ขึ้นไป |
| มือถือ | ⚠️ ได้แต่จอเล็ก | ใช้ดูสถานะได้ · Scan barcode/pallet ได้ · กรอกข้อมูลลำบาก |

---

**เวอร์ชันเอกสาร**: v1.2 · 3 สิงหาคม 2026 (W19+W19a shipped · +02 rewrite +08 new)
**ระบบเวอร์ชัน**: Phase 1 · `571bfb0` · 21 Flyway migrations
