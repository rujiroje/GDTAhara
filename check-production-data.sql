-- Check if we have production reports data
SELECT COUNT(*) as total_reports FROM production_reports;

-- Check date range of existing data
SELECT 
    MIN(start_date) as earliest_date,
    MAX(start_date) as latest_date,
    COUNT(*) as total_reports
FROM production_reports;

-- Check specific data for September 2025
SELECT 
    id,
    order_number,
    start_date,
    end_date,
    machine_id,
    product_id,
    target_qty
FROM production_reports 
WHERE start_date >= '2025-09-01' 
  AND start_date <= '2025-09-30'
ORDER BY start_date DESC;

-- Check machine and product data
SELECT DISTINCT 
    m.machine_name,
    p.product_code,
    p.product_name
FROM production_reports pr
LEFT JOIN machines m ON pr.machine_id = m.id
LEFT JOIN products p ON pr.product_id = p.id
WHERE pr.start_date >= '2025-09-01' 
  AND pr.start_date <= '2025-09-30';