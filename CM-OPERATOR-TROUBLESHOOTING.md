# CM OPERATOR TROUBLESHOOTING GUIDE
# =================================================================
# ปัญหาและแนวทางแก้ไข CM Operator ไม่สามารถทำงานได้ตามปกติ
# =================================================================

## ปัญหาที่พบ (Analysis Summary)

### 1. ระบบ Backend พร้อมใช้งาน ✅
- ✅ CmOperatorController.java: มี API endpoints ครบถ้วน
- ✅ CmOperatorService.java: Business logic สมบูรณ์
- ✅ MaterialStockService.java: Stock management เรียบร้อย
- ✅ MaterialStockTransactionRepository.java: Database queries ครบถ้วน

### 2. ระบบ Frontend มีโครงสร้างครบ ✅
- ✅ CmOperatorDashboard.jsx: UI component พร้อมใช้งาน
- ✅ Dashboard.jsx: Route mapping ถูกต้อง
- ✅ API calls: HTTP requests ถูกต้อง

### 3. ปัญหาหลักที่น่าจะเป็นสาเหตุ ⚠️

#### 3.1 ข้อมูลในฐานข้อมูลไม่เพียงพอ
- Material table อาจไม่มีข้อมูล
- MaterialStockTransaction ไม่มี stock คงเหลือ
- ไม่มี Lot Numbers ให้เลือก

#### 3.2 Authentication/Authorization Issues
- User อาจไม่มี CM Operator role
- Token JWT อาจหมดอายุ
- Permissions ไม่ถูกต้อง

#### 3.3 Database Connection Issues
- SQL Server connection ไม่เสถียร
- Table constraints หรือ foreign keys ขัดข้อง

## แนวทางแก้ไข (Solutions)

### ขั้นตอนที่ 1: เตรียมข้อมูลฐานข้อมูล 
```sql
-- ใช้ไฟล์ cm-operator-test-data.sql ที่สร้างไว้
-- ประกอบด้วย:
-- 1. Material data (4 types: VIRGIN, ADMER, EVOH, MIX)
-- 2. MaterialStockTransaction (stock IN/OUT)
-- 3. MaterialUsageLog (usage history)
```

### ขั้นตอนที่ 2: ตรวจสอบ User & Roles
```sql
-- ตรวจสอบ CM Operator users
SELECT id, username, role FROM Users WHERE role = 'CM Operator';

-- หากไม่มี, สร้าง test user
INSERT INTO Users (username, password, role, full_name, created_at) 
VALUES ('cmoperator', '$2a$10$...', 'CM Operator', 'Test CM Operator', GETDATE());
```

### ขั้นตอนที่ 3: เริ่ม Backend Server
```bash
# ใน directory gdtahara-backend
mvn spring-boot:run

# หรือ
java -jar target/gdtahara-backend-1.0.0.jar
```

### ขั้นตอนที่ 4: ทดสอบ API Endpoints
```bash
# 1. ทดสอบ master-data materials
curl -X GET "http://localhost:8080/api/master-data/materials" 
     -H "Authorization: Bearer <JWT_TOKEN>"

# 2. ทดสอบ CM Operator specific endpoints
curl -X GET "http://localhost:8080/api/cm-operator/materials/1/lot-numbers" 
     -H "Authorization: Bearer <JWT_TOKEN>"

# 3. ทดสอบ stock-out
curl -X POST "http://localhost:8080/api/cm-operator/stock-out" 
     -H "Authorization: Bearer <JWT_TOKEN>" 
     -H "Content-Type: application/json" 
     -d '{"productionReportId":1,"materialId":1,"lotNumber":"LOT001PE","quantity":10.5}'
```

### ขั้นตอนที่ 5: ดู Log Files
```bash
# ตรวจสอบ Spring Boot logs สำหรับ errors
tail -f logs/application.log

# หรือดูใน console output
```

## ไฟล์ที่เกี่ยวข้อง

### Backend Files ✅
- `src/main/java/com/gdtahara/gdtaharabackend/controller/CmOperatorController.java`
- `src/main/java/com/gdtahara/gdtaharabackend/service/CmOperatorService.java`
- `src/main/java/com/gdtahara/gdtaharabackend/service/MaterialStockService.java`
- `src/main/java/com/gdtahara/gdtaharabackend/repository/MaterialStockTransactionRepository.java`
- `src/main/java/com/gdtahara/gdtaharabackend/dto/StockOutRequestDto.java`

### Frontend Files ✅
- `gdtahara-frontend/src/components/cm-operator/CmOperatorDashboard.jsx`
- `gdtahara-frontend/src/pages/Dashboard.jsx`

### Test Data Files 🆕
- `cm-operator-test-data.sql` (สร้างใหม่)

## วิธีการ Debug

### 1. ตรวจสอบ Frontend Console
```javascript
// เปิด Browser DevTools (F12)
// ไปที่ Console tab
// ดู error messages สีแดง
// ตรวจสอบ Network tab สำหรับ HTTP requests
```

### 2. ตรวจสอบ Backend Logs
```
2025-01-XX XX:XX:XX.XXX  INFO ... : Started GdtaharaBackendApplication
2025-01-XX XX:XX:XX.XXX DEBUG ... : SQL: SELECT ... FROM Material
2025-01-XX XX:XX:XX.XXX ERROR ... : [ถ้ามี error จะแสดงที่นี่]
```

### 3. ตรวจสอบ Database
```sql
-- ตรวจสอบข้อมูล Material
SELECT COUNT(*) FROM Material;

-- ตรวจสอบ Stock Balance
SELECT material_id, SUM(CASE WHEN transaction_type='IN' THEN quantity ELSE -quantity END) 
FROM MaterialStockTransaction GROUP BY material_id;

-- ตรวจสอบ Active Production Reports
SELECT COUNT(*) FROM ProductionReport WHERE status = 'IN_PROGRESS';
```

## Expected Behavior (พฤติกรรมที่ควรจะเป็น)

### เมื่อ CM Operator login สำเร็จ:
1. ✅ แสดงหน้า "เลือกใบสั่งผลิต"
2. ✅ แสดงรายการ Active Production Reports
3. ✅ เลือกได้และไปหน้า "บันทึกการเบิกจ่าย"
4. ✅ เลือก Material Type → แสดง Materials
5. ✅ เลือก Material → แสดง Lot Numbers
6. ✅ กรอกจำนวน → บันทึกได้สำเร็จ

### หาก error ใดๆ:
- ❌ หน้าว่างเปล่า → Database connection issue
- ❌ "ไม่สามารถดึงข้อมูลเริ่มต้นได้" → API endpoint issue
- ❌ Dropdown ว่าง → ข้อมูล Material/Stock ไม่มี
- ❌ "เกิดข้อผิดพลาดในการบันทึก" → Backend validation issue

## Quick Fix Commands

```bash
# 1. Restart Backend
cd gdtahara-backend
mvn clean spring-boot:run

# 2. Restart Frontend
cd gdtahara-frontend
npm run dev

# 3. Check Database Connection
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P tst123##
```

## สรุป

ระบบ CM Operator มีโครงสร้างพร้อมใช้งานครบถ้วน ปัญหาหลักน่าจะอยู่ที่:
1. 🔸 ข้อมูลในฐานข้อมูลไม่เพียงพอ (ใช้ cm-operator-test-data.sql)
2. 🔸 Backend server ไม่ได้ทำงาน (เริ่ม mvn spring-boot:run)
3. 🔸 Authentication issues (ตรวจสอบ JWT token)

หลังจากแก้ไขปัญหาเหล่านี้ CM Operator ควรทำงานได้ตามปกติ ✅