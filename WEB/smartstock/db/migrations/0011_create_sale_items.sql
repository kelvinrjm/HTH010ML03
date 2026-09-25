-- Migration: 0011_create_sale_items
-- Description: Create sale_items table holding individual line items for each sale.

CREATE TABLE sale_items (
    sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id      INTEGER NOT NULL
                     REFERENCES sales (sale_id)
                     ON DELETE CASCADE,
    product_id   INTEGER NOT NULL
                     REFERENCES products (product_id)
                     ON DELETE RESTRICT,
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    unit_price   REAL    NOT NULL CHECK (unit_price >= 0),
    subtotal     REAL    NOT NULL CHECK (subtotal >= 0),
    created_at   TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_sale_items_sale_id    ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items (product_id);
