@echo off
echo ============================================
echo CM Operator Backend Restart Script
echo ============================================

cd /d "c:\Users\Rujiroje\OneDrive - Toyo Seikan (Thailand) Co.,Ltd\MyData\IT\TST\Target\2025\TR Blow\GDTahara\gdtahara-backend"

echo Killing existing Java processes...
taskkill /f /im java.exe 2>nul || echo No Java processes found

echo Waiting 3 seconds...
timeout /t 3

echo Starting Spring Boot application...
start "GDTahara Backend" cmd /k "mvn spring-boot:run"

echo.
echo Backend is starting... Please wait 30-60 seconds
echo Then try accessing: http://localhost:8080/api/auth/login
echo.
pause