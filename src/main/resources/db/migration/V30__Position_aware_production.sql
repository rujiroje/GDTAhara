-- V30: OP-WS-01 — Position-Aware Multi-OP Production Workstream
-- Covers: device registration (tablet+printer), sessions (tablet+handheld),
--         atomic box sequence, position columns on existing tables, archive tables

-- ============================================================
-- 1. Admin Device Registration — Tablet
-- ============================================================
CREATE TABLE tablet_registration (
    tablet_id       NVARCHAR(100)   PRIMARY KEY,
    mac_address     NVARCHAR(17)    NOT NULL,
    machine_id      NVARCHAR(20)    NOT NULL,
    position        NVARCHAR(10)    NOT NULL,   -- LEFT | RIGHT
    device_name     NVARCHAR(100)   NULL,
    model           NVARCHAR(100)   NULL,
    registered_by   NVARCHAR(50)    NOT NULL,
    registered_at   DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    is_active       BIT             NOT NULL DEFAULT 1,

    CONSTRAINT UK_tab_mac          UNIQUE (mac_address),
    CONSTRAINT UK_tab_machine_pos  UNIQUE (machine_id, position),
    CONSTRAINT CK_tab_pos          CHECK (position IN ('LEFT','RIGHT'))
);

-- ============================================================
-- 2. Admin Device Registration — Printer
-- ============================================================
CREATE TABLE printer_registration (
    printer_id      NVARCHAR(100)   PRIMARY KEY,
    mac_address     NVARCHAR(17)    NOT NULL,
    machine_id      NVARCHAR(20)    NOT NULL,
    position        NVARCHAR(10)    NOT NULL,
    printer_type    NVARCHAR(20)    NOT NULL,   -- THERMAL | LASER
    network_path    NVARCHAR(200)   NOT NULL,   -- tcp://host:9100
    hostname        NVARCHAR(100)   NULL,
    friendly_name   NVARCHAR(100)   NULL,
    model           NVARCHAR(100)   NULL,
    registered_by   NVARCHAR(50)    NOT NULL,
    registered_at   DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    is_active       BIT             NOT NULL DEFAULT 1,

    CONSTRAINT UK_prt_mac              UNIQUE (mac_address),
    CONSTRAINT UK_prt_machine_pos_type UNIQUE (machine_id, position, printer_type),
    CONSTRAINT CK_prt_pos              CHECK (position     IN ('LEFT','RIGHT')),
    CONSTRAINT CK_prt_type             CHECK (printer_type IN ('THERMAL','LASER'))
);

-- ============================================================
-- 3. Tablet Session (NG checker · position inherited from tablet)
-- ============================================================
CREATE TABLE production_session (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         NVARCHAR(50)    NOT NULL,
    tablet_id       NVARCHAR(100)   NOT NULL,
    machine_id      NVARCHAR(20)    NOT NULL,
    position        NVARCHAR(10)    NOT NULL,
    role            NVARCHAR(20)    NOT NULL DEFAULT N'NG_CHECKER',
    shift_code      NVARCHAR(10)    NULL,
    login_at        DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    logout_at       DATETIME2       NULL,

    CONSTRAINT FK_ps_tablet FOREIGN KEY (tablet_id) REFERENCES tablet_registration(tablet_id),
    CONSTRAINT CK_ps_pos    CHECK (position IN ('LEFT','RIGHT'))
);

CREATE INDEX IX_ps_active ON production_session(machine_id, position, logout_at);
CREATE INDEX IX_ps_user   ON production_session(user_id, login_at);

-- ============================================================
-- 4. Handheld Session (Packer · position selected at login)
-- ============================================================
CREATE TABLE handheld_session (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id             NVARCHAR(50)    NOT NULL,
    handheld_device_id  NVARCHAR(100)   NULL,
    machine_id          NVARCHAR(20)    NOT NULL,
    position            NVARCHAR(10)    NOT NULL,
    role                NVARCHAR(20)    NOT NULL DEFAULT N'PACKER',
    shift_code          NVARCHAR(10)    NULL,
    login_at            DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    logout_at           DATETIME2       NULL,
    last_activity_at    DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    auto_released       BIT             NOT NULL DEFAULT 0,

    CONSTRAINT CK_hh_pos CHECK (position IN ('LEFT','RIGHT'))
);

CREATE INDEX IX_hh_active   ON handheld_session(machine_id, position, logout_at);
CREATE INDEX IX_hh_activity ON handheld_session(last_activity_at);

-- ============================================================
-- 5. Atomic Box Number Sequence (per machine + lot)
-- ============================================================
CREATE TABLE machine_lot_box_sequence (
    machine_id      NVARCHAR(20)    NOT NULL,
    lot_id          NVARCHAR(50)    NOT NULL,
    current_number  INT             NOT NULL DEFAULT 0,
    last_updated_at DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (machine_id, lot_id)
);

-- ============================================================
-- 6. Add position + traceability columns to ng_logs (existing)
-- ============================================================
IF COL_LENGTH('ng_logs', 'position') IS NULL
BEGIN
    ALTER TABLE ng_logs
        ADD position              NVARCHAR(10)    NULL,
            ng_checker_id         NVARCHAR(50)    NULL,
            production_session_id BIGINT          NULL;
END;

-- ============================================================
-- 7. Add position + session columns to packaging_logs (existing)
-- ============================================================
IF COL_LENGTH('packaging_logs', 'position') IS NULL
BEGIN
    ALTER TABLE packaging_logs
        ADD position          NVARCHAR(10)    NULL,
            handheld_session_id BIGINT        NULL,
            packer_id           NVARCHAR(50)  NULL,
            ng_checker_id       NVARCHAR(50)  NULL,
            printer_id          NVARCHAR(100) NULL,
            print_status        NVARCHAR(20)  NULL DEFAULT N'PENDING',
            print_completed_at  DATETIME2     NULL,
            reprint_count       INT           NOT NULL DEFAULT 0,
            last_reprinted_at   DATETIME2     NULL;
END;

-- ============================================================
-- 8. Archive tables (mirror structure for data lifecycle)
-- ============================================================
SELECT * INTO packaging_logs_archive    FROM packaging_logs    WHERE 1=0;
SELECT * INTO ng_logs_archive           FROM ng_logs           WHERE 1=0;
SELECT * INTO production_session_archive FROM production_session WHERE 1=0;
SELECT * INTO handheld_session_archive  FROM handheld_session  WHERE 1=0;
SELECT * INTO setup_time_log_archive    FROM setup_time_log    WHERE 1=0;
SELECT * INTO downtime_events_archive   FROM downtime_events   WHERE 1=0;

-- ============================================================
-- 9. Archive job log
-- ============================================================
CREATE TABLE archive_job_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    job_run_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    archive_older_than  DATE            NOT NULL,
    delete_older_than   DATE            NOT NULL,
    status              NVARCHAR(20)    NOT NULL,
    tables_processed    INT             NOT NULL DEFAULT 0,
    rows_archived       INT             NOT NULL DEFAULT 0,
    rows_deleted        INT             NOT NULL DEFAULT 0,
    glacier_export_paths NVARCHAR(MAX)  NULL,   -- JSON
    error_message       NVARCHAR(MAX)   NULL,
    duration_seconds    INT             NULL,

    CONSTRAINT CK_ajl_status CHECK (status IN ('RUNNING','SUCCESS','FAILED','PARTIAL'))
);
