-- Add separate SQ/SS pieces columns if missing
IF COL_LENGTH('parameter_records', 'sq_pieces') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD sq_pieces INT NULL;
END
GO

IF COL_LENGTH('parameter_records', 'ss_pieces') IS NULL
BEGIN
    ALTER TABLE parameter_records ADD ss_pieces INT NULL;
END
GO

PRINT 'Added sq_pieces and ss_pieces columns if they were missing.';
