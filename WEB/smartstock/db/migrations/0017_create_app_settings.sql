-- Migration: 0017_create_app_settings
-- Description: Create app_settings table for configurable business parameters.

CREATE TABLE app_settings (
    setting_key   TEXT NOT NULL PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   TEXT NOT NULL DEFAULT '',
    updated_at    TEXT NOT NULL   -- ISO-8601 UTC timestamp
);

-- Seed initial default configuration settings
INSERT INTO app_settings (setting_key, setting_value, description, updated_at) VALUES
('service_level_z_score', '1.65', 'Z-score for target 95% service level safety stock', '2026-01-01T00:00:00Z'),
('ordering_cost', '25.0', 'Fixed cost per purchase order in standard currency', '2026-01-01T00:00:00Z'),
('holding_cost_rate', '0.20', 'Annual holding cost as a fraction of inventory unit cost (20%)', '2026-01-01T00:00:00Z'),
('dead_stock_threshold_days', '90', 'Inactivity threshold in days to flag inventory as dead stock', '2026-01-01T00:00:00Z'),
('forecast_horizon_days', '30', 'Default horizon days for sales demand forecasting', '2026-01-01T00:00:00Z'),
('alloc_weight_stock_health', '0.40', 'Priority weight for store stock health in multi-store allocation', '2026-01-01T00:00:00Z'),
('alloc_weight_days_remaining', '0.35', 'Priority weight for days of stock remaining in multi-store allocation', '2026-01-01T00:00:00Z'),
('alloc_weight_revenue', '0.25', 'Priority weight for revenue contribution in multi-store allocation', '2026-01-01T00:00:00Z');
