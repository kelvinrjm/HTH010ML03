-- Migration: 0006_create_product_suppliers
-- Description: Join table linking products to suppliers with lead times, MOQs, and preference flags.

CREATE TABLE product_suppliers (
    product_supplier_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id              INTEGER NOT NULL
                                REFERENCES products  (product_id)
                                ON DELETE CASCADE,
    supplier_id             INTEGER NOT NULL
                                REFERENCES suppliers (supplier_id)
                                ON DELETE RESTRICT,
    supplier_sku            TEXT    NOT NULL DEFAULT '',
    lead_time_days          INTEGER NOT NULL
                                CHECK (lead_time_days BETWEEN 1 AND 365),
    minimum_order_quantity  INTEGER NOT NULL DEFAULT 1
                                CHECK (minimum_order_quantity >= 1),
    is_preferred            INTEGER NOT NULL DEFAULT 0
                                CHECK (is_preferred IN (0, 1)),
    created_at              TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_product_suppliers_product_id  ON product_suppliers (product_id);
CREATE INDEX idx_product_suppliers_supplier_id ON product_suppliers (supplier_id);
CREATE INDEX idx_product_suppliers_is_preferred
    ON product_suppliers (product_id, is_preferred);
