-- Migration: 0010_create_sales
-- Description: Create sales table for transaction orders/invoices.

CREATE TABLE sales (
    sale_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id     INTEGER NOT NULL
                     REFERENCES stores (store_id)
                     ON DELETE RESTRICT,
    customer_id  INTEGER
                     REFERENCES customers (customer_id)
                     ON DELETE RESTRICT,
    sale_date    TEXT    NOT NULL,   -- ISO-8601 date (YYYY-MM-DD)
    total_amount REAL    NOT NULL DEFAULT 0.0 CHECK (total_amount >= 0),
    channel      TEXT    NOT NULL CHECK (channel IN ('in-store', 'online', 'wholesale')),
    created_at   TEXT    NOT NULL    -- ISO-8601 UTC
);

CREATE INDEX idx_sales_store_id    ON sales (store_id);
CREATE INDEX idx_sales_customer_id ON sales (customer_id);
CREATE INDEX idx_sales_sale_date   ON sales (sale_date);
