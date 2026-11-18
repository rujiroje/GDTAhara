-- W12.5a: Create v_plan_material_requirement view
-- Joins production_plan × products × bill_of_materials × bom_item
-- Used by MaterialRequirementService to avoid repeated JOIN logic
GO

CREATE OR ALTER VIEW v_plan_material_requirement AS
SELECT
    pp.id                                                           AS plan_id,
    pp.plan_date,
    pp.machine_id,
    pp.product_id,
    p.product_code,
    pp.target_qty,
    bi.id                                                           AS bom_item_id,
    bi.rm_code,
    bi.rm_name,
    bi.material_type,
    bi.unit,
    bi.quantity_per,
    bi.loss_percent,
    bi.is_scrap,
    (pp.target_qty * bi.quantity_per)                              AS planned_quantity,
    (pp.target_qty * bi.quantity_per
        * ISNULL(bi.loss_percent, 0) / 100.0)                     AS planned_loss_quantity
FROM production_plan pp
JOIN products p           ON pp.product_id  = p.id
JOIN bill_of_materials b  ON b.fg_code      = p.product_code
                          AND b.status       = 'ACTIVE'
                          AND b.effective_from <= pp.plan_date
                          AND (b.effective_to IS NULL OR b.effective_to >= pp.plan_date)
JOIN bom_item bi          ON bi.bom_id      = b.id;
GO

-- Verification query (run in SSMS after migration):
-- SELECT * FROM sys.views WHERE name = 'v_plan_material_requirement';
-- Expected: 1 row
