-- เพิ่ม shift column ให้ users table (รันแยกต่างหากเมื่อต้องการ)
-- สำหรับการปรับปรุงในอนาคต

-- ตรวจสอบว่ามี shift column หรือไม่
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'shift'
)
BEGIN
    -- เพิ่ม shift column
    ALTER TABLE users ADD shift VARCHAR(10) NULL;
    
    -- อัปเดตข้อมูลที่มีอยู่ตาม username pattern
    UPDATE users 
    SET shift = 'DAY' 
    WHERE role = 'SHIFT_LEADER' AND username LIKE '%เช้า%';
    
    UPDATE users 
    SET shift = 'NIGHT' 
    WHERE role = 'SHIFT_LEADER' AND username LIKE '%ดึก%';
    
    PRINT 'Added shift column and updated existing data';
END
ELSE
BEGIN
    PRINT 'Shift column already exists';
END

GO