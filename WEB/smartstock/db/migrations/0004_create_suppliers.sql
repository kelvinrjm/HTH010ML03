-- Migration: 0004_create_suppliers
-- Description: Create suppliers table with default lead time and contact information.

CREATE TABLE suppliers (
    supplier_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   TEXT    NOT NULL,
    contact_info           TEXT    NOT NULL DEFAULT '',
    default_lead_time_days INTEGER NOT NULL DEFAULT 7
                               CHECK (default_lead_time_days BETWEEN 1 AND 365),
    is_active              INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at             TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_suppliers_is_active ON suppliers (is_active);
