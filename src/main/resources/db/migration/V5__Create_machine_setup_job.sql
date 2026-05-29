CREATE TABLE machine_setup_job (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    machine_id            BIGINT        NOT NULL,
    from_product_id       BIGINT        NULL,
    to_product_id         BIGINT        NOT NULL,
    production_plan_id    BIGINT        NOT NULL,
    plan_date             DATE          NOT NULL,
    required_before       TIME          NULL,
    assigned_to_user_id   BIGINT        NULL,
    status                NVARCHAR(20)  NOT NULL CONSTRAINT df_msj_status DEFAULT 'PENDING',
    started_at            DATETIME2     NULL,
    completed_at          DATETIME2     NULL,
    duration_min          INT           NULL,
    mold_changed          BIT           NOT NULL CONSTRAINT df_msj_mold DEFAULT 0,
    mold_code_from        NVARCHAR(50)  NULL,
    mold_code_to          NVARCHAR(50)  NULL,
    temp_adjusted         BIT           NOT NULL CONSTRAINT df_msj_temp DEFAULT 0,
    cycle_adjusted        BIT           NOT NULL CONSTRAINT df_msj_cycle DEFAULT 0,
    blow_pin_aligned      BIT           NOT NULL CONSTRAINT df_msj_blow DEFAULT 0,
    fpi_passed            BIT           NOT NULL CONSTRAINT df_msj_fpi DEFAULT 0,
    skip_reason           NVARCHAR(500) NULL,
    notes                 NVARCHAR(MAX) NULL,
    completed_by_user_id  BIGINT        NULL,

    CONSTRAINT pk_machine_setup_job PRIMARY KEY (id),
    CONSTRAINT fk_msj_machine
        FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_msj_from_product
        FOREIGN KEY (from_product_id) REFERENCES products(id),
    CONSTRAINT fk_msj_to_product
        FOREIGN KEY (to_product_id) REFERENCES products(id),
    CONSTRAINT fk_msj_plan
        FOREIGN KEY (production_plan_id) REFERENCES production_plan(id),
    CONSTRAINT fk_msj_assigned_to
        FOREIGN KEY (assigned_to_user_id) REFERENCES users(id),
    CONSTRAINT fk_msj_completed_by
        FOREIGN KEY (completed_by_user_id) REFERENCES users(id)
)
