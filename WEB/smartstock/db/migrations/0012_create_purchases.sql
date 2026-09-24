-- Migration: 0012_create_purchases
-- Description: Create purchases table for supplier purchase orders.

CREATE TABLE purchases (
    purchase_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id               INTEGER NOT NULL
                               REFERENCES stores    (store_id)
                               ON DELETE RESTRICT,
    supplier_id            INTEGER NOT NULL
                               REFERENCES suppliers (supplier_id)
                               ON DELETE RESTRICT,
    order_date             TEXT    NOT NULL,   -- YYYY-MM-DD
    expected_delivery_date TEXT    NOT NULL,   -- YYYY-MM-DD
    status                 TEXT    NOT NULL DEFAULT 'draft'
                               CHECK (status IN ('draft', 'submitted', 'received', 'cancelled')),
    created_at             TEXT    NOT NULL    -- ISO-8601 UTC
);

CREATE INDEX idx_purchases_store_id    ON purchases (store_id);
CREATE INDEX idx_purchases_supplier_id ON purchases (supplier_id);
CREATE INDEX idx_purchases_status      ON purchases (status);
