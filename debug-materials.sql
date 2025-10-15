-- =================================================================
-- ตรวจสอบข้อมูล Materials ในฐานข้อมูล
-- =================================================================

-- ตรวจสอบ Materials ทั้งหมด
SELECT id, material_code, material_name, material_type, unit, created_at
FROM Material
ORDER BY material_type, material_code;

-- ตรวจสอบ Material Types ที่มีอยู่
SELECT DISTINCT material_type, COUNT(*) as count
FROM Material
GROUP BY material_type
ORDER BY material_type;

-- ตรวจสอบ Materials ตาม Type ที่ Frontend ใช้
SELECT material_type, material_code, material_name
FROM Material
WHERE material_type IN ('VIRGIN', 'ADMER', 'EVOH', 'MIX')
ORDER BY material_type, material_code;

-- ตรวจสอบว่ามี Materials อื่นๆ ที่ไม่ตรงกับ Frontend filter
SELECT material_type, material_code, material_name
FROM Material
WHERE material_type NOT IN ('VIRGIN', 'ADMER', 'EVOH', 'MIX')
ORDER BY material_type, material_code;