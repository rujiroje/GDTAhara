@echo off
echo =======================================
echo   ทดสอบหลังแก้ไข Sample Data  
echo =======================================
echo.

echo [1] ตรวจสอบการเชื่อมต่อฐานข้อมูล...
powershell -Command "try { $conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); Write-Host '✅ เชื่อมต่อฐานข้อมูลสำเร็จ'; $c.Close() } catch { Write-Host '❌ ไม่สามารถเชื่อมต่อฐานข้อมูลได้:' $_.Exception.Message }"

echo.
echo [2] ตรวจสอบข้อมูลจริงในฐานข้อมูล...
echo.

echo --- NG Logs (วันที่ 15 ก.ย. 2025) ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM ng_logs WHERE CAST(timestamp AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูล NG Logs วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo --- Downtime Events (วันที่ 15 ก.ย. 2025) ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM downtime_events WHERE CAST(start_time AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูล Downtime Events วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo --- Material Usage (วันที่ 15 ก.ย. 2025) ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM material_usage_logs WHERE CAST(timestamp AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูل Material Usage วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo =======================================
echo [3] ทดสอบ API Endpoints...
echo =======================================
echo.

echo กำลังทดสอบ API endpoints (ต้องเริ่ม backend ก่อน)...
echo.

echo --- Test Day Shift (กะกลางวัน) ---
curl -s -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=day" -H "Accept: application/json" > day-shift-test.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API กะกลางวัน: เรียกได้
    echo กำลังตรวจสอบว่ามี sample data หรือไม่...
    findstr /i "sample\|demo\|fake\|test\|เก็บตัวอย่าง\|ของเสียจากการตัด" day-shift-test.json >nul
    if %ERRORLEVEL% EQU 0 (
        echo ❌ ยังพบ sample data ในผลลัพธ์!
        findstr /i "sample\|demo\|fake\|test\|เก็บตัวอย่าง\|ของเสียจากการตัด" day-shift-test.json
    ) else (
        echo ✅ ไม่พบ sample data - ใช้ข้อมูลจริงเท่านั้น
    )
) else (
    echo ❌ API กะกลางวัน: ไม่สำเร็จ (ตรวจสอบว่า backend ทำงานอยู่หรือไม่)
)

echo.
echo --- Test Night Shift (กะกลางคืน) ---
curl -s -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=night" -H "Accept: application/json" > night-shift-test.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API กะกลางคืน: เรียกได้
    echo กำลังตรวจสอบว่ามี sample data หรือไม่...
    findstr /i "sample\|demo\|fake\|test\|ของเสียจากการเป่า\|รอยยิ่วม" night-shift-test.json >nul
    if %ERRORLEVEL% EQU 0 (
        echo ❌ ยังพบ sample data ในผลลัพธ์!
        findstr /i "sample\|demo\|fake\|test\|ของเสียจากการเป่า\|รอยยิ่วม" night-shift-test.json
    ) else (
        echo ✅ ไม่พบ sample data - ใช้ข้อมูลจริงเท่านั้น
    )
) else (
    echo ❌ API กะกลางคืน: ไม่สำเร็จ (ตรวจสอบว่า backend ทำงานอยู่หรือไม่)
)

echo.
echo --- Test Current Date (วันที่ปัจจุบัน 9 ต.ค. 2025) ---
curl -s -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-10-09&shift=day" -H "Accept: application/json" > current-date-test.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API วันที่ปัจจุบัน: เรียกได้
    echo กำลังตรวจสอบว่าแสดงข้อมูลว่างหรือ sample data...
    findstr /i "sample\|demo\|fake\|test" current-date-test.json >nul
    if %ERRORLEVEL% EQU 0 (
        echo ❌ ยังพบ sample data สำหรับวันที่ปัจจุบัน!
        findstr /i "sample\|demo\|fake\|test" current-date-test.json
    ) else (
        echo ✅ ไม่พบ sample data - แสดงข้อมูลว่างหรือข้อมูลจริงเท่านั้น
    )
) else (
    echo ❌ API วันที่ปัจจุบัน: ไม่สำเร็จ
)

echo.
echo =======================================
echo [4] สรุปผลการทดสอบ
echo =======================================
echo.

echo ✅ สิ่งที่แก้ไขในครั้งนี้:
echo    - ลบ sample data จาก ProductionService.java ทั้งหมด
echo    - ปิดใช้งาน /create-sample-data endpoint
echo    - เปลี่ยน Frontend port เป็น 5173
echo    - ระบบจะแสดงเฉพาะข้อมูลจริงหรือข้อมูลว่างเท่านั้น
echo.

echo 📁 ไฟล์ผลลัพธ์ที่สร้างขึ้น:
echo    - day-shift-test.json (ข้อมูลกะกลางวัน)
echo    - night-shift-test.json (ข้อมูลกะกลางคืน)
echo    - current-date-test.json (ข้อมูลวันที่ปัจจุบัน)
echo.

echo 🎯 ผลที่คาดหวัง:
echo    - วันที่ 15 ก.ย. 2025: แสดงข้อมูลจริงจากฐานข้อมูล (มี NG, Downtime, Material Usage)
echo    - วันที่ 9 ต.ค. 2025: แสดงข้อมูลว่าง (ไม่มีข้อมูลจริงในฐานข้อมูล)
echo    - ไม่มี sample data ปรากฏในทุกกรณี
echo.

echo 🌐 การทดสอบ Frontend:
echo    - เปิด http://localhost:5173 (port ใหม่)
echo    - ตรวจสอบว่าแสดงเฉพาะข้อมูลจริง
echo    - ไม่แสดงวันที่ที่เป็น sample data อีกต่อไป
echo.

echo การทดสอบเสร็จสิ้น!
pause