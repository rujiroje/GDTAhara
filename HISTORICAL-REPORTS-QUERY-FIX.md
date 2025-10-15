# 🔧 Historical Reports Query Fix - Summary

## ปัญหาที่พบ
- **PC รายงานย้อนหลัง** สามารถเรียก API ได้แล้ว (200 OK) แต่ไม่พบข้อมูล (Found 0 historical reports)
- Backend log แสดง: `Found 0 historical reports` แม้ว่าจะมีข้อมูลในฐานข้อมูล
- ปัญหาอยู่ที่ `getHistoricalReports` method ใน `ProductionService` ที่ return empty list เสมอ

## การแก้ไขที่ดำเนินการ

### 1. ✅ เพิ่ม Repository Method
**File:** `ProductionReportRepository.java`
```java
// Find historical reports by date range with optional machine and product filters
@Query("SELECT DISTINCT pr FROM ProductionReport pr " +
    "LEFT JOIN FETCH pr.machine m " +
    "LEFT JOIN FETCH pr.product p " +
    "WHERE pr.startDate >= :startDate " +
    "AND pr.startDate <= :endDate " +
    "AND (:machineId IS NULL OR pr.machine.id = :machineId) " +
    "AND (:productId IS NULL OR pr.product.id = :productId) " +
    "ORDER BY pr.startDate DESC")
List<ProductionReport> findHistoricalReports(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("machineId") Long machineId,
                                            @Param("productId") Long productId);
```

### 2. ✅ แก้ไข Service Implementation
**File:** `ProductionService.java`
- **ก่อน:** Method return `new ArrayList<>();` เสมอ
- **หลัง:** Implement การ query ข้อมูลจริงจากฐานข้อมูล

**Features ที่เพิ่ม:**
- ✅ Query production reports ตาม date range
- ✅ คำนวณ `goodQty` จาก packaging logs
- ✅ คำนวณ `ngQty` จาก NG logs  
- ✅ คำนวณ `yield` percentage
- ✅ คำนวณ `totalBoxes` จาก packaging count
- ✅ คำนวณ `totalScrapWeight` จาก scrap weight logs
- ✅ Error handling และ logging ที่ดีขึ้น
- ✅ Support machine และ product filtering

### 3. ✅ สร้างไฟล์ทดสอบ
**File:** `historical-reports-test-fixed.html`
- ทดสอบ API endpoint `/api/production/reports/production-historical`
- ทดสอบด้วยและไม่ด้วย authentication
- แสดงผลข้อมูลในรูปแบบที่อ่านง่าย
- SQL queries สำหรับตรวจสอบข้อมูลในฐานข้อมูล

## การทดสอบ

### Expected Result:
```json
[
  {
    "id": 123,
    "orderNumber": "ORD-001",
    "startDate": "2025-09-15",
    "endDate": "2025-09-16", 
    "machineName": "Machine A",
    "productName": "Product XYZ",
    "goodQty": 8500,
    "ngQty": 500,
    "yield": "94.44%",
    "totalBoxes": 85,
    "totalScrapWeight": 12.5
  }
]
```

### Database Check SQL:
```sql
-- ตรวจสอบข้อมูลเดือนกันยายน 2025
SELECT 
    id, order_number, start_date, end_date,
    machine_id, product_id, target_qty, status
FROM production_reports 
WHERE start_date >= '2025-09-01' 
  AND start_date <= '2025-09-30'
ORDER BY start_date DESC;
```

## Next Steps
1. 🔄 **Restart Backend Application** - เพื่อโหลด code ใหม่
2. 🧪 **Test API** - ใช้ไฟล์ `historical-reports-test-fixed.html`
3. 🌐 **Test Frontend** - ทดสอบในหน้า Historical Reports
4. 📊 **Verify Data** - ตรวจสอบข้อมูลจริงในฐานข้อมูล

## Files Modified
- ✅ `ProductionReportRepository.java` - เพิ่ม `findHistoricalReports` method
- ✅ `ProductionService.java` - แก้ไข `getHistoricalReports` implementation
- ✅ `historical-reports-test-fixed.html` - สร้างไฟล์ทดสอบใหม่

---
⚠️ **Important:** Backend ต้อง restart เพื่อให้การเปลี่ยนแปลงมีผล

🎯 **Expected Outcome:** Historical Reports จะแสดงข้อมูลจริงจากฐานข้อมูลแทนที่จะเป็น empty array