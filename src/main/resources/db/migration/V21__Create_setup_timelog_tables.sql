-- V21: Setup TimeLog system (W19a) — replaces checklist-based completion
-- Tables: setup_activity_code (master), setup_time_log (entries), setup_time_log_photo (attachments)
-- Old checklist tables (setup_checklist_templates, setup_job_step_results) remain read-only.

CREATE TABLE setup_activity_code (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    code                VARCHAR(10)     NOT NULL,
    description_th      NVARCHAR(200)   NOT NULL,
    description_en      NVARCHAR(200)   NULL,
    category            VARCHAR(50)     NULL,
    color_hex           VARCHAR(7)      NOT NULL DEFAULT '#607D8B',
    is_active           BIT             NOT NULL DEFAULT 1,
    display_order       INT             NOT NULL DEFAULT 99,
    created_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    created_by_user_id  BIGINT          NULL,

    CONSTRAINT UK_sac_code       UNIQUE (code),
    CONSTRAINT FK_sac_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

-- 10 default activity codes (Admin adds P and others after interview with senior tech)
INSERT INTO setup_activity_code (code, description_th, description_en, category, color_hex, display_order) VALUES
(N'M', N'เปลี่ยนแม่พิมพ์',       N'Mold Change',               N'MECHANICAL',   N'#F44336', 1),
(N'A', N'ปรับอุณหภูมิ',           N'Temperature Adjustment',     N'PROCESS',      N'#FF9800', 2),
(N'S', N'ปรับความเร็ว/Stroke',    N'Speed/Stroke Adjustment',    N'PROCESS',      N'#FFC107', 3),
(N'C', N'ตรวจสอบ Cycle Time',     N'Cycle Time Check',           N'PROCESS',      N'#2196F3', 4),
(N'Q', N'ตรวจคุณภาพ',             N'Quality Inspection',         N'QUALITY',      N'#9C27B0', 5),
(N'Z', N'ล้างทำความสะอาด',        N'Cleaning',                   N'MAINTENANCE',  N'#00BCD4', 6),
(N'R', N'ซ่อมเล็กน้อย',           N'Minor Repair',               N'MECHANICAL',   N'#795548', 7),
(N'H', N'รอ Heat-up',             N'Heat-up Wait',               N'PROCESS',      N'#FF5722', 8),
(N'B', N'รอวัตถุดิบ',             N'Material Wait',              N'MATERIAL',     N'#607D8B', 9),
(N'X', N'อื่นๆ',                  N'Other',                      N'OTHER',        N'#9E9E9E', 10);

CREATE TABLE setup_time_log (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    setup_job_id        BIGINT          NOT NULL,
    activity_code_id    BIGINT          NOT NULL,
    start_time          DATETIME2       NOT NULL,
    end_time            DATETIME2       NULL,
    duration_min        INT             NULL,
    sequence_no         INT             NULL,
    description         NVARCHAR(500)   NULL,
    created_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    created_by_user_id  BIGINT          NULL,
    updated_at          DATETIME2       NULL,
    updated_by_user_id  BIGINT          NULL,

    CONSTRAINT FK_stl_job        FOREIGN KEY (setup_job_id)        REFERENCES machine_setup_job(id),
    CONSTRAINT FK_stl_code       FOREIGN KEY (activity_code_id)    REFERENCES setup_activity_code(id),
    CONSTRAINT FK_stl_created_by FOREIGN KEY (created_by_user_id)  REFERENCES users(id),
    CONSTRAINT FK_stl_updated_by FOREIGN KEY (updated_by_user_id)  REFERENCES users(id)
);

CREATE INDEX IX_stl_job ON setup_time_log(setup_job_id, start_time);

CREATE TABLE setup_time_log_photo (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    time_log_id          BIGINT          NOT NULL,
    file_path            NVARCHAR(500)   NOT NULL,
    file_name            NVARCHAR(200)   NOT NULL,
    file_size_kb         INT             NULL,
    caption              NVARCHAR(200)   NULL,
    uploaded_at          DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    uploaded_by_user_id  BIGINT          NULL,

    CONSTRAINT FK_stlp_log  FOREIGN KEY (time_log_id)          REFERENCES setup_time_log(id),
    CONSTRAINT FK_stlp_user FOREIGN KEY (uploaded_by_user_id)  REFERENCES users(id)
);

CREATE INDEX IX_stlp_log ON setup_time_log_photo(time_log_id);
