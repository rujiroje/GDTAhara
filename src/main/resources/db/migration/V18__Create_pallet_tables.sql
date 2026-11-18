-- V18: Pallet Assembly (QP-PD-002-F046) — digitize pallet composition + label print
-- Adds: pallet table, pallet_close_log audit, pallet_id FK on sub_lot

CREATE TABLE pallet (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    pallet_number        VARCHAR(50)    NOT NULL,
    product_id           BIGINT         NOT NULL,
    product_code         VARCHAR(50)    NOT NULL,
    target_qty           INT            NULL,
    actual_qty           INT            NOT NULL DEFAULT 0,
    box_count            INT            NOT NULL DEFAULT 0,
    status               VARCHAR(20)    NOT NULL DEFAULT 'OPEN',  -- OPEN CLOSED PRINTED CANCELLED
    pallet_date          DATE           NOT NULL,
    lot_date_min         DATE           NULL,
    lot_date_max         DATE           NULL,
    created_at           DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    created_by_user_id   BIGINT         NOT NULL,
    closed_at            DATETIME2      NULL,
    closed_by_user_id    BIGINT         NULL,
    printed_at           DATETIME2      NULL,
    print_count          INT            NOT NULL DEFAULT 0,
    notes                NVARCHAR(500)  NULL,
    parent_pallet_id     BIGINT         NULL,
    rearrange_reason     NVARCHAR(500)  NULL,
    revision_suffix      VARCHAR(10)    NOT NULL DEFAULT '',

    CONSTRAINT fk_pallet_product     FOREIGN KEY (product_id)          REFERENCES products(id),
    CONSTRAINT fk_pallet_created_by  FOREIGN KEY (created_by_user_id)  REFERENCES users(id),
    CONSTRAINT fk_pallet_closed_by   FOREIGN KEY (closed_by_user_id)   REFERENCES users(id),
    CONSTRAINT fk_pallet_parent      FOREIGN KEY (parent_pallet_id)    REFERENCES pallet(id),
    CONSTRAINT uk_pallet_number_date_product_rev
        UNIQUE (pallet_number, pallet_date, product_id, revision_suffix)
);

CREATE INDEX ix_pallet_status    ON pallet(status);
CREATE INDEX ix_pallet_date      ON pallet(pallet_date DESC);
CREATE INDEX ix_pallet_product   ON pallet(product_id);
CREATE INDEX ix_pallet_parent    ON pallet(parent_pallet_id);

-- Extend sub_lot with proper pallet FK
ALTER TABLE sub_lot ADD pallet_id BIGINT NULL;
ALTER TABLE sub_lot ADD CONSTRAINT fk_sub_lot_pallet
    FOREIGN KEY (pallet_id) REFERENCES pallet(id);
CREATE INDEX ix_sub_lot_pallet ON sub_lot(pallet_id);

-- Audit log: every change to pallet composition is recorded
CREATE TABLE pallet_close_log (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    pallet_id      BIGINT        NOT NULL,
    action         VARCHAR(30)   NOT NULL,   -- CREATED BOX_ADDED BOX_REMOVED CLOSED PRINTED REOPENED REARRANGED_FROM
    sub_lot_id     BIGINT        NULL,
    ref_pallet_id  BIGINT        NULL,       -- for REARRANGED_FROM: points to source pallet
    performed_by   BIGINT        NOT NULL,
    performed_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    reason         NVARCHAR(500) NULL,

    CONSTRAINT fk_pcl_pallet  FOREIGN KEY (pallet_id)  REFERENCES pallet(id),
    CONSTRAINT fk_pcl_sublot  FOREIGN KEY (sub_lot_id) REFERENCES sub_lot(id),
    CONSTRAINT fk_pcl_user    FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE INDEX ix_pcl_pallet ON pallet_close_log(pallet_id, performed_at DESC);
