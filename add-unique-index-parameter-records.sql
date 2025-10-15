-- Ensure (report_id, record_time) has at most one row to avoid duplicate Standard/10:00 records
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.objects o ON i.object_id = o.object_id
    WHERE o.name = 'parameter_records' AND i.name = 'UX_parameter_records_report_time'
)
BEGIN
    CREATE UNIQUE INDEX UX_parameter_records_report_time
    ON parameter_records(report_id, record_time);
END
GO

PRINT 'Ensured unique index UX_parameter_records_report_time on (report_id, record_time)';