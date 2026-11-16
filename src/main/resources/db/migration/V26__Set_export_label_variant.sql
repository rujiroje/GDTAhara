-- V26: Tag export products with label_variant = 'EXPORT'
-- These products require an additional GOODS section on the packaging label.
UPDATE products SET label_variant = 'EXPORT'
WHERE product_code IN (
    'EBAJMY0130A',  -- Aji130
    'DBAJMY0130',   -- Aji130 (4 parison)
    'DBAJMY0260',   -- Aji260
    'EBAJMY0260A',  -- Aji260
    'EBAJMY0430A',  -- Aji430
    'EBAJMY0430',   -- Aji430
    'EBAJMY01000A', -- Aji 1kg
    'EBAJMY1000A',  -- Aji 1kg
    'EBAJMY01000',  -- Aji 1kg
    'DBKPMY0130',   -- QP130
    'EBKPMY0130',   -- QP130 EX
    'NBKPMY0130',   -- QP130
    'DBKPMY0310A',  -- QP310
    'DBKPMY0310',   -- QP310
    'EBKPMY0310',   -- QP310 EX
    'NBKPMY0310',   -- QP310
    'EBKPMY0520',   -- QP520
    'NBKPMY0520',   -- QP520
    'EBKPMY1000',   -- QP 1kg
    'NBKPMY1000',   -- QP 1kg
    'EBOTG0170A',   -- Otoki
    'EBOTG0170',    -- Ottogi 170
    'EBAGV0370',    -- AGV
    'EBCHG2000',    -- Handy (Gallon)
    'NBCHG02000A',  -- Handy (Gallon)
    'EBCOM0200',    -- Common 200
    'NBCOM0200'     -- Common 200
);
