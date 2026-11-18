-- V38: Mold Master + Machine Current Mold tracking for Cavity No. on packaging label
-- Enables: Admin manages mold list → Tech records mold change in setup job
--          → machine_current_mold tracks active mold per machine
--          → packaging_logs.mold_id captures which mold was in use when box was packed
--          → label shows Cavity No. from mold_master.cavity_numbers

-- ============================================================
-- 1. Mold Master  (Admin-managed list of moulds)
-- ============================================================
CREATE TABLE mold_master (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    mold_code               NVARCHAR(50)    NOT NULL,   -- e.g. "MD-RBL103-01"
    mold_name               NVARCHAR(200)   NOT NULL,   -- e.g. "4-Cavity 500ml Bottle"
    mold_name_en            NVARCHAR(200)   NULL,
    cavity_count            INT             NOT NULL DEFAULT 1,
    cavity_numbers          NVARCHAR(200)   NOT NULL,   -- comma-sep, e.g. "1,2,3,4"
    machine_type            NVARCHAR(50)    NULL,       -- compatible machine type (TAHARA, etc.)
    compatible_products     NVARCHAR(MAX)   NULL,       -- JSON array of product codes
    description             NVARCHAR(500)   NULL,
    is_active               BIT             NOT NULL DEFAULT 1,
    created_by              NVARCHAR(50)    NOT NULL,
    created_at              DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    updated_by              NVARCHAR(50)    NULL,
    updated_at              DATETIME2       NULL,

    CONSTRAINT UK_mm_code UNIQUE (mold_code)
);

-- ============================================================
-- 2. Machine Current Mold  (one row per machine · upserted on mold change)
--    Tech → setup job (mold_changed=1) → updates this table
-- ============================================================
CREATE TABLE machine_current_mold (
    machine_id          BIGINT          PRIMARY KEY,    -- FK to machines.id
    mold_id             BIGINT          NOT NULL,
    setup_job_id        BIGINT          NULL,           -- the setup_job that installed this mold
    installed_by        NVARCHAR(50)    NOT NULL,
    installed_at        DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_mcm_mold FOREIGN KEY (mold_id)      REFERENCES mold_master(id),
    CONSTRAINT FK_mcm_job  FOREIGN KEY (setup_job_id) REFERENCES machine_setup_job(id)
);

-- ============================================================
-- 3. Mold Change History  (full audit log of every mold swap)
-- ============================================================
CREATE TABLE mold_change_history (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id      BIGINT          NOT NULL,
    from_mold_id    BIGINT          NULL,       -- NULL if first ever install
    to_mold_id      BIGINT          NOT NULL,
    changed_by      NVARCHAR(50)    NOT NULL,
    changed_at      DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    setup_job_id    BIGINT          NULL,
    note            NVARCHAR(500)   NULL,

    CONSTRAINT FK_mch_from FOREIGN KEY (from_mold_id) REFERENCES mold_master(id),
    CONSTRAINT FK_mch_to   FOREIGN KEY (to_mold_id)   REFERENCES mold_master(id),
    CONSTRAINT FK_mch_job  FOREIGN KEY (setup_job_id) REFERENCES machine_setup_job(id)
);

CREATE INDEX IX_mch_machine ON mold_change_history(machine_id, changed_at);

-- ============================================================
-- 4. Add mold_id to machine_setup_job
--    (formal FK alongside existing free-text mold_code_to for backward compat)
-- ============================================================
IF COL_LENGTH('machine_setup_job', 'mold_id') IS NULL
BEGIN
    ALTER TABLE machine_setup_job
        ADD mold_id BIGINT NULL
            CONSTRAINT FK_msj_mold FOREIGN KEY REFERENCES mold_master(id);
END;

-- ============================================================
-- 5. Add mold_id to packaging_logs
--    Captures which mold (and therefore which cavity_numbers) was in use
--    at the moment each box was packed — immutable traceability.
-- ============================================================
IF COL_LENGTH('packaging_logs', 'mold_id') IS NULL
BEGIN
    ALTER TABLE packaging_logs
        ADD mold_id BIGINT NULL
            CONSTRAINT FK_pl_mold FOREIGN KEY REFERENCES mold_master(id);
END;
