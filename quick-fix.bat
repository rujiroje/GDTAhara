@echo off
echo 🔧 GD Tahara Quick Fix Script
echo ============================

echo.
echo 📋 Available fix options:
echo 1. Start in Emergency Mode (bypass repository issues)
echo 2. Clean build and retry
echo 3. Show repository fix guide
echo 4. Exit

set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" (
    echo 🚨 Starting in Emergency Mode...
    mvn spring-boot:run -Dspring-boot.run.arguments=--emergency
) else if "%choice%"=="2" (
    echo 🧹 Cleaning and rebuilding...
    mvn clean compile
    echo ✅ Clean build completed. Try starting normally.
) else if "%choice%"=="3" (
    echo 📖 Opening repository fix guide...
    type REPOSITORY-FIX-GUIDE.md
    pause
) else (
    echo 👋 Exiting...
)

pause
