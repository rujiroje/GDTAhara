CREATE TABLE IF NOT EXISTS parameter_records (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id BIGINT NOT NULL,
    record_type VARCHAR(50) DEFAULT 'STANDARD',
    timestamp DATETIME2 DEFAULT GETDATE(),
    
    -- Extruder Screw parameters - Main
    extruder_main_screw_rpm DECIMAL(10,2),
    extruder_main_lo_limit DECIMAL(10,2),
    extruder_main_resin_press DECIMAL(10,2),
    extruder_main_resin_temp DECIMAL(10,2),
    extruder_main_motor_current DECIMAL(10,2),
    
    -- Extruder Screw parameters - Virgin
    extruder_virgin_screw_rpm DECIMAL(10,2),
    extruder_virgin_lo_limit DECIMAL(10,2),
    extruder_virgin_resin_press DECIMAL(10,2),
    extruder_virgin_resin_temp DECIMAL(10,2),
    extruder_virgin_motor_current DECIMAL(10,2),
    
    -- Temperature parameters - Main
    temp_main_fb DECIMAL(10,2),
    temp_main_c1 DECIMAL(10,2),
    temp_main_c2 DECIMAL(10,2),
    temp_main_c3 DECIMAL(10,2),
    
    -- Other checks
    check_emergency_sw_status VARCHAR(10),
    check_sq_ss_pieces INT,
    
    -- Foreign key constraint
    CONSTRAINT FK_parameter_record_report FOREIGN KEY (report_id) REFERENCES production_reports(id)
);

-- Create indexes for performance
CREATE INDEX IDX_parameter_record_report_id ON parameter_records(report_id);
CREATE INDEX IDX_parameter_record_timestamp ON parameter_records(timestamp DESC);
