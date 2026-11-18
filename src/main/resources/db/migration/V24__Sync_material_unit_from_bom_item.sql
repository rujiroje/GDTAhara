-- BUG-07: materials.unit was imported as 'KG' for all rows.
-- bom_item.unit contains the correct SAP unit per material (PCS, G, M, SHT, BOX, EA, MM).
-- Fix: set materials.unit from the most-used bom_item.unit for each rm_code.
-- For ties, ROW_NUMBER picks one deterministically; FOIL-BK32 tie (M vs MM) resolved to M below.

UPDATE m
SET m.unit = bi_fix.unit
FROM materials m
INNER JOIN (
    SELECT rm_code, unit
    FROM (
        SELECT
            rm_code,
            unit,
            ROW_NUMBER() OVER (
                PARTITION BY rm_code
                ORDER BY COUNT(*) DESC, unit ASC   -- unit ASC breaks ties: M < MM, KG < PCS
            ) AS rn
        FROM bom_item
        WHERE unit IS NOT NULL AND LEN(LTRIM(RTRIM(unit))) > 0
        GROUP BY rm_code, unit
    ) ranked
    WHERE rn = 1
) bi_fix ON m.material_code = bi_fix.rm_code
WHERE m.unit <> bi_fix.unit OR m.unit IS NULL;
