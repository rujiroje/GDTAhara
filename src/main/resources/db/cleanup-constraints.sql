-- Database Cleanup Script for Unique Constraint Issues

-- ลบ unique constraints ที่ขัดแย้งกัน
BEGIN TRY
    -- ลบ constraint ของ products table
    IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ__products__AE1A8CC4E5C48C1A' AND type = 'UQ')
        ALTER TABLE products DROP CONSTRAINT UQ__products__AE1A8CC4E5C48C1A;
    
    -- ลบ constraint ของ users table  
    IF EXISTS (SELECT * FROM sys.objects WHERE name = 'UQ__users__F3DBC572EE38F3CB' AND type = 'UQ')
        ALTER TABLE users DROP CONSTRAINT UQ__users__F3DBC572EE38F3CB;
    
    PRINT 'Unique constraints cleanup completed successfully';
END TRY
BEGIN CATCH
    PRINT 'Error during cleanup: ' + ERROR_MESSAGE();
END CATCH;

-- สร้าง constraints ใหม่ด้วยชื่อที่เหมาะสม
BEGIN TRY
    -- สร้าง unique constraint สำหรับ products.product_code
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UK_products_product_code')
        ALTER TABLE products ADD CONSTRAINT UK_products_product_code UNIQUE (product_code);
    
    -- สร้าง unique constraint สำหรับ users.username
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UK_users_username')
        ALTER TABLE users ADD CONSTRAINT UK_users_username UNIQUE (username);
    
    PRINT 'New unique constraints created successfully';
END TRY
BEGIN CATCH
    PRINT 'Error creating new constraints: ' + ERROR_MESSAGE();
END CATCH;
