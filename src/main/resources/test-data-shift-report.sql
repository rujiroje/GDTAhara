-- Test data for Daily Shift Report
-- วันที่ 2025-09-15 สำหรับทดสอบการแยกข้อมูลตามกะ

-- 1. สร้าง Production Report สำหรับการทดสอบ
INSERT INTO production_reports (order_number, start_date, end_date, status, machine_id, product_id, target_qty, created_at)
VALUES 
('TEST-SHIFT-2025-09-15', '2025-09-15', '2025-09-15', 'IN_PROGRESS', 1, 1, 10000, '2025-09-15 02:00:00');

-- 2. ข้อมูล NG Logs สำหรับกะกลางวัน (03:00-15:00)
-- สมมติว่า production report ID = 1 (ปรับตามความเป็นจริง)
INSERT INTO ng_logs (report_id, ng_type_id, user_id, quantity, source, timestamp)
VALUES 
-- กะกลางวัน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, 1, 15, 'Manual', '2025-09-15 08:30:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, 1, 8, 'Manual', '2025-09-15 10:45:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, 1, 12, 'Manual', '2025-09-15 13:20:00'),

-- กะกลางคืน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, 1, 22, 'Manual', '2025-09-15 18:15:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 3, 1, 18, 'Manual', '2025-09-15 21:30:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, 1, 9, 'Manual', '2025-09-16 01:45:00');

-- 3. ข้อมูล Downtime Events สำหรับแต่ละกะ
INSERT INTO downtime_events (report_id, technician_id, start_time, end_time, reason)
VALUES 
-- กะกลางวัน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, '2025-09-15 09:15:00', '2025-09-15 09:35:00', 'เปลี่ยนแม่พิมพ์'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, '2025-09-15 12:00:00', '2025-09-15 12:30:00', 'พักกลางวัน'),

-- กะกลางคืน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, '2025-09-15 19:30:00', '2025-09-15 20:00:00', 'ซ่อมเครื่อง'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, '2025-09-15 23:45:00', '2025-09-16 00:15:00', 'เปลี่ยนวัตถุดิบ');

-- 4. ข้อมูล Material Usage Logs สำหรับแต่ละกะ
INSERT INTO material_usage_logs (report_id, technician_id, material_code, lot_number, quantity_kg, timestamp)
VALUES 
-- กะกลางวัน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, 'PP-001', 'LOT20250915-DAY', 450.5, '2025-09-15 08:30:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 1, 'AD-002', 'LOT20250915-AD', 25.8, '2025-09-15 11:15:00'),

-- กะกลางคืน
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, 'PP-001', 'LOT20250915-NIGHT', 520.3, '2025-09-15 18:45:00'),
((SELECT TOP 1 id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15'), 2, 'COLOR-003', 'LOT20250915-COL', 15.2, '2025-09-15 22:30:00');

-- คำสั่งเพื่อตรวจสอบข้อมูลที่สร้าง
-- SELECT * FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15';
-- SELECT * FROM ng_logs WHERE report_id = (SELECT id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15');
-- SELECT * FROM downtime_events WHERE report_id = (SELECT id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15');
-- SELECT * FROM material_usage_logs WHERE report_id = (SELECT id FROM production_reports WHERE order_number = 'TEST-SHIFT-2025-09-15');