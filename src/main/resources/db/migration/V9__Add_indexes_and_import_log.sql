-- Step 1: Create import_log table
CREATE TABLE import_log (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    filename              NVARCHAR(200)  NOT NULL,
    factory_code          NVARCHAR(20)   NULL,
    imported_by           BIGINT         NOT NULL,
    imported_at           DATETIME2      NOT NULL CONSTRAINT df_il_imported DEFAULT SYSUTCDATETIME(),
    rows_added            INT            NOT NULL CONSTRAINT df_il_added DEFAULT 0,
    rows_skipped_past     INT            NOT NULL CONSTRAINT df_il_skip_past DEFAULT 0,
    rows_skipped_started  INT            NOT NULL CONSTRAINT df_il_skip_started DEFAULT 0,
    rows_updated          INT            NOT NULL CONSTRAINT df_il_updated DEFAULT 0,
    errors_json           NVARCHAR(MAX)  NULL,
    CONSTRAINT pk_import_log PRIMARY KEY (id),
    CONSTRAINT fk_import_log_user FOREIGN KEY (imported_by) REFERENCES users(id)
);
GO

-- Step 2: Phase 1 indexes
CREATE INDEX idx_pp_machine_date      ON production_plan      (machine_id, plan_date);
CREATE INDEX idx_pp_status            ON production_plan      (status);
CREATE INDEX idx_msj_machine_date     ON machine_setup_job    (machine_id, plan_date);
CREATE INDEX idx_msj_assigned_status  ON machine_setup_job    (assigned_to_user_id, status);
CREATE INDEX idx_msj_status           ON machine_setup_job    (status);
CREATE INDEX idx_sl_report            ON sub_lot              (production_report_id);
CREATE INDEX idx_sl_pallet            ON sub_lot              (pallet_number);
CREATE INDEX idx_il_factory_date      ON import_log           (factory_code, imported_at);
GO
