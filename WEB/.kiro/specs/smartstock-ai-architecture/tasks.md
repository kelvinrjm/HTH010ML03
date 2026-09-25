# Implementation Plan: SmartStock AI — Database Layer

## Overview

Implement the SQLite persistence tier for SmartStock AI in isolation. This covers the project
scaffold, connection management, versioned migrations, all 14 domain query modules, and a
comprehensive pytest test suite. No ML pipeline, no Streamlit UI.

All code is Python. The fixture strategy uses in-memory SQLite (`":memory:"`) so every test
runs fully isolated without touching disk.

---

## Tasks

- [x] 1. Project scaffold and configuration
  - [x] 1.1 Create package root and config module
    - Create `smartstock/__init__.py` (empty, marks the package)
    - Create `smartstock/config.py` with the frozen `Config` dataclass exactly as specified in
      the design: `db_path`, `db_migration_timeout_seconds`, `forecast_horizon_days`,
      `forecast_fallback_min_days`, `service_level_z_score`, `ordering_cost`,
      `holding_cost_rate`, `dead_stock_threshold_days`, `retraining_min_new_records`,
      `alloc_weight_stock_health`, `alloc_weight_days_remaining`, `alloc_weight_revenue`
    - Validate in `__post_init__` that allocation weights sum to 1.0 ± 0.001; raise `ValueError`
      with a clear message if not
    - Expose a module-level `DEFAULT_CONFIG = Config()` singleton
    - _Requirements: 1.8, 2.10, 9.10, 14.4_

  - [x] 1.2 Create directory stubs and entry-point
    - Create `data/.gitkeep` and `models/.gitkeep` to establish the required top-level dirs
    - Create `app.py` at project root as a stub that only imports streamlit and calls
      `st.write("SmartStock AI")` — no business logic
    - Create `requirements.txt` with exact pinned versions for: `streamlit`, `pandas`, `numpy`,
      `scikit-learn`, `xgboost`, `plotly`, `hypothesis`, `pytest`, `pytest-cov`, `joblib`
    - _Requirements: 1.9, 1.10, 1.13, 1.14_

- [ ] 2. Database connection management
  - [-] 2.1 Implement `smartstock/db/connection.py`
    - Create `smartstock/db/__init__.py` (empty)
    - Implement `get_connection(config)` as a `contextlib.contextmanager` that:
      opens the SQLite file at `config.db_path`, executes `PRAGMA foreign_keys = ON` and
      `PRAGMA journal_mode = WAL`, sets `conn.row_factory = sqlite3.Row`, commits on clean
      exit, rolls back on exception, and closes unconditionally
    - Implement `open_connection(config)` that applies the same PRAGMAs and row_factory but
      leaves lifecycle management to the caller
    - _Requirements: 2.1, 2.2, 3.19_

  - [ ]* 2.2 Write unit tests for connection module (`tests/db/test_connection.py`)
    - Verify `PRAGMA foreign_keys` is ON after `get_connection`
    - Verify `journal_mode` is WAL
    - Verify `row_factory` is `sqlite3.Row`
    - Verify context manager commits on clean exit and rolls back on exception
    - Verify `open_connection` applies the same PRAGMAs
    - _Requirements: 2.2_

- [ ] 3. Migration system and schema initialisation
  - [~] 3.1 Write the 18 migration SQL files
    - Create `smartstock/db/migrations/` directory
    - Write `0001_create_schema_version.sql` through `0018_create_users.sql` exactly as
      specified in the design DDL section, including all CHECK constraints, UNIQUE constraints,
      composite indexes, partial indexes, and the two append-only triggers on `audit_log`
    - Each file must be self-contained (no dependency on files being executed beforehand except
      via FK references that previous migrations have already created)
    - _Requirements: 2.3, 2.4, 3.1–3.21_

  - [~] 3.2 Implement `smartstock/db/schema.py`
    - Implement `MigrationError(version, description, cause)` and `MigrationTimeoutError`
      exception classes
    - Implement `get_current_version(conn) -> int` — queries `schema_version`, returns 0 if
      table does not exist
    - Implement `list_pending_migrations(conn) -> list[Path]` — reads `MIGRATIONS_DIR`,
      filters out already-applied versions, returns sorted by numeric prefix
    - Implement `init_schema(conn, config)` — bootstraps migration 0001 if absent, then
      applies all pending migrations in ascending order, each in its own transaction; enforces
      `config.db_migration_timeout_seconds`; rolls back and raises `MigrationError` on failure
    - _Requirements: 2.3, 2.4, 2.5, 2.9_

  - [ ]* 3.3 Write unit tests for schema module (`tests/db/test_schema.py`)
    - Verify all 18 tables exist after `init_schema` on a fresh `:memory:` connection
    - Verify `schema_version` has exactly 18 rows after full init
    - Verify `get_current_version` returns 18 after full init, 0 on empty DB
    - Verify calling `init_schema` twice is idempotent (no duplicate rows in `schema_version`)
    - _Requirements: 2.3, 2.4_

  - [ ]* 3.4 Write migration-specific tests (`tests/db/test_migrations.py`)
    - Verify migrations are applied in ascending order regardless of filesystem ordering
    - Verify a bad SQL migration leaves `schema_version` unchanged (Property 3)
    - Verify `list_pending_migrations` returns only unapplied files
    - _Requirements: 2.5, 2.9_

- [ ] 4. Test fixture (`tests/db/conftest.py`)
  - [~] 4.1 Create pytest conftest with `mem_db` fixture
    - Create `tests/__init__.py` and `tests/db/__init__.py`
    - Implement the `mem_db` fixture exactly as shown in the design: creates a `Config`
      with `db_path=":memory:"`, calls `open_connection(config)`, runs `init_schema`,
      yields the connection, then closes it
    - No test may share state — each invocation gets a freshly initialised DB
    - _Requirements: 2.10, 14.4_

- [ ] 5. Property-based tests for core invariants
  - [ ]* 5.1 Write property test: FK enforcement (Property 1)
    - Use Hypothesis with ≥ 100 examples
    - For every FK column across all tables, generate a row referencing a non-existent parent
      key and assert `sqlite3.IntegrityError` is raised
    - Tag: `Feature: smartstock-ai-architecture, Property 1`
    - **Validates: Requirements 2.2, 3.19**

  - [ ]* 5.2 Write property test: migration ordering (Property 2)
    - Use Hypothesis to generate permuted sets of migration file names and verify that after
      `init_schema` the `schema_version` rows are strictly ascending with no gaps
    - Tag: `Feature: smartstock-ai-architecture, Property 2`
    - **Validates: Requirements 2.3, 2.4, 2.5**

  - [ ]* 5.3 Write property test: failed migration rollback (Property 3)
    - Inject a syntactically invalid SQL file into the migrations dir and confirm
      `schema_version` stays at its pre-attempt state
    - Tag: `Feature: smartstock-ai-architecture, Property 3`
    - **Validates: Requirement 2.9**

- [~] 6. Checkpoint — connection, schema, and fixtures
  - Ensure all tests written so far pass with `pytest tests/db/test_connection.py tests/db/test_schema.py tests/db/test_migrations.py --tb=short`
  - Fix any issues before proceeding to query modules.

- [ ] 7. Stores query module
  - [~] 7.1 Implement `smartstock/db/queries/stores.py`
    - Create `smartstock/db/queries/__init__.py` (empty)
    - Implement `insert_store`, `get_store`, `list_stores`, `update_store`, `deactivate_store`
      following the exact signatures in the design
    - `insert_store` sets `created_at` to `datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")`
    - `deactivate_store` sets `is_active = 0` (soft delete)
    - `list_stores` accepts optional `is_active` filter
    - _Requirements: 3.1_

  - [ ]* 7.2 Write unit tests for stores (`tests/db/test_queries_stores.py`)
    - Round-trip: insert → get → assert all fields match
    - `list_stores(is_active=1)` returns only active stores
    - `deactivate_store` sets `is_active = 0`; subsequent `get_store` reflects the change
    - FK rejection: inserting a `store_inventory` row referencing a non-existent `store_id`
      raises `sqlite3.IntegrityError`
    - _Requirements: 3.1, 3.19_

- [ ] 8. Suppliers query module
  - [~] 8.1 Implement `smartstock/db/queries/suppliers.py`
    - Implement `insert_supplier`, `get_supplier`, `list_suppliers`, `update_supplier`,
      `deactivate_supplier` following design signatures
    - `insert_supplier` sets `created_at` to UTC ISO-8601 string
    - `default_lead_time_days` validated by DB CHECK constraint (1–365)
    - _Requirements: 3.2_

  - [ ]* 8.2 Write unit tests for suppliers (`tests/db/test_queries_suppliers.py`)
    - Round-trip: insert → get → assert fields
    - `list_suppliers(is_active=0)` returns only inactive suppliers
    - `update_supplier` persists changes; `get_supplier` sees them
    - FK rejection: inserting a `purchase_orders` row with non-existent `supplier_id` raises
      `sqlite3.IntegrityError`
    - _Requirements: 3.2, 3.19_

- [ ] 9. Categories query module
  - [~] 9.1 Implement `smartstock/db/queries/categories.py`
    - Implement `insert_category`, `get_category`, `list_categories`, `get_category_tree`,
      `update_category`
    - `insert_category` accepts optional `parent_category_id` (nullable self-reference)
    - `get_category_tree` uses a recursive CTE to return the full hierarchy
    - _Requirements: 3.3_

  - [ ]* 9.2 Write unit tests for categories (`tests/db/test_queries_categories.py`)
    - Insert parent and child categories; verify `get_category_tree` returns both
    - `list_categories(parent_category_id=X)` returns only direct children of X
    - Self-referential FK: inserting a category with a non-existent `parent_category_id`
      raises `sqlite3.IntegrityError`
    - _Requirements: 3.3, 3.19_

- [ ] 10. Products query module
  - [~] 10.1 Implement `smartstock/db/queries/products.py`
    - Implement `insert_product`, `get_product`, `get_product_by_sku`, `list_products`,
      `update_product`, `deactivate_product`, `sku_exists`
    - Implement `link_supplier`, `get_preferred_supplier`, `list_product_suppliers`,
      `update_product_supplier` for the `product_suppliers` join table
    - `insert_product` sets `created_at`; enforces SKU uniqueness via DB UNIQUE constraint
    - `get_preferred_supplier` returns the supplier with `is_preferred = 1`; if multiple,
      picks lowest `lead_time_days` then lowest `minimum_order_quantity`
    - _Requirements: 3.4, 3.5, 3.6_

  - [ ]* 10.2 Write unit tests for products (`tests/db/test_queries_products.py`)
    - SKU uniqueness: inserting two products with the same SKU raises `sqlite3.IntegrityError`
    - `get_product_by_sku` round-trip
    - `sku_exists` returns True/False correctly
    - `get_preferred_supplier` tie-breaking: lowest lead time wins
    - FK: product with non-existent `category_id` raises `sqlite3.IntegrityError`
    - _Requirements: 3.4, 3.5, 3.6, 3.19_

- [ ] 11. Inventory query module
  - [~] 11.1 Implement `smartstock/db/queries/inventory.py`
    - Implement `InsufficientStockError(store_id, product_id, shortfall)`
    - Implement `upsert_inventory`, `get_inventory`, `list_inventory`, `increment_on_hand`,
      `decrement_on_hand`, `update_reorder_params`, `snapshot_all`
    - `upsert_inventory` uses `INSERT OR REPLACE` (or `INSERT … ON CONFLICT`) to handle
      the UNIQUE(store_id, product_id) constraint
    - `decrement_on_hand` reads current `quantity_on_hand`, raises `InsufficientStockError`
      if `current - delta < 0`, otherwise updates atomically
    - `snapshot_all` returns all rows — no caching
    - _Requirements: 3.7, 3.8, 3.21, 6.7, 6.10, 6.11_

  - [ ]* 11.2 Write unit tests for inventory (`tests/db/test_queries_inventory.py`)
    - `upsert_inventory` creates row; calling again updates it
    - `increment_on_hand` increases value by exact delta
    - `decrement_on_hand` with valid delta decreases value
    - `decrement_on_hand` that would go negative raises `InsufficientStockError`; value
      remains unchanged after the raise
    - `snapshot_all` returns all rows without caching
    - UNIQUE constraint: duplicate (store_id, product_id) via direct INSERT raises
    - _Requirements: 3.7, 3.8, 3.21_

  - [ ]* 11.3 Write property test: inventory never goes negative (Property 5)
    - Use Hypothesis to generate arbitrary sequences of `increment_on_hand` /
      `decrement_on_hand` calls and assert `quantity_on_hand` never becomes negative
    - Tag: `Feature: smartstock-ai-architecture, Property 5`
    - **Validates: Requirements 6.11, 13.1**

- [ ] 12. Sales query module
  - [~] 12.1 Implement `smartstock/db/queries/sales.py`
    - Implement `insert_sale`, `get_sale`, `list_sales`, `daily_sales_summary`,
      `rolling_mean_demand`, `rolling_stddev_demand`, `count_sale_days`
    - `list_sales` accepts keyword filters: `store_id`, `product_id`, `date_from`, `date_to`,
      `channel`; builds WHERE clause dynamically
    - `rolling_mean_demand` returns `None` when no sales exist in the window
    - `rolling_stddev_demand` returns `None` when fewer than 2 data points exist
    - `count_sale_days` returns count of distinct `transaction_date` values with
      `quantity_sold > 0`
    - _Requirements: 3.9_

  - [ ]* 12.2 Write unit tests for sales (`tests/db/test_queries_sales.py`)
    - `rolling_mean_demand` with no data returns `None`
    - `rolling_mean_demand` with known data returns correct float
    - `rolling_stddev_demand` with fewer than 2 points returns `None`
    - `daily_sales_summary` groups by date correctly
    - `count_sale_days` counts distinct sale dates only
    - `list_sales` date-range filter returns only rows within range
    - _Requirements: 3.9_

- [ ] 13. Purchases query module
  - [~] 13.1 Implement `smartstock/db/queries/purchases.py`
    - Implement `insert_purchase_order`, `get_purchase_order`, `list_purchase_orders`,
      `update_order_status`
    - Implement `insert_order_line`, `get_order_line`, `list_order_lines`, `receive_order_line`,
      `list_orders_for_reorder_report`
    - `receive_order_line` raises `ValueError` if `quantity_received > quantity_ordered`
    - `update_order_status` validates status is one of `draft/submitted/received/cancelled`
      (DB CHECK enforces this; Python can pre-validate for a cleaner error message)
    - _Requirements: 3.10, 3.11, 3.20_

  - [ ]* 13.2 Write unit tests for purchases (`tests/db/test_queries_purchases.py`)
    - Round-trip: insert order → insert line → get order → get line
    - `receive_order_line` with `quantity_received <= quantity_ordered` succeeds
    - `receive_order_line` with `quantity_received > quantity_ordered` raises `ValueError`
    - `update_order_status` to `received` persists correctly
    - `list_orders_for_reorder_report` returns the correct subset
    - _Requirements: 3.10, 3.11, 3.20_

- [ ] 14. Transfers query module
  - [~] 14.1 Implement `smartstock/db/queries/transfers.py`
    - Implement `transfer_stock(conn, *, from_store_id, to_store_id, product_id, quantity,
      actor, notes="")`
    - Must execute within the caller-supplied transaction (does NOT begin its own)
    - Decrements `from_store` via `decrement_on_hand` (raises `InsufficientStockError` if
      source would go negative)
    - Increments `to_store` via `increment_on_hand`
    - Writes two audit events: one for the source decrement, one for the destination increment
    - _Requirements: 3.7, 10.1, 10.3_

  - [ ]* 14.2 Write unit tests for transfers (`tests/db/test_queries_transfers.py`)
    - Successful transfer: source decremented, destination incremented by exact quantity
    - Two audit events written per transfer
    - Transfer from store with insufficient stock raises `InsufficientStockError`; both
      inventories remain unchanged after the raise
    - _Requirements: 3.7, 10.1_

- [ ] 15. Forecasts and ML model registry query module
  - [~] 15.1 Implement `smartstock/db/queries/forecasts.py`
    - Implement `insert_forecast`, `get_forecast`, `list_forecasts`, `replace_forecasts`,
      `list_forecasts_by_period`
    - `replace_forecasts` deletes all existing rows for (store_id, product_id, horizon_days)
      then bulk-inserts `rows`; wraps both operations in a single transaction
    - `list_forecasts_by_period` accepts `"weekly"` or `"monthly"` and groups accordingly
    - Implement `register_model`, `get_active_model`, `deactivate_models`, `list_models`
    - `register_model` calls `deactivate_models(target)` then inserts the new row with
      `is_active = 1`; wraps both in a single transaction to satisfy Property 7
    - _Requirements: 3.12, 3.13, 4.7, 5.4, 5.5_

  - [ ]* 15.2 Write unit tests for forecasts (`tests/db/test_queries_forecasts.py`)
    - `replace_forecasts` called once inserts rows; called again with same args replaces
      (no duplicates)
    - `list_forecasts_by_period("weekly")` returns summed predictions per calendar week
    - `get_active_model` returns the single active model for a target
    - `register_model` a second time for same target deactivates the first model
    - _Requirements: 3.12, 3.13, 4.7, 5.5_

  - [ ]* 15.3 Write property test: exactly one active model per target (Property 7)
    - Use Hypothesis to generate arbitrary sequences of `register_model` calls for the same
      target and assert `get_active_model` returns exactly one row after each call
    - Tag: `Feature: smartstock-ai-architecture, Property 7`
    - **Validates: Requirement 4.7**

  - [ ]* 15.4 Write property test: replace_forecasts is idempotent (Property 8)
    - Use Hypothesis to generate a list of forecast rows; call `replace_forecasts` twice with
      the same args; assert the resulting rows equal a single call's output
    - Tag: `Feature: smartstock-ai-architecture, Property 8`
    - **Validates: Requirement 5.5**

- [ ] 16. Audit query module
  - [~] 16.1 Implement `smartstock/db/queries/audit.py`
    - Implement `AuditExportTooLargeError`
    - Implement `insert_audit_event(conn, *, event_type, entity_type, entity_id, actor,
      before_state, after_state, notes="")` — serialises before/after to JSON, sets
      `occurred_at` to `utcnow()` ISO-8601 string
    - Implement `query_audit_log(conn, *, entity_type, entity_id, event_type, date_from,
      date_to, actor, page, page_size)` — returns `(rows, total_count)`, ordered by
      `occurred_at DESC`, LIMIT/OFFSET pagination
    - Implement `diff_view(before_state, after_state) -> list[dict]` — pure function, no DB
      call; returns list of `{"field": str, "before": Any, "after": Any}` for changed fields only
    - Implement `export_audit(conn, *, entity_type, entity_id, date_from, date_to, fmt)` —
      raises `AuditExportTooLargeError` if result set > 100,000 rows; returns JSON or CSV string
    - _Requirements: 3.17, 10.1–10.9_

  - [ ]* 16.2 Write unit tests for audit (`tests/db/test_queries_audit.py`)
    - `insert_audit_event` then attempt UPDATE on `audit_log` raises `sqlite3.OperationalError`
    - `insert_audit_event` then attempt DELETE on `audit_log` raises `sqlite3.OperationalError`
    - `query_audit_log` pagination: page 1 returns first N rows ordered by `occurred_at DESC`
    - `diff_view` with unchanged fields omits them; changed fields appear with before/after
    - `export_audit` with > 100k rows raises `AuditExportTooLargeError`
    - `export_audit` with fmt="csv" returns valid CSV; fmt="json" returns valid JSON
    - _Requirements: 10.3, 10.4, 10.6, 10.7, 10.8, 10.9_

  - [ ]* 16.3 Write property test: audit log is append-only (Property 6)
    - Use Hypothesis to generate arbitrary sequences of query-module operations; after each,
      assert `audit_log` row count never decreases and no existing row changes
    - Tag: `Feature: smartstock-ai-architecture, Property 6`
    - **Validates: Requirements 10.3, 10.7**

- [ ] 17. Users query module
  - [~] 17.1 Implement `smartstock/db/queries/users.py`
    - Implement `insert_user`, `get_user`, `get_user_by_username`, `list_users`, `update_user`,
      `deactivate_user`
    - `insert_user` sets `created_at` to UTC ISO-8601 string
    - Username UNIQUE constraint enforced by DB
    - `deactivate_user` sets `is_active = 0`
    - _Requirements: 3.18_

  - [ ]* 17.2 Write unit tests for users (`tests/db/test_queries_users.py`)
    - Round-trip: insert → get → assert all fields
    - `get_user_by_username` returns correct user
    - Duplicate username raises `sqlite3.IntegrityError`
    - `deactivate_user` sets `is_active = 0`; `list_users(is_active=1)` excludes them
    - _Requirements: 3.18_

- [ ] 18. Timestamp format property test
  - [ ]* 18.1 Write property test: all timestamps are ISO-8601 UTC (Property 4)
    - Use Hypothesis to generate valid inputs for each insert function across all query modules;
      after each insert, assert every `*_at` column matches `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$`
      and date-only columns match `^\d{4}-\d{2}-\d{2}$`
    - Tag: `Feature: smartstock-ai-architecture, Property 4`
    - **Validates: Requirement 2.7**

- [~] 19. Checkpoint — all query modules
  - Ensure all query module tests pass: `pytest tests/db/ -k "not smoke" --tb=short`
  - Fix any issues before the final integration task.

- [ ] 20. End-to-end smoke test
  - [~] 20.1 Write end-to-end smoke test (`tests/db/test_smoke.py`)
    - Using the `mem_db` fixture, execute the following sequence in order:
      1. Insert a store, a supplier, a category
      2. Insert a product linked to that category
      3. Link the product to the supplier via `link_supplier`
      4. Upsert inventory for (store, product) with `quantity_on_hand = 100`
      5. Insert a sale for that (store, product) and verify `count_sale_days = 1`
      6. Insert a purchase order and a purchase order line; call `receive_order_line`
      7. Call `transfer_stock` and verify source/destination inventories
      8. Insert a forecast and call `replace_forecasts`; verify idempotency
      9. Register an ML model; verify `get_active_model` returns it
      10. Insert audit events and verify append-only triggers fire on UPDATE/DELETE
      11. Insert a user; verify `get_user_by_username` returns them
    - Assert that FK enforcement rejects an invalid cross-table reference in at least one step
    - Assert zero unexpected exceptions throughout the sequence
    - _Requirements: 2.1, 2.2, 3.1–3.21, 10.7, 13.1_

- [~] 21. Final checkpoint — full test suite
  - Run `pytest tests/db/ --cov=smartstock/db --cov-report=term-missing -q` and verify:
    - Zero test failures
    - Aggregate line coverage for `smartstock/db/` ≥ 80%
    - No individual module under `smartstock/db/` falls below 70% line coverage
  - Fix any failures or coverage gaps before marking this task complete.
  - _Requirements: 14.3, 14.6_

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP; core implementation
  tasks are always required
- Property-based test tasks each target one named property from the design's Correctness
  Properties section; the Hypothesis tag format is `Feature: smartstock-ai-architecture, Property N`
- All tests use `Config(db_path=":memory:")` — no test may write to a real file on disk
- The `mem_db` fixture (Task 4.1) is a prerequisite for every query-module and property test
- `smartstock/db/queries/transfers.py` depends on both `inventory.py` and `audit.py`; implement
  those two modules first
- Run coverage with: `pytest tests/db/ --cov=smartstock/db --cov-report=term-missing`

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "3.1"] },
    { "id": 3, "tasks": ["3.2"] },
    { "id": 4, "tasks": ["3.3", "3.4", "4.1"] },
    { "id": 5, "tasks": ["5.1", "5.2", "5.3", "7.1", "8.1", "9.1", "10.1", "11.1", "12.1", "13.1", "15.1", "16.1", "17.1"] },
    { "id": 6, "tasks": ["7.2", "8.2", "9.2", "10.2", "11.2", "12.2", "13.2", "15.2", "16.2", "17.2"] },
    { "id": 7, "tasks": ["11.3", "15.3", "15.4", "16.3", "18.1"] },
    { "id": 8, "tasks": ["14.1"] },
    { "id": 9, "tasks": ["14.2"] },
    { "id": 10, "tasks": ["20.1"] }
  ]
}
```
