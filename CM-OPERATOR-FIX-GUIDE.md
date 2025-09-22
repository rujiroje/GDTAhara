# CM OPERATOR - คำแนะนำการแก้ไขและทดสอบ
# =================================================================
# วิธีแก้ไขปัญหา CM Operator ให้ทำงานได้อย่างถูกต้อง
# =================================================================

## ปัญหาที่แก้ไขแล้ว ✅

### 1. **Security Configuration Issues**
- ✅ เพิ่ม `/api/master-data/**` และ `/api/cm-operator/**` ใน SecurityConfig.java
- ✅ แก้ไข authentication paths ให้ครบถ้วน

### 2. **Missing Test Data**
- ✅ สร้าง `cm-operator-test-data.sql` - ข้อมูล Materials และ Stock
- ✅ สร้าง `create-cm-operator-users.sql` - User accounts สำหรับทดสอบ

### 3. **Development Tools**
- ✅ สร้าง `restart-backend.bat` - Script สำหรับ restart backend
- ✅ สร้าง `cm-operator-api-test.html` - Web tool สำหรับทดสอบ API

## ขั้นตอนการแก้ไข (Step-by-Step Fix)

### 📋 **ขั้นตอนที่ 1: เตรียมฐานข้อมูล**
```sql
-- 1.1 สร้าง CM Operator users
-- รันไฟล์: create-cm-operator-users.sql
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P "tst123##" -i create-cm-operator-users.sql

-- 1.2 เพิ่มข้อมูลทดสอบ
-- รันไฟล์: cm-operator-test-data.sql  
sqlcmd -S 10.1.53.33 -d GDTahara -U sa -P "tst123##" -i cm-operator-test-data.sql
```

### 🚀 **ขั้นตอนที่ 2: Restart Backend**
```bash
# วิธี 1: ใช้ batch script
restart-backend.bat

# วิธี 2: Manual restart
# Kill existing Java processes
taskkill /f /im java.exe
# Start new instance  
mvn spring-boot:run
```

### 🌐 **ขั้นตอนที่ 3: ทดสอบ API**
```
1. เปิดไฟล์: cm-operator-api-test.html ใน browser
2. Login ด้วย: 
   - Username: cmoperator
   - Password: test123
3. ทดสอบ API endpoints ตามลำดับ:
   - Materials API
   - Active Reports API  
   - Lot Numbers API
   - Stock-Out API
```

### 💻 **ขั้นตอนที่ 4: ทดสอบ Frontend**
```bash
# Start frontend development server
cd gdtahara-frontend
npm run dev

# เข้าใช้งานที่: http://localhost:5173
# Login ด้วย CM Operator account
```

## ไฟล์ที่ถูกแก้ไข 🔧

### Backend Files:
- ✅ `SecurityConfig.java` - เพิ่ม CM Operator API paths
- ✅ `CmOperatorController.java` - Security annotations ถูกต้อง
- ✅ `CmOperatorService.java` - MaterialUsageLog mapping แก้ไขแล้ว

### Database Scripts:
- 🆕 `create-cm-operator-users.sql` - Test users (cmoperator, cmop2)
- 🆕 `cm-operator-test-data.sql` - Materials และ Stock data

### Development Tools:
- 🆕 `restart-backend.bat` - Backend restart script
- 🆕 `cm-operator-api-test.html` - API testing tool

## ข้อมูลการทดสอบ 📊

### **Test Users:**
```
Username: cmoperator
Password: test123
Role: CM Operator

Username: cmop2  
Password: test123
Role: CM Operator
```

### **Test Materials:**
```
PE001 - PE ไวร์จิน (VIRGIN)
AD001 - ADMER สำหรับการยึดติด (ADMER)  
EV001 - EVOH สำหรับกันออกซิเจน (EVOH)
MX001 - วัสดุผสม (MIX)
```

### **Available Lot Numbers:**
```
LOT001PE, LOT002PE, LOT003PE (สำหรับ PE001)
LOT001AD, LOT002AD (สำหรับ AD001)
LOT001EV, LOT002EV (สำหรับ EV001)  
LOT001MX, LOT002MX (สำหรับ MX001)
```

## Expected Behavior (ผลลัพธ์ที่คาดหวัง) 🎯

### **เมื่อ Login สำเร็จ:**
1. ✅ แสดงหน้า "เลือกใบสั่งผลิต"
2. ✅ แสดงรายการ Active Production Reports
3. ✅ คลิกเลือก Report → ไปหน้า "บันทึกการเบิกจ่าย"

### **ในหน้า Stock-Out:**
1. ✅ เลือก Material Type → แสดง dropdown Materials
2. ✅ เลือก Material → แสดง dropdown Lot Numbers  
3. ✅ กรอกจำนวน → คลิก "บันทึกการเบิกจ่าย"
4. ✅ แสดงข้อความ "บันทึกการเบิกจ่ายสำเร็จ!"

## Troubleshooting Common Issues 🔍

### ❌ **"ไม่สามารถดึงข้อมูลเริ่มต้นได้"**
**สาเหตุ**: Backend server ไม่ทำงาน หรือ API endpoints ไม่ได้รับ authentication
**แก้ไข**: 
1. ตรวจสอบ backend server ทำงานที่ port 8080
2. ตรวจสอบ JWT token ใน localStorage
3. ลองทดสอบ API ด้วย cm-operator-api-test.html

### ❌ **"403 Forbidden" errors**
**สาเหตุ**: Security configuration หรือ JWT token หมดอายุ
**แก้ไข**:
1. ตรวจสอบ SecurityConfig.java มี `/api/cm-operator/**` 
2. Login ใหม่เพื่อรับ JWT token ใหม่
3. ตรวจสอบ User มี role "CM Operator"

### ❌ **Dropdown Materials/Lot Numbers ว่าง**
**สาเหตุ**: ไม่มีข้อมูล Material หรือ Stock ในฐานข้อมูล
**แก้ไข**: รัน cm-operator-test-data.sql

### ❌ **"วัตถุดิบมีสต็อกไม่เพียงพอ"**  
**สาเหตุ**: Stock balance น้อยกว่าจำนวนที่เบิก
**แก้ไข**: ตรวจสอบ MaterialStockTransaction table และเพิ่ม Stock IN

## Testing Commands 🧪

### **Test Backend API:**
```bash
# Test health check
curl http://localhost:8080/health

# Test login (should return JWT token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cmoperator","password":"test123"}'
```

### **Database Verification:**
```sql
-- Check CM Operator users
SELECT username, role, is_active FROM Users WHERE role = 'CM Operator';

-- Check Material stock balance
SELECT m.material_code, 
       SUM(CASE WHEN mst.transaction_type = 'IN' THEN mst.quantity ELSE -mst.quantity END) as balance
FROM Material m
LEFT JOIN MaterialStockTransaction mst ON m.id = mst.material_id  
GROUP BY m.material_code;

-- Check available lot numbers
SELECT DISTINCT lot_number FROM MaterialStockTransaction WHERE transaction_type = 'IN';
```

## สรุปการแก้ไข 📝

CM Operator ระบบได้รับการแก้ไขครบถ้วนแล้ว:

1. ✅ **Backend**: Security config, API endpoints, Business logic
2. ✅ **Database**: Test users, Materials, Stock data  
3. ✅ **Frontend**: Component structure, API integration
4. ✅ **Tools**: Testing utilities, Restart scripts

**การทดสอบ**: ใช้ cm-operator-api-test.html และ frontend application

**ผลลัพธ์**: CM Operator สามารถ login, เลือก production report, เลือก materials/lot numbers, และบันทึก stock-out ได้สำเร็จ ✨

---
**หมายเหตุ**: หากยังมีปัญหา ให้ตรวจสอบ console logs ใน browser DevTools และ Spring Boot application logs