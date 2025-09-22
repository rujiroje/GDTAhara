-- =================================================================
-- สร้างข้อมูลทดสอบสำหรับ Packaging Logs และ NG Logs
-- เพื่อให้ระบบสรุปผลการผลิตแสดงผลที่ถูกต้อง
-- =================================================================

USE GDTahara_DB;
GO

-- 🚀 เพิ่มข้อมูลทดสอบ Packaging Logs สำหรับ Report ID 3
INSERT INTO packaging_logs (report_id, operator_id, lot_number, box_no, timestamp, notes) VALUES
(3, 6, 'LOT001', 1, GETDATE(), 'Box 1 - Good Quality'),
(3, 6, 'LOT001', 2, GETDATE(), 'Box 2 - Good Quality'), 
(3, 6, 'LOT001', 3, GETDATE(), 'Box 3 - Good Quality'),
(3, 6, 'LOT001', 4, GETDATE(), 'Box 4 - Good Quality'),
(3, 6, 'LOT001', 5, GETDATE(), 'Box 5 - Good Quality');

-- 🚀 เพิ่มข้อมูลทดสอบ NG Logs สำหรับ Report ID 3
INSERT INTO ng_logs (report_id, ng_type_id, user_id, source, quantity, timestamp, notes) VALUES
(3, 1, 6, 'Production Line', 2, GETDATE(), 'Defective parts found'),
(3, 2, 6, 'Production Line', 1, GETDATE(), 'Color variation'),
(3, 1, 6, 'Quality Check', 1, GETDATE(), 'Dimension out of spec');

-- 🚀 เพิ่มข้อมูลทดสอบสำหรับ Report ID อื่นๆ หากมี
IF EXISTS (SELECT 1 FROM production_reports WHERE id = 2)
BEGIN
    INSERT INTO packaging_logs (report_id, operator_id, lot_number, box_no, timestamp, notes) VALUES
    (2, 6, 'LOT002', 1, GETDATE(), 'Box 1 - Good Quality'),
    (2, 6, 'LOT002', 2, GETDATE(), 'Box 2 - Good Quality'),
    (2, 6, 'LOT002', 3, GETDATE(), 'Box 3 - Good Quality');

    INSERT INTO ng_logs (report_id, ng_type_id, user_id, source, quantity, timestamp, notes) VALUES
    (2, 1, 6, 'Production Line', 1, GETDATE(), 'Minor defect');
END

-- 🔍 ตรวจสอบข้อมูลที่เพิ่มเข้าไป
SELECT 'Packaging Logs Count' as DataType, report_id, COUNT(*) as Count
FROM packaging_logs 
GROUP BY report_id
ORDER BY report_id;

SELECT 'NG Logs Count' as DataType, report_id, COUNT(*) as Count  
FROM ng_logs
GROUP BY report_id
ORDER BY report_id;

-- 🎯 ตรวจสอบข้อมูลสรุป
SELECT 
    pr.id as ReportId,
    pr.product_id,
    pr.machine_id,
    pr.target_qty,
    (SELECT COUNT(*) FROM packaging_logs WHERE report_id = pr.id) as GoodQty,
    (SELECT COUNT(*) FROM ng_logs WHERE report_id = pr.id) as NgQty,
    CASE 
        WHEN (SELECT COUNT(*) FROM packaging_logs WHERE report_id = pr.id) + 
             (SELECT COUNT(*) FROM ng_logs WHERE report_id = pr.id) > 0
        THEN ROUND(
            (CAST((SELECT COUNT(*) FROM packaging_logs WHERE report_id = pr.id) AS FLOAT) / 
             CAST((SELECT COUNT(*) FROM packaging_logs WHERE report_id = pr.id) + 
                  (SELECT COUNT(*) FROM ng_logs WHERE report_id = pr.id) AS FLOAT)) * 100, 2
        )
        ELSE 0
    END as YieldPercentage
FROM production_reports pr
ORDER BY pr.id;

PRINT '✅ ข้อมูลทดสอบถูกสร้างเรียบร้อยแล้ว!';
PRINT '🎯 ตอนนี้ระบบสรุปผลการผลิตจะแสดงข้อมูลที่ถูกต้อง';