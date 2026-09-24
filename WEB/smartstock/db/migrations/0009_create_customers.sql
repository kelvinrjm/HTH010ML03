-- Migration: 0009_create_customers
-- Description: Create customers table for retail and wholesale clients.

CREATE TABLE customers (
    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    email       TEXT    NOT NULL DEFAULT '',
    phone       TEXT    NOT NULL DEFAULT '',
    address     TEXT    NOT NULL DEFAULT '',
    created_at  TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_customers_name  ON customers (name);
CREATE INDEX idx_customers_email ON customers (email);
