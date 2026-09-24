-- Migration: 0005_create_products
-- Description: Create products table with SKU, category FK, unit costs and pricing.

CREATE TABLE products (
    product_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    sku             TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    description     TEXT    NOT NULL DEFAULT '',
    category_id     INTEGER NOT NULL
                        REFERENCES categories (category_id)
                        ON DELETE RESTRICT,
    unit_of_measure TEXT    NOT NULL DEFAULT 'unit',
    unit_cost       REAL    NOT NULL CHECK (unit_cost >= 0),
    unit_price      REAL    NOT NULL CHECK (unit_price >= 0),
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at      TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_is_active   ON products (is_active);
CREATE INDEX idx_products_sku         ON products (sku);
