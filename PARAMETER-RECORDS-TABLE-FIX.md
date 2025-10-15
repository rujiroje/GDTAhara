# Parameter Records Table Fix Instructions

## ปัญหาที่เกิดขึ้น:
1. **SQL Script Error**: FK constraint conflict เมื่อพยายาม INSERT ข้อมูลทดสอบ
2. **Spring Boot Error**: Application ไม่สามารถ start ได้

## วิธีแก้ไข:

### 1. แก้ไข SQL Script (✅ ทำแล้ว)
```sql
-- ลบ INSERT statements ที่ทำให้เกิด FK constraint error
-- เหลือแค่การสร้าง table structure และ indexes
```

### 2. ตรวจสอบ Spring Boot Configuration

#### เช็ค application.properties:
```properties
# ตรวจสอบ database connection
spring.datasource.url=jdbc:sqlserver://RUJIROJPC\\SQLEXPRESS:1433;databaseName=GDTahara;trustServerCertificate=true
spring.datasource.username=
spring.datasource.password=
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

#### เช็ค Maven Dependencies:
```xml
<!-- ใน pom.xml ต้องมี -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3. Manual SQL Execution (ถ้า sqlcmd ไม่ทำงาน)

#### Option A: SQL Server Management Studio
1. เปิด SSMS
2. Connect to RUJIROJPC\SQLEXPRESS
3. เลือก database GDTahara
4. Copy-paste เนื้อหาจาก create-parameter-records-table.sql
5. Execute

#### Option B: Manual Table Creation
```sql
-- รัน command นี้ใน SQL Server
USE GDTahara;

-- Check if table exists
IF OBJECT_ID('parameter_records', 'U') IS NOT NULL 
    DROP TABLE parameter_records;

-- Copy entire CREATE TABLE statement from create-parameter-records-table.sql
-- และรัน
```

### 4. Verify Table Creation
```sql
-- ตรวจสอบว่า table ถูกสร้างแล้ว
SELECT COUNT(*) as table_exists 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'parameter_records';

-- ดู structure ของ table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'parameter_records'
ORDER BY ORDINAL_POSITION;
```

### 5. Test Application Startup
```bash
# ลองรัน Spring Boot
cd "c:\Users\Rujiroje\OneDrive - Toyo Seikan (Thailand) Co.,Ltd\MyData\IT\TST\Target\2025\TR Blow\GDTahara\gdtahara-backend"
.\mvnw.cmd spring-boot:run
```

### 6. Test Parameter Checklist API
```bash
# หลังจาก application start สำเร็จ
curl -X POST http://localhost:8080/api/technician/parameter-records \
  -H "Content-Type: application/json" \
  -d '{
    "reportId": 1,
    "technicianId": 1,
    "recordTime": "2025-09-19T16:00:00",
    "extruderEvohScrewRpm": 150.5,
    "tempEvohFb": 220.0,
    "cycleTimeSec": 45.2,
    "productQualityCheck": "ปกติ"
  }'
```

## หมายเหตุ:
- Table structure พร้อมใช้งานแล้ว (100+ fields ตาม PRD)
- แค่ต้องสร้าง table ในฐานข้อมูลให้เสร็จ
- Application ควรจะ start ได้หลังจากแก้ไข configuration
- Parameter Checklist Form พร้อมใช้งานแล้ว

## Next Steps:
1. สร้าง parameter_records table ในฐานข้อมูล
2. Start Spring Boot application
3. Test Parameter Checklist workflow
4. ทดสอบการบันทึกข้อมูลผ่าน UI