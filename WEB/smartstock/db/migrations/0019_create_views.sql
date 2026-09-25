-- Migration: 0019_create_views
-- Description: Create compatibility views aligning table names with architectural specifications.

CREATE VIEW IF NOT EXISTS store_inventory AS
SELECT
    inventory_id,
    store_id,
    product_id,
    quantity_on_hand,
    quantity_reserved,
    quantity_on_order,
    reorder_point,
    reorder_quantity,
    last_updated
FROM inventory;

CREATE VIEW IF NOT EXISTS purchase_orders AS
SELECT
    purchase_id AS order_id,
    store_id,
    supplier_id,
    order_date,
    expected_delivery_date,
    status,
    created_at
FROM purchases;

CREATE VIEW IF NOT EXISTS purchase_order_lines AS
SELECT
    purchase_item_id AS line_id,
    purchase_id AS order_id,
    product_id,
    quantity_ordered,
    quantity_received,
    unit_cost,
    created_at
FROM purchase_items;

CREATE VIEW IF NOT EXISTS audit_log AS
SELECT
    audit_id,
    event_type,
    entity_type,
    entity_id,
    actor,
    occurred_at,
    before_state,
    after_state,
    notes
FROM audit_logs;

CREATE VIEW IF NOT EXISTS demand_forecasts AS
SELECT
    forecast_id,
    store_id,
    product_id,
    forecast_date,
    forecast_horizon_days,
    predicted_quantity,
    lower_bound,
    upper_bound,
    confidence_level,
    model_version,
    generated_at
FROM forecasts;

CREATE VIEW IF NOT EXISTS sales_transactions AS
SELECT
    si.sale_item_id AS transaction_id,
    s.store_id,
    si.product_id,
    s.sale_date AS transaction_date,
    si.quantity AS quantity_sold,
    si.unit_price AS unit_price_at_sale,
    s.channel,
    si.created_at
FROM sales s
JOIN sale_items si ON s.sale_id = si.sale_id;
