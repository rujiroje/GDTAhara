-- V36: C-03 — CM Material Stock Card Dashboard
-- Tables: material_reorder_config, low_stock_alert
-- Views:  v_stock_movement, v_material_balance, v_material_lot_balance
-- Note:   Reads from material_stock_transactions (IN) + tank_refill (OUT/CORRECTION)
-- Note:   C-01 (V34) must be applied first (tank_refill table required)

-- ============================================================
-- 1. Reorder Configuration (admin-managed per material)
-- ============================================================
CREATE TABLE material_reorder_config (
    material_code       NVARCHAR(50)    PRIMARY KEY,
    reorder_point_kg    DECIMAL(10,2)   NOT NULL,
    safety_stock_kg     DECIMAL(10,2)   NULL,
    reorder_qty_kg      DECIMAL(10,2)   NULL,
    lead_time_days      INT             NOT NULL DEFAULT 2,
    is_active           BIT             NOT NULL DEFAULT 1,
    updated_by          NVARCHAR(50)    NULL,
    updated_at          DATETIME2       NULL
);

-- Seed common materials from BOM examples
INSERT INTO material_reorder_config (material_code, reorder_point_kg, safety_stock_kg, reorder_qty_kg, lead_time_days) VALUES
(N'NPE-F1',      1000, 500,  1500, 2),
(N'EVOH-FA',      500, 200,   800, 3),
(N'ADS-LA',       300, 100,   500, 2),
(N'BPEGF31NPB', 1000, 500,  1500, 2);

-- ============================================================
-- 2. Low Stock Alert Log (deduplicated per material per day)
-- ============================================================
CREATE TABLE low_stock_alert (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    material_code       NVARCHAR(50)    NOT NULL,
    balance_at_alert    DECIMAL(10,2)   NOT NULL,
    reorder_point       DECIMAL(10,2)   NOT NULL,
    est_days_remaining  DECIMAL(5,1)    NULL,
    alert_date          DATE            NOT NULL,
    telegram_sent       BIT             NOT NULL DEFAULT 0,
    telegram_sent_at    DATETIME2       NULL,
    resolved_at         DATETIME2       NULL,

    CONSTRAINT UK_lsa   UNIQUE (material_code, alert_date)
);

CREATE INDEX IX_lsa_pending ON low_stock_alert(resolved_at, alert_date);

-- ============================================================
-- 3. View: v_stock_movement (IN from stock_transactions + OUT/CORRECTION from tank_refill)
-- ============================================================
CREATE VIEW v_stock_movement AS
    -- IN: material received (transactionType = 'IN')
    SELECT
        N'IN'               AS movement_type,
        m.code              AS material_code,
        mst.lotNumber       AS lot_ref,
        mst.quantity        AS qty_kg,
        NULL                AS negative_qty,
        mst.timestamp       AS movement_at,
        CAST(mst.user_id AS NVARCHAR(50)) AS actor,
        NULL                AS machine_id,
        NULL                AS tank_id,
        NULL                AS note,
        CAST(0 AS BIT)      AS is_correction,
        mst.id              AS source_id,
        N'material_stock_transactions' AS source_table
    FROM material_stock_transactions mst
    JOIN materials m ON m.id = mst.material_id
    WHERE mst.transactionType = N'IN'

    UNION ALL

    -- OUT / CORRECTION: tank refills (positive = OUT, negative = CORRECTION)
    SELECT
        CASE WHEN tr.qty_kg < 0 THEN N'CORRECTION' ELSE N'OUT' END AS movement_type,
        tr.material_code,
        tr.material_lot_code                AS lot_ref,
        CASE WHEN tr.qty_kg > 0 THEN tr.qty_kg ELSE NULL END AS qty_kg,
        CASE WHEN tr.qty_kg < 0 THEN tr.qty_kg ELSE NULL END AS negative_qty,
        tr.refilled_at                      AS movement_at,
        tr.refilled_by                      AS actor,
        tr.machine_id,
        tr.tank_id,
        tr.note,
        tr.is_correction,
        tr.id                               AS source_id,
        N'tank_refill'                      AS source_table
    FROM tank_refill tr;

GO

-- ============================================================
-- 4. View: v_material_balance (total balance per material)
-- ============================================================
CREATE VIEW v_material_balance AS
    SELECT
        material_code,
        SUM(CASE WHEN movement_type = N'IN'         THEN qty_kg      ELSE 0 END) AS total_in_kg,
        SUM(CASE WHEN movement_type = N'OUT'        THEN qty_kg      ELSE 0 END) AS total_out_kg,
        SUM(CASE WHEN movement_type = N'CORRECTION' THEN negative_qty ELSE 0 END) AS total_correction_kg,
        SUM(CASE
            WHEN movement_type = N'IN'         THEN  qty_kg
            WHEN movement_type = N'OUT'        THEN -qty_kg
            WHEN movement_type = N'CORRECTION' THEN -negative_qty
            ELSE 0
        END)                                    AS balance_kg,
        COUNT(DISTINCT lot_ref)                 AS lot_count,
        MAX(movement_at)                        AS last_movement_at
    FROM v_stock_movement
    GROUP BY material_code;

GO

-- ============================================================
-- 5. View: v_material_lot_balance (per material per lot · active only)
-- ============================================================
CREATE VIEW v_material_lot_balance AS
    SELECT
        material_code,
        lot_ref,
        SUM(CASE
            WHEN movement_type = N'IN'         THEN  qty_kg
            WHEN movement_type = N'OUT'        THEN -qty_kg
            WHEN movement_type = N'CORRECTION' THEN -negative_qty
            ELSE 0
        END)                                    AS balance_kg,
        MIN(CASE WHEN movement_type = N'IN' THEN movement_at END) AS received_at,
        MAX(movement_at)                        AS last_activity_at
    FROM v_stock_movement
    GROUP BY material_code, lot_ref
    HAVING SUM(CASE
        WHEN movement_type = N'IN'         THEN  qty_kg
        WHEN movement_type = N'OUT'        THEN -qty_kg
        WHEN movement_type = N'CORRECTION' THEN -negative_qty
        ELSE 0
    END) > 0;
