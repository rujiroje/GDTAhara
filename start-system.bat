@echo off
echo =======================================
echo   เริ่มต้นระบบ GDTahara
echo =======================================
echo.

REM ตรวจสอบว่า Java ทำงานได้หรือไม่
echo [1] ตรวจสอบ Java...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ไม่พบ Java! กรุณาติดตั้ง Java 17+ ก่อน
    pause
    exit /b 1
)
echo ✅ Java พร้อมใช้งาน
echo.

REM เริ่ม Backend
echo [2] เริ่มต้น Spring Boot Backend...
cd /d "%~dp0"
start "GDTahara Backend" cmd /k ".\mvnw.cmd spring-boot:run"

echo ⏳ รอ Backend เริ่มต้น (10 วินาที)...
timeout /t 10 >nul

REM เริ่ม Frontend
echo [3] เริ่มต้น React Frontend...
cd gdtahara-frontend
start "GDTahara Frontend" cmd /k "npm run dev"

echo.
echo =======================================
echo   ระบบเริ่มต้นแล้ว!
echo =======================================
echo 🌐 Backend:  http://localhost:8080
echo 🎨 Frontend: http://localhost:5173
echo.
echo กด Ctrl+C ในหน้าต่าง Terminal เพื่อปิดระบบ
echo.

REM เปิดเบราว์เซอร์
echo [4] เปิดเบราว์เซอร์...
timeout /t 5 >nul
start http://localhost:5173

echo.
echo Press any key to exit...
pause >nul