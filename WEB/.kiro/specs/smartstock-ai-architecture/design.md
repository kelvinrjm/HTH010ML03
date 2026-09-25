# Design Document — SmartStock AI Database Layer

## Overview

This document covers the design of the SmartStock AI **database layer** in isolation. It is
intentionally scoped to the persistence tier: schema, migrations, connection management, and CRUD
query helpers. The ML pipeline, forecasting engines, and Streamlit UI are addressed in separate
design documents.

### Goals

- Define a normalised, FK-enforced SQLite schema that satisfies Requirements 2, 3, and the
  data-persistence portions of Requirements 4–11.
- Establish a versioned migration system so that schema evolution is traceable and repeatable.
- Provide thin, typed CRUD helpers that engines call; no business logic lives in `smartstock/db/`.
- Enable full test isolation via an in-memory SQLite database.

### Out of Scope

- Business logic (calculations, rules, recommendations) — belongs in `smartstock/engines/`.
- ML feature engineering and model artefacts — belongs in `smartstock/ml/`.
- Streamlit UI components.

---

## Architecture

### Layered Dependency Graph

```
┌─────────────────────────────────────────────────┐
│               Streamlit Pages (UI)              │
│           smartstock/pages/NN_*.py              │
└──────────────────────┬──────────────────────────┘
                       │ calls
┌──────────────────────▼──────────────────────────┐
│                   Engines                        │
│   smartstock/engines/{inventory,reorder,...}.py │
└──────┬──────────────────────────────┬───────────┘
       │ calls                        │ calls
┌──────▼──────────┐          ┌────────▼───────────┐
│   ML Pipeline   │          │   DB Query Layer   │
│ smartstock/ml/  │          │ smartstock/db/     │
│                 │          │  queries/*.py      │
└─────────────────┘          └────────┬───────────┘
                                      │ uses
                             ┌────────▼───────────┐
                             │  Connection Mgmt   │
                             │ db/connection.py   │
                             └────────┬───────────┘
                                      │
                             ┌────────▼───────────┐
                             │   SQLite File      │
                             │  data/smartstock   │
                             │        .db         │
                             └────────────────────┘
```

### File Layout

```
smartstock/
├── __init__.py
├── config.py                          ← DB path, all config constants
├── db/
│   ├── __init__.py
│   ├── connection.py                  ← get_connection(), context manager
│   ├── schema.py                      ← init_schema(), run_migrations()
│   ├── migrations/
│   │   ├── 0001_create_schema_version.sql
│   │   ├── 0002_create_stores.sql
│   │   ├── 0003_create_suppliers.sql
│   │   ├── 0004_create_categories.sql
│   │   ├── 0005_create_products.sql
│   │   ├── 0006_create_product_suppliers.sql
│   │   ├── 0007_create_store_inventory.sql
│   │   ├── 0008_create_sales_transactions.sql
│   │   ├── 0009_create_purchase_orders.sql
│   │   ├── 0010_create_purchase_order_lines.sql
│   │   ├── 0011_create_demand_forecasts.sql
│   │   ├── 0012_create_ml_model_registry.sql
│   │   ├── 0013_create_dead_stock_flags.sql
│   │   ├── 0014_create_allocation_plans.sql
│   │   ├── 0015_create_allocation_plan_lines.sql
│   │   ├── 0016_create_audit_log.sql
│   │   ├── 0017_create_app_settings.sql
│   │   └── 0018_create_users.sql
│   └── queries/
│       ├── __init__.py
│       ├── stores.py
│       ├── suppliers.py
│       ├── categories.py
│       ├── products.py
│       ├── inventory.py
│       ├── sales.py
│       ├── purchases.py
│       ├── transfers.py               ← stock_transfers view / helper
│       ├── forecasts.py
│       ├── audit.py
│       └── users.py
data/
└── smartstock.db                      ← runtime SQLite file (gitignored)
tests/
└── db/
    ├── __init__.py
    ├── conftest.py                    ← in-memory DB fixture
    ├── test_connection.py
    ├── test_schema.py
    ├── test_migrations.py
    ├── test_queries_stores.py
    ├── test_queries_suppliers.py
    ├── test_queries_categories.py
    ├── test_queries_products.py
    ├── test_queries_inventory.py
    ├── test_queries_sales.py
    ├── test_queries_purchases.py
    ├── test_queries_forecasts.py
    ├── test_queries_audit.py
    └── test_queries_users.py
```

---

## Components and Interfaces

### `smartstock/config.py`

Central source of truth for all configuration constants. No other module in `smartstock/` defines
application-level constants or reads environment variables.

```python
import os
from dataclasses import dataclass, field

@dataclass(frozen=True)
class Config:
    # --- Database ---
    db_path: str = field(
        default_factory=lambda: os.environ.get(
            "SMARTSTOCK_DB_PATH",
            os.path.join(os.path.dirname(__file__), "..", "data", "smartstock.db"),
        )
    )
    db_migration_timeout_seconds: int = 30

    # --- Forecasting ---
    forecast_horizon_days: int = 30          # default; range 7–90
    forecast_fallback_min_days: int = 30     # min sales days before ML kicks in

    # --- Reorder ---
    service_level_z_score: float = 1.65
    ordering_cost: float = 25.0              # monetary value per order
    holding_cost_rate: float = 0.20          # fraction of unit cost per year

    # --- Dead Stock ---
    dead_stock_threshold_days: int = 90      # range 1–3650

    # --- ML retraining ---
    retraining_min_new_records: int = 500    # range 1–1_000_000

    # --- Allocation weights (must sum to 1.0 ± 0.001) ---
    alloc_weight_stock_health: float = 0.40
    alloc_weight_days_remaining: float = 0.35
    alloc_weight_revenue: float = 0.25


# Module-level singleton used across the application.
# Tests override by passing an explicit Config instance to functions.
DEFAULT_CONFIG = Config()
```

**Design rationale:** A frozen dataclass makes the config hashable and immutable at runtime.
Tests can construct `Config(db_path=":memory:")` and pass it explicitly to any function that
accepts a `config` parameter, achieving full isolation without monkeypatching.

---

### `smartstock/db/connection.py`

Manages SQLite connections. Every connection:
- Enables `PRAGMA foreign_keys = ON` immediately after opening.
- Enables WAL journal mode for concurrent read performance.
- Uses `row_factory = sqlite3.Row` so callers can access columns by name.
- Is yielded as a context manager; the caller owns commit/rollback.

```python
import sqlite3
import contextlib
from smartstock.config import Config, DEFAULT_CONFIG

@contextlib.contextmanager
def get_connection(config: Config = DEFAULT_CONFIG):
    """
    Yield an open sqlite3.Connection for the path in `config.db_path`.

    On enter:
      - Opens the connection.
      - Executes PRAGMA foreign_keys = ON.
      - Executes PRAGMA journal_mode = WAL.
      - Sets row_factory = sqlite3.Row.
    On exit:
      - Commits if no exception was raised.
      - Rolls back if an exception is active.
      - Closes the connection unconditionally.

    Raises:
        sqlite3.OperationalError: if the file path is inaccessible.
    """
    ...

def open_connection(config: Config = DEFAULT_CONFIG) -> sqlite3.Connection:
    """
    Open and configure a connection without a context manager.
    Callers are responsible for commit/rollback/close.
    Prefer get_connection() in production code.
    """
    ...
```

**Design rationale:** A context manager is the only way connections are created in production
code. This prevents connection leaks and ensures PRAGMA settings are always applied. The
lower-level `open_connection` is provided for test fixtures that need manual lifecycle control.

---

### `smartstock/db/schema.py`

Responsible for schema initialisation and migration execution. Nothing else.

```python
import sqlite3
from pathlib import Path
from smartstock.config import Config, DEFAULT_CONFIG

MIGRATIONS_DIR = Path(__file__).parent / "migrations"

def init_schema(conn: sqlite3.Connection, config: Config = DEFAULT_CONFIG) -> None:
    """
    Run all pending migrations in ascending version order.

    Steps:
      1. Ensure the schema_version table exists (bootstrap migration 0001).
      2. Read applied versions from schema_version.
      3. Discover all *.sql files in MIGRATIONS_DIR, sort by version prefix.
      4. For each unapplied migration (in order):
         a. Begin a transaction.
         b. Execute the SQL.
         c. INSERT into schema_version.
         d. Commit.
         e. On any error: rollback, raise MigrationError with version and reason.
      6. Total wall-clock time is enforced against config.db_migration_timeout_seconds.

    Raises:
        MigrationError: if any migration fails. Schema is left at the last
                        successfully applied version.
        MigrationTimeoutError: if total migration time exceeds the configured limit.
    """
    ...

def get_current_version(conn: sqlite3.Connection) -> int:
    """Return the highest applied migration version, or 0 if none applied."""
    ...

def list_pending_migrations(conn: sqlite3.Connection) -> list[Path]:
    """Return sorted list of migration files not yet applied."""
    ...


class MigrationError(Exception):
    """Raised when a migration script fails to apply."""
    def __init__(self, version: int, description: str, cause: Exception): ...

class MigrationTimeoutError(Exception):
    """Raised when migrations exceed the configured timeout."""
    ...
```

---

### `smartstock/db/queries/` — Domain Query Modules

Each module exposes typed CRUD functions. All functions accept a `conn: sqlite3.Connection`
as their first argument; they never open their own connections. No function contains business
logic — no if-branches that implement domain rules.

Return types use `TypedDict` or `sqlite3.Row` so callers receive named-column access.

Common conventions across all query modules:

| Convention | Detail |
|---|---|
| `get_by_id(conn, id)` | Returns `sqlite3.Row \| None` |
| `list_*(conn, **filters)` | Returns `list[sqlite3.Row]`; accepts optional keyword filters |
| `insert_*(conn, **fields)` → `int` | Returns the new rowid |
| `update_*(conn, id, **fields)` → `bool` | Returns True if a row was updated |
| `delete_*(conn, id)` → `bool` | Returns True if a row was deleted (soft-deletes set `is_active = 0`) |
| `exists_*(conn, **fields)` → `bool` | Cheap existence check |

#### `queries/stores.py`

```python
def insert_store(conn, *, name: str, location: str, store_type: str,
                 is_active: int = 1) -> int: ...
def get_store(conn, store_id: int) -> sqlite3.Row | None: ...
def list_stores(conn, *, is_active: int | None = None) -> list[sqlite3.Row]: ...
def update_store(conn, store_id: int, **fields) -> bool: ...
def deactivate_store(conn, store_id: int) -> bool: ...
```

#### `queries/suppliers.py`

```python
def insert_supplier(conn, *, name: str, contact_info: str,
                    default_lead_time_days: int, is_active: int = 1) -> int: ...
def get_supplier(conn, supplier_id: int) -> sqlite3.Row | None: ...
def list_suppliers(conn, *, is_active: int | None = None) -> list[sqlite3.Row]: ...
def update_supplier(conn, supplier_id: int, **fields) -> bool: ...
def deactivate_supplier(conn, supplier_id: int) -> bool: ...
```

#### `queries/categories.py`

```python
def insert_category(conn, *, name: str,
                    parent_category_id: int | None = None) -> int: ...
def get_category(conn, category_id: int) -> sqlite3.Row | None: ...
def list_categories(conn, *, parent_category_id: int | None = None) -> list[sqlite3.Row]: ...
def get_category_tree(conn) -> list[sqlite3.Row]: ...  # recursive CTE
def update_category(conn, category_id: int, **fields) -> bool: ...
```

#### `queries/products.py`

```python
def insert_product(conn, *, sku: str, name: str, description: str,
                   category_id: int, unit_of_measure: str,
                   unit_cost: float, unit_price: float,
                   is_active: int = 1) -> int: ...
def get_product(conn, product_id: int) -> sqlite3.Row | None: ...
def get_product_by_sku(conn, sku: str) -> sqlite3.Row | None: ...
def list_products(conn, *, category_id: int | None = None,
                  is_active: int | None = None) -> list[sqlite3.Row]: ...
def update_product(conn, product_id: int, **fields) -> bool: ...
def deactivate_product(conn, product_id: int) -> bool: ...
def sku_exists(conn, sku: str) -> bool: ...

# Product-supplier links
def link_supplier(conn, *, product_id: int, supplier_id: int,
                  supplier_sku: str, lead_time_days: int,
                  minimum_order_quantity: int,
                  is_preferred: int = 0) -> int: ...
def get_preferred_supplier(conn, product_id: int) -> sqlite3.Row | None: ...
def list_product_suppliers(conn, product_id: int) -> list[sqlite3.Row]: ...
def update_product_supplier(conn, product_supplier_id: int, **fields) -> bool: ...
```

#### `queries/inventory.py`

```python
def upsert_inventory(conn, *, store_id: int, product_id: int,
                     quantity_on_hand: int, quantity_reserved: int = 0,
                     quantity_on_order: int = 0,
                     reorder_point: int = 0,
                     reorder_quantity: int = 0) -> int: ...
def get_inventory(conn, store_id: int,
                  product_id: int) -> sqlite3.Row | None: ...
def list_inventory(conn, *, store_id: int | None = None,
                   product_id: int | None = None) -> list[sqlite3.Row]: ...
def increment_on_hand(conn, store_id: int, product_id: int,
                      delta: int) -> bool: ...
def decrement_on_hand(conn, store_id: int, product_id: int,
                      delta: int) -> bool: ...
    # Raises InsufficientStockError if result would go below zero.
def update_reorder_params(conn, store_id: int, product_id: int,
                          reorder_point: int,
                          reorder_quantity: int) -> bool: ...
def snapshot_all(conn) -> list[sqlite3.Row]: ...
    # Returns all rows; no caching.

class InsufficientStockError(Exception):
    def __init__(self, store_id: int, product_id: int, shortfall: int): ...
```

#### `queries/sales.py`

```python
def insert_sale(conn, *, store_id: int, product_id: int,
                transaction_date: str, quantity_sold: int,
                unit_price_at_sale: float,
                channel: str) -> int: ...
def get_sale(conn, transaction_id: int) -> sqlite3.Row | None: ...
def list_sales(conn, *, store_id: int | None = None,
               product_id: int | None = None,
               date_from: str | None = None,
               date_to: str | None = None,
               channel: str | None = None) -> list[sqlite3.Row]: ...
def daily_sales_summary(conn, store_id: int, product_id: int,
                        days: int = 30) -> list[sqlite3.Row]: ...
    # Returns rows: (transaction_date, total_sold)
def rolling_mean_demand(conn, store_id: int, product_id: int,
                        days: int = 30) -> float | None: ...
def rolling_stddev_demand(conn, store_id: int, product_id: int,
                          days: int = 30) -> float | None: ...
def count_sale_days(conn, store_id: int, product_id: int) -> int: ...
    # Returns distinct transaction_dates with quantity_sold > 0
```

#### `queries/purchases.py`

```python
# Purchase orders
def insert_purchase_order(conn, *, store_id: int, supplier_id: int,
                          order_date: str,
                          expected_delivery_date: str,
                          status: str = "draft") -> int: ...
def get_purchase_order(conn, order_id: int) -> sqlite3.Row | None: ...
def list_purchase_orders(conn, *, store_id: int | None = None,
                         supplier_id: int | None = None,
                         status: str | None = None) -> list[sqlite3.Row]: ...
def update_order_status(conn, order_id: int, status: str) -> bool: ...

# Purchase order lines
def insert_order_line(conn, *, order_id: int, product_id: int,
                      quantity_ordered: int, unit_cost: float) -> int: ...
def get_order_line(conn, line_id: int) -> sqlite3.Row | None: ...
def list_order_lines(conn, order_id: int) -> list[sqlite3.Row]: ...
def receive_order_line(conn, line_id: int,
                       quantity_received: int) -> bool: ...
    # Raises ValueError if quantity_received > quantity_ordered.
def list_orders_for_reorder_report(conn, *,
    store_id: int | None = None) -> list[sqlite3.Row]: ...
```

#### `queries/transfers.py`

Stock transfers are not a first-class table (not in the requirements schema), but the
Inventory_Engine needs an atomic "move stock from store A to store B" helper:

```python
def transfer_stock(conn, *, from_store_id: int, to_store_id: int,
                   product_id: int, quantity: int,
                   actor: str, notes: str = "") -> None: ...
    # Decrements from_store, increments to_store, and writes two audit events,
    # all in the same transaction passed in by the caller.
    # Raises InsufficientStockError if from_store would go negative.
```

#### `queries/forecasts.py`

```python
def insert_forecast(conn, *, store_id: int, product_id: int,
                    forecast_date: str, forecast_horizon_days: int,
                    predicted_quantity: float, lower_bound: float,
                    upper_bound: float, confidence_level: float,
                    model_version: str) -> int: ...
def get_forecast(conn, forecast_id: int) -> sqlite3.Row | None: ...
def list_forecasts(conn, *, store_id: int | None = None,
                   product_id: int | None = None,
                   forecast_date: str | None = None) -> list[sqlite3.Row]: ...
def replace_forecasts(conn, store_id: int, product_id: int,
                      horizon_days: int,
                      rows: list[dict]) -> None: ...
    # Deletes existing rows for (store, product, horizon), then bulk-inserts rows.
    # Wrapped in a single transaction.
def list_forecasts_by_period(conn, store_id: int, product_id: int,
                             period: str) -> list[sqlite3.Row]: ...
    # period: "weekly" | "monthly"

# ML model registry
def register_model(conn, *, model_name: str, model_version: str,
                   model_type: str, target: str, artefact_path: str,
                   training_mae: float, training_rmse: float,
                   training_r2: float) -> int: ...
def get_active_model(conn, target: str) -> sqlite3.Row | None: ...
def deactivate_models(conn, target: str) -> int: ...
    # Sets all is_active=0 for the target; returns count updated.
def list_models(conn, *, target: str | None = None,
                is_active: int | None = None) -> list[sqlite3.Row]: ...
```

#### `queries/audit.py`

```python
from typing import Any

def insert_audit_event(conn, *, event_type: str, entity_type: str,
                       entity_id: int | str, actor: str,
                       before_state: Any | None,
                       after_state: Any | None,
                       notes: str = "") -> int: ...
    # Serialises before_state / after_state to JSON strings.
    # occurred_at is set to utcnow() inside this function.

def query_audit_log(conn, *, entity_type: str | None = None,
                    entity_id: int | str | None = None,
                    event_type: str | None = None,
                    date_from: str | None = None,
                    date_to: str | None = None,
                    actor: str | None = None,
                    page: int = 1,
                    page_size: int = 100) -> tuple[list[sqlite3.Row], int]: ...
    # Returns (rows, total_count). Ordered by occurred_at DESC.
    # page and page_size implement LIMIT/OFFSET pagination.

def diff_view(before_state: dict | None,
              after_state: dict | None) -> list[dict]: ...
    # Pure function — no DB call.
    # Returns list of {"field": str, "before": Any, "after": Any}
    # for fields that changed. Omits unchanged fields.

def export_audit(conn, *, entity_type: str | None = None,
                 entity_id: int | str | None = None,
                 date_from: str | None = None,
                 date_to: str | None = None,
                 fmt: str = "json") -> str: ...
    # Returns JSON string or CSV string.
    # Raises AuditExportTooLargeError if result set > 100_000 rows.

class AuditExportTooLargeError(Exception): ...
```

#### `queries/users.py`

```python
def insert_user(conn, *, username: str, password_hash: str,
                role: str, is_active: int = 1) -> int: ...
def get_user(conn, user_id: int) -> sqlite3.Row | None: ...
def get_user_by_username(conn, username: str) -> sqlite3.Row | None: ...
def list_users(conn, *, is_active: int | None = None) -> list[sqlite3.Row]: ...
def update_user(conn, user_id: int, **fields) -> bool: ...
def deactivate_user(conn, user_id: int) -> bool: ...
```

---

## Data Models

### Entity-Relationship Overview

```
categories (self-ref parent_category_id)
    │
    ▼
products ──────────────────► product_suppliers ◄── suppliers
    │                                │
    │                                ▼
    │                          (lead_time, MOQ)
    │
    ├──► store_inventory ◄── stores
    │         │
    │         ▼
    ├──► sales_transactions
    │
    ├──► purchase_order_lines ◄── purchase_orders ◄── suppliers
    │                                                       │
    │                                                    stores
    │
    ├──► demand_forecasts ◄── stores
    │
    ├──► dead_stock_flags ◄── stores
    │
    ├──► allocation_plan_lines ◄── allocation_plans
    │                                    │
    │                                 stores
    │
    └──► ml_model_registry (no FK to products; target-level)

audit_log (references any entity by entity_type + entity_id)
app_settings (key-value; no FKs)
users (standalone; actor name stored as string in audit_log)
```

---

### Full DDL

All DDL is embedded in the numbered migration files. The canonical definitions follow.

---

#### `0001_create_schema_version.sql`

```sql
CREATE TABLE IF NOT EXISTS schema_version (
    version       INTEGER NOT NULL,
    description   TEXT    NOT NULL,
    applied_at    TEXT    NOT NULL   -- ISO-8601 UTC
);
```

---

#### `0002_create_stores.sql`

```sql
CREATE TABLE stores (
    store_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    location    TEXT    NOT NULL DEFAULT '',
    store_type  TEXT    NOT NULL CHECK (store_type IN ('retail', 'wholesale')),
    is_active   INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at  TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_stores_is_active  ON stores (is_active);
CREATE INDEX idx_stores_store_type ON stores (store_type);
```

---

#### `0003_create_suppliers.sql`

```sql
CREATE TABLE suppliers (
    supplier_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   TEXT    NOT NULL,
    contact_info           TEXT    NOT NULL DEFAULT '',
    default_lead_time_days INTEGER NOT NULL DEFAULT 7
                               CHECK (default_lead_time_days BETWEEN 1 AND 365),
    is_active              INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at             TEXT    NOT NULL
);

CREATE INDEX idx_suppliers_is_active ON suppliers (is_active);
```

---

#### `0004_create_categories.sql`

```sql
CREATE TABLE categories (
    category_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name               TEXT    NOT NULL,
    parent_category_id INTEGER REFERENCES categories (category_id)
                           ON DELETE RESTRICT,
    created_at         TEXT    NOT NULL
);

CREATE INDEX idx_categories_parent_category_id
    ON categories (parent_category_id);
```

---

#### `0005_create_products.sql`

```sql
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
    created_at      TEXT    NOT NULL
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_is_active   ON products (is_active);
CREATE INDEX idx_products_sku         ON products (sku);
```

---

#### `0006_create_product_suppliers.sql`

```sql
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
    created_at              TEXT    NOT NULL
);

CREATE INDEX idx_product_suppliers_product_id  ON product_suppliers (product_id);
CREATE INDEX idx_product_suppliers_supplier_id ON product_suppliers (supplier_id);
CREATE INDEX idx_product_suppliers_is_preferred
    ON product_suppliers (product_id, is_preferred);
```

---

#### `0007_create_store_inventory.sql`

```sql
CREATE TABLE store_inventory (
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
    last_updated       TEXT    NOT NULL,

    UNIQUE (store_id, product_id),
    -- quantity_reserved must not exceed quantity_on_hand
    CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_store_inventory_store_id   ON store_inventory (store_id);
CREATE INDEX idx_store_inventory_product_id ON store_inventory (product_id);
```

---

#### `0008_create_sales_transactions.sql`

```sql
CREATE TABLE sales_transactions (
    transaction_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id           INTEGER NOT NULL
                           REFERENCES stores   (store_id)
                           ON DELETE RESTRICT,
    product_id         INTEGER NOT NULL
                           REFERENCES products (product_id)
                           ON DELETE RESTRICT,
    transaction_date   TEXT    NOT NULL,   -- ISO-8601 date (YYYY-MM-DD)
    quantity_sold      INTEGER NOT NULL CHECK (quantity_sold > 0),
    unit_price_at_sale REAL    NOT NULL CHECK (unit_price_at_sale >= 0),
    channel            TEXT    NOT NULL
                           CHECK (channel IN ('in-store', 'online', 'wholesale')),
    created_at         TEXT    NOT NULL
);

CREATE INDEX idx_sales_transactions_store_id
    ON sales_transactions (store_id);
CREATE INDEX idx_sales_transactions_product_id
    ON sales_transactions (product_id);
CREATE INDEX idx_sales_transactions_transaction_date
    ON sales_transactions (transaction_date);
CREATE INDEX idx_sales_transactions_store_product_date
    ON sales_transactions (store_id, product_id, transaction_date);
```

---

#### `0009_create_purchase_orders.sql`

```sql
CREATE TABLE purchase_orders (
    order_id                INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id                INTEGER NOT NULL
                                REFERENCES stores    (store_id)
                                ON DELETE RESTRICT,
    supplier_id             INTEGER NOT NULL
                                REFERENCES suppliers (supplier_id)
                                ON DELETE RESTRICT,
    order_date              TEXT    NOT NULL,
    expected_delivery_date  TEXT    NOT NULL,
    status                  TEXT    NOT NULL DEFAULT 'draft'
                                CHECK (status IN
                                    ('draft','submitted','received','cancelled')),
    created_at              TEXT    NOT NULL
);

CREATE INDEX idx_purchase_orders_store_id    ON purchase_orders (store_id);
CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX idx_purchase_orders_status      ON purchase_orders (status);
```

---

#### `0010_create_purchase_order_lines.sql`

```sql
CREATE TABLE purchase_order_lines (
    line_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id           INTEGER NOT NULL
                           REFERENCES purchase_orders (order_id)
                           ON DELETE CASCADE,
    product_id         INTEGER NOT NULL
                           REFERENCES products        (product_id)
                           ON DELETE RESTRICT,
    quantity_ordered   INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_received  INTEGER NOT NULL DEFAULT 0
                           CHECK (quantity_received >= 0),
    unit_cost          REAL    NOT NULL CHECK (unit_cost >= 0),
    created_at         TEXT    NOT NULL,

    -- quantity_received must not exceed quantity_ordered
    CHECK (quantity_received <= quantity_ordered)
);

CREATE INDEX idx_purchase_order_lines_order_id   ON purchase_order_lines (order_id);
CREATE INDEX idx_purchase_order_lines_product_id ON purchase_order_lines (product_id);
```

---

#### `0011_create_demand_forecasts.sql`

```sql
CREATE TABLE demand_forecasts (
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
    generated_at          TEXT    NOT NULL
);

CREATE INDEX idx_demand_forecasts_store_id
    ON demand_forecasts (store_id);
CREATE INDEX idx_demand_forecasts_product_id
    ON demand_forecasts (product_id);
CREATE INDEX idx_demand_forecasts_forecast_date
    ON demand_forecasts (forecast_date);
CREATE INDEX idx_demand_forecasts_store_product
    ON demand_forecasts (store_id, product_id, forecast_horizon_days);
```

---

#### `0012_create_ml_model_registry.sql`

```sql
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
```

The partial unique index on `(target) WHERE is_active = 1` enforces the invariant that at most
one active model exists per target at the database level, without relying on application code
alone.

---

#### `0013_create_dead_stock_flags.sql`

```sql
CREATE TABLE dead_stock_flags (
    flag_id                INTEGER PRIMARY KEY AUTOINCREMENT,
    store_id               INTEGER NOT NULL
                               REFERENCES stores   (store_id)
                               ON DELETE RESTRICT,
    product_id             INTEGER NOT NULL
                               REFERENCES products (product_id)
                               ON DELETE RESTRICT,
    flagged_at             TEXT    NOT NULL,
    days_without_sale      INTEGER NOT NULL CHECK (days_without_sale >= 0),
    quantity_at_risk       INTEGER NOT NULL CHECK (quantity_at_risk >= 0),
    estimated_value_at_risk REAL,                  -- NULL when unit_cost unavailable
    recommendation         TEXT    NOT NULL
                               CHECK (recommendation IN
                                   ('markdown', 'discount', 'write-off')),
    resolved_at            TEXT,                   -- NULL = unresolved
    resolution_notes       TEXT    NOT NULL DEFAULT ''
                               CHECK (length(resolution_notes) <= 1000)
);

CREATE INDEX idx_dead_stock_flags_store_id   ON dead_stock_flags (store_id);
CREATE INDEX idx_dead_stock_flags_product_id ON dead_stock_flags (product_id);
CREATE INDEX idx_dead_stock_flags_resolved_at
    ON dead_stock_flags (resolved_at);
-- Partial index for fast lookup of open flags
CREATE INDEX idx_dead_stock_flags_unresolved
    ON dead_stock_flags (store_id, product_id) WHERE resolved_at IS NULL;
```

---

#### `0014_create_allocation_plans.sql`

```sql
CREATE TABLE allocation_plans (
    plan_id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id                INTEGER NOT NULL
                                  REFERENCES products (product_id)
                                  ON DELETE RESTRICT,
    allocation_rule           TEXT    NOT NULL
                                  CHECK (allocation_rule IN
                                      ('proportional', 'priority')),
    total_quantity_available  INTEGER NOT NULL
                                  CHECK (total_quantity_available >= 0),
    plan_date                 TEXT    NOT NULL,
    created_at                TEXT    NOT NULL
);

CREATE INDEX idx_allocation_plans_product_id ON allocation_plans (product_id);
CREATE INDEX idx_allocation_plans_plan_date  ON allocation_plans (plan_date);
```

---

#### `0015_create_allocation_plan_lines.sql`

```sql
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
```

---

#### `0016_create_audit_log.sql`

```sql
CREATE TABLE audit_log (
    audit_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type   TEXT    NOT NULL
                     CHECK (length(event_type) BETWEEN 1 AND 100),
    entity_type  TEXT    NOT NULL,
    entity_id    TEXT    NOT NULL,   -- stored as text; ints and UUIDs both fit
    actor        TEXT    NOT NULL
                     CHECK (length(actor) BETWEEN 1 AND 255),
    occurred_at  TEXT    NOT NULL,
    before_state TEXT,               -- JSON or NULL for creation events
    after_state  TEXT,               -- JSON or NULL for deletion events
    notes        TEXT    NOT NULL DEFAULT ''
);

CREATE INDEX idx_audit_log_entity_type  ON audit_log (entity_type);
CREATE INDEX idx_audit_log_entity_id    ON audit_log (entity_id);
CREATE INDEX idx_audit_log_event_type   ON audit_log (event_type);
CREATE INDEX idx_audit_log_occurred_at  ON audit_log (occurred_at);
CREATE INDEX idx_audit_log_actor        ON audit_log (actor);
```

No UPDATE or DELETE triggers are created. Append-only behaviour is enforced by:
1. The absence of `update_audit` / `delete_audit` functions in `queries/audit.py`.
2. A SQLite trigger that rejects any UPDATE or DELETE on `audit_log`:

```sql
CREATE TRIGGER audit_log_no_update
BEFORE UPDATE ON audit_log
BEGIN
    SELECT RAISE(ABORT, 'audit_log is append-only: UPDATE not permitted');
END;

CREATE TRIGGER audit_log_no_delete
BEFORE DELETE ON audit_log
BEGIN
    SELECT RAISE(ABORT, 'audit_log is append-only: DELETE not permitted');
END;
```

---

#### `0017_create_app_settings.sql`

```sql
CREATE TABLE app_settings (
    setting_key   TEXT NOT NULL PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   TEXT NOT NULL DEFAULT '',
    updated_at    TEXT NOT NULL
);
```

Seed rows are inserted in a follow-up migration (`0019_seed_app_settings.sql`) to keep DDL
separate from data.

---

#### `0018_create_users.sql`

```sql
CREATE TABLE users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL
                      CHECK (role IN ('admin', 'manager', 'viewer')),
    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at    TEXT    NOT NULL
);

CREATE INDEX idx_users_username  ON users (username);
CREATE INDEX idx_users_is_active ON users (is_active);
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a
system — essentially, a formal statement about what the system should do. Properties serve as the
bridge between human-readable specifications and machine-verifiable correctness guarantees.*

---

### Property 1: Every connection enforces foreign-key constraints

*For any* connection returned by `get_connection()` or `open_connection()`, inserting a row that
references a non-existent parent key in any FK column SHALL raise `sqlite3.IntegrityError`.

**Validates: Requirements 2.2, 3.19**

---

### Property 2: Migration application is monotonically ordered

*For any* non-empty set of migration files present in `migrations/`, after `init_schema()` completes
successfully, the `schema_version` rows SHALL be ordered strictly ascending by `version` with no
gaps between the lowest and highest applied version.

**Validates: Requirements 2.3, 2.4, 2.5**

---

### Property 3: Failed migration leaves schema unchanged

*For any* migration file whose SQL is syntactically or semantically invalid, running
`init_schema()` SHALL leave the `schema_version` table at its state before that migration was
attempted and SHALL NOT partially apply the failed migration's DDL.

**Validates: Requirement 2.9**

---

### Property 4: All timestamp values are ISO-8601 UTC strings

*For any* record inserted via any function in `smartstock/db/queries/`, every column whose name
ends with `_at` or equals `transaction_date`, `order_date`, `expected_delivery_date`,
`forecast_date`, or `plan_date` SHALL match the pattern `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$`
(for `*_at` columns) or `^\d{4}-\d{2}-\d{2}$` (for date-only columns).

**Validates: Requirement 2.7**

---

### Property 5: Inventory on-hand never goes negative

*For any* sequence of `decrement_on_hand` calls, if the resulting `quantity_on_hand` would fall
below zero, the function SHALL raise `InsufficientStockError` and the value in `store_inventory`
SHALL remain unchanged.

**Validates: Requirements 6.11, 13.1**

---

### Property 6: Audit log is append-only

*For any* call sequence that includes `insert_audit_event`, followed by any other combination of
query-module calls, the total row count of `audit_log` SHALL never decrease and no existing row's
content SHALL change.

**Validates: Requirements 10.3, 10.7**

---

### Property 7: Exactly one active model per target

*For any* sequence of `register_model` calls for the same target, after each call completes, the
count of rows in `ml_model_registry` where `target = T AND is_active = 1` SHALL equal exactly 1.

**Validates: Requirement 4.7**

---

### Property 8: replace_forecasts is idempotent

*For any* (store_id, product_id, horizon_days) triple and any list of forecast rows, calling
`replace_forecasts` twice with the same arguments SHALL produce the same resulting rows in
`demand_forecasts` as calling it once — no duplicates accumulate.

**Validates: Requirement 5.5**

---

## Error Handling

| Scenario | Module | Behaviour |
|---|---|---|
| SQLite file path inaccessible | `connection.py` | Raises `sqlite3.OperationalError` with the path; caller surfaces to operator. |
| Migration script syntax error | `schema.py` | Rolls back the transaction, raises `MigrationError(version, reason)`. |
| Migration timeout exceeded | `schema.py` | Raises `MigrationTimeoutError`; no partial schema change is committed. |
| FK violation on insert/update | Any query module | Propagates `sqlite3.IntegrityError` to the calling engine unchanged. |
| `decrement_on_hand` below zero | `queries/inventory.py` | Raises `InsufficientStockError(store_id, product_id, shortfall)`. |
| `receive_order_line` qty > ordered | `queries/purchases.py` | Raises `ValueError` with a clear message. |
| `audit_log` UPDATE or DELETE | SQLite trigger | Raises `sqlite3.OperationalError` with message "audit_log is append-only". |
| Audit export exceeds 100k rows | `queries/audit.py` | Raises `AuditExportTooLargeError`; no partial file produced. |
| Missing model artefact | `queries/forecasts.py` | `get_active_model` returns the row; artefact existence is the ML layer's concern. |
| Invalid config weight range | `config.py` | Validated at `Config.__post_init__` via `__post_init__`; raises `ValueError`. |

---

## Testing Strategy

### Approach

The test suite uses **pytest** with an in-memory SQLite fixture. No external services are needed.
Each test file is isolated: the fixture creates a fresh `":memory:"` database and runs all
migrations before the test, then tears it down after.

Because the DB layer is pure Python + stdlib, tests run fast enough that a property-based
approach (using **Hypothesis**) is cost-effective for the universal invariants identified above.

### Test Fixture (`tests/db/conftest.py`)

```python
import pytest
import sqlite3
from smartstock.config import Config
from smartstock.db.connection import open_connection
from smartstock.db.schema import init_schema

@pytest.fixture
def mem_db() -> sqlite3.Connection:
    config = Config(db_path=":memory:")
    conn = open_connection(config)
    init_schema(conn, config)
    yield conn
    conn.close()
```

### Unit / Example-Based Tests

| File | What is tested |
|---|---|
| `test_connection.py` | PRAGMA settings applied, WAL mode set, row_factory active |
| `test_schema.py` | All 18 tables exist after init; `schema_version` row count = 18 |
| `test_migrations.py` | Ascending order regardless of filesystem order; failed migration leaves version unchanged |
| `test_queries_stores.py` | Insert, get, list, deactivate round-trips; FK rejection |
| `test_queries_products.py` | SKU uniqueness constraint; `get_product_by_sku` |
| `test_queries_inventory.py` | `upsert_inventory`, `increment_on_hand`, `decrement_on_hand` below zero raises |
| `test_queries_sales.py` | `rolling_mean_demand` with no data returns None; with data returns correct float |
| `test_queries_purchases.py` | `receive_order_line` rejects qty > ordered; status transition helpers |
| `test_queries_forecasts.py` | `replace_forecasts` deletes old rows then inserts; `get_active_model` returns correct row |
| `test_queries_audit.py` | Insert then UPDATE raises; `diff_view` pure function correctness; export size limit |

### Property-Based Tests (Hypothesis)

Each property-based test configures Hypothesis for **≥ 100 iterations**.

```python
# Tag format: Feature: smartstock-ai-architecture, Property {N}: {text}
```

| Test | Property | Tag |
|---|---|---|
| `test_fk_enforcement` | Property 1: FK violation raises IntegrityError for any FK column | `Feature: smartstock-ai-architecture, Property 1` |
| `test_migration_order` | Property 2: migrations applied in ascending order for any file set | `Feature: smartstock-ai-architecture, Property 2` |
| `test_migration_rollback` | Property 3: failed migration leaves schema_version unchanged | `Feature: smartstock-ai-architecture, Property 3` |
| `test_timestamp_format` | Property 4: all *_at columns match ISO-8601 UTC for any valid insert | `Feature: smartstock-ai-architecture, Property 4` |
| `test_inventory_no_negative` | Property 5: decrement below zero raises, value unchanged | `Feature: smartstock-ai-architecture, Property 5` |
| `test_audit_append_only` | Property 6: audit_log row count never decreases | `Feature: smartstock-ai-architecture, Property 6` |
| `test_single_active_model` | Property 7: exactly one active model per target after any register sequence | `Feature: smartstock-ai-architecture, Property 7` |
| `test_replace_forecasts_idempotent` | Property 8: calling replace_forecasts twice = calling it once | `Feature: smartstock-ai-architecture, Property 8` |

### Coverage Target

- All modules under `smartstock/db/` must reach **≥ 80% line coverage** in aggregate.
- No single module may fall below **70% line coverage** (Requirement 14.6).
- Run with: `pytest tests/db/ --cov=smartstock/db --cov-report=term-missing`

### Isolation Guarantee

- No test may write to a real file on disk; all tests use `Config(db_path=":memory:")`.
- Tests must not share state between test functions (each uses the `mem_db` fixture freshly).
- No test may import Streamlit or any engine module.
