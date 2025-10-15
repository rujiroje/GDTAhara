@echo off
echo =======================================
echo   ทดสอบการแก้ไขข้อมูลรายงานสรุปรายวัน
echo =======================================
echo.

echo [เตรียมข้อมูล] กำลังตรวจสอบการเชื่อมต่อฐานข้อมูล...
echo.

REM ทดสอบการเชื่อมต่อฐานข้อมูล
powershell -Command "try { $conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); Write-Host '✅ เชื่อมต่อฐานข้อมูลสำเร็จ'; $c.Close() } catch { Write-Host '❌ ไม่สามารถเชื่อมต่อฐานข้อมูลได้:' $_.Exception.Message }"

echo.
echo [ทดสอบ 1] ข้อมูลจริงในฐานข้อมูล...
echo.

REM ตรวจสอบข้อมูลจริงในฐานข้อมูล
echo --- NG Logs ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM ng_logs WHERE CAST(timestamp AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูล NG Logs วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo --- Downtime Events ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM downtime_events WHERE CAST(start_time AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูล Downtime Events วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo --- Material Usage ---
powershell -Command "$conn = 'Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true'; $c = New-Object System.Data.SqlClient.SqlConnection($conn); $c.Open(); $cmd = $c.CreateCommand(); $cmd.CommandText = 'SELECT COUNT(*) as count FROM material_usage_logs WHERE CAST(timestamp AS DATE) = ''2025-09-15'''; $reader = $cmd.ExecuteReader(); if ($reader.Read()) { Write-Host 'จำนวนข้อมูล Material Usage วันที่ 15 ก.ย. 2025:' $reader[''count''] 'รายการ' }; $reader.Close(); $c.Close()"

echo.
echo =======================================
echo [ทดสอบ 2] API Endpoints...
echo =======================================
echo.

echo กำลังทดสอบ API endpoints (ต้องเริ่ม backend ก่อน)...
echo.

echo --- กะกลางวัน (Day Shift) ---
curl -s -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=day" -H "Accept: application/json" > day-shift-test.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API กะกลางวัน: เรียกได้
    echo ข้อมูลที่ได้รับ:
    type day-shift-test.json | findstr /i "ngSummary\|downtimeHistory\|materialUsageLogs" 2>nul || echo "    (ไม่สามารถแสดงรายละเอียดได้)"
) else (
    echo ❌ API กะกลางวัน: ไม่สำเร็จ (ตรวจสอบว่า backend ทำงานอยู่หรือไม่)
)

echo.
echo --- กะกลางคืน (Night Shift) ---
curl -s -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=night" -H "Accept: application/json" > night-shift-test.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ API กะกลางคืน: เรียกได้
    echo ข้อมูลที่ได้รับ:
    type night-shift-test.json | findstr /i "ngSummary\|downtimeHistory\|materialUsageLogs" 2>nul || echo "    (ไม่สามารถแสดงรายละเอียดได้)"
) else (
    echo ❌ API กะกลางคืน: ไม่สำเร็จ (ตรวจสอบว่า backend ทำงานอยู่หรือไม่)
)

echo.
echo =======================================
echo [สรุปผลการทดสอบ]
echo =======================================
echo.

echo ✅ สิ่งที่แก้ไขแล้ว:
echo    - ลบข้อมูลตัวอย่าง (sample data) ออกจาก ProductionService
echo    - แก้ไข vite.config.js สำหรับ Windows path issues
echo    - เพิ่มข้อมูลจริงในฐานข้อมูลสำหรับทดสอบ
echo.

echo 📁 ไฟล์ผลลัพธ์:
echo    - day-shift-test.json (ข้อมูลกะกลางวัน)
echo    - night-shift-test.json (ข้อมูลกะกลางคืน)
echo.

echo 🔍 วิธีตรวจสอบเพิ่มเติม:
echo    1. เปิดไฟล์ JSON ที่สร้างขึ้นเพื่อดูข้อมูลที่ API ส่งกลับ
echo    2. เปรียบเทียบกับข้อมูลในฐานข้อมูล
echo    3. ตรวจสอบว่าไม่มีข้อมูลปลอมแล้ว
echo.

echo ต้องการเปิด Frontend สำหรับทดสอบ UI? (Y/N)
choice /c YN /n
if %ERRORLEVEL% EQU 1 (
    echo เปิด Frontend...
    start http://localhost:5173
)

echo.
echo การทดสอบเสร็จสิ้น!
pause