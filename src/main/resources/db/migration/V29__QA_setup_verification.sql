-- V29: Q-01 — QA Setup Verification Workflow (Tech → QA → Tech)
-- Tables: qa_verification, qa_reject_reason_master
-- Seed: activity_code 'Q' (already exists in setup_activity_code — skip duplicate)
-- Also adds new activity code for QA verification category distinction

-- ============================================================
-- 1. QA Reject Reason Master (admin-managed dropdown)
-- ============================================================
CREATE TABLE qa_reject_reason_master (
    code        NVARCHAR(50)    PRIMARY KEY,
    name_th     NVARCHAR(200)   NOT NULL,
    name_en     NVARCHAR(200)   NOT NULL,
    category    NVARCHAR(50)    NOT NULL DEFAULT N'SETUP_QUALITY',
    sort_order  INT             NOT NULL DEFAULT 0,
    is_active   BIT             NOT NULL DEFAULT 1
);

INSERT INTO qa_reject_reason_master (code, name_th, name_en, category, sort_order) VALUES
(N'MOLD_ALIGN', N'Mold alignment ผิด',       N'Mold alignment incorrect',     N'SETUP_QUALITY', 1),
(N'DIMENSION',  N'Dimension out of spec',    N'Dimension out of specification',N'SETUP_QUALITY', 2),
(N'SURFACE',    N'Surface defect',           N'Surface defect',               N'SETUP_QUALITY', 3),
(N'COLOR',      N'สีไม่ตรง',                N'Color mismatch',               N'SETUP_QUALITY', 4),
(N'WEIGHT',     N'น้ำหนักไม่ได้',           N'Weight not in tolerance',      N'SETUP_QUALITY', 5),
(N'LABEL',      N'Label/marking ผิด',        N'Label/marking incorrect',      N'SETUP_QUALITY', 6),
(N'OTHER',      N'อื่นๆ (ระบุใน note)',     N'Other (specify in note)',       N'SETUP_QUALITY', 99);

-- ============================================================
-- 2. QA Verification (main workflow table)
-- ============================================================
CREATE TABLE qa_verification (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,

    -- Link to Setup TimeLog (the "Q" activity that triggered this)
    setup_time_log_id        BIGINT          NOT NULL,
    machine_id               NVARCHAR(20)    NOT NULL,
    sub_lot_id               BIGINT          NULL,
    product_code             NVARCHAR(50)    NULL,

    -- Requester (Tech)
    requester_id             NVARCHAR(50)    NOT NULL,
    request_note             NVARCHAR(1000)  NULL,
    request_photo_ids        NVARCHAR(MAX)   NULL,   -- JSON array

    -- Status
    status                   NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',
    assigned_qa              NVARCHAR(50)    NULL,

    -- Timing
    requested_at             DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    accepted_at              DATETIME2       NULL,
    completed_at             DATETIME2       NULL,
    duration_seconds         INT             NULL,

    -- Result
    result                   NVARCHAR(20)    NULL,   -- APPROVE | REJECT
    qa_note                  NVARCHAR(1000)  NULL,
    reject_reason_category   NVARCHAR(50)    NULL,
    qa_photo_ids             NVARCHAR(MAX)   NULL,   -- JSON array

    CONSTRAINT FK_qv_stl      FOREIGN KEY (setup_time_log_id) REFERENCES setup_time_log(id),
    CONSTRAINT CK_qv_status   CHECK (status IN ('PENDING','ACCEPTED','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT CK_qv_result   CHECK (result IS NULL OR result IN ('APPROVE','REJECT'))
);

CREATE INDEX IX_qv_status    ON qa_verification(status);
CREATE INDEX IX_qv_machine   ON qa_verification(machine_id, status);
CREATE INDEX IX_qv_assigned  ON qa_verification(assigned_qa, status);
CREATE INDEX IX_qv_requested ON qa_verification(requested_at);
