-- V33: O-12 — Packaging Material Alt-Group Auto-Deduction
-- Tables: packaging_alternate_group, packaging_alternate_item,
--         handheld_session_packaging_choice, packaging_choice_change_log,
--         packaging_deduction_log
-- Alter:  materials (add alt_group_code, variant_type)
-- Note:   packaging_logs = box_label in spec

-- ============================================================
-- 1. Alternate Group Master
-- ============================================================
CREATE TABLE packaging_alternate_group (
    group_code      NVARCHAR(50)    PRIMARY KEY,
    group_name_th   NVARCHAR(200)   NULL,
    group_name_en   NVARCHAR(200)   NULL,
    base_item_code  NVARCHAR(50)    NULL,   -- canonical item code (e.g. SSPP110A)
    auto_detected   BIT             NOT NULL DEFAULT 1,
    admin_override  BIT             NOT NULL DEFAULT 0,
    created_at      DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2       NULL,
    is_active       BIT             NOT NULL DEFAULT 1
);

-- ============================================================
-- 2. Group Members
-- ============================================================
CREATE TABLE packaging_alternate_item (
    item_code           NVARCHAR(50)    NOT NULL,
    group_code          NVARCHAR(50)    NOT NULL,
    variant_type        NVARCHAR(20)    NOT NULL DEFAULT N'STANDARD',
    category            NVARCHAR(20)    NOT NULL,
    display_priority    INT             NOT NULL DEFAULT 0,
    auto_assigned       BIT             NOT NULL DEFAULT 1,

    PRIMARY KEY (item_code, group_code),
    CONSTRAINT FK_pai_group     FOREIGN KEY (group_code)  REFERENCES packaging_alternate_group(group_code),
    CONSTRAINT CK_pai_variant   CHECK (variant_type IN ('NEW','REUSED','STANDARD')),
    CONSTRAINT CK_pai_category  CHECK (category     IN ('HIBE','UNBW','VERP'))
);

CREATE INDEX IX_pai_group ON packaging_alternate_item(group_code);

-- ============================================================
-- 3. Session Packaging Choice (per handheld session)
-- ============================================================
CREATE TABLE handheld_session_packaging_choice (
    session_id          BIGINT          NOT NULL,
    group_code          NVARCHAR(50)    NOT NULL,
    selected_item_code  NVARCHAR(50)    NOT NULL,
    auto_selected       BIT             NOT NULL DEFAULT 0,
    selected_at         DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    PRIMARY KEY (session_id, group_code),
    CONSTRAINT FK_hspc_session  FOREIGN KEY (session_id)  REFERENCES handheld_session(id),
    CONSTRAINT FK_hspc_group    FOREIGN KEY (group_code)  REFERENCES packaging_alternate_group(group_code)
);

-- ============================================================
-- 4. Mid-Session Change Log
-- ============================================================
CREATE TABLE packaging_choice_change_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id      BIGINT          NOT NULL,
    group_code      NVARCHAR(50)    NOT NULL,
    from_item_code  NVARCHAR(50)    NOT NULL,
    to_item_code    NVARCHAR(50)    NOT NULL,
    changed_by      NVARCHAR(50)    NOT NULL,
    changed_at      DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    reason          NVARCHAR(500)   NULL
);

CREATE INDEX IX_pccl_session ON packaging_choice_change_log(session_id, changed_at);

-- ============================================================
-- 5. Deduction Log (per box · full audit)
-- ============================================================
CREATE TABLE packaging_deduction_log (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    packaging_log_id        BIGINT          NOT NULL,   -- FK to packaging_logs (box_label in spec)
    bom_item_code           NVARCHAR(50)    NOT NULL,
    is_in_alt_group         BIT             NOT NULL DEFAULT 0,
    alt_group_code          NVARCHAR(50)    NULL,
    was_chosen              BIT             NOT NULL DEFAULT 0,
    actual_deducted_code    NVARCHAR(50)    NULL,       -- NULL if skipped
    deducted_qty            DECIMAL(12,4)   NULL,
    material_category       NVARCHAR(20)    NULL,
    session_id              BIGINT          NULL,
    deducted_at             DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_pdl_box   FOREIGN KEY (packaging_log_id) REFERENCES packaging_logs(id)
);

CREATE INDEX IX_pdl_box  ON packaging_deduction_log(packaging_log_id);
CREATE INDEX IX_pdl_time ON packaging_deduction_log(deducted_at);
CREATE INDEX IX_pdl_item ON packaging_deduction_log(bom_item_code, deducted_at);

-- ============================================================
-- 6. Add alt group fields to materials
-- ============================================================
IF COL_LENGTH('materials', 'alt_group_code') IS NULL
BEGIN
    ALTER TABLE materials
        ADD alt_group_code  NVARCHAR(50)    NULL,
            variant_type    NVARCHAR(20)    NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'IX_mat_alt_group' AND object_id = OBJECT_ID('materials')
)
    CREATE INDEX IX_mat_alt_group ON materials(alt_group_code);
