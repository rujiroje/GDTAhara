# =================================================================
# PowerShell Script สำหรับทดสอบระบบ GDTahara
# =================================================================

Write-Host "🔍 Testing GDTahara System Performance..." -ForegroundColor Cyan

# 1. Test Backend Health
Write-Host "`n1. Testing Backend Health..." -ForegroundColor Yellow
$startTime = Get-Date
try {
    $healthResponse = Invoke-WebRequest -Uri "http://localhost:8080/health" -Method GET -TimeoutSec 10
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalMilliseconds
    Write-Host "✅ Backend Health: OK (${responseTime}ms)" -ForegroundColor Green
    Write-Host "   Response: $($healthResponse.Content)" -ForegroundColor White
} catch {
    Write-Host "❌ Backend Health: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Test Database Connection via Auth endpoint
Write-Host "`n2. Testing Database Connection..." -ForegroundColor Yellow
$startTime = Get-Date
try {
    # Try a basic auth endpoint to see if database is responsive
    $authResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body '{"username":"test","password":"test"}' -ContentType "application/json" -TimeoutSec 10
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalMilliseconds
    Write-Host "✅ Database Connection: OK (${responseTime}ms)" -ForegroundColor Green
} catch {
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalMilliseconds
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Database Connection: OK (${responseTime}ms) - Auth working, credentials invalid" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Database Connection: SLOW or ERROR (${responseTime}ms)" -ForegroundColor Yellow
        Write-Host "   Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
    }
}

# 3. Test Frontend Server
Write-Host "`n3. Testing Frontend Server..." -ForegroundColor Yellow
$startTime = Get-Date
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:5173" -Method GET -TimeoutSec 5
    $endTime = Get-Date
    $responseTime = ($endTime - $startTime).TotalMilliseconds
    Write-Host "✅ Frontend Server: OK (${responseTime}ms)" -ForegroundColor Green
} catch {
    Write-Host "❌ Frontend Server: NOT RUNNING" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   💡 Run: npm run dev in gdtahara-frontend folder" -ForegroundColor Cyan
}

# 4. Network Connectivity Test
Write-Host "`n4. Testing Network to Database..." -ForegroundColor Yellow
$networkTest = Test-NetConnection -ComputerName "10.1.53.33" -Port 1433 -WarningAction SilentlyContinue
if ($networkTest.TcpTestSucceeded) {
    Write-Host "✅ Database Network: OK" -ForegroundColor Green
} else {
    Write-Host "❌ Database Network: FAILED" -ForegroundColor Red
}

# 5. Summary and Recommendations
Write-Host "`n📋 Performance Analysis:" -ForegroundColor Cyan
Write-Host "   - If response times > 2000ms: Database is slow" -ForegroundColor White
Write-Host "   - If response times > 5000ms: Check network or database server" -ForegroundColor White
Write-Host "   - If Frontend not running: Install Node.js and run 'npm run dev'" -ForegroundColor White

Write-Host "`n🎯 Next Steps:" -ForegroundColor Green
Write-Host "   1. Open test-api.html: http://localhost:8080/test-api.html" -ForegroundColor White
Write-Host "   2. Test login with sample credentials" -ForegroundColor White
Write-Host "   3. Check data loading in browser developer tools" -ForegroundColor White

Write-Host "`n🔧 If system is slow:" -ForegroundColor Yellow
Write-Host "   1. Check database has sample data" -ForegroundColor White
Write-Host "   2. Run SQL scripts to populate test data" -ForegroundColor White
Write-Host "   3. Monitor Spring Boot logs for slow queries" -ForegroundColor White