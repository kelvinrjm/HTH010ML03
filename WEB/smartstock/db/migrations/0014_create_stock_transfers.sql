-- Migration: 0014_create_stock_transfers
-- Description: Create stock_transfers table to track inventory movements between stores.

CREATE TABLE stock_transfers (
    transfer_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    from_store_id INTEGER NOT NULL
                      REFERENCES stores   (store_id)
                      ON DELETE RESTRICT,
    to_store_id   INTEGER NOT NULL
                      REFERENCES stores   (store_id)
                      ON DELETE RESTRICT,
    product_id    INTEGER NOT NULL
                      REFERENCES products (product_id)
                      ON DELETE RESTRICT,
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    transfer_date TEXT    NOT NULL,   -- YYYY-MM-DD
    status        TEXT    NOT NULL DEFAULT 'completed'
                      CHECK (status IN ('pending', 'in_transit', 'completed', 'cancelled')),
    actor         TEXT    NOT NULL DEFAULT 'system',
    notes         TEXT    NOT NULL DEFAULT '',
    created_at    TEXT    NOT NULL,   -- ISO-8601 UTC

    CHECK (from_store_id <> to_store_id)
);

CREATE INDEX idx_stock_transfers_from_store ON stock_transfers (from_store_id);
CREATE INDEX idx_stock_transfers_to_store   ON stock_transfers (to_store_id);
CREATE INDEX idx_stock_transfers_product    ON stock_transfers (product_id);
CREATE INDEX idx_stock_transfers_date       ON stock_transfers (transfer_date);
