-- V35: C-02 — Waste Weight Recording + 2-tier Edit Workflow
-- Tables: waste_type_master, waste_weight_record, waste_edit_log, waste_edit_request
-- Note:   attachment_photo already has WASTE_WEIGHT + WASTE_EDIT_REQUEST ref_types (V27)
-- Note:   telegram_room_config DOC row already seeded in V31; guard with IF NOT EXISTS

-- ============================================================
-- 1. Waste Type Master
-- ============================================================
CREATE TABLE waste_type_master (
    code        NVARCHAR(30)    PRIMARY KEY,
    name_th     NVARCHAR(200)   NOT NULL,
    name_en     NVARCHAR(200)   NOT NULL,
    category    NVARCHAR(50)    NULL,   -- ROH_SCRAP | REGRIND | CONTAMINATED | OTHER
    is_active   BIT             NOT NULL DEFAULT 1,
    sort_order  INT             NOT NULL DEFAULT 0
);

INSERT INTO waste_type_master (code, name_th, name_en, category, sort_order) VALUES
(N'BAG_SCRAP_PE',   N'ถุงเศษ PE',            N'Bag Scrap PE',       N'ROH_SCRAP',    1),
(N'BAG_SCRAP_EVOH', N'ถุงเศษ EVOH',          N'Bag Scrap EVOH',     N'ROH_SCRAP',    2),
(N'REGRIND',        N'เศษพลาสติกก่อนบด',     N'Regrind',            N'REGRIND',      3),
(N'CONTAMINATED',   N'ถุงเศษปนเปื้อน',       N'Contaminated waste', N'CONTAMINATED', 4),
(N'OTHER',          N'อื่นๆ',                N'Other',              N'OTHER',        99);

-- ============================================================
-- 2. Waste Weight Record (main table · immutable after shift)
-- ============================================================
CREATE TABLE waste_weight_record (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id          NVARCHAR(20)    NOT NULL,
    wo_number           NVARCHAR(50)    NULL,       -- optional at entry · required in edit request
    waste_type_code     NVARCHAR(30)    NOT NULL,
    weight_kg           DECIMAL(10,3)   NOT NULL,
    note                NVARCHAR(500)   NULL,
    photo_ids           NVARCHAR(MAX)   NULL,       -- JSON array (attachment_photo)

    recorded_by         NVARCHAR(50)    NOT NULL,
    recorded_at         DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    shift_code          NVARCHAR(10)    NOT NULL,
    shift_date          DATE            NOT NULL,

    version             INT             NOT NULL DEFAULT 1,
    last_edited_by      NVARCHAR(50)    NULL,
    last_edited_at      DATETIME2       NULL,
    edit_count          INT             NOT NULL DEFAULT 0,
    is_active           BIT             NOT NULL DEFAULT 1,

    CONSTRAINT FK_wwr_type FOREIGN KEY (waste_type_code) REFERENCES waste_type_master(code)
);

CREATE INDEX IX_wwr_shift ON waste_weight_record(machine_id, shift_date, shift_code);
CREATE INDEX IX_wwr_user  ON waste_weight_record(recorded_by, recorded_at);
CREATE INDEX IX_wwr_time  ON waste_weight_record(recorded_at);

-- ============================================================
-- 3. Edit Log (full audit · every change preserved)
-- ============================================================
CREATE TABLE waste_edit_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    record_id           BIGINT          NOT NULL,
    edit_type           NVARCHAR(20)    NOT NULL,
    edited_by           NVARCHAR(50)    NOT NULL,
    edited_at           DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    old_machine_id      NVARCHAR(20)    NULL,
    old_wo_number       NVARCHAR(50)    NULL,
    old_waste_type_code NVARCHAR(30)    NULL,
    old_weight_kg       DECIMAL(10,3)   NULL,
    old_note            NVARCHAR(500)   NULL,

    new_machine_id      NVARCHAR(20)    NULL,
    new_wo_number       NVARCHAR(50)    NULL,
    new_waste_type_code NVARCHAR(30)    NULL,
    new_weight_kg       DECIMAL(10,3)   NULL,
    new_note            NVARCHAR(500)   NULL,

    reason              NVARCHAR(500)   NOT NULL,
    edit_request_id     BIGINT          NULL,

    CONSTRAINT FK_wel_record    FOREIGN KEY (record_id) REFERENCES waste_weight_record(id),
    CONSTRAINT CK_wel_type      CHECK (edit_type IN ('SELF_EDIT','DOC_APPROVED'))
);

CREATE INDEX IX_wel_record ON waste_edit_log(record_id, edited_at);

-- ============================================================
-- 4. Edit Request (cross-shift · Doc approval workflow)
-- ============================================================
CREATE TABLE waste_edit_request (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    record_id                   BIGINT          NOT NULL,
    requester_id                NVARCHAR(50)    NOT NULL,
    requested_at                DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    requested_machine_id        NVARCHAR(20)    NULL,
    requested_wo_number         NVARCHAR(50)    NOT NULL,
    requested_waste_type_code   NVARCHAR(30)    NULL,
    requested_weight_kg         DECIMAL(10,3)   NULL,
    requested_note              NVARCHAR(500)   NULL,
    reason                      NVARCHAR(1000)  NOT NULL,
    request_photo_ids           NVARCHAR(MAX)   NULL,   -- JSON array

    status                      NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',
    doc_id                      NVARCHAR(50)    NULL,
    doc_responded_at            DATETIME2       NULL,
    doc_note                    NVARCHAR(500)   NULL,

    telegram_sent               BIT             NOT NULL DEFAULT 0,
    telegram_sent_at            DATETIME2       NULL,

    CONSTRAINT FK_wer_record    FOREIGN KEY (record_id) REFERENCES waste_weight_record(id),
    CONSTRAINT CK_wer_status    CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);

CREATE INDEX IX_wer_status  ON waste_edit_request(status);
CREATE INDEX IX_wer_pending ON waste_edit_request(status, requested_at);

-- ============================================================
-- 5. Guard: ensure DOC telegram room exists (idempotent)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM telegram_room_config WHERE room_code = N'DOC')
    INSERT INTO telegram_room_config (room_code, room_name, description)
    VALUES (N'DOC', N'GDTAHARA Doc Team', N'Doc team · approve requests + document control');
