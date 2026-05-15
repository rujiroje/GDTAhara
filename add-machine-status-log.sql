-- Migration: Create machine_status_logs table
-- Run on SQL Server: GDTahara database
-- New table, zero impact on existing data

CREATE TABLE machine_status_logs (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id    BIGINT NOT NULL,
    status        NVARCHAR(30) NOT NULL,   -- RUNNING | IDLE | SETUP | PLANNED_STOP | UNPLANNED_STOP
    start_time    DATETIME2 NOT NULL,
    end_time      DATETIME2 NULL,          -- NULL = currently active
    reason        NVARCHAR(500) NULL,
    source        NVARCHAR(20) NOT NULL DEFAULT 'MANUAL',  -- MANUAL | IOT (ready for future)
    recorded_by   BIGINT NULL,             -- FK to users
    created_at    DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_msl_machine  FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_msl_user     FOREIGN KEY (recorded_by) REFERENCES users(id),
    CONSTRAINT chk_msl_status  CHECK (status IN ('RUNNING','IDLE','SETUP','PLANNED_STOP','UNPLANNED_STOP')),
    CONSTRAINT chk_msl_source  CHECK (source IN ('MANUAL','IOT'))
);

CREATE INDEX idx_msl_machine_time ON machine_status_logs (machine_id, start_time DESC);
CREATE INDEX idx_msl_active       ON machine_status_logs (machine_id, end_time) WHERE end_time IS NULL;
