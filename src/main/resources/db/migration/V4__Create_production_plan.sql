CREATE TABLE production_plan (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    plan_date        DATE           NOT NULL,
    machine_id       BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    target_qty       INT            NOT NULL,
    manpower_d_ratio DECIMAL(5,2)   NOT NULL CONSTRAINT df_pp_d_ratio DEFAULT 0.50,
    manpower_n_ratio DECIMAL(5,2)   NOT NULL CONSTRAINT df_pp_n_ratio DEFAULT 0.50,
    sap_wo_number    NVARCHAR(50)   NULL,
    source           NVARCHAR(20)   NOT NULL,
    excel_file_ref   NVARCHAR(200)  NULL,
    status           NVARCHAR(20)   NOT NULL CONSTRAINT df_pp_status DEFAULT 'draft',
    created_by       BIGINT         NOT NULL,
    created_at       DATETIME2      NOT NULL CONSTRAINT df_pp_created_at DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2      NOT NULL CONSTRAINT df_pp_updated_at DEFAULT SYSUTCDATETIME(),

    CONSTRAINT pk_production_plan PRIMARY KEY (id),
    CONSTRAINT uk_production_plan_machine_date_product
        UNIQUE (machine_id, plan_date, product_id),
    CONSTRAINT fk_production_plan_machine
        FOREIGN KEY (machine_id) REFERENCES machine(id),
    CONSTRAINT fk_production_plan_product
        FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_production_plan_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
)
