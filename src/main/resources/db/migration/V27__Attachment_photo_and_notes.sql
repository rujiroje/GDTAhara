-- V27: T-01 — Photo attachment + notes for Setup TimeLog and Machine Downtime
-- Creates shared attachment_photo table (polymorphic) and adds note/photo_ids columns

-- ============================================================
-- 1. Add note + photo_ids columns to setup_time_log
-- ============================================================
ALTER TABLE setup_time_log
    ADD start_note      NVARCHAR(500)   NULL,
        stop_note       NVARCHAR(500)   NULL,
        start_photo_ids NVARCHAR(MAX)   NULL,   -- JSON array of attachment_photo.id
        stop_photo_ids  NVARCHAR(MAX)   NULL;   -- JSON array of attachment_photo.id

-- ============================================================
-- 2. Add note + photo_ids columns to downtime_events
-- ============================================================
ALTER TABLE downtime_events
    ADD start_note      NVARCHAR(500)   NULL,
        stop_note       NVARCHAR(500)   NULL,
        start_photo_ids NVARCHAR(MAX)   NULL,
        stop_photo_ids  NVARCHAR(MAX)   NULL;

-- ============================================================
-- 3. Shared attachment_photo table (polymorphic)
--    ref_type values grow as more features are added
-- ============================================================
CREATE TABLE attachment_photo (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    ref_type        NVARCHAR(30)    NOT NULL,   -- 'SETUP_TIME_LOG' | 'MACHINE_DOWNTIME' | ...
    ref_id          BIGINT          NOT NULL,
    attach_phase    NVARCHAR(10)    NOT NULL,   -- 'START' | 'STOP' | 'NOTE'
    file_url        NVARCHAR(500)   NOT NULL,
    file_name       NVARCHAR(255)   NOT NULL,
    file_size_kb    INT             NOT NULL DEFAULT 0,
    mime_type       NVARCHAR(50)    NOT NULL DEFAULT N'image/jpeg',
    caption         NVARCHAR(200)   NULL,
    display_order   INT             NOT NULL DEFAULT 0,
    uploaded_by     NVARCHAR(50)    NOT NULL,
    uploaded_at     DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT CK_ap_ref_type     CHECK (ref_type IN (
        'SETUP_TIME_LOG','MACHINE_DOWNTIME','HELP_REQUEST',
        'QA_VERIFICATION','BOX_REPRINT','TANK_REFILL',
        'WASTE_WEIGHT','WASTE_EDIT_REQUEST','NG_TYPE')),
    CONSTRAINT CK_ap_phase        CHECK (attach_phase IN ('START','STOP','NOTE'))
);

CREATE INDEX IX_ap_ref       ON attachment_photo(ref_type, ref_id);
CREATE INDEX IX_ap_upload    ON attachment_photo(uploaded_at);
