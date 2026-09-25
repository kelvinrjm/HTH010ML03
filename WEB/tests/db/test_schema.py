"""Unit tests for schema initialization and version management."""

import sqlite3

from smartstock.config import Config
from smartstock.db.operations import list_tables, table_exists
from smartstock.db.schema import (
    get_current_version,
    init_schema,
    list_pending_migrations,
)


EXPECTED_TABLES = [
    "schema_version",
    "users",
    "categories",
    "suppliers",
    "products",
    "product_suppliers",
    "stores",
    "inventory",
    "customers",
    "sales",
    "sale_items",
    "purchases",
    "purchase_items",
    "stock_transfers",
    "audit_logs",
    "forecasts",
    "app_settings",
    "ml_model_registry",
    "dead_stock_flags",
    "allocation_plans",
    "allocation_plan_lines",
]

EXPECTED_VIEWS = [
    "store_inventory",
    "purchase_orders",
    "purchase_order_lines",
    "audit_log",
    "demand_forecasts",
    "sales_transactions",
]


def test_init_schema_creates_all_tables_and_views(mem_db: sqlite3.Connection):
    """Verify that every domain table and architectural view is created."""
    tables = list_tables(mem_db)

    for expected in EXPECTED_TABLES:
        assert expected in tables, f"Expected table '{expected}' not found in database"

    for expected_view in EXPECTED_VIEWS:
        assert table_exists(mem_db, expected_view), f"Expected view '{expected_view}' not found"


def test_schema_version_tracking(mem_db: sqlite3.Connection):
    """Verify schema version is recorded accurately."""
    current_ver = get_current_version(mem_db)
    assert current_ver == 19

    rows = mem_db.execute("SELECT version, description, applied_at FROM schema_version ORDER BY version").fetchall()
    assert len(rows) == 19
    assert rows[0]["version"] == 1
    assert rows[-1]["version"] == 19


def test_init_schema_is_idempotent(mem_db: sqlite3.Connection, mem_config: Config):
    """Verify that calling init_schema repeatedly causes no duplicate migrations or errors."""
    pending_before = list_pending_migrations(mem_db)
    assert len(pending_before) == 0

    # Call init_schema again
    init_schema(mem_db, mem_config)

    assert get_current_version(mem_db) == 19
    count = mem_db.execute("SELECT COUNT(*) FROM schema_version").fetchone()[0]
    assert count == 19


def test_app_settings_seeded(mem_db: sqlite3.Connection):
    """Verify that default app settings were seeded during migration 0017."""
    rows = mem_db.execute("SELECT setting_key, setting_value FROM app_settings").fetchall()
    settings_dict = {row["setting_key"]: row["setting_value"] for row in rows}

    assert "service_level_z_score" in settings_dict
    assert "ordering_cost" in settings_dict
    assert "holding_cost_rate" in settings_dict
    assert "dead_stock_threshold_days" in settings_dict
    assert float(settings_dict["holding_cost_rate"]) == 0.20
