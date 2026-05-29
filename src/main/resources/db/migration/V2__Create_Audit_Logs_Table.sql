-- AuditLog table: records every state-changing operation with who/what/when/before/after
IF OBJECT_ID('dbo.audit_logs', 'U') IS NULL
BEGIN
    CREATE TABLE audit_logs (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        request_id  NVARCHAR(36),
        username    NVARCHAR(100) NOT NULL,
        action      NVARCHAR(30)  NOT NULL,
        entity_type NVARCHAR(100) NOT NULL,
        entity_id   BIGINT,
        endpoint    NVARCHAR(500),
        before_json NVARCHAR(MAX),
        after_json  NVARCHAR(MAX),
        created_at  DATETIME2     NOT NULL DEFAULT GETDATE(),
        ip_address  NVARCHAR(50)
    );

    CREATE INDEX idx_audit_username  ON audit_logs (username);
    CREATE INDEX idx_audit_entity    ON audit_logs (entity_type, entity_id);
    CREATE INDEX idx_audit_created_at ON audit_logs (created_at);
END
