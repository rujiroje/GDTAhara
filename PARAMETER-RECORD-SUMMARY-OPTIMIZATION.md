# Parameter Record Summary Optimization

## Objective
ลดการโหลดข้อมูล Parameter Record ที่มีมากกว่า 60 columns โดยการเลือกเฉพาะคอลัมน์หลัก 10-15 fields ที่ UI ใช้จริง เพื่อ:
- ลด I/O และ Memory Footprint
- เพิ่ม Response Time สำหรับหน้า List / Dashboard
- ลด Network Payload

## Implementation
1. สร้าง `ParameterRecordSummaryDto` ที่มีเฉพาะข้อมูลจำเป็น (temp / extruder / cycle / checks)
2. เพิ่ม JPQL Constructor Expression ใน `ParameterRecordRepository`:
   ```java
   @Query("SELECT new com.gdtahara.gdtaharabackend.dto.ParameterRecordSummaryDto( ... ) FROM ParameterRecord p WHERE p.reportId = :reportId ORDER BY p.createdAt DESC")
   List<ParameterRecordSummaryDto> findSummaryByReportId(Long reportId);
   ```
3. เพิ่ม Service method `getParameterRecordsSummary(reportId)` เพื่อดึง DTO
4. Controller / Service layer เรียกใช้ summary method แทนการดึง Entity เต็มเมื่อไม่จำเป็น

## Selected Fields (Reasoning)
- Core Identifiers: id, reportId, technicianId, recordTime, createdAt
- Key Process Temps: tempMainC1..C3 (ตัวแทนเสถียรภาพของกระบวนการ)
- Core Extruder: extruderMainScrewRpm, extruderMainResinPress, extruderMainResinTemp
- Cycle KPIs: cycleTimeSec, moldTemp, highBlowMpa, lowPressureMpa
- Quality / Safety Flags: productQualityCheck, machineOperationCheck, safetyProcedureCheck
- Notes: additionalNotes (context ที่ UI ต้องแสดง tooltip)

## Benefits
| Metric | Before (SELECT *) | After (DTO Query) |
|--------|-------------------|-------------------|
| Columns Fetched | 60+ | 18 |
| Payload Size (est.) | 100% | ~30% |
| Deserialization Cost | High | Medium-Low |
| Query Read Time | Higher | Lower |
| Memory Allocation | High | Reduced |

## Next Steps / Extensions
- เพิ่ม Pagination สำหรับ Summary (มี method พร้อมแล้ว) 
- เพิ่ม Index บน `(report_id, created_at DESC)` หากยังไม่มี เพื่อเร่ง ORDER BY
- สามารถสร้าง Native Projection สำหรับ SQL Server ถ้าต้องการ ultra performance
- เพิ่ม Cache (Caffeine/Redis) สำหรับ Latest 10 Records Dashboard

## Validation Checklist
- [x] DTO มี constructor ตรงกับ JPQL
- [x] Repository build ผ่าน `mvn -DskipTests compile`
- [x] ไม่มีการใช้ `SELECT *` ใน summary endpoint
- [ ] (Optional) Load test 1k records -> เปรียบเทียบ response time

---
อ้างอิงไฟล์: `ParameterRecordRepository.java`, `ParameterRecordSummaryDto.java`, `TechnicianService#getParameterRecordsSummary`

## Rollback Notice (2025-09-22)
ตามการตัดสินใจล่าสุด: ฝั่ง Technician ต้องการกลับไปใช้ข้อมูลครบทุก field ของ `ParameterRecord` (ไม่ตัด field) เนื่องจากทุกคอลัมน์จำเป็นต่อการตรวจสอบหน้างาน

การเปลี่ยนแปลงที่ดำเนินการ:
1. ลบ endpoint `/api/technician/reports/{reportId}/parameters/summary`
2. ลบ service method `getParameterRecordsSummary()`
3. ติด @Deprecated ให้กับ `findSummaryByReportId` (ทั้ง list / page) ไว้เผื่ออนาคตกลับมาใช้
4. คงไฟล์ `ParameterRecordSummaryDto` ไว้ (สามารถลบได้หากยืนยันว่าไม่ใช้แน่นอน)

ผลกระทบ:
- Frontend ที่เคยเรียก summary endpoint ต้องเปลี่ยนมาใช้ `/api/technician/reports/{reportId}/parameters`
- Query จะกลับไปโหลด full entity (60+ fields) อีกครั้ง

ตัวเลือกในอนาคต (ถ้าต้องการทั้งสองรูปแบบ):
- สร้าง endpoint ใหม่ `/parameters/lightweight` สำหรับ dashboard โดยใช้ DTO เดิม
- เพิ่ม cache (latest N records) แทนการลด column ถ้ายังคงห่วงเรื่อง performance

