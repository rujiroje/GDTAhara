-- V15: Add machine_type to ng_types and machines tables
-- Insert ASB and Tahara specific NG types

-- Batch 1: DDL — add columns first
ALTER TABLE ng_types ADD machine_type VARCHAR(20) NULL;
ALTER TABLE machines ADD machine_type VARCHAR(20) NULL;

GO

-- Batch 2: DML — columns now exist, safe to reference them
IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-001')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-001', N'ก้นดับเบิ้ลช๊อต', 'Double-Shot Bottom', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-002')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-002', N'ขวดเป็นคลื่น', 'Wavy Bottle', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-003')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-003', N'เกลียวยุบ', 'Collapsed Thread', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-004')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-004', N'ตะเข็บเป็นรอย', 'Seam Mark', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-005')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-005', N'ก้นเอียง', 'Tilted Bottom', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-006')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-006', N'ก้นเป็นติ่ง', 'Bottom Protrusion', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-007')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-007', N'คอขวดโดนหนีบ', 'Pinched Neck', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-TAH-008')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-TAH-008', N'ก้นขวดโดนหนีบ', 'Pinched Bottom', 'Operator', 'TAHARA');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-ASB-001')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-ASB-001', N'ก้นนิ่ม', 'Soft Bottom', 'Operator', 'ASB');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-ASB-002')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-ASB-002', N'โฟร์มาร์ค', 'Flow Mark', 'Operator', 'ASB');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-ASB-003')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-ASB-003', N'วงแหวนคอขวด', 'Neck Ring', 'Operator', 'ASB');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-ASB-004')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-ASB-004', N'เส้นลึก', 'Deep Line', 'Operator', 'ASB');

IF NOT EXISTS (SELECT 1 FROM ng_types WHERE ng_code = 'NG-ASB-005')
    INSERT INTO ng_types (ng_code, ng_description_th, ng_description_en, ng_type, machine_type)
    VALUES ('NG-ASB-005', N'ฝุ่นติด', 'Dust Contamination', 'Operator', 'ASB');
