-- V31: NG-WS-01 — NG Recording UX + Threshold Alert + Correction Workflow
-- Tables: ng_type_master, ng_quick_button_config, ng_threshold_config,
--         ng_alert, ng_alert_event, ng_correction_log, telegram_room_config

-- ============================================================
-- 1. NG Type Master (seed 20 types)
-- ============================================================
IF OBJECT_ID('ng_type_master', 'U') IS NULL
BEGIN
    CREATE TABLE ng_type_master (
        code        NVARCHAR(30)    PRIMARY KEY,
        name_th     NVARCHAR(100)   NOT NULL,
        name_en     NVARCHAR(100)   NOT NULL,
        icon        NVARCHAR(20)    NULL,
        category    NVARCHAR(50)    NULL,
        sort_order  INT             NOT NULL DEFAULT 0,
        is_active   BIT             NOT NULL DEFAULT 1
    );
END;

-- Seed common NG types (skip if already seeded)
IF NOT EXISTS (SELECT 1 FROM ng_type_master WHERE code = N'SHORT_SHOT')
INSERT INTO ng_type_master (code, name_th, name_en, icon, category, sort_order) VALUES
(N'SHORT_SHOT',    N'Short shot',          N'Short shot',            N'🩹', N'MOLDING',       1),
(N'BLACK_SPOT',    N'Black spot',          N'Black spot',            N'⚫', N'CONTAMINATION', 2),
(N'CUT_MARK',      N'Cut mark',            N'Cut mark',              N'✂️', N'TRIMMING',      3),
(N'BUBBLE',        N'Bubble',              N'Bubble',                N'💧', N'MOLDING',       4),
(N'FLOW_MARK',     N'Flow mark',           N'Flow mark',             N'🌊', N'MOLDING',       5),
(N'CONTAMINATION', N'Contamination',       N'Contamination',         N'⚠️', N'CONTAMINATION', 6),
(N'WELD_LINE',     N'Weld line',           N'Weld line',             N'🔵', N'MOLDING',       7),
(N'DIM_OFF_SPEC',  N'Dim off spec',        N'Dimension off spec',    N'📏', N'DIMENSION',     8),
(N'BURN_MARK',     N'Burn mark',           N'Burn mark',             N'⚡', N'MOLDING',       9),
(N'WARP',          N'Warp',                N'Warp',                  N'🌐', N'DIMENSION',     10),
(N'COLOR_MISS',    N'Color miss',          N'Color mismatch',        N'🎨', N'COLOR',         11),
(N'SINK_MARK',     N'Sink mark',           N'Sink mark',             N'🔶', N'MOLDING',       12),
(N'SILVER_STREAK', N'Silver streak',       N'Silver streak',         N'💫', N'MATERIAL',      13),
(N'SHAPE',         N'Shape defect',        N'Shape defect',          N'📐', N'DIMENSION',     14),
(N'LABEL',         N'Label issue',         N'Label issue',           N'🏷️',N'FINISH',         15),
(N'CHEM_SPOT',     N'Chem spot',           N'Chemical spot',         N'🧪', N'CONTAMINATION', 16),
(N'HOLE',          N'Hole defect',         N'Hole defect',           N'🕳️',N'MOLDING',        17),
(N'ATTACH_DEF',    N'Attach defect',       N'Attach defect',         N'📎', N'ASSEMBLY',      18),
(N'TEMP_ISSUE',    N'Temperature issue',   N'Temperature issue',     N'🌡️',N'PROCESS',        19),
(N'OTHER',         N'อื่นๆ',              N'Other',                 N'❓', N'MISC',           99);

-- ============================================================
-- 2. Quick Button Configuration (6 fixed slots per machine)
-- ============================================================
CREATE TABLE ng_quick_button_config (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id      NVARCHAR(20)    NOT NULL,
    slot_number     INT             NOT NULL,       -- 1-6
    ng_type_code    NVARCHAR(30)    NOT NULL,
    updated_by      NVARCHAR(50)    NOT NULL,
    updated_at      DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT UK_ngqb          UNIQUE (machine_id, slot_number),
    CONSTRAINT CK_ngqb_slot     CHECK (slot_number BETWEEN 1 AND 6),
    CONSTRAINT FK_ngqb_type     FOREIGN KEY (ng_type_code) REFERENCES ng_type_master(code)
);

-- ============================================================
-- 3. Add correction / soft-delete columns to ng_logs
-- ============================================================
IF COL_LENGTH('ng_logs', 'is_deleted') IS NULL
BEGIN
    ALTER TABLE ng_logs
        ADD is_deleted  BIT         NOT NULL DEFAULT 0,
            deleted_at  DATETIME2   NULL,
            deleted_by  NVARCHAR(50) NULL;
END;

-- ============================================================
-- 4. NG Correction Log (undo within 60 s)
-- ============================================================
CREATE TABLE ng_correction_log (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    ng_log_id               BIGINT          NOT NULL,
    action                  NVARCHAR(20)    NOT NULL DEFAULT N'UNDO',
    action_by               NVARCHAR(50)    NOT NULL,
    action_at               DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    original_qty            INT             NOT NULL,
    original_type           NVARCHAR(30)    NOT NULL,
    reason                  NVARCHAR(500)   NULL,
    time_since_original_ms  INT             NOT NULL,

    CONSTRAINT CK_ncl_action CHECK (action IN ('UNDO'))
);

CREATE INDEX IX_ncl_time ON ng_correction_log(action_at);

-- ============================================================
-- 5. NG Threshold Configuration (per machine · hourly count)
-- ============================================================
CREATE TABLE ng_threshold_config (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id      NVARCHAR(20)    NOT NULL,
    ng_type_code    NVARCHAR(30)    NOT NULL,
    threshold_count INT             NOT NULL,
    severity        NVARCHAR(10)    NOT NULL,   -- LOW | MED | HIGH | URGENT
    notify_leader   BIT             NOT NULL DEFAULT 0,
    notify_tech     BIT             NOT NULL DEFAULT 0,
    notify_qa       BIT             NOT NULL DEFAULT 0,
    is_active       BIT             NOT NULL DEFAULT 1,
    created_by      NVARCHAR(50)    NOT NULL,
    created_at      DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    updated_by      NVARCHAR(50)    NULL,
    updated_at      DATETIME2       NULL,

    CONSTRAINT UK_ngtc          UNIQUE (machine_id, ng_type_code),
    CONSTRAINT FK_ngtc_type     FOREIGN KEY (ng_type_code) REFERENCES ng_type_master(code),
    CONSTRAINT CK_ngtc_severity CHECK (severity IN ('LOW','MED','HIGH','URGENT'))
);

-- ============================================================
-- 6. NG Alert (breach event record)
-- ============================================================
CREATE TABLE ng_alert (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id              NVARCHAR(20)    NOT NULL,
    position                NVARCHAR(10)    NULL,
    ng_type_code            NVARCHAR(30)    NOT NULL,
    hour_window_start       DATETIME2       NOT NULL,
    breach_count            INT             NOT NULL,
    threshold_count         INT             NOT NULL,
    severity                NVARCHAR(10)    NOT NULL,
    notify_leader           BIT             NOT NULL DEFAULT 0,
    notify_tech             BIT             NOT NULL DEFAULT 0,
    notify_qa               BIT             NOT NULL DEFAULT 0,

    status                  NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',
    accepted_by             NVARCHAR(50)    NULL,
    accepted_at             DATETIME2       NULL,
    arrived_at              DATETIME2       NULL,
    resolved_at             DATETIME2       NULL,

    arrival_otp             NVARCHAR(6)     NULL,
    arrival_otp_expiry      DATETIME2       NULL,
    arrival_verified        BIT             NOT NULL DEFAULT 0,

    telegram_escalated      BIT             NOT NULL DEFAULT 0,
    telegram_escalated_at   DATETIME2       NULL,
    telegram_room           NVARCHAR(50)    NULL,

    triggered_at            DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT CK_nga_status CHECK (status IN ('PENDING','ACCEPTED','ARRIVED','RESOLVED','SNOOZED'))
);

CREATE INDEX IX_nga_status  ON ng_alert(status);
CREATE INDEX IX_nga_machine ON ng_alert(machine_id, triggered_at);
CREATE INDEX IX_nga_pending ON ng_alert(status, triggered_at);

-- ============================================================
-- 7. NG Alert Event Log
-- ============================================================
CREATE TABLE ng_alert_event (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    alert_id    BIGINT          NOT NULL,
    event_type  NVARCHAR(30)    NOT NULL,
    actor_id    NVARCHAR(50)    NULL,
    actor_role  NVARCHAR(20)    NULL,
    note        NVARCHAR(500)   NULL,
    event_at    DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_ngae_alert FOREIGN KEY (alert_id) REFERENCES ng_alert(id)
);

CREATE INDEX IX_ngae_alert ON ng_alert_event(alert_id, event_at);

-- ============================================================
-- 8. Telegram Room Configuration
-- ============================================================
CREATE TABLE telegram_room_config (
    room_code       NVARCHAR(30)    PRIMARY KEY,
    room_name       NVARCHAR(100)   NOT NULL,
    chat_id         NVARCHAR(50)    NULL,
    bot_token_env   NVARCHAR(100)   NULL,   -- env var name, not the token itself
    description     NVARCHAR(200)   NULL,
    is_active       BIT             NOT NULL DEFAULT 1
);

INSERT INTO telegram_room_config (room_code, room_name, description) VALUES
(N'GENERAL', N'GDTAHARA General', N'ห้องรวม · everyone'),
(N'LEADER',  N'GDTAHARA Leaders', N'Leader team'),
(N'TECH',    N'GDTAHARA Tech',    N'Tech team'),
(N'QA',      N'GDTAHARA QA',      N'QA team'),
(N'MANAGER', N'GDTAHARA Manager', N'PD/EXEC · for escalation'),
(N'CM',      N'GDTAHARA CM Team', N'CM team · material tracking'),
(N'DOC',     N'GDTAHARA Doc Team',N'Doc team · approve requests');
