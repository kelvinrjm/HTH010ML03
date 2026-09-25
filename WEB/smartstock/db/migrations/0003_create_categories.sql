-- Migration: 0003_create_categories
-- Description: Create categories table with hierarchical parent-child relationships.

CREATE TABLE categories (
    category_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT    NOT NULL,
    parent_category_id INTEGER REFERENCES categories (category_id)
                           ON DELETE RESTRICT,
    created_at         TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_categories_parent_category_id
    ON categories (parent_category_id);
