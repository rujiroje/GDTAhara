-- W12.5a: Create BOM core tables
-- bill_of_materials (header), bom_item (lines), bom_import_log (audit)

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bill_of_materials')
BEGIN
    CREATE TABLE bill_of_materials (
        id                    BIGINT IDENTITY(1,1) NOT NULL,
        fg_code               NVARCHAR(50)   NOT NULL,
        alternative_number    INT            NOT NULL CONSTRAINT df_bom_alt DEFAULT 1,
        plant_code            NVARCHAR(20)   NOT NULL CONSTRAINT df_bom_plant DEFAULT '5100',
        material_group        NVARCHAR(50)   NULL,
        sap_material_type     NVARCHAR(10)   NULL,
        base_quantity         DECIMAL(12,4)  NOT NULL CONSTRAINT df_bom_baseq DEFAULT 1,
        unit                  NVARCHAR(10)   NOT NULL CONSTRAINT df_bom_unit DEFAULT 'PCS',
        effective_from        DATE           NOT NULL,
        effective_to          DATE           NULL,
        status                NVARCHAR(20)   NOT NULL CONSTRAINT df_bom_status DEFAULT 'ACTIVE',
        imported_from_file    NVARCHAR(200)  NULL,
        imported_at           DATETIME2      NOT NULL CONSTRAINT df_bom_imp_at DEFAULT SYSUTCDATETIME(),
        imported_by           BIGINT         NOT NULL,
        created_at            DATETIME2      NOT NULL CONSTRAINT df_bom_created DEFAULT SYSUTCDATETIME(),

        CONSTRAINT pk_bill_of_materials PRIMARY KEY (id),
        CONSTRAINT uk_bom_fg_alt_plant_effective
            UNIQUE (fg_code, alternative_number, plant_code, effective_from),
        CONSTRAINT fk_bom_imported_by FOREIGN KEY (imported_by) REFERENCES users(id)
    );

    CREATE INDEX idx_bom_fg_status  ON bill_of_materials(fg_code, status);
    CREATE INDEX idx_bom_effective   ON bill_of_materials(effective_from, effective_to);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bom_item')
BEGIN
    CREATE TABLE bom_item (
        id                    BIGINT IDENTITY(1,1) NOT NULL,
        bom_id                BIGINT         NOT NULL,
        item_number           INT            NOT NULL,
        rm_code               NVARCHAR(50)   NOT NULL,
        rm_name               NVARCHAR(200)  NULL,
        quantity_per          DECIMAL(12,4)  NOT NULL,
        unit                  NVARCHAR(10)   NOT NULL,
        loss_percent          DECIMAL(5,2)   NULL,
        material_type         NVARCHAR(10)   NOT NULL,
        is_scrap              BIT            NOT NULL CONSTRAINT df_bi_scrap DEFAULT 0,
        bom_status            INT            NOT NULL CONSTRAINT df_bi_status DEFAULT 1,
        notes                 NVARCHAR(500)  NULL,

        CONSTRAINT pk_bom_item PRIMARY KEY (id),
        CONSTRAINT uk_bom_item_bom_seq UNIQUE (bom_id, item_number),
        CONSTRAINT fk_bom_item_bom FOREIGN KEY (bom_id) REFERENCES bill_of_materials(id)
    );

    CREATE INDEX idx_bi_bom     ON bom_item(bom_id);
    CREATE INDEX idx_bi_rm_code ON bom_item(rm_code);
    CREATE INDEX idx_bi_scrap   ON bom_item(is_scrap) WHERE is_scrap = 1;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'bom_import_log')
BEGIN
    CREATE TABLE bom_import_log (
        id                    BIGINT IDENTITY(1,1) NOT NULL,
        filename              NVARCHAR(200)  NOT NULL,
        file_type             NVARCHAR(10)   NOT NULL,
        factory_code          NVARCHAR(20)   NULL,
        imported_by           BIGINT         NOT NULL,
        imported_at           DATETIME2      NOT NULL CONSTRAINT df_bil_at DEFAULT SYSUTCDATETIME(),
        rows_added            INT            NOT NULL CONSTRAINT df_bil_added DEFAULT 0,
        rows_updated          INT            NOT NULL CONSTRAINT df_bil_updated DEFAULT 0,
        rows_skipped          INT            NOT NULL CONSTRAINT df_bil_skipped DEFAULT 0,
        rows_error            INT            NOT NULL CONSTRAINT df_bil_error DEFAULT 0,
        errors_json           NVARCHAR(MAX)  NULL,
        duration_ms           INT            NULL,

        CONSTRAINT pk_bom_import_log PRIMARY KEY (id),
        CONSTRAINT fk_bil_user FOREIGN KEY (imported_by) REFERENCES users(id)
    );

    CREATE INDEX idx_bil_file_type_date ON bom_import_log(file_type, imported_at DESC);
END
GO
