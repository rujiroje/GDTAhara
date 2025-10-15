@echo off
echo ============================================
echo GDTahara Backend Quick Test Script
echo ============================================

cd /d "c:\Users\Rujiroje\OneDrive - Toyo Seikan (Thailand) Co.,Ltd\MyData\IT\TST\Target\2025\TR Blow\GDTahara\gdtahara-backend"

echo [1/3] Compiling Java sources...
mvnw.cmd compile -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Compilation failed!
    echo Check for syntax errors and try again.
    pause
    exit /b 1
)

echo ✅ Compilation successful!
echo.

echo [2/3] Testing quick startup (10 seconds)...
timeout /t 2 >nul

start "Backend Test" cmd /c "mvnw.cmd spring-boot:run & timeout /t 10 & taskkill /f /im java.exe"

echo [3/3] Waiting for startup test...
timeout /t 12 >nul

echo.
echo ✅ Backend startup test completed!
echo If no errors appeared above, the backend should start successfully.
echo To run normally, use: .\restart-backend.bat
echo.
pause