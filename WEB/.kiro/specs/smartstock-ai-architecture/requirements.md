# Requirements Document

## Introduction

SmartStock AI is an ML-powered inventory management, demand forecasting, and business intelligence
application targeting small retailers and wholesalers. The application provides actionable insights
through automated demand forecasting, intelligent reorder recommendations, dead stock detection,
multi-store allocation, and a full audit trail — all surfaced through an interactive Streamlit
dashboard. The tech stack is Python, Streamlit, Pandas, NumPy, Scikit-learn, XGBoost, Plotly,
and SQLite.

This document captures all functional and non-functional requirements for the SmartStock AI
architecture. It is intentionally pre-implementation: it describes *what* the system must do and
*what constraints* it must satisfy, not *how* code is written. Modules are built from this plan
one at a time.

---

## Glossary

- **Application**: The SmartStock AI Streamlit application as a whole.
- **Database**: The SQLite database that persists all application state.
- **ML_Pipeline**: The subsystem responsible for training, validating, and persisting ML models.
- **Forecaster**: The demand forecasting component that produces SKU-level sales predictions.
- **Inventory_Engine**: The subsystem that calculates current stock levels and stock health metrics.
- **Deadstock_Detector**: The subsystem that identifies slow-moving or unsellable inventory.
- **Reorder_Engine**: The subsystem that calculates reorder points, quantities, and urgency.
- **Allocator**: The multi-store stock allocation subsystem.
- **Audit_Engine**: The subsystem that records every state-changing operation with full context.
- **UI**: The Streamlit front-end layer.
- **SKU**: Stock Keeping Unit — the atomic unit of a product variant tracked in inventory.
- **Store**: A physical or logical retail/wholesale location that holds inventory.
- **Supplier**: An entity from whom the business purchases SKUs.
- **Lead_Time**: The number of calendar days between placing a purchase order and receiving stock.
- **Safety_Stock**: A buffer quantity held to guard against forecast error and supply variability.
- **Reorder_Point**: The stock level at which a replenishment order should be placed.
- **EOQ**: Economic Order Quantity — the optimal order quantity that minimises total holding and
  ordering costs.
- **Dead_Stock**: Inventory that has not sold within a configurable threshold period and is
  unlikely to sell without intervention.
- **Allocation_Rule**: A named policy (e.g. proportional, priority-based) governing how shared
  stock is distributed across Stores.
- **Audit_Event**: An immutable record of a single state-changing action including actor, timestamp,
  before-state, and after-state.
- **Feature_Vector**: The row of engineered numerical features fed into an ML model.
- **Forecast_Horizon**: The number of future days for which the Forecaster produces predictions.
- **Confidence_Interval**: A lower/upper bound pair expressing forecast uncertainty at a given
  probability level.
- **Model_Registry**: The persistent store of trained model artefacts and their metadata.
- **Config**: The application-level configuration object (thresholds, defaults, paths).

---

## Requirements

---

### Requirement 1: Project Folder Structure

**User Story:** As a developer, I want the project to follow a well-defined, predictable folder structure, so that every module has a clear home, onboarding is straightforward, and the codebase scales without coupling concerns.

#### Acceptance Criteria

1. THE Application SHALL organise source code under a single top-level package directory named `smartstock/`, containing an `__init__.py` file to mark it as a Python package.
2. THE Application SHALL place all Streamlit page scripts under `smartstock/pages/`, where each script filename begins with a two-digit numeric prefix followed by an underscore and a descriptive name (e.g., `01_overview.py`) following Streamlit's multi-page naming convention.
3. THE Application SHALL place all database access logic under `smartstock/db/`, with schema definitions, migration scripts, and query helpers each in a distinct module file within that directory, such that no module within `smartstock/db/` contains logic from more than one of those three categories.
4. THE Application SHALL place all ML-related code — feature engineering, training, evaluation, and model loading — under `smartstock/ml/`, with each concern in its own distinct module file.
5. THE Application SHALL place all business-logic engines (Inventory_Engine, Forecaster, Deadstock_Detector, Reorder_Engine, Allocator, Audit_Engine) each in their own dedicated module file under `smartstock/engines/`, where each module file contains exactly one engine class or function set corresponding to its filename.
6. THE Application SHALL place shared utility functions (date helpers, formatting, validation) under `smartstock/utils/`, where each utility category resides in its own module file and no utility module imports from `smartstock/engines/`, `smartstock/ml/`, or `smartstock/db/`.
7. THE Application SHALL place all Plotly chart-building functions under `smartstock/charts/`, where every function in this directory returns a Plotly figure object and contains no business logic or database queries.
8. THE Application SHALL place all application configuration and environment defaults in a single `smartstock/config.py` module, and no other module within `smartstock/` SHALL define application-level configuration constants or environment variable reads.
9. THE Application SHALL store trained model artefacts and metadata under `models/` at the project root, outside the `smartstock/` Python package directory.
10. THE Application SHALL store the SQLite database file under `data/` at the project root, outside the `smartstock/` Python package directory.
11. THE Application SHALL store seed data, import templates, and sample CSVs under `data/samples/` at the project root.
12. THE Application SHALL include a `tests/` directory at the project root that mirrors the `smartstock/` package structure, such that for every module `smartstock/X/Y.py` there exists a corresponding test file at `tests/X/test_Y.py`.
13. THE Application SHALL provide either a `requirements.txt` or a `pyproject.toml` at the project root listing all runtime and development dependencies with pinned exact versions (no open ranges such as `>=` or `~=`).
14. THE Application SHALL provide a single entrypoint script `app.py` at the project root that launches the Streamlit application, where `app.py` contains no business logic and only invokes the Streamlit entry point.

---

### Requirement 2: Database Architecture

**User Story:** As a developer, I want a well-structured, normalised SQLite database, so that all application data is stored reliably, queries are efficient, and schema changes are traceable.

#### Acceptance Criteria

1. THE Database SHALL use a single SQLite file as the persistence layer for all application state.
2. THE Database SHALL enforce foreign-key constraints at connection time via `PRAGMA foreign_keys = ON`.
3. THE Database SHALL apply all schema changes through versioned migration scripts stored in `smartstock/db/migrations/`, each file named `{version}_{description}.sql`, where `version` is a zero-padded 4-digit integer (e.g., `0001_create_users.sql`).
4. THE Database SHALL record the current schema version in a `schema_version` table containing at minimum the version number and the UTC timestamp at which each migration was applied.
5. WHEN the Application starts, THE Database SHALL apply all pending migrations in ascending version order before any other database operation is performed, completing within 30 seconds; if not completed within 30 seconds, the startup SHALL be aborted and an error surfaced to the operator.
6. THE Database SHALL use `INTEGER PRIMARY KEY AUTOINCREMENT` for all surrogate primary keys.
7. THE Database SHALL store all timestamps as ISO-8601 strings in UTC with second-level precision (e.g., `YYYY-MM-DDTHH:MM:SSZ`).
8. THE Database SHALL define indexes on every foreign-key column and on every column used as a filter or sort key in business-logic queries, with each index name following the convention `idx_{table}_{column}`.
9. IF a migration script fails, THEN THE Database SHALL roll back the entire failed migration as a single atomic transaction, leave the schema at the last successfully applied version, and surface an error message indicating the failed migration version and failure reason to the operator before the Application continues.
10. THE Database SHALL be configurable via `Config` to point to a file path, enabling test isolation through an in-memory (`:memory:`) or temporary file path, where the default file path SHALL be a non-empty string of at most 260 characters.
11. IF the SQLite file path specified in `Config` is inaccessible or cannot be created at Application start, THEN THE Database SHALL surface an error indicating the invalid path and halt further initialisation.

---

### Requirement 3: Database Tables and Relationships

**User Story:** As a developer, I want a complete, normalised set of database tables with explicit relationships, so that every domain entity is stored once, referential integrity is enforced, and queries for any workflow can be expressed as simple joins.

#### Acceptance Criteria

1. THE Database SHALL contain a `stores` table with columns: `store_id`, `name`, `location`, `store_type` (retail/wholesale), `is_active`, `created_at`.
2. THE Database SHALL contain a `suppliers` table with columns: `supplier_id`, `name`, `contact_info`, `default_lead_time_days`, `is_active`, `created_at`.
3. THE Database SHALL contain a `categories` table with columns: `category_id`, `name`, `parent_category_id` (nullable self-reference for sub-categories), `created_at`.
4. THE Database SHALL contain a `products` table with columns: `product_id`, `sku`, `name`, `description`, `category_id` (FK → categories), `unit_of_measure`, `unit_cost`, `unit_price`, `is_active`, `created_at`.
5. THE Database SHALL enforce a unique constraint on `products.sku`.
6. THE Database SHALL contain a `product_suppliers` table linking products to suppliers with columns: `product_supplier_id`, `product_id` (FK → products), `supplier_id` (FK → suppliers), `supplier_sku`, `lead_time_days`, `minimum_order_quantity`, `is_preferred`, `created_at`.
7. THE Database SHALL contain a `store_inventory` table with columns: `inventory_id`, `store_id` (FK → stores), `product_id` (FK → products), `quantity_on_hand`, `quantity_reserved`, `quantity_on_order`, `reorder_point`, `reorder_quantity`, `last_updated`.
8. THE Database SHALL enforce a unique constraint on (`store_inventory.store_id`, `store_inventory.product_id`).
9. THE Database SHALL contain a `sales_transactions` table with columns: `transaction_id`, `store_id` (FK → stores), `product_id` (FK → products), `transaction_date`, `quantity_sold`, `unit_price_at_sale`, `channel` (in-store/online/wholesale), `created_at`.
10. THE Database SHALL contain a `purchase_orders` table with columns: `order_id`, `store_id` (FK → stores), `supplier_id` (FK → suppliers), `order_date`, `expected_delivery_date`, `status` (draft/submitted/received/cancelled), `created_at`.
11. THE Database SHALL contain a `purchase_order_lines` table with columns: `line_id`, `order_id` (FK → purchase_orders), `product_id` (FK → products), `quantity_ordered`, `quantity_received`, `unit_cost`, `created_at`.
12. THE Database SHALL contain a `demand_forecasts` table with columns: `forecast_id`, `store_id` (FK → stores), `product_id` (FK → products), `forecast_date`, `forecast_horizon_days`, `predicted_quantity`, `lower_bound`, `upper_bound`, `confidence_level` (decimal value between 0.0 and 1.0 inclusive), `model_version`, `generated_at`.
13. THE Database SHALL contain a `ml_model_registry` table with columns: `model_id`, `model_name`, `model_version`, `model_type` (xgboost/random_forest/etc.), `target` (demand/deadstock/etc.), `artefact_path`, `training_mae`, `training_rmse`, `training_r2`, `trained_at`, `is_active`.
14. THE Database SHALL contain a `dead_stock_flags` table with columns: `flag_id`, `store_id` (FK → stores), `product_id` (FK → products), `flagged_at`, `days_without_sale` (non-negative integer), `quantity_at_risk` (non-negative integer), `estimated_value_at_risk` (non-negative decimal), `recommendation` (markdown/discount/write-off), `resolved_at` (nullable), `resolution_notes`.
15. THE Database SHALL contain a `allocation_plans` table with columns: `plan_id`, `product_id` (FK → products), `allocation_rule`, `total_quantity_available` (non-negative integer), `plan_date`, `created_at`.
16. THE Database SHALL contain an `allocation_plan_lines` table with columns: `line_id`, `plan_id` (FK → allocation_plans), `store_id` (FK → stores), `allocated_quantity` (non-negative integer), `priority_score` (decimal value between 0.0 and 1.0 inclusive), `created_at`.
17. THE Database SHALL contain an `audit_log` table with columns: `audit_id`, `event_type`, `entity_type`, `entity_id`, `actor`, `occurred_at`, `before_state` (JSON text), `after_state` (JSON text), `notes`.
18. THE Database SHALL contain an `app_settings` table with columns: `setting_key`, `setting_value`, `description`, `updated_at` — used to persist configurable thresholds without code changes.
19. THE Database SHALL enforce foreign key constraints on all FK columns such that inserting or updating a row with a non-existent referenced key is rejected with a referential integrity error.
20. IF a `purchase_order_lines` row is inserted or updated with `quantity_received` greater than `quantity_ordered`, THEN THE Database SHALL reject the operation with a constraint violation error.
21. IF a `store_inventory` row is inserted or updated with `quantity_reserved` greater than `quantity_on_hand`, THEN THE Database SHALL reject the operation with a constraint violation error.

---

### Requirement 4: ML Pipeline

**User Story:** As a data scientist, I want a reproducible ML pipeline that trains, evaluates, versions, and serves models, so that forecasts and detections are traceable, comparable, and updatable without breaking the running application.

#### Acceptance Criteria

1. THE ML_Pipeline SHALL implement a feature-engineering step that transforms raw `sales_transactions` and `store_inventory` records into Feature_Vectors, where a valid Feature_Vector contains all required feature groups with no null values.
2. THE ML_Pipeline SHALL engineer at minimum the following feature groups: rolling sales statistics (7-day, 30-day, 90-day mean and standard deviation), day-of-week and month cyclical encodings, price and cost features, stock-level ratios, supplier lead-time features, and product category embeddings (integer-encoded).
3. WHEN a training run is initiated, THE ML_Pipeline SHALL split historical data into training and validation sets using a time-based split where the most recent 20% of records by timestamp form the validation set and no validation record's timestamp is earlier than any training record's timestamp, enforcing zero future data leakage.
4. THE ML_Pipeline SHALL train at least two candidate model types per target — XGBoost and a Scikit-learn baseline (e.g. RandomForestRegressor or LinearRegression) — and select the candidate with lower validation RMSE as the active model; if two candidates produce equal RMSE, THE ML_Pipeline SHALL select the XGBoost model.
5. WHEN a model training run completes, THE ML_Pipeline SHALL record the model artefact path, metrics (MAE, RMSE, R²), model type, version, hyperparameters, training date range, and feature count in `ml_model_registry` within 5 seconds of training completion.
6. THE ML_Pipeline SHALL serialise trained model artefacts to the `models/` directory using `joblib` and include the feature column order as part of the artefact metadata, such that the artefact file alone contains all information required to reproduce inference without additional metadata files.
7. WHEN a new model version is registered, THE ML_Pipeline SHALL atomically set all existing active records for that target to `is_active = FALSE` before inserting the new record with `is_active = TRUE`, ensuring exactly one active model exists per target at all times.
8. THE ML_Pipeline SHALL expose a `load_active_model(target)` function that queries `ml_model_registry` for the single record where `is_active = TRUE` for the given target and returns the deserialised model artefact from the path in that record.
9. IF a model artefact file referenced in `ml_model_registry` is missing from disk, THEN THE ML_Pipeline SHALL raise a recoverable error indicating the missing artefact path and fall back to the model with the most recent `trained_at` timestamp whose artefact file is present on disk.
10. THE ML_Pipeline SHALL provide a retraining schedule configuration in `Config` specifying the minimum number of new sales records required before retraining is triggered automatically, where the configured value is a positive integer between 1 and 1,000,000.
11. THE ML_Pipeline SHALL log all training runs — including hyperparameters, feature counts, data date range, selected model, and training duration in seconds — to the `audit_log` table via the Audit_Engine within 10 seconds of training completion.
12. FOR ALL trained models, THE ML_Pipeline SHALL produce a feature-importance report where each feature's importance score is a decimal value between 0.0 and 1.0 inclusive and all scores sum to 1.0, persisted as a JSON file named `{model_name}_{model_version}_feature_importance.json` alongside the model artefact.
13. IF the feature count in an inference request does not match the feature count registered in the model artefact metadata, THEN THE ML_Pipeline SHALL raise a recoverable error indicating the mismatch and halt inference for that request without producing a prediction.

---

### Requirement 5: Demand Forecasting Workflow

**User Story:** As a store manager, I want daily demand forecasts per SKU per store, so that I can plan purchases accurately and avoid both stockouts and overstock.

#### Acceptance Criteria

1. WHEN a forecast run is executed for a (store, product) pair, THE Forecaster SHALL produce a `predicted_quantity` point forecast and a 95% Confidence_Interval (`lower_bound`, `upper_bound`) for each day in the Forecast_Horizon.
2. THE Forecaster SHALL support a configurable Forecast_Horizon between 7 and 90 days, defaulting to 30 days.
3. WHEN the Forecaster generates predictions, THE Forecaster SHALL use only the active model returned by `ML_Pipeline.load_active_model("demand")`.
4. THE Forecaster SHALL store all generated forecasts in the `demand_forecasts` table, recording the `model_version` used for each forecast row.
5. WHEN a new forecast run completes for a (store, product) pair, THE Forecaster SHALL replace any existing forecast rows for the same pair and horizon in `demand_forecasts` rather than appending duplicates.
6. IF fewer than 30 calendar days with at least 1 recorded sale exist for a (store, product) pair, THEN THE Forecaster SHALL fall back to a moving-average forecast computed as the mean of daily `quantity_sold` over those available days (capped at 30), and record `model_version = "fallback_moving_average"`.
7. WHEN a forecast run completes, THE Forecaster SHALL aggregate daily forecasts into weekly summary rows aligned to calendar weeks (Monday–Sunday) and monthly summary rows aligned to calendar months, storing the summed `predicted_quantity` and averaged `lower_bound`/`upper_bound` per period for display in the UI.
8. THE Forecaster SHALL expose a `forecast_accuracy` function that computes MAE and MAPE for past forecasts against actual sales, enabling ongoing model monitoring.
9. WHEN a forecast run is initiated manually from the UI, THE Audit_Engine SHALL record an Audit_Event of type `forecast_run` including the store, date range, and model version used.
10. WHEN a batch forecast run is invoked across all stores and products with `active` status, THE Forecaster SHALL complete all forecasts within 60 seconds for a catalogue of up to 1,000 SKUs across 10 stores on a host with at least 4 CPU cores and 16 GB RAM.
11. IF `ML_Pipeline.load_active_model("demand")` fails or returns no model, THEN THE Forecaster SHALL abort the forecast run, persist no new forecast rows, and surface an error message indicating that no active demand model is available.
12. WHEN a scheduled daily forecast run is triggered at a configured time, THE Forecaster SHALL execute a batch forecast run across all stores and products with `active` status and record an Audit_Event of type `forecast_run` including the store, date range, and model version used.

---

### Requirement 6: Inventory Calculation Workflow

**User Story:** As a store manager, I want accurate, real-time inventory metrics per SKU per store, so that I always know what I have, what is reserved, and what health status each item is in.

#### Acceptance Criteria

1. THE Inventory_Engine SHALL calculate `quantity_available` as `quantity_on_hand − quantity_reserved` for each (store, product) pair.
2. THE Inventory_Engine SHALL calculate `days_of_stock_remaining` as `quantity_available ÷ average_daily_demand` where `average_daily_demand` is the 30-day rolling mean from `sales_transactions`.
3. WHEN `average_daily_demand` is zero for a product, THE Inventory_Engine SHALL set `days_of_stock_remaining` to `NULL` and flag the product as having no recent sales history.
4. THE Inventory_Engine SHALL assign each (store, product) pair a stock health status using strict less-than/less-than-or-equal comparisons: `critical` if `days_of_stock_remaining < lead_time_days`; `low` if `days_of_stock_remaining >= lead_time_days` and `days_of_stock_remaining < 2 × lead_time_days`; `healthy` if `days_of_stock_remaining >= 2 × lead_time_days` and `days_of_stock_remaining <= 6 × lead_time_days`; `overstock` if `days_of_stock_remaining > 6 × lead_time_days`.
5. THE Inventory_Engine SHALL calculate total inventory value per store as `SUM(quantity_on_hand × unit_cost)` across all products where `is_active = true`.
6. THE Inventory_Engine SHALL calculate inventory turnover ratio as `total_units_sold_last_90_days ÷ average_quantity_on_hand_last_90_days` per SKU per store, where `average_quantity_on_hand_last_90_days` is the mean of daily end-of-day `quantity_on_hand` values over that 90-day window.
7. WHEN a sales transaction is recorded, THE Inventory_Engine SHALL update `store_inventory.quantity_on_hand` by decrementing the sold quantity within the same database transaction to prevent partial updates.
8. WHEN a purchase order status changes to `received`, THE Inventory_Engine SHALL increment `store_inventory.quantity_on_hand` by the `quantity_received` for each line item and decrement `quantity_on_order` accordingly.
9. IF a `purchase_order_lines` row being received references a (store, product) pair that does not exist in `store_inventory`, THEN THE Inventory_Engine SHALL reject the receipt operation and surface an error identifying the missing inventory record.
10. THE Inventory_Engine SHALL provide a `snapshot` function that returns the current inventory state for all (store, product) pairs as a Pandas DataFrame reflecting committed database state at the time of the call, with no caching from prior calls.
11. IF `quantity_on_hand` would be decremented below zero by a transaction, THEN THE Inventory_Engine SHALL reject the transaction and surface an error identifying the product identifier, store identifier, and shortfall quantity.

---

### Requirement 7: Dead Stock Detection Workflow

**User Story:** As a store owner, I want to be automatically alerted to inventory that has stopped selling, so that I can take corrective action (discounting, redistribution, or write-off) before capital remains locked indefinitely.

#### Acceptance Criteria

1. THE Deadstock_Detector SHALL classify a (store, product) pair as Dead_Stock when the number of days since the last recorded sale exceeds a configurable `dead_stock_threshold_days` (default: 90 days, minimum: 1 day, maximum: 3650 days).
2. THE Deadstock_Detector SHALL calculate `days_without_sale` for each (store, product) pair as the difference in calendar days between the current date and the most recent `transaction_date` in `sales_transactions`; if no `transaction_date` exists for a (store, product) pair, THE Deadstock_Detector SHALL treat `days_without_sale` as equal to the number of days since the product was first added to that store's inventory.
3. THE Deadstock_Detector SHALL calculate `estimated_value_at_risk` as `quantity_on_hand × unit_cost` for each flagged (store, product) pair where `quantity_on_hand` is greater than zero; if `unit_cost` is unavailable, THE Deadstock_Detector SHALL exclude the pair from value calculations and record the flag without an `estimated_value_at_risk` value.
4. WHEN a Dead_Stock flag is generated, THE Deadstock_Detector SHALL assign a `recommendation` of `markdown` when `days_without_sale` is less than 2 × `dead_stock_threshold_days`, `discount` when `days_without_sale` is greater than or equal to 2 × and less than or equal to 3 × `dead_stock_threshold_days`, or `write-off` when `days_without_sale` exceeds 3 × `dead_stock_threshold_days`.
5. THE Deadstock_Detector SHALL use the active ML model for target `"deadstock"` to score each candidate item and include the ML confidence score (a value between 0.00 and 1.00 inclusive) in the flag record where a model is available, falling back to rule-based classification when no active model exists for target `"deadstock"`.
6. THE Deadstock_Detector SHALL write new flags to `dead_stock_flags` only when no unresolved flag already exists for the same (store, product) pair; an unresolved flag is defined as a flag record where `resolved_at` is null.
7. WHEN a Dead_Stock flag is resolved via UI action, THE Deadstock_Detector SHALL set `resolved_at` to the current timestamp and record a non-empty `resolution_notes` value (maximum 1000 characters) on the flag record, and THE Audit_Engine SHALL record an Audit_Event of type `deadstock_resolved`; IF `resolution_notes` is empty or absent, THEN THE Deadstock_Detector SHALL reject the resolution action with an error indicating that resolution notes are required.
8. THE Deadstock_Detector SHALL produce a store-level summary report containing total `estimated_value_at_risk` across all flagged pairs, count of flagged items per `recommendation` category, and percentage of active SKUs flagged calculated as (count of flagged active SKUs ÷ total active SKUs in that store) × 100, rounded to two decimal places.
9. WHEN the Deadstock_Detector runs a detection pass, THE Audit_Engine SHALL record an Audit_Event of type `deadstock_detection_run` including the detection run date, count of (store, product) pairs evaluated, and count of new flags written during that pass.
10. IF the Deadstock_Detector encounters an error accessing `sales_transactions` or `dead_stock_flags` during a detection pass, THEN THE Deadstock_Detector SHALL abort the detection pass, preserve all existing flag records without modification, and record an Audit_Event of type `deadstock_detection_run` indicating the failure.

---

### Requirement 8: Reorder Calculation Workflow

**User Story:** As a purchasing manager, I want automated reorder recommendations with calculated quantities and urgency rankings, so that I can place purchase orders efficiently without manual spreadsheet work.

#### Acceptance Criteria

1. THE Reorder_Engine SHALL calculate the Reorder_Point for each (store, product) pair as `(average_daily_demand × lead_time_days) + safety_stock`, where `average_daily_demand` is the mean of daily sales units over the trailing 30 days and `lead_time_days` is a positive integer in the range 1–365.
2. THE Reorder_Engine SHALL calculate Safety_Stock as `Z × σ_demand × √lead_time_days` where `Z` is a configurable service-level Z-score (default: 1.65 for 95% service level) and `σ_demand` is the standard deviation of daily sales over the trailing 30 days.
3. THE Reorder_Engine SHALL calculate EOQ as `√(2 × annual_demand × ordering_cost ÷ holding_cost_rate)` where `annual_demand` is derived as `average_daily_demand × 365`, `ordering_cost` is a configurable positive monetary value per order, and `holding_cost_rate` is a configurable value in the range 0.01–1.0 representing the fraction of unit cost incurred as holding cost per year, both stored in `Config`.
4. WHEN `quantity_available` falls at or below `reorder_point` for a (store, product) pair, THE Reorder_Engine SHALL generate a reorder recommendation and persist it in `store_inventory` by updating `reorder_point` and setting `reorder_quantity` to `max(EOQ, supplier_minimum_order_quantity)` for the preferred supplier; if no preferred supplier exists, `reorder_quantity` SHALL be set to EOQ.
5. THE Reorder_Engine SHALL assign an urgency rank to each reorder recommendation as follows: `critical` if `quantity_available ≤ 0`; `high` if `quantity_available > 0` and `quantity_available < 50% of reorder_point`; `medium` if `quantity_available ≥ 50% of reorder_point` and `quantity_available ≤ reorder_point`; `low` if `quantity_available > reorder_point` and `quantity_available ≤ 1.25 × reorder_point`.
6. THE Reorder_Engine SHALL identify the preferred supplier for each product from `product_suppliers.is_preferred` and include supplier name, lead time, and minimum order quantity in the recommendation output; if multiple records have `is_preferred = true` for the same product, THE Reorder_Engine SHALL select the one with the lowest `lead_time_days`, resolving ties by selecting the lowest `minimum_order_quantity`.
7. WHEN a user submits a set of 1–500 selected reorder recommendations for purchase order generation, THE Reorder_Engine SHALL create a draft `purchase_order` record and corresponding `purchase_order_lines` in a single database transaction, rolling back all changes and returning an error indicating the failure reason if the transaction cannot be completed.
8. WHEN a purchase order draft is created from recommendations, THE Audit_Engine SHALL record an Audit_Event of type `purchase_order_created` including the order ID, supplier, and total line count.
9. WHEN a consolidated reorder report is requested, THE Reorder_Engine SHALL produce the report grouping all current reorder recommendations by supplier so that a single purchase order can cover multiple SKUs from the same supplier.
10. IF no supplier is linked to a product via `product_suppliers`, THEN THE Reorder_Engine SHALL include the product in the recommendations list with a `supplier_required` warning flag rather than silently omitting it.

---

### Requirement 9: Multi-Store Allocation Workflow

**User Story:** As a regional manager, I want automated stock allocation recommendations across stores when supply is constrained, so that inventory is distributed optimally rather than on a first-come, first-served basis.

#### Acceptance Criteria

1. THE Allocator SHALL support at least two named Allocation_Rules: `proportional` (distribute stock in proportion to each store's forecast demand) and `priority` (distribute stock according to a configurable store priority ranking).
2. WHEN an allocation plan is computed, THE Allocator SHALL ensure that `SUM(allocated_quantity across all stores) ≤ total_quantity_available` for every product, enforcing no over-allocation.
3. THE Allocator SHALL compute a `priority_score` for each (store, product) pair used in the `priority` rule as a weighted combination of: stock health status weight, days of stock remaining weight, and store revenue contribution weight — with configurable weights in `Config`, where each weight is a value between 0.0 and 1.0 inclusive and all three weights sum to exactly 1.0.
4. THE Allocator SHALL write the completed allocation plan to `allocation_plans` and all per-store allocations to `allocation_plan_lines` in a single database transaction, such that if any write fails, all writes in that transaction are rolled back and no partial plan is persisted.
5. WHEN an allocation plan is saved, THE Audit_Engine SHALL record an Audit_Event of type `allocation_plan_created` including the plan ID, product, rule used, and total quantity allocated.
6. THE Allocator SHALL support a dry-run mode that returns the allocation plan as a DataFrame without writing to the database, for preview in the UI before confirmation.
7. IF `total_quantity_available` is zero for a product, THEN THE Allocator SHALL return an empty allocation plan for that product with zero quantity allocated to all stores and surface a warning message indicating that no stock is available for that product, rather than dividing by zero.
8. THE Allocator SHALL generate a comparison view showing the difference between the `proportional` and `priority` allocation outcomes for the same input, where the view includes per-store allocated quantity and the signed difference (priority minus proportional) for each store.
9. IF the database transaction in criterion 4 fails, THEN THE Allocator SHALL surface an error message indicating that the allocation plan could not be saved, and the allocation plan SHALL NOT be partially written to either `allocation_plans` or `allocation_plan_lines`.
10. IF any configured weight in `Config` for the `priority` rule is outside the range 0.0 to 1.0, or the three weights do not sum to 1.0 (within a tolerance of ±0.001), THEN THE Allocator SHALL reject the configuration and surface an error message indicating which weight values are invalid before computing any allocation.

---

### Requirement 10: Audit Workflow

**User Story:** As a business owner, I want a complete, tamper-evident audit trail of every state-changing action in the system, so that I can investigate discrepancies, satisfy compliance requirements, and understand the history of any entity.

#### Acceptance Criteria

1. THE Audit_Engine SHALL record an Audit_Event to the `audit_log` table for every state-changing operation performed by any engine or UI action, including: inventory adjustments, sales imports, purchase order creation and status changes, forecast runs, dead stock flagging and resolution, and allocation plan creation.
2. EACH Audit_Event SHALL capture: `event_type` (string identifier of 1–100 characters), `entity_type` (table name), `entity_id` (primary key value), `actor` (username of 1–255 characters or the literal string "system"), `occurred_at` (UTC ISO-8601 timestamp), `before_state` (JSON snapshot of the record before change, or null for creation events), and `after_state` (JSON snapshot of the record after change, or null for deletion events).
3. THE Audit_Engine SHALL write Audit_Events within the same database transaction as the state-changing operation so that the audit record and the change are atomically committed or rolled back together.
4. THE Audit_Engine SHALL provide a `query_audit_log` function that accepts filters for `entity_type`, `entity_id`, `event_type`, date range (start date inclusive, end date inclusive), and actor, returns a paginated result set ordered by `occurred_at` descending, with each page containing at most 100 records, and includes a total record count in the response.
5. IF an Audit_Event write fails due to a database error, THEN THE Audit_Engine SHALL surface the error and prevent the parent operation from being committed, treating audit failure as a blocking error.
6. THE Audit_Engine SHALL provide a `diff_view` function that computes a field-level diff between `before_state` and `after_state`, presenting each changed field as its field name alongside its before value and after value, and omitting unchanged fields from the output.
7. THE `audit_log` table SHALL be append-only: no engine, UI action, or migration script SHALL issue `UPDATE` or `DELETE` statements against `audit_log` rows.
8. WHEN a user requests an audit export for a specified entity or date range, THE Audit_Engine SHALL produce the report as either a CSV or JSON file and make it available for download within 30 seconds for result sets of up to 100,000 records.
9. IF the requested audit export result set exceeds 100,000 records, THEN THE Audit_Engine SHALL reject the request with an error indicating that the export range is too large, and SHALL not produce a partial file.
10. WHEN a state-changing operation creates a new entity record, THE Audit_Engine SHALL record the Audit_Event with `before_state` set to null and `after_state` containing the full snapshot of the newly created record.

---

### Requirement 11: Streamlit Page Structure

**User Story:** As an end user, I want a logically organised, multi-page Streamlit application with consistent navigation, so that I can access every feature quickly and the interface feels coherent rather than a collection of disconnected tools.

#### Acceptance Criteria

1. THE UI SHALL implement a multi-page Streamlit application with the following top-level pages: **Dashboard** (home/KPI overview), **Inventory** (current stock levels and health), **Forecasting** (demand forecasts and accuracy), **Reorder** (recommendations and purchase orders), **Dead Stock** (flagged items and resolutions), **Allocation** (multi-store plans), **Audit** (event log and exports), and **Settings** (thresholds, model config, store and supplier management).
2. THE UI SHALL display a persistent sidebar containing: the application name and logo, the active store selector (applies a store filter to all pages), and the navigation menu.
3. THE UI SHALL apply a consistent Plotly chart theme (colour palette, font, grid style) across all pages, defined in a single `smartstock/charts/theme.py` module.
4. WHEN the active store selector value changes, THE UI SHALL refresh all page content to reflect data for the selected store within 2 seconds, without requiring a full page reload.
5. THE Dashboard page SHALL display at minimum: total SKU count, total inventory value, number of items with stock_health_status of "critical" or "low", number of open Dead_Stock flags, forecast accuracy (MAPE) for the last 30 days, and a 30-day sales trend chart — all filtered to the active store.
6. THE Inventory page SHALL display a filterable, sortable table of all (store, product) pairs with columns: SKU, name, category, quantity_on_hand, quantity_available, days_of_stock, stock_health_status, and inventory_value; the table SHALL support filtering by category and stock_health_status, and sorting by any column, and SHALL display a maximum of 200 rows per page with pagination controls when the total row count exceeds 200.
7. THE Forecasting page SHALL display a line chart of forecast vs. actual sales for a selected SKU, a forecast horizon selector allowing values of 7, 14, 30, or 60 days, and a model performance summary table showing at minimum MAPE, MAE, and RMSE for the selected SKU.
8. THE Reorder page SHALL display the reorder recommendation list grouped by urgency tier (critical, high, medium, low), allow selection of one or more recommendations via checkboxes, and provide a "Create Draft Purchase Order" action that creates a draft purchase order for all selected recommendations.
9. THE Dead Stock page SHALL display all open Dead_Stock flags grouped by recommendation category, show estimated value at risk per category, and allow resolution of individual flags by submitting a resolution action and a notes field of up to 1000 characters.
10. THE Allocation page SHALL allow the user to select a product, enter an available quantity between 1 and 999,999, choose an Allocation_Rule, preview the dry-run allocation plan showing per-store quantities before confirming, and confirm to save the allocation.
11. THE Audit page SHALL display a searchable, filterable audit log table supporting filter by event type, date range (start date and end date), and entity ID, and SHALL provide buttons to export the current filtered result as CSV or JSON with a maximum of 100,000 rows per export.
12. THE Settings page SHALL allow configuration of all `app_settings` values (thresholds, service level Z-score, ordering cost, holding cost rate, dead stock threshold days, store priority weights) through form inputs that validate each value against its defined minimum and maximum bounds before submission, and SHALL persist accepted changes to the database via the Audit_Engine.
13. IF a page encounters an unhandled exception during data loading, THEN THE UI SHALL display a user-friendly error message indicating that data could not be loaded and provide a "Retry" button that re-attempts the data load, without exposing raw stack traces to the end user.
14. THE UI SHALL be usable at a minimum browser viewport width of 1024px without horizontal scrolling, where all tables, charts, and form inputs remain fully visible and interactive at that viewport width.
15. IF the active store selector contains no stores (store list is empty), THEN THE UI SHALL display an informational message indicating that no stores are configured and disable all page content that depends on a store selection.
16. IF a Settings page form submission contains a value outside the defined minimum or maximum bounds for that setting, THEN THE UI SHALL display an inline validation error identifying the out-of-range field and its allowed range, and SHALL NOT persist the change.

---

## Non-Functional Requirements

### Requirement 12: Performance

**User Story:** As an end user, I want the application to respond quickly to common interactions, so that daily operational use does not feel sluggish.

#### Acceptance Criteria

1. THE UI SHALL load any page's initial data within 3 seconds on a machine with 4 CPU cores, 8 GB RAM, and a database containing up to 3 years of daily transactions for 1,000 SKUs across 10 stores.
2. THE Forecaster SHALL complete a full batch forecast run (1,000 SKUs × 10 stores, 30-day horizon) within 60 seconds on the same reference hardware.
3. THE Inventory_Engine SHALL return the full inventory snapshot DataFrame within 2 seconds for a catalogue of 1,000 SKUs across 10 stores.
4. THE ML_Pipeline SHALL complete a full model training cycle (feature engineering + training + evaluation) within 10 minutes for 3 years of daily sales data covering 1,000 SKUs.
5. WHEN a page's initial data load exceeds 500 milliseconds, THE UI SHALL display a loading indicator to the user until the data is available.
6. IF a batch forecast run does not complete within 60 seconds, THEN THE Forecaster SHALL preserve all forecast rows already written during that run and surface an error message indicating which store/product combinations did not complete.
7. IF the Inventory_Engine `snapshot` function does not return within 2 seconds, THEN THE Inventory_Engine SHALL surface a timeout error and retain the last successfully returned snapshot until the next successful call.

---

### Requirement 13: Reliability and Data Integrity

**User Story:** As a business owner, I want the application to protect my data and behave predictably under error conditions, so that I can trust the numbers it shows me.

#### Acceptance Criteria

1. THE Database SHALL wrap all multi-step writes (inventory updates, purchase order creation, allocation plan saves) in explicit database transactions so that partial writes cannot corrupt application state.
2. IF the Application crashes mid-write, THEN THE Database SHALL recover to the last fully committed transaction state on next startup.
3. WHEN a user submits a form containing quantities, dates, or prices, THE Application SHALL validate each field against its constraints — quantities as integers greater than or equal to 0, dates as valid calendar dates, and prices as decimal values between 0.01 and 999,999,999.99 — before writing to the database, and SHALL display an error message adjacent to each invalid field indicating what constraint was violated, without writing any part of the submission to the database.
4. WHEN the Application starts, THE Application SHALL execute a database integrity check; IF the integrity check reports any errors, THEN THE Application SHALL display an alert to the operator describing the check failure before rendering any UI, and SHALL log the full check result to the application log.
5. IF a database transaction fails to commit, THEN THE Application SHALL roll back all changes from that transaction, display an error message to the user indicating the operation did not complete, and leave the prior committed state intact.

---

### Requirement 14: Maintainability and Testability

**User Story:** As a developer, I want the codebase to be modular and testable, so that each engine can be developed, tested, and replaced independently without cascading changes.

#### Acceptance Criteria

1. THE Application SHALL enforce a layered architecture where the UI layer calls only engine modules, engine modules call only `db/` or `ml/` modules, and `db/` modules contain no business logic.
2. EACH engine module SHALL expose a documented public interface (functions or a class with docstrings) and SHALL NOT directly import Streamlit components, ensuring engines are testable in isolation without a running Streamlit session.
3. THE Application SHALL achieve at minimum 80% line coverage across all modules under `smartstock/engines/` and `smartstock/ml/` via the test suite in `tests/`.
4. THE Application SHALL use `Config` for all configurable values (thresholds, paths, defaults) so that tests can inject overridden config without modifying source files.
5. IF a required dependency of an engine module is unavailable at import time, THEN THE engine module SHALL raise an `ImportError` with a message identifying the missing dependency, rather than failing silently or raising an unrelated error at call time.
6. THE Application SHALL achieve at minimum 70% line coverage per individual module under `smartstock/engines/` and `smartstock/ml/`, such that no single module's coverage falls below 70% even if the aggregate across all modules meets or exceeds 80%.
7. THE Application SHALL enforce via static analysis (e.g., a linting rule or CI check) that no module under `smartstock/db/` contains domain-specific conditional logic (branching on business entity state), such that the static analysis check fails if such logic is detected.
