-- Migration: 0016_create_forecasts
-- Description: Create forecasts table for storing product/store/date demand predictions.

CREATE TABLE forecasts (
    forecast_id           INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id              INTEGER NOT NULL
                              REFERENCES stores   (store_id)
                              ON DELETE RESTRICT,
    product_id            INTEGER NOT NULL
                              REFERENCES products (product_id)
                              ON DELETE RESTRICT,
    forecast_date         TEXT    NOT NULL,   -- YYYY-MM-DD
    forecast_horizon_days INTEGER NOT NULL
                              CHECK (forecast_horizon_days BETWEEN 7 AND 90),
    predicted_quantity    REAL    NOT NULL CHECK (predicted_quantity >= 0),
    lower_bound           REAL    NOT NULL,
    upper_bound           REAL    NOT NULL,
    confidence_level      REAL    NOT NULL
                              CHECK (confidence_level BETWEEN 0.0 AND 1.0),
    model_version         TEXT    NOT NULL,
    generated_at          TEXT    NOT NULL    -- ISO-8601 UTC timestamp
);

CREATE INDEX idx_forecasts_store_id      ON forecasts (store_id);
CREATE INDEX idx_forecasts_product_id    ON forecasts (product_id);
CREATE INDEX idx_forecasts_forecast_date ON forecasts (forecast_date);
CREATE INDEX idx_forecasts_store_product ON forecasts (store_id, product_id, forecast_horizon_days);
