-- V28: T-03 — Realtime Tech Help Request Notification
-- Tables: help_request, help_request_event
-- Supports: WebSocket broadcast, OTP arrival verification (O-10 pattern)

CREATE TABLE help_request (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    machine_id          NVARCHAR(20)    NOT NULL,
    requester_id        NVARCHAR(50)    NOT NULL,   -- Op username
    requester_role      NVARCHAR(20)    NOT NULL,   -- OPERATOR | LEADER
    position            NVARCHAR(10)    NULL,       -- LEFT | RIGHT
    category            NVARCHAR(30)    NOT NULL,   -- SETUP | QUALITY | MECHANICAL | SAFETY | OTHER
    urgency             NVARCHAR(10)    NOT NULL,   -- LOW | MED | HIGH | URGENT
    message             NVARCHAR(1000)  NULL,
    photo_ids           NVARCHAR(MAX)   NULL,       -- JSON array of attachment_photo.id

    -- Status tracking
    status              NVARCHAR(20)    NOT NULL DEFAULT N'PENDING',
    assigned_to         NVARCHAR(50)    NULL,       -- Tech username after accept

    -- Timing
    requested_at        DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
    notified_at         DATETIME2       NULL,
    accepted_at         DATETIME2       NULL,
    arrived_at          DATETIME2       NULL,       -- confirmed via OTP
    resolved_at         DATETIME2       NULL,
    decline_reason      NVARCHAR(500)   NULL,

    -- OTP arrival (linked with O-10)
    arrival_otp         NVARCHAR(6)     NULL,
    arrival_otp_expiry  DATETIME2       NULL,
    arrival_verified    BIT             NOT NULL DEFAULT 0,

    CONSTRAINT CK_hr_status   CHECK (status   IN ('PENDING','ACCEPTED','IN_PROGRESS','RESOLVED','DECLINED','ESCALATED')),
    CONSTRAINT CK_hr_urgency  CHECK (urgency  IN ('LOW','MED','HIGH','URGENT')),
    CONSTRAINT CK_hr_category CHECK (category IN ('SETUP','QUALITY','MECHANICAL','SAFETY','OTHER')),
    CONSTRAINT CK_hr_position CHECK (position IS NULL OR position IN ('LEFT','RIGHT'))
);

CREATE INDEX IX_hr_status   ON help_request(status);
CREATE INDEX IX_hr_machine  ON help_request(machine_id, status);
CREATE INDEX IX_hr_assigned ON help_request(assigned_to, status);
CREATE INDEX IX_hr_time     ON help_request(requested_at);

-- ============================================================
-- Audit / event log
-- ============================================================
CREATE TABLE help_request_event (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    request_id  BIGINT          NOT NULL,
    event_type  NVARCHAR(30)    NOT NULL,   -- CREATED|NOTIFIED|ACCEPTED|SNOOZED|DECLINED|ARRIVED|RESOLVED|ESCALATED
    actor_id    NVARCHAR(50)    NOT NULL,
    actor_role  NVARCHAR(20)    NOT NULL,
    note        NVARCHAR(500)   NULL,
    event_at    DATETIME2       NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_hre_request FOREIGN KEY (request_id) REFERENCES help_request(id) ON DELETE CASCADE
);

CREATE INDEX IX_hre_request ON help_request_event(request_id, event_at);
