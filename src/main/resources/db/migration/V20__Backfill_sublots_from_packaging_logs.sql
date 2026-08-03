-- V20: Backfill sub_lot records from existing packaging_logs
-- Each packaging box label barcode = {product_code}-{lot_number}-{box_no:3d}
-- Track-Out and Pallet Assembly scan this barcode, so each PackagingLog needs a SubLot.
--
-- Uses ROW_NUMBER() to pick exactly one row per unique sub_lot_number,
-- then filters out sub_lot_numbers that already exist.

INSERT INTO sub_lot (production_report_id, sub_lot_number, box_quantity, confirmed_at, confirmed_by_user_id, status)
SELECT
    src.report_id,
    src.sub_lot_number,
    ISNULL(src.qty_per_box, 0),
    src.[timestamp],
    src.operator_id,
    'draft'
FROM (
    SELECT
        pl.report_id,
        pl.operator_id,
        pl.[timestamp],
        prod.qty_per_box,
        CONCAT(prod.product_code, '-', pl.lot_number, '-',
               RIGHT('000' + CAST(pl.box_no AS VARCHAR(10)), 3)) AS sub_lot_number,
        ROW_NUMBER() OVER (
            PARTITION BY CONCAT(prod.product_code, '-', pl.lot_number, '-',
                                RIGHT('000' + CAST(pl.box_no AS VARCHAR(10)), 3))
            ORDER BY pl.id
        ) AS rn
    FROM packaging_logs pl
    JOIN production_reports pr ON pr.id = pl.report_id
    JOIN products prod ON prod.id = pr.product_id
) src
WHERE src.rn = 1
  AND NOT EXISTS (
      SELECT 1 FROM sub_lot sl WHERE sl.sub_lot_number = src.sub_lot_number
  );
