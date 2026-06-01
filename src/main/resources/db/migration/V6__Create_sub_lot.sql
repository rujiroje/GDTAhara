CREATE TABLE sub_lot (
    id                      BIGINT IDENTITY(1,1) NOT NULL,
    production_report_id    BIGINT         NOT NULL,
    sub_lot_number          NVARCHAR(100)  NOT NULL,
    pallet_number           NVARCHAR(50)   NULL,
    box_quantity            INT            NOT NULL,
    weight_kg               DECIMAL(10,3)  NULL,
    confirmed_at            DATETIME2      NOT NULL CONSTRAINT df_sl_confirmed_at DEFAULT SYSUTCDATETIME(),
    confirmed_by_user_id    BIGINT         NOT NULL,
    status                  NVARCHAR(20)   NOT NULL CONSTRAINT df_sl_status DEFAULT 'draft',
    zpl_label_printed_ref   NVARCHAR(100)  NULL,

    CONSTRAINT pk_sub_lot PRIMARY KEY (id),
    CONSTRAINT uk_sub_lot_number UNIQUE (sub_lot_number),
    CONSTRAINT fk_sub_lot_report
        FOREIGN KEY (production_report_id) REFERENCES production_reports(id),
    CONSTRAINT fk_sub_lot_confirmed_by
        FOREIGN KEY (confirmed_by_user_id) REFERENCES users(id)
)
