# GDTahara Database Testing Functions
# ฟังก์ชันสำหรับทดสอบฐานข้อมูล SQL Server ของระบบ GDTahara

# กำหนดค่าการเชื่อมต่อฐานข้อมูล
$global:ConnectionString = "Server=10.1.53.33,1433;Database=GDTahara;User Id=sa;Password=tst123##;TrustServerCertificate=true"

# ฟังก์ชันสำหรับทดสอบการเชื่อมต่อ
function Test-GDTaharaConnection {
    try {
        $connection = New-Object System.Data.SqlClient.SqlConnection($global:ConnectionString)
        $connection.Open()
        Write-Host "✅ เชื่อมต่อ GDTahara Database สำเร็จ!" -ForegroundColor Green
        $connection.Close()
        return $true
    }
    catch {
        Write-Host "❌ ไม่สามารถเชื่อมต่อได้: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# ฟังก์ชันสำหรับรัน SQL Query
function Invoke-GDTaharaQuery {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Query,
        [switch]$ShowResults = $true
    )
    
    try {
        $connection = New-Object System.Data.SqlClient.SqlConnection($global:ConnectionString)
        $connection.Open()
        
        $command = $connection.CreateCommand()
        $command.CommandText = $Query
        
        $reader = $command.ExecuteReader()
        $results = @()
        
        if ($ShowResults) {
            Write-Host "=== Query Results ===" -ForegroundColor Yellow
        }
        
        while ($reader.Read()) {
            $row = @{}
            for ($i = 0; $i -lt $reader.FieldCount; $i++) {
                $row[$reader.GetName($i)] = $reader.GetValue($i)
            }
            $results += New-Object PSObject -Property $row
            
            if ($ShowResults) {
                $rowText = ""
                foreach ($field in $row.Keys) {
                    $rowText += "$field`: $($row[$field]) | "
                }
                Write-Host ($rowText.TrimEnd(" | "))
            }
        }
        
        $reader.Close()
        $connection.Close()
        
        if ($ShowResults) {
            Write-Host "=== Total Records: $($results.Count) ===" -ForegroundColor Green
        }
        
        return $results
    }
    catch {
        Write-Host "❌ Query Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ฟังก์ชันตรวจสอบข้อมูล Production Reports
function Get-ProductionReports {
    param([int]$Top = 10)
    
    Write-Host "🔍 ตรวจสอบ Production Reports (TOP $Top)" -ForegroundColor Cyan
    $query = "SELECT TOP $Top id, order_number, start_date, end_date, status, created_at FROM production_reports ORDER BY created_at DESC"
    Invoke-GDTaharaQuery -Query $query
}

# ฟังก์ชันตรวจสอบข้อมูล NG Logs
function Get-NgLogs {
    param([int]$Top = 10)
    
    Write-Host "🔍 ตรวจสอบ NG Logs (TOP $Top)" -ForegroundColor Cyan
    $query = "SELECT TOP $Top nl.id, nl.report_id, nl.ng_type_id, nl.quantity, nl.timestamp, nt.ng_code, nt.ng_description_th, nt.ng_type FROM ng_logs nl LEFT JOIN ng_types nt ON nl.ng_type_id = nt.id ORDER BY nl.timestamp DESC"
    Invoke-GDTaharaQuery -Query $query
}

# ฟังก์ชันตรวจสอบข้อมูลแยกตามกะ (Shift-based data)
function Get-ShiftData {
    param(
        [Parameter(Mandatory=$true)]
        [DateTime]$Date,
        [Parameter(Mandatory=$true)]
        [ValidateSet("day", "night")]
        [string]$Shift
    )
    
    # คำนวณเวลาตามกะ
    if ($Shift -eq "day") {
        $startTime = $Date.Date.AddHours(3)  # 03:00
        $endTime = $Date.Date.AddHours(15)   # 15:00
        Write-Host "🌅 ตรวจสอบข้อมูลกะกลางวัน: $($startTime.ToString('yyyy-MM-dd HH:mm')) - $($endTime.ToString('yyyy-MM-dd HH:mm'))" -ForegroundColor Yellow
    } else {
        $startTime = $Date.Date.AddHours(15) # 15:00
        $endTime = $Date.Date.AddDays(1).AddHours(3) # 03:00 วันถัดไป
        Write-Host "🌙 ตรวจสอบข้อมูลกะกลางคืน: $($startTime.ToString('yyyy-MM-dd HH:mm')) - $($endTime.ToString('yyyy-MM-dd HH:mm'))" -ForegroundColor Blue
    }
    
    # ค้นหา Production Report ในช่วงเวลานั้น
    $reportQuery = "SELECT id, order_number, start_date, end_date, status FROM production_reports WHERE start_date <= '$($endTime.ToString('yyyy-MM-dd HH:mm:ss'))' AND end_date >= '$($startTime.ToString('yyyy-MM-dd HH:mm:ss'))'"
    
    Write-Host "📊 Production Reports ในช่วงกะ:" -ForegroundColor Green
    $reports = Invoke-GDTaharaQuery -Query $reportQuery
    
    if ($reports -and $reports.Count -gt 0) {
        foreach ($report in $reports) {
            $reportId = $report.id
            Write-Host "`n--- NG Logs สำหรับ Report ID: $reportId ---" -ForegroundColor Magenta
            
            $ngQuery = "SELECT nl.id, nl.ng_type_id, nl.quantity, nl.timestamp, nt.ng_code, nt.ng_description_th FROM ng_logs nl LEFT JOIN ng_types nt ON nl.ng_type_id = nt.id WHERE nl.report_id = $reportId AND nl.timestamp BETWEEN '$($startTime.ToString('yyyy-MM-dd HH:mm:ss'))' AND '$($endTime.ToString('yyyy-MM-dd HH:mm:ss'))' ORDER BY nl.timestamp DESC"
            Invoke-GDTaharaQuery -Query $ngQuery
        }
    } else {
        Write-Host "⚠️ ไม่พบ Production Reports ในช่วงกะนี้" -ForegroundColor Yellow
    }
}

# ฟังก์ชันตรวจสอบจำนวนข้อมูลในแต่ละ table
function Get-TableCounts {
    Write-Host "📈 จำนวนข้อมูลในแต่ละ Table:" -ForegroundColor Cyan
    
    $tables = @(
        "production_reports",
        "ng_logs", 
        "downtime_events",
        "material_usage_logs",
        "ng_types",
        "users"
    )
    
    foreach ($table in $tables) {
        $count = Invoke-GDTaharaQuery -Query "SELECT COUNT(*) as total FROM $table" -ShowResults:$false
        if ($count) {
            Write-Host "$table : $($count[0].total) records" -ForegroundColor White
        }
    }
}

# ฟังก์ชันสำหรับเพิ่มข้อมูลทดสอบ
function Add-TestData {
    Write-Host "➕ เพิ่มข้อมูลทดสอบ..." -ForegroundColor Yellow
    
    # เพิ่มข้อมูลทดสอบสำหรับ NG Logs ในวันที่ 15 กันยายน
    $testDate = Get-Date "2025-09-15"
    
    # ข้อมูลกะกลางวัน (03:00-15:00)
    $dayShiftNgLogs = @(
        @{ report_id = 2; ng_type_id = 4; quantity = 23; timestamp = $testDate.AddHours(8).AddMinutes(30) },  # คอออกครีบ
        @{ report_id = 2; ng_type_id = 8; quantity = 8; timestamp = $testDate.AddHours(10).AddMinutes(15) },   # ปากเบี้ยว
        @{ report_id = 2; ng_type_id = 1; quantity = 5; timestamp = $testDate.AddHours(12).AddMinutes(45) }    # จุดดำ
    )
    
    # ข้อมูลกะกลางคืน (15:00-03:00)
    $nightShiftNgLogs = @(
        @{ report_id = 2; ng_type_id = 1; quantity = 31; timestamp = $testDate.AddHours(18).AddMinutes(20) },  # จุดดำ
        @{ report_id = 2; ng_type_id = 4; quantity = 19; timestamp = $testDate.AddHours(21).AddMinutes(10) },  # คอออกครีบ
        @{ report_id = 2; ng_type_id = 7; quantity = 12; timestamp = $testDate.AddHours(23).AddMinutes(55) }   # เศษก้นติด
    )
    
    Write-Host "📝 เพิ่มข้อมูล NG Logs สำหรับกะกลางวัน..." -ForegroundColor Green
    foreach ($log in $dayShiftNgLogs) {
        $insertQuery = "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES ($($log.report_id), $($log.ng_type_id), 1, $($log.quantity), '$($log.timestamp.ToString('yyyy-MM-dd HH:mm:ss'))')"
        Invoke-GDTaharaQuery -Query $insertQuery -ShowResults:$false
    }
    
    Write-Host "📝 เพิ่มข้อมูล NG Logs สำหรับกะกลางคืน..." -ForegroundColor Blue
    foreach ($log in $nightShiftNgLogs) {
        $insertQuery = "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES ($($log.report_id), $($log.ng_type_id), 1, $($log.quantity), '$($log.timestamp.ToString('yyyy-MM-dd HH:mm:ss'))')"
        Invoke-GDTaharaQuery -Query $insertQuery -ShowResults:$false
    }
    
    Write-Host "✅ เพิ่มข้อมูลทดสอบเสร็จสิ้น!" -ForegroundColor Green
}

# ฟังก์ชันแสดงวิธีใช้งาน
function Show-Usage {
    Write-Host @"
🔧 GDTahara Database Testing Functions 🔧

การใช้งาน:
1. Test-GDTaharaConnection           - ทดสอบการเชื่อมต่อ
2. Get-ProductionReports -Top 5      - ดู Production Reports
3. Get-NgLogs -Top 20                - ดู NG Logs
4. Get-TableCounts                   - ดูจำนวนข้อมูลทั้งหมด
5. Get-ShiftData -Date "2025-09-15" -Shift "day"    - ดูข้อมูลกะกลางวัน
6. Get-ShiftData -Date "2025-09-15" -Shift "night"  - ดูข้อมูลกะกลางคืน
7. Add-TestData                      - เพิ่มข้อมูลทดสอบ
8. Invoke-GDTaharaQuery -Query "SELECT * FROM users" - รัน SQL ใดๆ

ตัวอย่าง:
  PS> Get-ShiftData -Date (Get-Date "2025-09-15") -Shift "day"
  PS> Invoke-GDTaharaQuery -Query "SELECT COUNT(*) FROM ng_logs WHERE CAST(timestamp AS DATE) = '2025-09-15'"

"@ -ForegroundColor Yellow
}

# แสดงวิธีใช้งานเมื่อโหลดไฟล์
Write-Host "✨ GDTahara Database Testing Functions โหลดเสร็จสิ้น!" -ForegroundColor Green
Show-Usage