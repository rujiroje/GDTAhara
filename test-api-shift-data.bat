@echo off
echo ================================
echo   ทดสอบ API - รายงานสรุปรายวันแยกกะ
echo ================================
echo.

echo [1] ทดสอบกะกลางวัน (03:00-15:00)
curl -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=day" -H "Content-Type: application/json" > day-shift-result.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ กะกลางวัน: สำเร็จ
    type day-shift-result.json
) else (
    echo ❌ กะกลางวัน: ไม่สำเร็จ
)
echo.
echo ----------------------------------------
echo.

echo [2] ทดสอบกะกลางคืน (15:00-03:00)
curl -X GET "http://localhost:8080/api/production/daily-summary-by-shift?date=2025-09-15&shift=night" -H "Content-Type: application/json" > night-shift-result.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ กะกลางคืน: สำเร็จ
    type night-shift-result.json
) else (
    echo ❌ กะกลางคืน: ไม่สำเร็จ
)
echo.
echo ----------------------------------------
echo.

echo [3] ตรวจสอบข้อมูลฐานข้อมูล
curl -X GET "http://localhost:8080/api/pc/debug/service-test" -H "Content-Type: application/json" > debug-result.json 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Debug API: สำเร็จ
    type debug-result.json
) else (
    echo ❌ Debug API: ไม่สำเร็จ
)
echo.
echo ========================================
echo การทดสอบเสร็จสิ้น
echo ========================================
echo.

echo สรุปผลการทดสอบ:
echo - day-shift-result.json  (ข้อมูลกะกลางวัน)
echo - night-shift-result.json (ข้อมูลกะกลางคืน) 
echo - debug-result.json (ข้อมูล debug)
echo.

pause