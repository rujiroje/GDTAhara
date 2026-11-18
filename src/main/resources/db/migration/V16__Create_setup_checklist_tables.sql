-- V16: Dynamic setup checklist templates + per-step photo attachments

CREATE TABLE setup_checklist_templates (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    step_order  INT            NOT NULL DEFAULT 0,
    label       NVARCHAR(200)  NOT NULL,
    machine_type VARCHAR(20)   NULL,        -- NULL = applies to all machine types
    is_required BIT            NOT NULL DEFAULT 0,
    is_active   BIT            NOT NULL DEFAULT 1
);

-- Migrate the 5 existing hardcoded steps into the table
INSERT INTO setup_checklist_templates (step_order, label, machine_type, is_required, is_active) VALUES
(1, N'เปลี่ยน Mold',                        NULL, 0, 1),
(2, N'ปรับอุณหภูมิ (Temperature)',           NULL, 0, 1),
(3, N'ปรับ Cycle Time',                      NULL, 0, 1),
(4, N'จัดตำแหน่ง Blow Pin',                 NULL, 0, 1),
(5, N'First Piece Inspection (FPI) ผ่าน',   NULL, 1, 1);

CREATE TABLE setup_job_step_results (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    setup_job_id     BIGINT         NOT NULL,
    template_id      BIGINT         NOT NULL,
    is_done          BIT            NOT NULL DEFAULT 0,
    photo_filename   NVARCHAR(200)  NULL,
    notes            NVARCHAR(1000) NULL,
    completed_at     DATETIME2      NULL,
    CONSTRAINT FK_step_result_job      FOREIGN KEY (setup_job_id) REFERENCES machine_setup_job(id),
    CONSTRAINT FK_step_result_template FOREIGN KEY (template_id)  REFERENCES setup_checklist_templates(id)
);

CREATE INDEX IX_step_results_job ON setup_job_step_results (setup_job_id);
