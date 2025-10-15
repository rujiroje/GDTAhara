-- =================================================================
-- แก้ไขปัญหา Materials ไม่แสดงใน CM Operator
-- =================================================================

-- 1. ตรวจสอบ Table Structure
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'materials' OR TABLE_NAME = 'Material'
ORDER BY ORDINAL_POSITION;

-- 2. ตรวจสอบข้อมูล Materials ทั้งหมด
SELECT COUNT(*) as total_materials FROM Material;

-- 3. ตรวจสอบ Material Types
SELECT material_type, COUNT(*) as count
FROM Material
GROUP BY material_type
ORDER BY material_type;

-- 4. เพิ่มข้อมูล Materials ตัวอย่างถ้าไม่มี
-- ลบข้อมูลเก่าก่อน (ถ้ามี)
DELETE FROM Material WHERE material_code IN ('PE001', 'AD001', 'EV001', 'MX001');

-- เพิ่มข้อมูลใหม่ที่ตรงกับ Frontend filter
INSERT INTO Material (material_code, material_name, material_type, unit)
VALUES 
    ('PE001', 'PE ไวร์จิน', 'VIRGIN', 'KG'),
    ('PE002', 'PE ไวร์จิน ชนิด 2', 'VIRGIN', 'KG'),
    ('AD001', 'ADMER สำหรับการยึดติด', 'ADMER', 'KG'),
    ('AD002', 'ADMER ชนิดพิเศษ', 'ADMER', 'KG'),
    ('EV001', 'EVOH สำหรับกันออกซิเจน', 'EVOH', 'KG'),
    ('EV002', 'EVOH ชั้นบาง', 'EVOH', 'KG'),
    ('MX001', 'วัสดุผสม ทั่วไป', 'MIX', 'KG'),
    ('MX002', 'วัสดุผสม พิเศษ', 'MIX', 'KG');

-- 5. ตรวจสอบข้อมูลหลังเพิ่ม
SELECT material_code, material_name, material_type, unit
FROM Material
WHERE material_type IN ('VIRGIN', 'ADMER', 'EVOH', 'MIX')
ORDER BY material_type, material_code;

-- 6. ตรวจสอบว่ามี Materials อื่นที่ไม่ตรงกับ Frontend filter
SELECT DISTINCT material_type
FROM Material
WHERE material_type NOT IN ('VIRGIN', 'ADMER', 'EVOH', 'MIX');