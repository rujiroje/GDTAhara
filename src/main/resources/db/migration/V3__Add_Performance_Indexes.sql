-- Phase 0 Hardening: Performance Indexes (IDX-1 through IDX-9)
-- All statements are idempotent (check before create).

-- IDX-1: production_reports.status  (used by findByStatus, findByStatusIn)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_pr_status' AND object_id = OBJECT_ID('production_reports'))
    CREATE INDEX idx_pr_status ON production_reports(status);

-- IDX-2: production_reports date range  (used by date-range filter queries)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_pr_dates' AND object_id = OBJECT_ID('production_reports'))
    CREATE INDEX idx_pr_dates ON production_reports(start_date, end_date);

-- IDX-3+4: ng_logs.report_id + timestamp  (used by hourly summary and report queries)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ngl_report_ts' AND object_id = OBJECT_ID('ng_logs'))
    CREATE INDEX idx_ngl_report_ts ON ng_logs(report_id, timestamp);

-- IDX-5: packaging_logs.report_id + lot_number  (used by getNextBoxNumber and packaging queries)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_pkg_report_lot' AND object_id = OBJECT_ID('packaging_logs'))
    CREATE INDEX idx_pkg_report_lot ON packaging_logs(report_id, lot_number);

-- IDX-6: material_stock_transactions.material_id  (used by getStockBalanceByMaterialId)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_mst_material' AND object_id = OBJECT_ID('material_stock_transactions'))
    CREATE INDEX idx_mst_material ON material_stock_transactions(material_id);

-- IDX-7: machine_status_logs.machine_id + end_time  (used by findByMachineIdAndEndTimeIsNull)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_msl_machine_end' AND object_id = OBJECT_ID('machine_status_logs'))
    CREATE INDEX idx_msl_machine_end ON machine_status_logs(machine_id, end_time);

-- IDX-8: downtime_events.report_id
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_dte_report' AND object_id = OBJECT_ID('downtime_events'))
    CREATE INDEX idx_dte_report ON downtime_events(report_id);

-- IDX-9: scrap_weight_logs.report_id
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_swl_report' AND object_id = OBJECT_ID('scrap_weight_logs'))
    CREATE INDEX idx_swl_report ON scrap_weight_logs(report_id);
