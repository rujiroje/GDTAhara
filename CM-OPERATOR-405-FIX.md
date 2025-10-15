# CM OPERATOR - การแก้ไข Frontend Error 405
# =================================================================
# แก้ไขปัญหา "405 Method Not Allowed" และทำให้ CM Operator ทำงานได้
# =================================================================

## ✅ ปัญหาที่แก้ไขแล้ว

### 1. **405 Method Not Allowed Error**
- **ปัญหา**: CM Operator Dashboard เรียก `/api/pc/reports/active` แต่ endpoint ไม่มีใน ProductionControlController
- **แก้ไข**: เพิ่ม `@GetMapping("/reports/active")` ใน ProductionControlController
- **ผลลัพธ์**: CM Operator สามารถดึงข้อมูล Active Production Reports ได้

### 2. **Missing Test Data**  
- **ปัญหา**: ไม่มี Production Reports ในฐานข้อมูลสำหรับทดสอบ
- **แก้ไข**: สร้าง `production-reports-test-data.sql`
- **ข้อมูล**: 3 Active Reports, 2 Completed Reports พร้อม Machines และ Products

### 3. **API Testing Enhancement**
- **ปัญหา**: ไม่มี tool ทดสอบ endpoint ใหม่
- **แก้ไข**: อัพเดท `cm-operator-api-test.html` 
- **ฟีเจอร์**: เพิ่มปุ่มทดสอบ Production Reports API

## 🔧 การแก้ไขที่ทำ

### **Backend: ProductionControlController.java**
```java
// เพิ่ม endpoint ใหม่
@GetMapping("/reports/active")
public ResponseEntity<?> getActiveReports() {
    // ดึง reports ที่มีสถานะ IN_PROGRESS, ACTIVE
    // ถ้าไม่มี ส่ง reports ล่าสุด 5 รายการ
    // ส่งกลับ JSON format ที่ frontend ต้องการ
}
```

### **Database: production-reports-test-data.sql**
```sql
-- Machines: BM001, BM002, IM001
-- Products: PET500, PET1000, PP250  
-- Production Reports: 3 Active + 2 Completed
-- Status: 'IN_PROGRESS', 'ACTIVE', 'COMPLETED'
```

### **Testing: cm-operator-api-test.html**
```html
<!-- เพิ่มปุ่มทดสอบ -->
<button onclick="testProductionReports()">Test Production Reports</button>

<!-- อัพเดท testActiveReports() function -->
// รองรับ response format ใหม่ { data: [...], status: "success" }
```

## 🚀 ขั้นตอนการทดสอบ

### **ขั้นตอนที่ 1: รัน SQL Scripts**
```sql
-- 1. สร้าง Production Reports และ related data
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P "tst123##" -i production-reports-test-data.sql

-- 2. สร้าง CM Operator users (ถ้ายังไม่ได้ทำ)
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P "tst123##" -i create-cm-operator-users.sql

-- 3. สร้าง Materials และ Stock data (ถ้ายังไม่ได้ทำ)
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P "tst123##" -i cm-operator-test-data.sql
```

### **ขั้นตอนที่ 2: Restart Backend**
```bash
# ใช้ batch script
restart-backend.bat

# หรือ manual
cd gdtahara-backend
mvn spring-boot:run
```

### **ขั้นตอนที่ 3: ทดสอบ API**
```bash
# เปิด cm-operator-api-test.html ใน browser
# 1. Login: cmoperator / test123
# 2. ทดสอบ Active Reports API
# 3. ทดสอบ Materials API 
# 4. ทดสอบ Lot Numbers API
# 5. ทดสอบ Stock-Out API
```

### **ขั้นตอนที่ 4: ทดสอบ Frontend**
```bash
cd gdtahara-frontend
npm run dev

# เข้าใช้งาน: http://localhost:5173
# Login: cmoperator / test123
# ทดสอบ CM Operator workflow
```

## 📊 Expected Results (ผลลัพธ์ที่คาดหวัง)

### **API Test Results:**
```json
✅ Active Reports API: Found 3 reports
{
  "data": [
    {
      "id": 1,
      "orderNumber": "ORD-2025-001", 
      "machineName": "Blow Molding Machine #1",
      "productName": "PET Bottle 500ml",
      "status": "IN_PROGRESS"
    }
  ],
  "status": "success",
  "message": "พบใบสั่งผลิตที่กำลังทำงาน 3 รายการ"
}
```

### **CM Operator Frontend Flow:**
1. ✅ Login สำเร็จ → แสดงหน้า CM Operator Dashboard
2. ✅ แสดงรายการ "เลือกใบสั่งผลิต" (3 รายการ)
3. ✅ คลิกเลือก Report → ไปหน้า "บันทึกการเบิกจ่าย"
4. ✅ เลือก Material Type → แสดง Materials dropdown
5. ✅ เลือก Material → แสดง Lot Numbers dropdown
6. ✅ กรอกจำนวน → บันทึกสำเร็จ

## 🔍 Troubleshooting

### **❌ ยังได้ 405 Error**
```bash
# ตรวจสอบ backend restart สมบูรณ์
curl http://localhost:8080/api/pc/reports/active
# ควรได้ 403 Forbidden (ต้องการ auth) แทน 405

# ตรวจสอบ code changes saved
git status
git diff
```

### **❌ "ไม่มีใบสั่งผลิตที่กำลังทำงาน"**
```sql
-- ตรวจสอบข้อมูล Production Reports
SELECT id, order_number, status FROM ProductionReport 
WHERE status IN ('IN_PROGRESS', 'ACTIVE');

-- ถ้าไม่มี รัน production-reports-test-data.sql
```

### **❌ Materials/Lot Numbers ว่าง**
```sql
-- ตรวจสอบ Materials
SELECT COUNT(*) FROM Material;

-- ตรวจสอบ Stock Transactions  
SELECT COUNT(*) FROM MaterialStockTransaction;

-- ถ้าไม่มี รัน cm-operator-test-data.sql
```

## 📁 ไฟล์ที่สร้าง/แก้ไข

### **แก้ไขแล้ว:**
- ✅ `ProductionControlController.java` - เพิ่ม `/reports/active` endpoint
- ✅ `cm-operator-api-test.html` - เพิ่มการทดสอบ Production Reports

### **สร้างใหม่:**
- 🆕 `production-reports-test-data.sql` - ข้อมูล Production Reports ตัวอย่าง

### **ไฟล์ที่มีอยู่แล้ว:**
- ✅ `create-cm-operator-users.sql` - CM Operator users
- ✅ `cm-operator-test-data.sql` - Materials และ Stock data
- ✅ `restart-backend.bat` - Backend restart script

## 🎯 Next Steps

1. **รัน SQL Scripts ทั้งหมด** เพื่อเตรียมข้อมูลครบถ้วน
2. **Restart Backend** เพื่อให้ code changes มีผล
3. **ทดสอบ API** ด้วย cm-operator-api-test.html
4. **ทดสอบ Frontend** ด้วย CM Operator account
5. **Verify Complete Workflow** จาก login ถึง stock-out

## สรุป ✨

**ปัญหา 405 Method Not Allowed ได้รับการแก้ไขแล้ว** โดยการเพิ่ม `/api/pc/reports/active` endpoint ใน ProductionControlController

**CM Operator ตอนนี้ควรทำงานได้ปกติ** หลังจาก restart backend และรัน SQL scripts

**ขั้นตอนถัดไป**: ทดสอบ complete user flow และยืนยันว่าทุกฟีเจอร์ทำงานถูกต้อง 🚀