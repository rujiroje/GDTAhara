-- V34: C-01 — CM Material Meter Tracking + Refill + Shift Handoff
-- Tables: machine_tank, tank_meter_reading, tank_refill,
--         shift_material_consumption, shift_boundary_config, shift_meter_reminder
-- Note:   material_stock_in in spec = material_stock_transactions in this project
-- Note:   CM room + DOC room already seeded in V31; only INSERT IF NOT EXISTS

-- ============================================================
-- 1. Machine Tank Master (fixed per machine · admin config)
-- ============================================================
CREATE TABLE machine_tank (
    tank_id                 NVARCHAR(50)    PRIMARY KEY,    -- e.g. 'RBL103_A'
    machine_id              NVARCHAR(20)    NOT NULL,
    tank_code               NVARCHAR(20)    NOT NULL,       -- 'A' | 'B' | 'C'
    tank_name               NVARCHAR(100)   NULL,
    default_material_code   NVARCHAR(50)    NOT NULL,       -- expected pellet type
    capacity_kg             DECIMAL(10,2)   NULL,
    physical_position       NVARCHAR(50)    NULL,
    is_active               BIT             NOT NULL DEFAULT 1,

    CONSTRAINT UK_mt_machine_code   UNIQUE (machine_id, tank_code)
);

CREATE INDEX IX_mt_machine ON machine_tank(machine_id);

-- ============================================================
-- 2. Tank Meter Readings (start / end / adhoc per shift)
-- ============================================================
CREATE TABLE tank_meter_reading (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    tank_id             NVARCHAR(50)    NOT NULL,
    machine_id          NVARCHAR(20)    NOT NULL,   -- denorm for query
    reading_type        NVARCHAR(20)    NOT NULL,   -- SHIFT_START | SHIFT_END | ADHOC
    meter_value_kg      DECIMAL(10,2)   NOT NULL,
    shift_code          NVARCHAR(10)    NOT NULL,
    shift_date          DATE            NOT NULL,
    recorded_by         NVARCHAR(50)    NOT NULL,
    recorded_at         DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    note                NVARCHAR(500)   NULL,
    auto_generated      BIT             NOT NULL DEFAULT 0,
    source_reading_id   BIGINT          NULL,       -- handoff: copied from SHIFT_END

    CONSTRAINT FK_tmr_tank      FOREIGN KEY (tank_id) REFERENCES machine_tank(tank_id),
    CONSTRAINT FK_tmr_source    FOREIGN KEY (source_reading_id) REFERENCES tank_meter_reading(id),
    CONSTRAINT CK_tmr_type      CHECK (reading_type IN ('SHIFT_START','SHIFT_END','ADHOC'))
);

CREATE INDEX IX_tmr_shift   ON tank_meter_reading(tank_id, shift_date, shift_code, reading_type);
CREATE INDEX IX_tmr_machine ON tank_meter_reading(machine_id, shift_date, shift_code);

-- ============================================================
-- 3. Refill Events (with lot traceability · negative = correction)
-- ============================================================
CREATE TABLE tank_refill (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    tank_id                 NVARCHAR(50)    NOT NULL,
    machine_id              NVARCHAR(20)    NOT NULL,   -- denorm
    material_code           NVARCHAR(50)    NOT NULL,   -- denorm
    material_stock_id       BIGINT          NULL,       -- FK to material_stock_transactions.id (nullable for corrections)
    material_lot_code       NVARCHAR(50)    NOT NULL,   -- denorm
    qty_kg                  DECIMAL(10,2)   NOT NULL,   -- can be NEGATIVE (correction)
    is_correction           BIT             NOT NULL DEFAULT 0,
    corrects_refill_id      BIGINT          NULL,       -- FK to original refill if correction
    refilled_by             NVARCHAR(50)    NOT NULL,
    refilled_at             DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    shift_code              NVARCHAR(10)    NOT NULL,
    shift_date              DATE            NOT NULL,
    material_type_override  BIT             NOT NULL DEFAULT 0,
    note                    NVARCHAR(500)   NULL,       -- REQUIRED when qty_kg < 0
    photo_ids               NVARCHAR(MAX)   NULL,       -- JSON array (reuse attachment_photo)

    CONSTRAINT FK_tr_tank       FOREIGN KEY (tank_id) REFERENCES machine_tank(tank_id),
    CONSTRAINT FK_tr_corrects   FOREIGN KEY (corrects_refill_id) REFERENCES tank_refill(id)
);

CREATE INDEX IX_tr_tank  ON tank_refill(tank_id, refilled_at);
CREATE INDEX IX_tr_shift ON tank_refill(machine_id, shift_date, shift_code);
CREATE INDEX IX_tr_stock ON tank_refill(material_stock_id);

-- ============================================================
-- 4. Shift Consumption Summary (precomputed on SHIFT_END)
-- ============================================================
CREATE TABLE shift_material_consumption (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    tank_id             NVARCHAR(50)    NOT NULL,
    machine_id          NVARCHAR(20)    NOT NULL,
    material_code       NVARCHAR(50)    NOT NULL,
    shift_code          NVARCHAR(10)    NOT NULL,
    shift_date          DATE            NOT NULL,
    starting_meter_kg   DECIMAL(10,2)   NOT NULL,
    total_refills_kg    DECIMAL(10,2)   NOT NULL,   -- SUM including negatives
    ending_meter_kg     DECIMAL(10,2)   NOT NULL,
    computed_used_kg    DECIMAL(10,2)   NOT NULL,   -- start + refills - end
    refill_count        INT             NOT NULL DEFAULT 0,
    correction_count    INT             NOT NULL DEFAULT 0,
    computed_at         DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    computed_by         NVARCHAR(50)    NULL,
    is_final            BIT             NOT NULL DEFAULT 0,

    CONSTRAINT UK_smc   UNIQUE (tank_id, shift_date, shift_code)
);

CREATE INDEX IX_smc_machine ON shift_material_consumption(machine_id, shift_date, shift_code);

-- ============================================================
-- 5. Shift Boundary Configuration
-- ============================================================
CREATE TABLE shift_boundary_config (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    shift_code              NVARCHAR(10)    NOT NULL UNIQUE,
    start_time              TIME            NOT NULL,
    end_time                TIME            NOT NULL,
    reminder_grace_minutes  INT             NOT NULL DEFAULT 30,
    is_active               BIT             NOT NULL DEFAULT 1
);

INSERT INTO shift_boundary_config (shift_code, start_time, end_time, reminder_grace_minutes) VALUES
(N'A', '07:00', '15:00', 30),
(N'B', '15:00', '23:00', 30),
(N'C', '23:00', '07:00', 30);

-- ============================================================
-- 6. Missed Meter Reminder (alert + audit)
-- ============================================================
CREATE TABLE shift_meter_reminder (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id              NVARCHAR(20)    NOT NULL,
    tank_id                 NVARCHAR(50)    NOT NULL,
    shift_code              NVARCHAR(10)    NOT NULL,
    shift_date              DATE            NOT NULL,
    expected_by             DATETIME2       NOT NULL,   -- shift_end + grace
    first_alerted_at        DATETIME2       NULL,
    last_alerted_at         DATETIME2       NULL,
    alert_count             INT             NOT NULL DEFAULT 0,
    telegram_sent           BIT             NOT NULL DEFAULT 0,
    escalated_to_manager    BIT             NOT NULL DEFAULT 0,
    resolved_at             DATETIME2       NULL,
    status                  NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',

    CONSTRAINT CK_smr_status CHECK (status IN ('PENDING','RESOLVED','ESCALATED'))
);

CREATE INDEX IX_smr_pending ON shift_meter_reminder(status, expected_by);

-- ============================================================
-- 7. Add remaining_qty to material_stock_transactions
--    (needed so tank_refill can deduct available stock per lot)
-- ============================================================
IF COL_LENGTH('material_stock_transactions', 'remaining_qty') IS NULL
BEGIN
    ALTER TABLE material_stock_transactions
        ADD remaining_qty DECIMAL(10,2) NULL;
END;
