-- V32: O-11 — Box Label Reprint Reason Logging + Traceability

-- ============================================================
-- 1. Reprint Reason Master
-- ============================================================
CREATE TABLE box_reprint_reason_master (
    code        NVARCHAR(30)    PRIMARY KEY,
    name_th     NVARCHAR(200)   NOT NULL,
    name_en     NVARCHAR(200)   NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    is_active   BIT             NOT NULL DEFAULT 1
);

INSERT INTO box_reprint_reason_master (code, name_th, name_en, sort_order) VALUES
(N'LABEL_DAMAGED', N'Label เสียหาย (พิมพ์ไม่ชัด · ฉีก · เปื้อน)', N'Label damaged',  1),
(N'LABEL_LOST',    N'Label หาย',                                    N'Label lost',     2),
(N'PRINT_WRONG',   N'พิมพ์ผิด (barcode/text ไม่ถูก)',              N'Print wrong',    3),
(N'PRINTER_ISSUE', N'Printer ค้าง · ทำใหม่',                        N'Printer stuck',  4),
(N'DATA_CHANGE',   N'เปลี่ยนข้อมูล box',                            N'Data change',    5),
(N'OTHER',         N'อื่นๆ (ระบุใน detail)',                        N'Other',          99);

-- ============================================================
-- 2. Box Reprint Log
-- ============================================================
CREATE TABLE box_reprint_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    packaging_log_id    BIGINT          NOT NULL,   -- FK to packaging_logs
    reprint_sequence    INT             NOT NULL,   -- 1, 2, 3... per box
    reprinted_by        NVARCHAR(50)    NOT NULL,
    reprinted_at        DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    reason_code         NVARCHAR(30)    NOT NULL,
    reason_detail       NVARCHAR(500)   NOT NULL,   -- required · min 10 chars enforced in app
    photo_ids           NVARCHAR(MAX)   NULL,       -- JSON array
    printer_id          NVARCHAR(100)   NULL,
    print_status        NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',
    print_completed_at  DATETIME2       NULL,
    handheld_session_id BIGINT          NULL,

    CONSTRAINT FK_brl_box    FOREIGN KEY (packaging_log_id)    REFERENCES packaging_logs(id),
    CONSTRAINT FK_brl_reason FOREIGN KEY (reason_code)         REFERENCES box_reprint_reason_master(code),
    CONSTRAINT UK_brl_seq    UNIQUE (packaging_log_id, reprint_sequence)
);

CREATE INDEX IX_brl_box  ON box_reprint_log(packaging_log_id);
CREATE INDEX IX_brl_user ON box_reprint_log(reprinted_by, reprinted_at);
CREATE INDEX IX_brl_time ON box_reprint_log(reprinted_at);
