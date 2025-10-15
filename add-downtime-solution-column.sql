-- Adds 'solution' column to downtime_events if it does not exist (SQL Server)
IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    INNER JOIN sys.tables t ON c.object_id = t.object_id
    WHERE t.name = 'downtime_events' AND c.name = 'solution'
)
BEGIN
    ALTER TABLE dbo.downtime_events
    ADD solution NVARCHAR(MAX) NULL;
END;
