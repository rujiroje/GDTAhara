-- =================================================================
-- Test User Creation for CM Operator
-- =================================================================

-- สร้าง test user สำหรับ CM Operator (password: test123)
-- BCrypt hash ของ "test123" คือ $2a$10$N.zmdr/YzI8aY6N2xBfC3eE/TSLB5CBU4WCQ8DXnzUWwpKjb.eMTq
INSERT INTO Users (username, password_hash, role, full_name, is_active, created_at, updated_at)
VALUES 
    ('cmoperator', '$2a$10$N.zmdr/YzI8aY6N2xBfC3eE/TSLB5CBU4WCQ8DXnzUWwpKjb.eMTq', 'CM Operator', 'Test CM Operator', 1, GETDATE(), GETDATE()),
    ('cmop2', '$2a$10$N.zmdr/YzI8aY6N2xBfC3eE/TSLB5CBU4WCQ8DXnzUWwpKjb.eMTq', 'CM Operator', 'CM Operator 2', 1, GETDATE(), GETDATE());

-- ตรวจสอบ CM Operator users
SELECT id, username, role, full_name, is_active 
FROM Users 
WHERE role = 'CM Operator';