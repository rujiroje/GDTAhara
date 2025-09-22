Write-Host "🔧 GD Tahara Database Recovery Script" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Yellow

$recoverySteps = @(
    "1. Stop the application if running",
    "2. Run constraint cleanup SQL script", 
    "3. Start application with recovery profile",
    "4. Verify database schema",
    "5. Test basic functionality"
)

Write-Host "📋 Recovery Steps:" -ForegroundColor Cyan
foreach ($step in $recoverySteps) {
    Write-Host "   $step" -ForegroundColor White
}

Write-Host ""
Write-Host "🚀 Quick Recovery Commands:" -ForegroundColor Green
Write-Host "1. Clean build:" -ForegroundColor Yellow
Write-Host "   mvn clean compile" -ForegroundColor White
Write-Host ""
Write-Host "2. Run with recovery profile:" -ForegroundColor Yellow  
Write-Host "   mvn spring-boot:run -Dspring-boot.run.profiles=recovery" -ForegroundColor White
Write-Host ""
Write-Host "3. Or run with specific database settings:" -ForegroundColor Yellow
Write-Host "   java -jar target/*.jar --spring.profiles.active=recovery" -ForegroundColor White
Write-Host ""

Write-Host "⚠️  Important Notes:" -ForegroundColor Red
Write-Host "- Recovery mode will recreate all database tables" -ForegroundColor White
Write-Host "- Existing data will be lost in recovery mode" -ForegroundColor White
Write-Host "- Make sure to backup important data before recovery" -ForegroundColor White
Write-Host ""

Write-Host "🔍 Troubleshooting:" -ForegroundColor Cyan
Write-Host "- If 'reportId' error persists, check ParameterRecordRepository" -ForegroundColor White
Write-Host "- If unique constraint errors continue, run cleanup-constraints.sql manually" -ForegroundColor White
Write-Host "- Check database connection settings in application.properties" -ForegroundColor White
