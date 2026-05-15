-- Migration: Add category and downtime_type to downtime_events
-- Run on SQL Server: GDTahara database
-- Safe to run: adds nullable columns only, no existing data is affected

ALTER TABLE downtime_events
    ADD category NVARCHAR(50) NULL;

ALTER TABLE downtime_events
    ADD downtime_type NVARCHAR(100) NULL;

-- category values: 'PLANNED' | 'UNPLANNED' | 'EXTERNAL'
-- downtime_type values (examples):
--   PLANNED   -> 'PM', 'MOLD_CHANGE', 'SETUP', 'CLEANING', 'TRIAL'
--   UNPLANNED -> 'BREAKDOWN', 'MATERIAL_SHORT', 'QUALITY_HOLD', 'OPERATOR_ABSENT'
--   EXTERNAL  -> 'NO_ORDER', 'WAITING_MATERIAL', 'UTILITY_FAILURE'
