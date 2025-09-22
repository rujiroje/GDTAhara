-- =================================================================
-- Production Reports Test Data for CM Operator
-- =================================================================

-- สร้างข้อมูล Machines ตัวอย่าง
INSERT INTO Machine (machine_name, machine_code, machine_type, description, is_active, created_at, updated_at)
VALUES 
    ('Blow Molding Machine #1', 'BM001', 'Blow Molding', 'เครื่องจักรเป่าขวดหลัก', 1, GETDATE(), GETDATE()),
    ('Blow Molding Machine #2', 'BM002', 'Blow Molding', 'เครื่องจักรเป่าขวดสำรอง', 1, GETDATE(), GETDATE()),
    ('Injection Molding Machine #1', 'IM001', 'Injection Molding', 'เครื่องฉีดขึ้นรูป', 1, GETDATE(), GETDATE());

-- สร้างข้อมูล Products ตัวอย่าง
INSERT INTO Product (product_name, product_code, product_type, description, unit, is_active, created_at, updated_at)
VALUES 
    ('PET Bottle 500ml', 'PET500', 'Bottle', 'ขวด PET ขนาด 500ml', 'PCS', 1, GETDATE(), GETDATE()),
    ('PET Bottle 1000ml', 'PET1000', 'Bottle', 'ขวด PET ขนาด 1000ml', 'PCS', 1, GETDATE(), GETDATE()),
    ('PP Container 250ml', 'PP250', 'Container', 'กล่องพลาสติก PP ขนาด 250ml', 'PCS', 1, GETDATE(), GETDATE());

-- สร้างข้อมูล Production Reports ตัวอย่าง (Active และ Completed)
INSERT INTO ProductionReport (order_number, machine_id, product_id, target_qty, actual_qty, start_date, end_date, status, created_by_user_id, created_at, updated_at)
VALUES 
    -- Active Reports (สำหรับ CM Operator เลือก)
    ('ORD-2025-001', 1, 1, 10000, 2500, CAST(GETDATE() AS DATE), NULL, 'IN_PROGRESS', 1, GETDATE(), GETDATE()),
    ('ORD-2025-002', 2, 2, 8000, 1200, CAST(GETDATE() AS DATE), NULL, 'IN_PROGRESS', 1, GETDATE(), GETDATE()),
    ('ORD-2025-003', 1, 3, 5000, 800, CAST(GETDATE() AS DATE), NULL, 'ACTIVE', 1, GETDATE(), GETDATE()),
    
    -- Completed Reports (สำหรับประวัติ)
    ('ORD-2025-004', 3, 1, 12000, 12000, DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), 'COMPLETED', 1, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE())),
    ('ORD-2025-005', 1, 2, 6000, 6000, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), 'COMPLETED', 1, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -3, GETDATE()));

-- ตรวจสอบข้อมูลที่สร้าง
SELECT 
    pr.id,
    pr.order_number,
    m.machine_name,
    p.product_name,
    pr.target_qty,
    pr.actual_qty,
    pr.start_date,
    pr.end_date,
    pr.status
FROM ProductionReport pr
JOIN Machine m ON pr.machine_id = m.id
JOIN Product p ON pr.product_id = p.id
ORDER BY pr.created_at DESC;

-- แสดงเฉพาะ Active Reports
SELECT 
    pr.id,
    pr.order_number,
    m.machine_name,
    p.product_name,
    pr.target_qty,
    pr.actual_qty,
    pr.status
FROM ProductionReport pr
JOIN Machine m ON pr.machine_id = m.id
JOIN Product p ON pr.product_id = p.id
WHERE pr.status IN ('IN_PROGRESS', 'ACTIVE')
ORDER BY pr.created_at DESC;