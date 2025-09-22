-- =================================================================
-- SQL Server Query สำหรับตรวจสอบข้อมุล Materials
-- สำหรับแก้ปัญหา CM Operator Material Selection
-- =================================================================

-- 1. ตรวจสอบ Table Structure
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'materials'
ORDER BY ORDINAL_POSITION;

-- 2. ตรวจสอบจำนวน Materials ทั้งหมด
SELECT COUNT(*) as total_materials FROM materials;

-- 3. ตรวจสอบ Material Types ที่มีอยู่
SELECT 
    materialType, 
    COUNT(*) as count
FROM materials
GROUP BY materialType
ORDER BY materialType;

-- 4. ดูข้อมูล Sample Materials
SELECT TOP 10
    id,
    materialCode,
    materialName,
    materialType,
    unit
FROM materials
ORDER BY id;

-- 5. ตรวจสอบว่ามี NULL materialType หรือไม่
SELECT COUNT(*) as null_material_types
FROM materials
WHERE materialType IS NULL OR materialType = '';

-- 6. ตรวจสอบว่าไม่มีข้อมูล Materials หรือไม่
IF NOT EXISTS (SELECT 1 FROM materials)
BEGIN
    PRINT 'ไม่มีข้อมูล Materials ในฐานข้อมูล - จำเป็นต้องเพิ่มข้อมูลทดสอบ'
    
    -- เพิ่มข้อมูลทดสอบ
    INSERT INTO materials (materialCode, materialName, materialType, unit)
    VALUES 
        ('PE001', 'PE ไวร์จิน', 'VIRGIN', 'KG'),
        ('PE002', 'PE ไวร์จิน ชนิด 2', 'VIRGIN', 'KG'),
        ('AD001', 'ADMER สำหรับการยึดติด', 'ADMER', 'KG'),
        ('AD002', 'ADMER ชนิดพิเศษ', 'ADMER', 'KG'),
        ('EV001', 'EVOH สำหรับกันออกซิเจน', 'EVOH', 'KG'),
        ('EV002', 'EVOH ชั้นบาง', 'EVOH', 'KG'),
        ('MX001', 'วัสดุผสม ทั่วไป', 'MIX', 'KG'),
        ('MX002', 'วัสดุผสม พิเศษ', 'MIX', 'KG');
        
    PRINT 'เพิ่มข้อมูล Materials ทดสอบเรียบร้อยแล้ว'
END
ELSE
BEGIN
    PRINT 'พบข้อมูล Materials ในฐานข้อมูลแล้ว'
END

-- 7. ตรวจสอบข้อมูลหลังการปรับปรุง
SELECT 
    materialType,
    COUNT(*) as count,
    STRING_AGG(materialCode, ', ') as material_codes
FROM materials
WHERE materialType IS NOT NULL AND materialType != ''
GROUP BY materialType
ORDER BY materialType;