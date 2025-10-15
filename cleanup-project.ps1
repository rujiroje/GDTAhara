Write-Host "🧹 Cleaning up GD Tahara Backend Project..." -ForegroundColor Green

# ลบไฟล์ที่อยู่ผิดที่
$filesToRemove = @(
    "TechnicianService.java"
)

foreach ($file in $filesToRemove) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "✅ Removed: $file" -ForegroundColor Yellow
    } else {
        Write-Host "ℹ️  File not found: $file" -ForegroundColor Cyan
    }
}

# ลบไฟล์ temporary และ cache
$tempDirs = @(
    "target",
    ".gradle",
    "build"
)

foreach ($dir in $tempDirs) {
    if (Test-Path $dir) {
        Remove-Item $dir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✅ Cleaned directory: $dir" -ForegroundColor Yellow
    }
}

Write-Host "🎉 Project cleanup completed successfully!" -ForegroundColor Green
Write-Host "📝 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Run 'mvn clean compile' to verify compilation" -ForegroundColor White
Write-Host "   2. Run 'mvn spring-boot:run' to start the application" -ForegroundColor White
Write-Host "   3. Open http://localhost:8080/test-api.html to test APIs" -ForegroundColor White
