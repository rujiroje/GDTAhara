-- V17: Add note column to ng_logs for "ปัญหาอื่นๆ" free-text detail
ALTER TABLE ng_logs ADD note NVARCHAR(500) NULL;
