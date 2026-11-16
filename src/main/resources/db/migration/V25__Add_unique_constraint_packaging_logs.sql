-- V25: Deduplicate packaging_logs then add UNIQUE constraint
-- Duplicates exist from race conditions before this fix — keep only the earliest id per group.

DELETE pl
FROM packaging_logs pl
INNER JOIN (
    SELECT MIN(id) AS keep_id, report_id, lot_number, box_no
    FROM packaging_logs
    GROUP BY report_id, lot_number, box_no
    HAVING COUNT(*) > 1
) dups ON  pl.report_id  = dups.report_id
       AND pl.lot_number = dups.lot_number
       AND pl.box_no     = dups.box_no
       AND pl.id        != dups.keep_id;

ALTER TABLE packaging_logs
ADD CONSTRAINT UQ_packaging_logs_report_lot_box
UNIQUE (report_id, lot_number, box_no);
