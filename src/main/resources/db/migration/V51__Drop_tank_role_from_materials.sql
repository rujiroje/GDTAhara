-- C-04 redesign: tank role for materials is derived from materialType, not a separate column
ALTER TABLE materials DROP COLUMN tank_role;
