-- Migration: 0008_create_inventory
-- Description: Create inventory table tracking stock per product per store.

CREATE TABLE inventory (
    inventory_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id           INTEGER NOT NULL
                           REFERENCES stores   (store_id)
                           ON DELETE RESTRICT,
    product_id         INTEGER NOT NULL
                           REFERENCES products (product_id)
                           ON DELETE RESTRICT,
    quantity_on_hand   INTEGER NOT NULL DEFAULT 0
                           CHECK (quantity_on_hand >= 0),
    quantity_reserved  INTEGER NOT NULL DEFAULT 0
                           CHECK (quantity_reserved >= 0),
    quantity_on_order  INTEGER NOT NULL DEFAULT 0
                           CHECK (quantity_on_order >= 0),
    reorder_point      INTEGER NOT NULL DEFAULT 0
                           CHECK (reorder_point >= 0),
    reorder_quantity   INTEGER NOT NULL DEFAULT 0
                           CHECK (reorder_quantity >= 0),
    last_updated       TEXT    NOT NULL,   -- ISO-8601 UTC timestamp

    UNIQUE (store_id, product_id),
    CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_inventory_store_id   ON inventory (store_id);
CREATE INDEX idx_inventory_product_id ON inventory (product_id);
