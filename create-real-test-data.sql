-- สร้างข้อมูลจริงใน ScrapWeightLog สำหรับการทดสอบ
-- ข้อมูลน้ำหนักของเสีย PE/PP จาก Technician

-- สร้าง Technician users ก่อน (ถ้ายังไม่มี)
INSERT INTO users (username, password, role, created_at, updated_at)
SELECT 'Technician-1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TECHNICIAN', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'Technician-1')
UNION ALL
SELECT 'Technician-2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TECHNICIAN', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'Technician-2');

GO

-- แทรกข้อมูลจริงสำหรับการทดสอบ (ใช้วันที่ที่เรากำลังทดสอบ)
INSERT INTO scrap_weight_logs (
    report_id, 
    scrap_type, 
    mat_type, 
    weight_kg, 
    technician_id, 
    timestamp
) 
SELECT 
    pr.id,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 1 THEN 'ถุงสีแดง'
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 2 THEN 'ถุงสีเขียว'
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 3 THEN 'ถุงสีน้ำเงิน'
        ELSE 'ถุงสีเหลือง'
    END as scrap_type,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 2 = 1 THEN 'PE'
        ELSE 'PP'
    END as mat_type,
    CASE 
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 1 THEN 15.500
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 2 THEN 10.200
        WHEN ROW_NUMBER() OVER (ORDER BY pr.id) % 4 = 3 THEN 7.800
        ELSE 6.300
    END as weight_kg,
    u.id as technician_id,
    GETDATE() as timestamp
FROM production_reports pr
CROSS JOIN (SELECT TOP 1 id FROM users WHERE role = 'TECHNICIAN') u
WHERE pr.start_date >= CAST(GETDATE() - 30 AS DATE) -- Last 30 days
AND NOT EXISTS (
    SELECT 1 FROM scrap_weight_logs swl 
    WHERE swl.report_id = pr.id
);

GO

-- ตรวจสอบข้อมูลที่เพิ่มเข้าไป
SELECT 
    swl.mat_type + ' - ' + swl.scrap_type as material_type,
    SUM(swl.weight_kg) as total_weight,
    COUNT(*) as count,
    pr.start_date
FROM scrap_weight_logs swl
JOIN production_reports pr ON swl.report_id = pr.id
WHERE pr.start_date >= CAST(GETDATE() - 7 AS DATE)
GROUP BY swl.mat_type, swl.scrap_type, pr.start_date
ORDER BY pr.start_date DESC, total_weight DESC;

GO

-- สร้าง Shift Leader users หากยังไม่มี (ใช้ role ตามจริงใน database)
INSERT INTO users (username, password, role, created_at, updated_at)
SELECT 'หัวหน้ากะเช้า', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Shift Leader', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'หัวหน้ากะเช้า')

UNION ALL

SELECT 'หัวหน้ากะดึก', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Shift Leader', GETDATE(), GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'หัวหน้ากะดึก');

GO

-- ตรวจสอบข้อมูล Shift Leaders ที่สร้าง
SELECT username, role
FROM users 
WHERE role IN ('Shift Leader', 'LD', 'SHIFT_LEADER')
ORDER BY role, username;

PRINT 'Created real test data for ScrapWeightLog and Shift Leaders';