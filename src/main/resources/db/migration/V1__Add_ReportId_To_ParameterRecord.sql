-- เพิ่ม reportId column ใน parameter_records table ถ้ายังไม่มี

ALTER TABLE parameter_records 
ADD COLUMN report_id BIGINT;

-- เพิ่ม foreign key constraint
ALTER TABLE parameter_records 
ADD CONSTRAINT FK_parameter_record_report 
FOREIGN KEY (report_id) REFERENCES production_reports(id);

-- สร้าง index สำหรับ performance
CREATE INDEX IDX_parameter_record_report_id ON parameter_records(report_id);
CREATE INDEX IDX_parameter_record_timestamp ON parameter_records(timestamp);
