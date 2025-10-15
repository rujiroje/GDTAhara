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
        [string]$Query
    )
    
    try {
        $connection = New-Object System.Data.SqlClient.SqlConnection($global:ConnectionString)
        $connection.Open()
        
        $command = $connection.CreateCommand()
        $command.CommandText = $Query
        
        $reader = $command.ExecuteReader()
        Write-Host "=== Query Results ===" -ForegroundColor Yellow
        
        while ($reader.Read()) {
            $rowText = ""
            for ($i = 0; $i -lt $reader.FieldCount; $i++) {
                $fieldName = $reader.GetName($i)
                $fieldValue = $reader.GetValue($i)
                $rowText += "$fieldName = $fieldValue | "
            }
            Write-Host $rowText.TrimEnd(" | ")
        }
        
        $reader.Close()
        $connection.Close()
        Write-Host "=== Query Complete ===" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Query Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# ฟังก์ชันตรวจสอบข้อมูล Production Reports
function Get-ProductionReports {
    param([int]$Top = 10)
    
    Write-Host "🔍 ตรวจสอบ Production Reports (TOP $Top)" -ForegroundColor Cyan
    $query = "SELECT TOP $Top id, order_number, start_date, end_date, status FROM production_reports ORDER BY id DESC"
    Invoke-GDTaharaQuery -Query $query
}

# ฟังก์ชันตรวจสอบข้อมูล NG Logs
function Get-NgLogs {
    param([int]$Top = 10)
    
    Write-Host "🔍 ตรวจสอบ NG Logs (TOP $Top)" -ForegroundColor Cyan
    $query = "SELECT TOP $Top nl.id, nl.report_id, nl.ng_type_id, nl.quantity, nl.timestamp, nt.ng_code, nt.ng_description_th FROM ng_logs nl LEFT JOIN ng_types nt ON nl.ng_type_id = nt.id ORDER BY nl.timestamp DESC"
    Invoke-GDTaharaQuery -Query $query
}

# ฟังก์ชันตรวจสอบข้อมูลแยกตามกะ (Shift-based data)
function Get-ShiftData {
    param(
        [Parameter(Mandatory=$true)]
        [DateTime]$Date,
        [Parameter(Mandatory=$true)]
        [string]$Shift
    )
    
    # คำนวณเวลาตามกะ
    if ($Shift -eq "day") {
        $startTime = $Date.Date.AddHours(3)  # 03:00
        $endTime = $Date.Date.AddHours(15)   # 15:00
        Write-Host "🌅 ตรวจสอบข้อมูลกะกลางวัน" -ForegroundColor Yellow
    } else {
        $startTime = $Date.Date.AddHours(15) # 15:00
        $endTime = $Date.Date.AddDays(1).AddHours(3) # 03:00 วันถัดไป
        Write-Host "🌙 ตรวจสอบข้อมูลกะกลางคืน" -ForegroundColor Blue
    }
    
    Write-Host "เวลา: $($startTime.ToString('yyyy-MM-dd HH:mm')) ถึง $($endTime.ToString('yyyy-MM-dd HH:mm'))"
    
    # ค้นหา NG Logs ในช่วงเวลานั้น
    $ngQuery = "SELECT nl.id, nl.report_id, nl.ng_type_id, nl.quantity, nl.timestamp, nt.ng_code, nt.ng_description_th FROM ng_logs nl LEFT JOIN ng_types nt ON nl.ng_type_id = nt.id WHERE nl.timestamp BETWEEN '$($startTime.ToString('yyyy-MM-dd HH:mm:ss'))' AND '$($endTime.ToString('yyyy-MM-dd HH:mm:ss'))' ORDER BY nl.timestamp DESC"
    Invoke-GDTaharaQuery -Query $ngQuery
}

# ฟังก์ชันตรวจสอบจำนวนข้อมูลในแต่ละ table
function Get-TableCounts {
    Write-Host "📈 จำนวนข้อมูลในแต่ละ Table:" -ForegroundColor Cyan
    
    $tables = @("production_reports", "ng_logs", "downtime_events", "material_usage_logs", "ng_types", "users")
    
    foreach ($table in $tables) {
        $query = "SELECT COUNT(*) as total FROM $table"
        Write-Host "--- $table ---" -ForegroundColor Yellow
        Invoke-GDTaharaQuery -Query $query
    }
}

# ฟังก์ชันสำหรับเพิ่มข้อมูลทดสอบ
function Add-TestData {
    Write-Host "➕ เพิ่มข้อมูลทดสอบ..." -ForegroundColor Yellow
    
    # เพิ่มข้อมูลทดสอบสำหรับ NG Logs ในวันที่ 15 กันยายน 2025
    $testDate = Get-Date "2025-09-15"
    
    # ข้อมูลกะกลางวัน (08:30, 10:15, 12:45)
    $dayQueries = @(
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 4, 1, 23, '2025-09-15 08:30:00')",
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 8, 1, 8, '2025-09-15 10:15:00')",
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 1, 1, 5, '2025-09-15 12:45:00')"
    )
    
    # ข้อมูลกะกลางคืน (18:20, 21:10, 23:55)
    $nightQueries = @(
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 1, 1, 31, '2025-09-15 18:20:00')",
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 4, 1, 19, '2025-09-15 21:10:00')",
        "INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, timestamp) VALUES (2, 7, 1, 12, '2025-09-15 23:55:00')"
    )
    
    Write-Host "📝 เพิ่มข้อมูล NG Logs สำหรับกะกลางวัน..." -ForegroundColor Green
    foreach ($query in $dayQueries) {
        Invoke-GDTaharaQuery -Query $query
    }
    
    Write-Host "📝 เพิ่มข้อมูล NG Logs สำหรับกะกลางคืน..." -ForegroundColor Blue
    foreach ($query in $nightQueries) {
        Invoke-GDTaharaQuery -Query $query
    }
    
    Write-Host "✅ เพิ่มข้อมูลทดสอบเสร็จสิ้น!" -ForegroundColor Green
}

# ฟังก์ชันแสดงวิธีใช้งาน
function Show-Usage {
    Write-Host ""
    Write-Host "🔧 GDTahara Database Testing Functions 🔧" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "การใช้งาน:"
    Write-Host "1. Test-GDTaharaConnection                    - ทดสอบการเชื่อมต่อ"
    Write-Host "2. Get-ProductionReports -Top 5               - ดู Production Reports"
    Write-Host "3. Get-NgLogs -Top 20                         - ดู NG Logs"
    Write-Host "4. Get-TableCounts                            - ดูจำนวนข้อมูลทั้งหมด"
    Write-Host "5. Get-ShiftData -Date (Get-Date '2025-09-15') -Shift 'day'    - ดูข้อมูลกะกลางวัน"
    Write-Host "6. Get-ShiftData -Date (Get-Date '2025-09-15') -Shift 'night'  - ดูข้อมูลกะกลางคืน"
    Write-Host "7. Add-TestData                               - เพิ่มข้อมูลทดสอบ"
    Write-Host "8. Invoke-GDTaharaQuery -Query 'SELECT * FROM users'          - รัน SQL ใดๆ"
    Write-Host ""
}

# แสดงวิธีใช้งานเมื่อโหลดไฟล์
Write-Host "✨ GDTahara Database Testing Functions โหลดเสร็จสิ้น!" -ForegroundColor Green
Show-Usage