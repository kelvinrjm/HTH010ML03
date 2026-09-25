-- Migration: 0001_create_schema_version
-- Description: Create the schema_version tracking table.

CREATE TABLE IF NOT EXISTS schema_version (
    version       INTEGER PRIMARY KEY,
    description   TEXT    NOT NULL,
    applied_at    TEXT    NOT NULL   -- ISO-8601 UTC timestamp
);
