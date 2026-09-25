-- Migration: 0007_create_stores
-- Description: Create stores table for multi-store retail and wholesale locations.

CREATE TABLE stores (
    store_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    location    TEXT    NOT NULL DEFAULT '',
    store_type  TEXT    NOT NULL CHECK (store_type IN ('retail', 'wholesale')),
    is_active   INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at  TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_stores_is_active  ON stores (is_active);
CREATE INDEX idx_stores_store_type ON stores (store_type);
