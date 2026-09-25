-- Migration: 0013_create_purchase_items
-- Description: Create purchase_items table holding line items for purchase orders.

CREATE TABLE purchase_items (
    purchase_item_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id       INTEGER NOT NULL
                          REFERENCES purchases (purchase_id)
                          ON DELETE CASCADE,
    product_id        INTEGER NOT NULL
                          REFERENCES products  (product_id)
                          ON DELETE RESTRICT,
    quantity_ordered  INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_received INTEGER NOT NULL DEFAULT 0
                          CHECK (quantity_received >= 0),
    unit_cost         REAL    NOT NULL CHECK (unit_cost >= 0),
    created_at        TEXT    NOT NULL,   -- ISO-8601 UTC

    CHECK (quantity_received <= quantity_ordered)
);

CREATE INDEX idx_purchase_items_purchase_id ON purchase_items (purchase_id);
CREATE INDEX idx_purchase_items_product_id  ON purchase_items (product_id);
