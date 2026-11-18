-- BUG-06: materials.material_name stored as '?' for Thai characters
-- because data was inserted before V22 altered columns to NVARCHAR.
-- bom_item.rm_name was imported after NVARCHAR conversion and retains correct Thai text.
-- Fix: copy the correct rm_name from bom_item back into materials.

UPDATE m
SET m.material_name = bi_fix.rm_name
FROM materials m
INNER JOIN (
    SELECT rm_code, MAX(rm_name) AS rm_name
    FROM bom_item
    WHERE rm_name IS NOT NULL
      AND rm_name NOT LIKE '%?%'
      AND LEN(LTRIM(RTRIM(rm_name))) > 0
    GROUP BY rm_code
) bi_fix ON m.material_code = bi_fix.rm_code
WHERE m.material_name LIKE '%?%'
   OR m.material_name IS NULL;
