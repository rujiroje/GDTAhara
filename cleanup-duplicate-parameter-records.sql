-- Cleanup duplicate parameter_records for the same (report_id, record_time)
-- 1) Backup current data
IF OBJECT_ID('dbo.parameter_records_backup_20251015', 'U') IS NULL
BEGIN
    SELECT * INTO dbo.parameter_records_backup_20251015 FROM dbo.parameter_records;
    PRINT 'Backup table created: dbo.parameter_records_backup_20251015';
END
ELSE
BEGIN
    PRINT 'Backup table already exists: dbo.parameter_records_backup_20251015 (skipped)';
END
GO

-- 2) Show duplicate keys (preview)
SELECT report_id, record_time, COUNT(*) AS cnt
FROM dbo.parameter_records
GROUP BY report_id, record_time
HAVING COUNT(*) > 1
ORDER BY report_id, record_time;
GO

-- 3) Preview rows that would be deleted (keeps the latest by created_at, then id)
;WITH ranked AS (
    SELECT id, report_id, record_time, created_at,
           ROW_NUMBER() OVER (PARTITION BY report_id, record_time ORDER BY created_at DESC, id DESC) AS rn
    FROM dbo.parameter_records
)
SELECT pr.*
FROM dbo.parameter_records pr
JOIN ranked r ON r.id = pr.id
WHERE r.rn > 1
ORDER BY pr.report_id, pr.record_time, pr.created_at;
GO

-- 4) Delete duplicates while keeping the latest row per (report_id, record_time)
;WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY report_id, record_time ORDER BY created_at DESC, id DESC) AS rn
    FROM dbo.parameter_records
)
DELETE pr
FROM dbo.parameter_records pr
JOIN ranked r ON r.id = pr.id
WHERE r.rn > 1;
GO

-- 5) Verify no duplicates remain
SELECT report_id, record_time, COUNT(*) AS cnt
FROM dbo.parameter_records
GROUP BY report_id, record_time
HAVING COUNT(*) > 1;
GO

-- 6) Create unique index to enforce constraint going forward
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.objects o ON i.object_id = o.object_id
    WHERE o.name = 'parameter_records' AND i.name = 'UX_parameter_records_report_time'
)
BEGIN
    CREATE UNIQUE INDEX UX_parameter_records_report_time
    ON dbo.parameter_records(report_id, record_time);
END
GO

PRINT 'Duplicate cleanup complete and unique index ensured.';