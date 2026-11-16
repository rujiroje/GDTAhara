-- V37: DA-01 — NG Type Master Reference Photos
-- Adds: photo_ids, primary_photo_id to ng_type_master
-- Note:   attachment_photo already has ref_type='NG_TYPE' in CHECK constraint (V27)
-- Note:   caption + display_order already on attachment_photo (V27) — no ALTER needed

-- ============================================================
-- 1. Add photo columns to ng_type_master
-- ============================================================
IF COL_LENGTH('ng_type_master', 'photo_ids') IS NULL
BEGIN
    ALTER TABLE ng_type_master
        ADD photo_ids           NVARCHAR(MAX)   NULL,   -- JSON array of attachment_photo.id
            primary_photo_id    BIGINT          NULL;   -- FK to attachment_photo (nullable)
END;
