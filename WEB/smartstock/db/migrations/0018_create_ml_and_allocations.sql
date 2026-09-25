-- Migration: 0018_create_ml_and_allocations
-- Description: Create ml_model_registry, dead_stock_flags, allocation_plans, and allocation_plan_lines.

CREATE TABLE ml_model_registry (
    model_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    model_name     TEXT    NOT NULL,
    model_version  TEXT    NOT NULL,
    model_type     TEXT    NOT NULL,   -- xgboost, random_forest, linear, etc.
    target         TEXT    NOT NULL,   -- demand, deadstock, etc.
    artefact_path  TEXT    NOT NULL,
    training_mae   REAL    NOT NULL,
    training_rmse  REAL    NOT NULL,
    training_r2    REAL    NOT NULL,
    trained_at     TEXT    NOT NULL,
    is_active      INTEGER NOT NULL DEFAULT 0 CHECK (is_active IN (0, 1))
);

CREATE INDEX idx_ml_model_registry_target    ON ml_model_registry (target);
CREATE INDEX idx_ml_model_registry_is_active ON ml_model_registry (is_active);
CREATE UNIQUE INDEX idx_ml_model_registry_target_active
    ON ml_model_registry (target) WHERE is_active = 1;

CREATE TABLE dead_stock_flags (
    flag_id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id                INTEGER NOT NULL
                                REFERENCES stores   (store_id)
                                ON DELETE RESTRICT,
    product_id              INTEGER NOT NULL
                                REFERENCES products (product_id)
                                ON DELETE RESTRICT,
    flagged_at              TEXT    NOT NULL,
    days_without_sale       INTEGER NOT NULL CHECK (days_without_sale >= 0),
    quantity_at_risk        INTEGER NOT NULL CHECK (quantity_at_risk >= 0),
    estimated_value_at_risk REAL,
    recommendation          TEXT    NOT NULL
                                CHECK (recommendation IN ('markdown', 'discount', 'write-off')),
    resolved_at             TEXT,
    resolution_notes        TEXT    NOT NULL DEFAULT ''
                                CHECK (length(resolution_notes) <= 1000)
);

CREATE INDEX idx_dead_stock_flags_store_id   ON dead_stock_flags (store_id);
CREATE INDEX idx_dead_stock_flags_product_id ON dead_stock_flags (product_id);
CREATE INDEX idx_dead_stock_flags_resolved_at ON dead_stock_flags (resolved_at);
CREATE INDEX idx_dead_stock_flags_unresolved
    ON dead_stock_flags (store_id, product_id) WHERE resolved_at IS NULL;

CREATE TABLE allocation_plans (
    plan_id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id               INTEGER NOT NULL
                                 REFERENCES products (product_id)
                                 ON DELETE RESTRICT,
    allocation_rule          TEXT    NOT NULL
                                 CHECK (allocation_rule IN ('proportional', 'priority')),
    total_quantity_available INTEGER NOT NULL
                                 CHECK (total_quantity_available >= 0),
    plan_date                TEXT    NOT NULL,
    created_at               TEXT    NOT NULL
);

CREATE INDEX idx_allocation_plans_product_id ON allocation_plans (product_id);
CREATE INDEX idx_allocation_plans_plan_date  ON allocation_plans (plan_date);

CREATE TABLE allocation_plan_lines (
    line_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id            INTEGER NOT NULL
                           REFERENCES allocation_plans (plan_id)
                           ON DELETE CASCADE,
    store_id           INTEGER NOT NULL
                           REFERENCES stores           (store_id)
                           ON DELETE RESTRICT,
    allocated_quantity INTEGER NOT NULL
                           CHECK (allocated_quantity >= 0),
    priority_score     REAL    NOT NULL
                           CHECK (priority_score BETWEEN 0.0 AND 1.0),
    created_at         TEXT    NOT NULL
);

CREATE INDEX idx_allocation_plan_lines_plan_id  ON allocation_plan_lines (plan_id);
CREATE INDEX idx_allocation_plan_lines_store_id ON allocation_plan_lines (store_id);
