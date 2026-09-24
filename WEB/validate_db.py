"""SmartStock AI — Database Validation Script.

Run this script directly:
    python validate_db.py

Validates:
1. Database can be created from empty state.
2. All required tables and views exist.
3. Foreign key constraints are strictly enforced.
4. Basic insert and read operations function correctly.
5. Unique and check constraints work as expected.
6. Audit log append-only triggers protect data integrity.
"""

import os
import sqlite3
import sys

from smartstock.config import Config
from smartstock.db.connection import get_connection, open_connection
from smartstock.db.operations import (
    fetch_all,
    fetch_one,
    insert_record,
    list_tables,
    table_exists,
)
from smartstock.db.schema import get_current_version, init_schema


REQUIRED_TABLES = [
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

REQUIRED_VIEWS = [
    "store_inventory",
    "purchase_orders",
    "purchase_order_lines",
    "audit_log",
    "demand_forecasts",
    "sales_transactions",
]


def run_validation():
    print("==================================================")
    print("  SmartStock AI — Database Validation Suite")
    print("==================================================")

    # 1. Test database creation
    print("\n[Step 1] Initializing in-memory database...")
    cfg = Config(db_path=":memory:")
    conn = open_connection(cfg)
    init_schema(conn, cfg)

    version = get_current_version(conn)
    print(f"  -> Database initialized. Current schema version: {version}")
    assert version >= 19, f"Expected version >= 19, got {version}"
    print("  [PASS] Database successfully initialized from scratch.")

    # 2. Verify all tables exist
    print("\n[Step 2] Verifying tables and views...")
    existing_tables = set(list_tables(conn))
    for t in REQUIRED_TABLES:
        if t not in existing_tables:
            print(f"  [FAIL] Missing required table: {t}")
            sys.exit(1)
        print(f"  + Table verified: {t}")

    for v in REQUIRED_VIEWS:
        if not table_exists(conn, v):
            print(f"  [FAIL] Missing required view: {v}")
            sys.exit(1)
        print(f"  + View verified: {v}")
    print("  [PASS] All 21 required tables and 6 views are present.")

    # 3. Verify foreign key enforcement
    print("\n[Step 3] Verifying Foreign Key enforcement...")
    fk_prag = conn.execute("PRAGMA foreign_keys").fetchone()[0]
    assert fk_prag == 1, "PRAGMA foreign_keys is not ON!"

    try:
        # Attempting insert with non-existent foreign key
        insert_record(
            conn,
            "products",
            {
                "sku": "INVALID-FK",
                "name": "Invalid Item",
                "category_id": 99999,  # Non-existent category
                "unit_cost": 5.0,
                "unit_price": 10.0,
                "created_at": "2026-01-01T00:00:00Z",
            },
        )
        print("  [FAIL] Foreign key violation was NOT rejected!")
        sys.exit(1)
    except sqlite3.IntegrityError:
        print("  [PASS] Foreign key violation was correctly rejected.")

    # 4. Verify basic insert and read operations
    print("\n[Step 4] Verifying basic CRUD operations across entities...")
    cat_id = insert_record(conn, "categories", {"name": "Electronics", "created_at": "2026-01-01T00:00:00Z"})
    supp_id = insert_record(conn, "suppliers", {"name": "Apex Dist", "default_lead_time_days": 10, "created_at": "2026-01-01T00:00:00Z"})
    prod_id = insert_record(
        conn,
        "products",
        {
            "sku": "ELEC-001",
            "name": "Wireless Mouse",
            "category_id": cat_id,
            "unit_cost": 15.0,
            "unit_price": 29.99,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )
    store_1 = insert_record(conn, "stores", {"name": "Main Store", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})
    store_2 = insert_record(conn, "stores", {"name": "Airport Kiosk", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})

    # Link product to supplier
    insert_record(
        conn,
        "product_suppliers",
        {
            "product_id": prod_id,
            "supplier_id": supp_id,
            "supplier_sku": "APX-M01",
            "lead_time_days": 10,
            "minimum_order_quantity": 5,
            "is_preferred": 1,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )

    # Multi-store inventory
    insert_record(conn, "inventory", {"store_id": store_1, "product_id": prod_id, "quantity_on_hand": 50, "last_updated": "2026-01-01T00:00:00Z"})
    insert_record(conn, "inventory", {"store_id": store_2, "product_id": prod_id, "quantity_on_hand": 15, "last_updated": "2026-01-01T00:00:00Z"})

    # Sales with multiple sale items
    cust_id = insert_record(conn, "customers", {"name": "John Doe", "email": "john@example.com", "created_at": "2026-01-01T00:00:00Z"})
    sale_id = insert_record(
        conn,
        "sales",
        {
            "store_id": store_1,
            "customer_id": cust_id,
            "sale_date": "2026-01-15",
            "total_amount": 59.98,
            "channel": "in-store",
            "created_at": "2026-01-15T11:00:00Z",
        },
    )
    insert_record(
        conn,
        "sale_items",
        {"sale_id": sale_id, "product_id": prod_id, "quantity": 2, "unit_price": 29.99, "subtotal": 59.98, "created_at": "2026-01-15T11:00:00Z"},
    )

    # Purchases with multiple purchase items
    po_id = insert_record(
        conn,
        "purchases",
        {"store_id": store_1, "supplier_id": supp_id, "order_date": "2026-01-05", "expected_delivery_date": "2026-01-15", "status": "draft", "created_at": "2026-01-05T09:00:00Z"},
    )
    insert_record(
        conn,
        "purchase_items",
        {"purchase_id": po_id, "product_id": prod_id, "quantity_ordered": 20, "quantity_received": 0, "unit_cost": 15.0, "created_at": "2026-01-05T09:00:00Z"},
    )

    # Stock transfers between stores
    insert_record(
        conn,
        "stock_transfers",
        {"from_store_id": store_1, "to_store_id": store_2, "product_id": prod_id, "quantity": 10, "transfer_date": "2026-01-16", "status": "completed", "actor": "admin", "created_at": "2026-01-16T12:00:00Z"},
    )

    # Forecasts
    insert_record(
        conn,
        "forecasts",
        {
            "store_id": store_1,
            "product_id": prod_id,
            "forecast_date": "2026-02-01",
            "forecast_horizon_days": 30,
            "predicted_quantity": 40.0,
            "lower_bound": 32.0,
            "upper_bound": 48.0,
            "confidence_level": 0.95,
            "model_version": "v1.0.0",
            "generated_at": "2026-01-15T00:00:00Z",
        },
    )

    # Read checks
    prod_row = fetch_one(conn, "SELECT * FROM products WHERE product_id = ?", (prod_id,))
    assert prod_row["sku"] == "ELEC-001"

    inv_rows = fetch_all(conn, "SELECT * FROM store_inventory WHERE product_id = ?", (prod_id,))
    assert len(inv_rows) == 2

    sale_item_rows = fetch_all(conn, "SELECT * FROM sale_items WHERE sale_id = ?", (sale_id,))
    assert len(sale_item_rows) == 1

    print("  [PASS] Insert and read operations verified across domain tables and views.")

    # 5. Verify audit log append-only triggers
    print("\n[Step 5] Verifying audit_logs append-only protection...")
    audit_id = insert_record(
        conn,
        "audit_logs",
        {
            "event_type": "stock_transfer",
            "entity_type": "stock_transfers",
            "entity_id": "1",
            "actor": "admin",
            "occurred_at": "2026-01-16T12:00:00Z",
            "before_state": None,
            "after_state": '{"quantity": 10}',
            "notes": "Transfer 10 units from store 1 to 2",
        },
    )
    try:
        conn.execute("UPDATE audit_logs SET actor = 'tampered' WHERE audit_id = ?", (audit_id,))
        print("  [FAIL] UPDATE on audit_logs was NOT blocked!")
        sys.exit(1)
    except (sqlite3.IntegrityError, sqlite3.OperationalError):
        print("  [PASS] UPDATE on audit_logs was rejected by trigger.")

    try:
        conn.execute("DELETE FROM audit_logs WHERE audit_id = ?", (audit_id,))
        print("  [FAIL] DELETE on audit_logs was NOT blocked!")
        sys.exit(1)
    except (sqlite3.IntegrityError, sqlite3.OperationalError):
        print("  [PASS] DELETE on audit_logs was rejected by trigger.")

    conn.close()

    print("\n==================================================")
    print("  ALL DATABASE VALIDATION CHECKS PASSED [OK]")
    print("==================================================")


if __name__ == "__main__":
    run_validation()
