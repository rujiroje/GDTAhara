# 🚨 URGENT: Historical Reports API Fix

## ❌ **Root Cause ที่พบ:**
**Double `/api` prefix ใน URL**

### ปัญหา:
```
baseURL = 'http://localhost:8080/api'
URL call = '/api/production/reports/production-historical'
Result = 'http://localhost:8080/api/api/production/reports/production-historical'
                                    ^^^^^^^ ซ้ำ!
```

### Error ที่เจอ:
- 404 Not Found
- URL: `/api/api/production/reports/production-historical`
- สาเหตุ: baseURL มี `/api` อยู่แล้ว แต่ code ยังใส่ `/api` อีก

## ✅ **การแก้ไขเร่งด่วน:**

### 1. **ProductionControlDashboard.jsx:**
```javascript
// เดิม (ผิด)
const url = `/api/production/reports/production-historical?${params}`;

// ใหม่ (ถูกต้อง) 
const url = `/production/reports/production-historical?${params}`;
```

### 2. **HistoricalReports.jsx:**
```javascript
// เดิม (ผิด)
const url = `/api/production/reports/production-historical?${params}`;

// ใหม่ (ถูกต้อง)
const url = `http://localhost:8080/api/production/reports/production-historical?${params}`;
```

### 3. **Endpoints อื่นๆ ที่แก้ไข:**
- `/production/machines` (ลบ `/api` prefix)
- `/production/products` (ลบ `/api` prefix)

## 🧪 **วิธีทดสอบทันที:**

1. **รีเฟรช Browser:**
   ```
   Ctrl + F5 (Hard refresh)
   หรือ F12 → Network → Clear
   ```

2. **ทดสอบด้วย HTML file:**
   ```
   เปิด urgent-api-fix-test.html ในเบราเซอร์
   ```

3. **ทดสอบใน Application:**
   ```
   - ไปหน้า "รายงานย้อนหลัง"
   - เลือกวันที่ 01/09/2025 - 03/10/2025  
   - กดปุ่ม "ค้นหา"
   - ดูใน DevTools Network tab
   ```

## 📊 **Expected Results:**

### ✅ ถูกต้อง:
```
GET http://localhost:8080/api/production/reports/production-historical
Status: 200 OK
Response: [array of historical reports]
```

### ❌ ผิด (ไม่ควรเจออีก):
```
GET http://localhost:8080/api/api/production/reports/production-historical
Status: 404 Not Found
```

## 🎯 **URL Mapping ที่ถูกต้อง:**

| Feature | Correct URL |
|---------|-------------|
| Historical Reports | `/api/production/reports/production-historical` |
| Dashboard Summary | `/api/production/dashboard-summary` |
| Machines List | `/api/production/machines` |
| Products List | `/api/production/products` |

## ⚡ **Impact:**
- ✅ แก้ไข 404 errors ใน Historical Reports
- ✅ แก้ไข baseURL conflicts  
- ✅ ทำให้ API calls ทำงานถูกต้อง
- ✅ เตรียมพร้อมสำหรับ production deployment

---
*URGENT FIX Applied: October 3, 2025*
*Issue: Double API prefix causing 404 errors*