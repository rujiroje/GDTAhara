-- สร้างข้อมูลตัวอย่างสำหรับ ScrapWeightLog
-- เพื่อทดสอบการแสดงตารางสรุปของเสีย จาก Technician

-- ตรวจสอบ Production Reports ที่มีอยู่
SELECT TOP 5 id, order_number, machine_id, product_id, start_date, end_date 
FROM production_reports 
ORDER BY id DESC;

-- แทรกข้อมูล Scrap Weight Log ตัวอย่าง (ใช้ report_id ที่มีอยู่)
-- แทนที่ 1, 2, 3 ด้วย report_id ที่มีจริงในฐานข้อมูล

DECLARE @reportId BIGINT = (SELECT TOP 1 id FROM production_reports ORDER BY id DESC);

INSERT INTO scrap_weight_logs (report_id, scrap_type, mat_type, weight_kg, technician, timestamp, created_at, updated_at)
VALUES 
-- PE Materials
(@reportId, 'ถุงสีแดง', 'PE', 15.500, 'นาย ก', GETDATE(), GETDATE(), GETDATE()),
(@reportId, 'ถุงสีน้ำเงิน', 'PE', 7.800, 'นาย ข', GETDATE(), GETDATE(), GETDATE()),
(@reportId, 'ถุงใส', 'PE', 4.200, 'นาย ค', GETDATE(), GETDATE(), GETDATE()),

-- PP Materials  
(@reportId, 'ถุงสีเขียว', 'PP', 10.200, 'นาย ก', GETDATE(), GETDATE(), GETDATE()),
(@reportId, 'ถุงสีเหลือง', 'PP', 6.300, 'นาย ข', GETDATE(), GETDATE(), GETDATE()),
(@reportId, 'ถุงสีม่วง', 'PP', 3.900, 'นาย ค', GETDATE(), GETDATE(), GETDATE());

-- ตรวจสอบข้อมูลที่เพิ่มเข้าไป
SELECT * FROM scrap_weight_logs WHERE report_id = @reportId;

-- ตรวจสอบผลรวมน้ำหนักตาม scrap_type และ mat_type
SELECT 
    mat_type + ' - ' + scrap_type as combined_type,
    SUM(weight_kg) as total_weight,
    COUNT(*) as count
FROM scrap_weight_logs 
WHERE report_id = @reportId
GROUP BY mat_type, scrap_type
ORDER BY total_weight DESC;