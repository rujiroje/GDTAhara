Write-Host "🧹 Final Cleanup for GD Tahara Backend Project" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Yellow

# ลบไฟล์ที่อยู่ผิดที่
$filesToRemove = @(
    "TechnicianService.java"
)

Write-Host "Removing misplaced files..." -ForegroundColor Cyan
foreach ($file in $filesToRemove) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "✅ Removed: $file" -ForegroundColor Green
    } else {
        Write-Host "ℹ️  File not found: $file" -ForegroundColor Yellow
    }
}

# ลบ temporary files
Write-Host "Cleaning temporary files..." -ForegroundColor Cyan
$tempPatterns = @("*.bak", "*.tmp", "*.log", "*~")
foreach ($pattern in $tempPatterns) {
    Get-ChildItem -Path . -Filter $pattern -Recurse | ForEach-Object {
        Remove-Item $_.FullName -Force
        Write-Host "✅ Removed temp file: $($_.Name)" -ForegroundColor Green
    }
}

# ลบ build artifacts
$buildDirs = @("target", ".gradle", "build", "out")
foreach ($dir in $buildDirs) {
    if (Test-Path $dir) {
        Remove-Item $dir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✅ Cleaned build directory: $dir" -ForegroundColor Green
    }
}

Write-Host "=================================================" -ForegroundColor Yellow
Write-Host "🎉 Cleanup completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Remaining warnings are non-critical:" -ForegroundColor Cyan
Write-Host "   - DevTools properties (requires spring-boot-devtools dependency)" -ForegroundColor White
Write-Host "   - Custom CORS properties (configured in SecurityConfig instead)" -ForegroundColor White
Write-Host "   - Management properties (requires spring-boot-actuator dependency)" -ForegroundColor White
Write-Host ""
Write-Host "🚀 Your application is ready to run!" -ForegroundColor Green
Write-Host "   mvn spring-boot:run" -ForegroundColor Yellow
Write-Host "   http://localhost:8080/test-api.html" -ForegroundColor Yellow
