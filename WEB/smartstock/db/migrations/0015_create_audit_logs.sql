-- Migration: 0015_create_audit_logs
-- Description: Create audit_logs table with append-only triggers to record all state changes.

CREATE TABLE audit_logs (
    audit_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type   TEXT    NOT NULL
                     CHECK (length(event_type) BETWEEN 1 AND 100),
    entity_type  TEXT    NOT NULL,
    entity_id    TEXT    NOT NULL,   -- String or numeric ID
    actor        TEXT    NOT NULL
                     CHECK (length(actor) BETWEEN 1 AND 255),
    occurred_at  TEXT    NOT NULL,   -- ISO-8601 UTC timestamp
    before_state TEXT,               -- JSON text or NULL for creation events
    after_state  TEXT,               -- JSON text or NULL for deletion events
    notes        TEXT    NOT NULL DEFAULT ''
);

CREATE INDEX idx_audit_logs_entity_type  ON audit_logs (entity_type);
CREATE INDEX idx_audit_logs_entity_id    ON audit_logs (entity_id);
CREATE INDEX idx_audit_logs_event_type   ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_occurred_at  ON audit_logs (occurred_at);
CREATE INDEX idx_audit_logs_actor        ON audit_logs (actor);

-- Append-only enforcement via SQLite triggers
CREATE TRIGGER audit_logs_no_update
BEFORE UPDATE ON audit_logs
BEGIN
    SELECT RAISE(ABORT, 'audit_logs is append-only: UPDATE not permitted');
END;

CREATE TRIGGER audit_logs_no_delete
BEFORE DELETE ON audit_logs
BEGIN
    SELECT RAISE(ABORT, 'audit_logs is append-only: DELETE not permitted');
END;
