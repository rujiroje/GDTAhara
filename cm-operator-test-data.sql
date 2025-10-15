-- =================================================================
-- File: cm-operator-test-data.sql
-- ไฟล์สำหรับเตรียมข้อมูลทดสอบ CM Operator
-- =================================================================

-- เพิ่มข้อมูล Material ตัวอย่างสำหรับ CM Operator
INSERT INTO Material (material_code, material_name, material_type, description, unit, created_at, updated_at)
VALUES 
    ('PE001', 'PE ไวร์จิน', 'VIRGIN', 'เม็ดพลาสติก PE สำหรับชั้นในของขวด', 'KG', GETDATE(), GETDATE()),
    ('AD001', 'ADMER สำหรับการยึดติด', 'ADMER', 'วัสดุยึดติดระหว่างชั้น', 'KG', GETDATE(), GETDATE()),
    ('EV001', 'EVOH สำหรับกันออกซิเจน', 'EVOH', 'วัสดุกันออกซิเจนชั้นกลาง', 'KG', GETDATE(), GETDATE()),
    ('MX001', 'วัสดุผสม', 'MIX', 'วัสดุผสมสำหรับการผลิต', 'KG', GETDATE(), GETDATE());

-- เพิ่มข้อมูล Stock เริ่มต้นสำหรับแต่ละ Material
INSERT INTO MaterialStockTransaction (material_id, transaction_type, quantity, lot_number, timestamp, reference_number, user_id)
VALUES 
    -- Material PE001 (Virgin)
    (1, 'IN', 1000.00, 'LOT001PE', GETDATE(), 'INV-2025-001', 1),
    (1, 'IN', 1500.00, 'LOT002PE', GETDATE(), 'INV-2025-002', 1),
    (1, 'IN', 800.00, 'LOT003PE', GETDATE(), 'INV-2025-003', 1),
    
    -- Material AD001 (Admer)
    (2, 'IN', 500.00, 'LOT001AD', GETDATE(), 'INV-2025-004', 1),
    (2, 'IN', 300.00, 'LOT002AD', GETDATE(), 'INV-2025-005', 1),
    
    -- Material EV001 (EVOH)
    (3, 'IN', 200.00, 'LOT001EV', GETDATE(), 'INV-2025-006', 1),
    (3, 'IN', 250.00, 'LOT002EV', GETDATE(), 'INV-2025-007', 1),
    
    -- Material MX001 (Mix)
    (4, 'IN', 600.00, 'LOT001MX', GETDATE(), 'INV-2025-008', 1),
    (4, 'IN', 400.00, 'LOT002MX', GETDATE(), 'INV-2025-009', 1);

-- ข้อมูลการทดสอบ: กรณีมีการใช้งานไปแล้วบางส่วน
INSERT INTO MaterialStockTransaction (material_id, transaction_type, quantity, lot_number, timestamp, reference_number, user_id, production_report_id)
VALUES 
    -- การใช้งาน PE001
    (1, 'OUT', 100.00, 'LOT001PE', DATEADD(HOUR, 1, GETDATE()), 'USE-2025-001', 1, 1),
    (1, 'OUT', 150.00, 'LOT001PE', DATEADD(HOUR, 2, GETDATE()), 'USE-2025-002', 1, 1),
    
    -- การใช้งาน AD001
    (2, 'OUT', 50.00, 'LOT001AD', DATEADD(HOUR, 1, GETDATE()), 'USE-2025-003', 1, 1),
    
    -- การใช้งาน EVOH
    (3, 'OUT', 25.00, 'LOT001EV', DATEADD(HOUR, 1, GETDATE()), 'USE-2025-004', 1, 1);

-- เพิ่มข้อมูล MaterialUsageLog เพื่อให้สอดคล้องกัน (ใช้ field names ตาม Model)
INSERT INTO material_usage_logs (report_id, technician_id, material_code, lot_number, quantity_kg, timestamp)
VALUES 
    (1, 1, 'PE001', 'LOT001PE', 100.00, DATEADD(HOUR, 1, GETDATE())),
    (1, 1, 'PE001', 'LOT001PE', 150.00, DATEADD(HOUR, 2, GETDATE())),
    (1, 1, 'AD001', 'LOT001AD', 50.00, DATEADD(HOUR, 1, GETDATE())),
    (1, 1, 'EV001', 'LOT001EV', 25.00, DATEADD(HOUR, 1, GETDATE()));

-- แสดงข้อมูลสรุป
SELECT 
    m.material_code,
    m.material_name,
    m.material_type,
    SUM(CASE WHEN mst.transaction_type = 'IN' THEN mst.quantity ELSE -mst.quantity END) as current_balance
FROM Material m
LEFT JOIN MaterialStockTransaction mst ON m.id = mst.material_id
GROUP BY m.id, m.material_code, m.material_name, m.material_type
ORDER BY m.material_type, m.material_code;

-- แสดงข้อมูล Lot Numbers ที่มีสำหรับแต่ละ Material
SELECT 
    m.material_code,
    m.material_name,
    mst.lot_number,
    SUM(CASE WHEN mst.transaction_type = 'IN' THEN mst.quantity ELSE -mst.quantity END) as lot_balance
FROM Material m
JOIN MaterialStockTransaction mst ON m.id = mst.material_id
GROUP BY m.id, m.material_code, m.material_name, mst.lot_number
HAVING SUM(CASE WHEN mst.transaction_type = 'IN' THEN mst.quantity ELSE -mst.quantity END) > 0
ORDER BY m.material_code, mst.lot_number;