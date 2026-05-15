-- Migration: Create recipes table
-- New table, zero impact on existing data

CREATE TABLE recipes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    recipe_code     NVARCHAR(50) NOT NULL UNIQUE,
    recipe_name     NVARCHAR(200) NOT NULL,
    product_id      BIGINT NOT NULL,
    machine_id      BIGINT NULL,           -- NULL = ใช้ได้กับทุกเครื่อง
    version         NVARCHAR(20) NOT NULL DEFAULT '1.0',
    is_active       BIT NOT NULL DEFAULT 1,
    -- Process parameters (nullable — ใส่เท่าที่รู้)
    target_cycle_time_sec   DECIMAL(8,2) NULL,
    target_temp_zone1       DECIMAL(6,2) NULL,
    target_temp_zone2       DECIMAL(6,2) NULL,
    target_temp_head        DECIMAL(6,2) NULL,
    target_blow_pressure    DECIMAL(6,2) NULL,
    notes           NVARCHAR(MAX) NULL,
    created_by      BIGINT NULL,
    created_at      DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2 NULL,

    CONSTRAINT fk_recipe_product  FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_recipe_machine  FOREIGN KEY (machine_id) REFERENCES machines(id),
    CONSTRAINT fk_recipe_user     FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_recipe_product  ON recipes (product_id);
CREATE INDEX idx_recipe_machine  ON recipes (machine_id);
CREATE INDEX idx_recipe_active   ON recipes (is_active);
