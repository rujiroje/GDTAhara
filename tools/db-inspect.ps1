param(
    [string]$Server = '10.1.53.33,1433',
    [string]$Database = 'GDTahara',
    [string]$User = 'sa',
    [string]$Password = 'tst123##',
    [Nullable[DateTime]]$Date = $null,
    [Nullable[Int64]]$ReportId = $null
)

$ErrorActionPreference = 'Stop'

function Invoke-DbQuery {
    param(
        [Parameter(Mandatory=$true)][string]$Query
    )
    $connStr = "Server=$Server;Database=$Database;User Id=$User;Password=$Password;TrustServerCertificate=true"
    $conn = New-Object System.Data.SqlClient.SqlConnection $connStr
    try {
        $conn.Open()
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = $Query
        $rdr = $cmd.ExecuteReader()
        $schema = @()
        for ($i=0; $i -lt $rdr.FieldCount; $i++) { $schema += $rdr.GetName($i) }
        if ($schema.Count -gt 0) { Write-Output ('# ' + ($schema -join ',')) }
        while ($rdr.Read()) {
            $values = for ($i=0; $i -lt $rdr.FieldCount; $i++) { $rdr.GetValue($i) }
            Write-Output ($values -join ',')
        }
        $rdr.Close()
    } finally {
        if ($conn.State -ne 'Closed') { $conn.Close() }
    }
}

Write-Output '== Connectivity test =='
try {
    Invoke-DbQuery -Query 'SELECT 1 AS ok'
    Write-Output 'Connectivity: OK'
} catch {
    Write-Output ('Connectivity: FAIL -> ' + $_.Exception.Message)
    exit 1
}

Write-Output "== Tables =="
Invoke-DbQuery -Query "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_SCHEMA, TABLE_NAME"

Write-Output "== ng_types columns =="
Invoke-DbQuery -Query "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='ng_types' ORDER BY ORDINAL_POSITION"

Write-Output "== ng_types blank-all rows (desc/code/type all empty) =="
Invoke-DbQuery -Query "SELECT COUNT(*) AS blank_all FROM ng_types WHERE (ng_description_th IS NULL OR LTRIM(RTRIM(ng_description_th))='') AND (ng_code IS NULL OR LTRIM(RTRIM(ng_code))='') AND (ng_type IS NULL OR LTRIM(RTRIM(ng_type))='')"

Write-Output "== ng_types rows with description explicitly set to a placeholder (length<=4) =="
Invoke-DbQuery -Query "SELECT COUNT(*) AS short_desc FROM ng_types WHERE ng_description_th IS NOT NULL AND LEN(LTRIM(RTRIM(ng_description_th)))<=4"

Write-Output "== ng_types sample top 20 =="
Invoke-DbQuery -Query "SELECT TOP (20) id, LTRIM(RTRIM(ng_description_th)) AS desc_th, LTRIM(RTRIM(ng_code)) AS code, LTRIM(RTRIM(ng_type)) AS type FROM ng_types ORDER BY id DESC"

Write-Output "== ng_logs aggregated labels (last 7 days) =="
$q = @"
DECLARE @start DATETIME2 = DATEADD(DAY,-7,SYSDATETIME());
DECLARE @end   DATETIME2 = SYSDATETIME();
SELECT TOP (50)
       nt.id AS ng_type_id,
       LTRIM(RTRIM(nt.ng_description_th)) AS desc_th,
       LTRIM(RTRIM(nt.ng_code))           AS code,
       LTRIM(RTRIM(nt.ng_type))           AS type,
       SUM(COALESCE(nl.quantity,0))       AS qty,
       COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ') AS label_used
FROM ng_logs nl
JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.[timestamp] BETWEEN @start AND @end
GROUP BY nt.id, nt.ng_description_th, nt.ng_code, nt.ng_type
ORDER BY qty DESC;
"@
Invoke-DbQuery -Query $q

Write-Output "== ng_logs linkage checks =="
Invoke-DbQuery -Query "SELECT COUNT(*) AS total_ng_logs FROM ng_logs"
Invoke-DbQuery -Query "SELECT COUNT(*) AS ng_type_id_null FROM ng_logs WHERE ng_type_id IS NULL"
Invoke-DbQuery -Query "SELECT COUNT(*) AS ng_type_missing_master FROM ng_logs l WHERE NOT EXISTS (SELECT 1 FROM ng_types t WHERE t.id = l.ng_type_id)"
Invoke-DbQuery -Query "SELECT MIN([timestamp]) AS min_ts, MAX([timestamp]) AS max_ts FROM ng_logs"
Invoke-DbQuery -Query "SELECT TOP (10) id, report_id, ng_type_id, quantity, [timestamp] FROM ng_logs ORDER BY [timestamp] DESC"

Write-Output "== ng_logs label_used would be 'ไม่ระบุ' (all three fields empty after TRIM) =="
Invoke-DbQuery -Query @"
SELECT COUNT(*) AS label_is_mai_rabu
FROM ng_logs nl
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),'')) IS NULL
"@

Write-Output "== Recent ng_logs with computed label_used (last 90 days) =="
$q2 = @"
DECLARE @start DATETIME2 = DATEADD(DAY,-90,SYSDATETIME());
DECLARE @end   DATETIME2 = SYSDATETIME();
SELECT TOP (50)
    nl.id, nl.report_id, nl.ng_type_id, nl.quantity, nl.[timestamp],
    LTRIM(RTRIM(nt.ng_description_th)) AS desc_th,
    LTRIM(RTRIM(nt.ng_code))           AS code,
    LTRIM(RTRIM(nt.ng_type))           AS type,
    COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ') AS label_used
FROM ng_logs nl
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.[timestamp] BETWEEN @start AND @end
ORDER BY nl.[timestamp] DESC;
"@
Invoke-DbQuery -Query $q2

Write-Output '== Done =='

# Optional: If a date is provided, inspect NG labels for that production day (03:00–03:00)
if ($Date -ne $null) {
    $d = Get-Date $Date
    $start = (Get-Date -Date ($d.Date.AddHours(3)) -Format 'yyyy-MM-dd HH:mm:ss')
    $end   = (Get-Date -Date ($d.Date.AddDays(1).AddHours(3).AddSeconds(-1)) -Format 'yyyy-MM-dd HH:mm:ss')
    Write-Output ("== NG label aggregation for production day {0} (window {1} to {2}) ==" -f ($d.ToString('yyyy-MM-dd')), $start, $end)
    $q3 = @"
DECLARE @start DATETIME2 = '$start';
DECLARE @end   DATETIME2 = '$end';
SELECT 
    COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ') AS label_used,
    SUM(COALESCE(nl.quantity,0)) AS qty
FROM ng_logs nl
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.[timestamp] BETWEEN @start AND @end
GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ')
ORDER BY qty DESC;
"@
    Invoke-DbQuery -Query $q3

    Write-Output '== NG label by machine/product for the same window =='
    $q4 = @"
DECLARE @start DATETIME2 = '$start';
DECLARE @end   DATETIME2 = '$end';
SELECT 
    m.machine_name,
    p.product_name,
    COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ') AS label_used,
    SUM(COALESCE(nl.quantity,0)) AS qty
FROM ng_logs nl
JOIN production_reports r ON r.id = nl.report_id
LEFT JOIN machines m ON m.id = r.machine_id
LEFT JOIN products p ON p.id = r.product_id
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.[timestamp] BETWEEN @start AND @end
GROUP BY m.machine_name, p.product_name, COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ')
ORDER BY m.machine_name, p.product_name, qty DESC;
"@
    Invoke-DbQuery -Query $q4
}

# Optional: If a reportId is provided, inspect NG labels for that report
if ($ReportId -ne $null) {
    Write-Output ("== NG label aggregation for report_id {0} ==" -f $ReportId)
    $q5 = @"
SELECT 
    COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ') AS label_used,
    SUM(COALESCE(nl.quantity,0)) AS qty
FROM ng_logs nl
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.report_id = $ReportId
GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(nt.ng_description_th)),''), NULLIF(LTRIM(RTRIM(nt.ng_code)),''), NULLIF(LTRIM(RTRIM(nt.ng_type)),''), N'ไม่ระบุ')
ORDER BY qty DESC;
"@
    Invoke-DbQuery -Query $q5

    Write-Output '== Raw ng_logs for this report (latest 50) =='
    $q6 = @"
SELECT TOP (50)
    nl.id, nl.report_id, nl.ng_type_id, nl.quantity, nl.[timestamp],
    LTRIM(RTRIM(nt.ng_description_th)) AS desc_th,
    LTRIM(RTRIM(nt.ng_code))           AS code,
    LTRIM(RTRIM(nt.ng_type))           AS type
FROM ng_logs nl
LEFT JOIN ng_types nt ON nt.id = nl.ng_type_id
WHERE nl.report_id = $ReportId
ORDER BY nl.[timestamp] DESC;
"@
    Invoke-DbQuery -Query $q6
}
