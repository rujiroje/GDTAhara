-- Migration: Create pm_schedules table
-- New table, zero impact on existing data

CREATE TABLE pm_schedules (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id      BIGINT NOT NULL,
    task_name       NVARCHAR(200) NOT NULL,
    description     NVARCHAR(MAX) NULL,
    interval_days   INT NOT NULL,          -- ทำซ้ำทุกกี่วัน
    last_done_date  DATE NULL,
    next_due_date   DATE NOT NULL,
    status          NVARCHAR(20) NOT NULL DEFAULT 'UPCOMING', -- UPCOMING | DUE | OVERDUE | DONE
    assigned_to     BIGINT NULL,           -- FK to users (technician)
    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2 NULL,

    CONSTRAINT fk_pm_machine     FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_pm_technician  FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT chk_pm_status     CHECK (status IN ('UPCOMING','DUE','OVERDUE','DONE'))
);

CREATE INDEX idx_pm_machine    ON pm_schedules (machine_id);
CREATE INDEX idx_pm_due        ON pm_schedules (next_due_date, status);
CREATE INDEX idx_pm_active     ON pm_schedules (is_active, status);
