"""Unit tests verifying foreign keys, relational constraints, and triggers."""

import sqlite3
import pytest

from smartstock.db.operations import fetch_all, fetch_one, insert_record


def test_foreign_key_enforcement(mem_db: sqlite3.Connection):
    """Verify foreign key enforcement rejects orphaned records."""
    # Attempting to insert product referencing a non-existent category_id (9999)
    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "products",
            {
                "sku": "ORPHAN-01",
                "name": "Orphaned Item",
                "category_id": 9999,
                "unit_cost": 10.0,
                "unit_price": 20.0,
                "created_at": "2026-01-01T00:00:00Z",
            },
        )


def test_product_sku_uniqueness(mem_db: sqlite3.Connection):
    """Verify that product SKU uniqueness constraint is enforced."""
    cat_id = insert_record(mem_db, "categories", {"name": "Electronics", "created_at": "2026-01-01T00:00:00Z"})

    insert_record(
        mem_db,
        "products",
        {
            "sku": "SKU-UNIQUE-01",
            "name": "Item A",
            "category_id": cat_id,
            "unit_cost": 10.0,
            "unit_price": 20.0,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )

    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "products",
            {
                "sku": "SKU-UNIQUE-01",  # Duplicate SKU
                "name": "Item B",
                "category_id": cat_id,
                "unit_cost": 15.0,
                "unit_price": 30.0,
                "created_at": "2026-01-01T00:00:00Z",
            },
        )


def test_multi_store_inventory(mem_db: sqlite3.Connection):
    """Verify that one product can exist across multiple stores with independent quantities."""
    cat_id = insert_record(mem_db, "categories", {"name": "Groceries", "created_at": "2026-01-01T00:00:00Z"})
    prod_id = insert_record(
        mem_db,
        "products",
        {
            "sku": "GROC-01",
            "name": "Cereal Box",
            "category_id": cat_id,
            "unit_cost": 2.50,
            "unit_price": 4.99,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )

    store_1 = insert_record(
        mem_db, "stores", {"name": "North Branch", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"}
    )
    store_2 = insert_record(
        mem_db, "stores", {"name": "South Branch", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"}
    )

    # Insert inventory for store 1 (50 on hand)
    inv1 = insert_record(
        mem_db,
        "inventory",
        {
            "store_id": store_1,
            "product_id": prod_id,
            "quantity_on_hand": 50,
            "quantity_reserved": 5,
            "last_updated": "2026-01-01T00:00:00Z",
        },
    )

    # Insert inventory for store 2 (120 on hand)
    inv2 = insert_record(
        mem_db,
        "inventory",
        {
            "store_id": store_2,
            "product_id": prod_id,
            "quantity_on_hand": 120,
            "quantity_reserved": 10,
            "last_updated": "2026-01-01T00:00:00Z",
        },
    )

    assert inv1 != inv2

    # Verify querying by store yields distinct stock levels
    s1_inv = fetch_one(mem_db, "SELECT * FROM inventory WHERE store_id = ? AND product_id = ?", (store_1, prod_id))
    s2_inv = fetch_one(mem_db, "SELECT * FROM inventory WHERE store_id = ? AND product_id = ?", (store_2, prod_id))

    assert s1_inv["quantity_on_hand"] == 50
    assert s2_inv["quantity_on_hand"] == 120

    # Verify duplicate pair (store_1, prod_id) is rejected by UNIQUE constraint
    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "inventory",
            {
                "store_id": store_1,
                "product_id": prod_id,
                "quantity_on_hand": 10,
                "last_updated": "2026-01-01T00:00:00Z",
            },
        )


def test_inventory_check_constraints(mem_db: sqlite3.Connection):
    """Verify check constraints prevent reserved quantity exceeding quantity on hand."""
    cat_id = insert_record(mem_db, "categories", {"name": "Apparel", "created_at": "2026-01-01T00:00:00Z"})
    prod_id = insert_record(
        mem_db,
        "products",
        {
            "sku": "SHIRT-01",
            "name": "Cotton T-Shirt",
            "category_id": cat_id,
            "unit_cost": 5.0,
            "unit_price": 15.0,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )
    store_id = insert_record(
        mem_db, "stores", {"name": "Outlet A", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"}
    )

    # quantity_reserved (20) > quantity_on_hand (10) must be rejected
    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "inventory",
            {
                "store_id": store_id,
                "product_id": prod_id,
                "quantity_on_hand": 10,
                "quantity_reserved": 20,
                "last_updated": "2026-01-01T00:00:00Z",
            },
        )


def test_sales_and_multiple_sale_items(mem_db: sqlite3.Connection):
    """Verify a sale header can contain multiple line items."""
    cat_id = insert_record(mem_db, "categories", {"name": "Books", "created_at": "2026-01-01T00:00:00Z"})
    p1 = insert_record(
        mem_db, "products", {"sku": "BK-01", "name": "Python 101", "category_id": cat_id, "unit_cost": 10.0, "unit_price": 25.0, "created_at": "2026-01-01T00:00:00Z"}
    )
    p2 = insert_record(
        mem_db, "products", {"sku": "BK-02", "name": "AI Basics", "category_id": cat_id, "unit_cost": 12.0, "unit_price": 30.0, "created_at": "2026-01-01T00:00:00Z"}
    )
    store_id = insert_record(mem_db, "stores", {"name": "Bookstore", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})
    cust_id = insert_record(mem_db, "customers", {"name": "Alice Smith", "email": "alice@example.com", "created_at": "2026-01-01T00:00:00Z"})

    sale_id = insert_record(
        mem_db,
        "sales",
        {
            "store_id": store_id,
            "customer_id": cust_id,
            "sale_date": "2026-01-15",
            "total_amount": 80.0,
            "channel": "in-store",
            "created_at": "2026-01-15T10:30:00Z",
        },
    )

    insert_record(
        mem_db,
        "sale_items",
        {"sale_id": sale_id, "product_id": p1, "quantity": 2, "unit_price": 25.0, "subtotal": 50.0, "created_at": "2026-01-15T10:30:00Z"},
    )
    insert_record(
        mem_db,
        "sale_items",
        {"sale_id": sale_id, "product_id": p2, "quantity": 1, "unit_price": 30.0, "subtotal": 30.0, "created_at": "2026-01-15T10:30:00Z"},
    )

    items = fetch_all(mem_db, "SELECT * FROM sale_items WHERE sale_id = ?", (sale_id,))
    assert len(items) == 2

    # Querying compatibility view sales_transactions
    txs = fetch_all(mem_db, "SELECT * FROM sales_transactions WHERE store_id = ?", (store_id,))
    assert len(txs) == 2


def test_purchases_and_multiple_purchase_items(mem_db: sqlite3.Connection):
    """Verify purchase orders and line items enforce quantity checks."""
    supplier_id = insert_record(mem_db, "suppliers", {"name": "Acme Supplies", "created_at": "2026-01-01T00:00:00Z"})
    cat_id = insert_record(mem_db, "categories", {"name": "Hardware", "created_at": "2026-01-01T00:00:00Z"})
    p1 = insert_record(mem_db, "products", {"sku": "HW-01", "name": "Wrench", "category_id": cat_id, "unit_cost": 8.0, "unit_price": 16.0, "created_at": "2026-01-01T00:00:00Z"})
    store_id = insert_record(mem_db, "stores", {"name": "Hardware Store", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})

    po_id = insert_record(
        mem_db,
        "purchases",
        {
            "store_id": store_id,
            "supplier_id": supplier_id,
            "order_date": "2026-01-10",
            "expected_delivery_date": "2026-01-20",
            "status": "draft",
            "created_at": "2026-01-10T09:00:00Z",
        },
    )

    # Valid line item
    insert_record(
        mem_db,
        "purchase_items",
        {"purchase_id": po_id, "product_id": p1, "quantity_ordered": 100, "quantity_received": 50, "unit_cost": 8.0, "created_at": "2026-01-10T09:00:00Z"},
    )

    # Invalid: quantity_received > quantity_ordered
    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "purchase_items",
            {"purchase_id": po_id, "product_id": p1, "quantity_ordered": 10, "quantity_received": 15, "unit_cost": 8.0, "created_at": "2026-01-10T09:00:00Z"},
        )


def test_stock_transfers_between_stores(mem_db: sqlite3.Connection):
    """Verify stock transfers between stores and check constraint against self-transfer."""
    cat_id = insert_record(mem_db, "categories", {"name": "Parts", "created_at": "2026-01-01T00:00:00Z"})
    prod_id = insert_record(mem_db, "products", {"sku": "PRT-01", "name": "Bolt", "category_id": cat_id, "unit_cost": 0.5, "unit_price": 1.0, "created_at": "2026-01-01T00:00:00Z"})
    s1 = insert_record(mem_db, "stores", {"name": "Warehouse North", "store_type": "wholesale", "created_at": "2026-01-01T00:00:00Z"})
    s2 = insert_record(mem_db, "stores", {"name": "Store South", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})

    # Valid transfer between s1 and s2
    xfer_id = insert_record(
        mem_db,
        "stock_transfers",
        {
            "from_store_id": s1,
            "to_store_id": s2,
            "product_id": prod_id,
            "quantity": 25,
            "transfer_date": "2026-01-12",
            "status": "completed",
            "actor": "admin",
            "notes": "Restocking South store",
            "created_at": "2026-01-12T14:00:00Z",
        },
    )
    assert xfer_id > 0

    # Self-transfer (from_store_id == to_store_id) must be rejected
    with pytest.raises(sqlite3.IntegrityError):
        insert_record(
            mem_db,
            "stock_transfers",
            {
                "from_store_id": s1,
                "to_store_id": s1,
                "product_id": prod_id,
                "quantity": 10,
                "transfer_date": "2026-01-12",
                "created_at": "2026-01-12T14:00:00Z",
            },
        )


def test_audit_logs_append_only_triggers(mem_db: sqlite3.Connection):
    """Verify that audit_logs is append-only: UPDATE and DELETE statements are rejected by triggers."""
    audit_id = insert_record(
        mem_db,
        "audit_logs",
        {
            "event_type": "inventory_adjustment",
            "entity_type": "inventory",
            "entity_id": "1",
            "actor": "manager",
            "occurred_at": "2026-01-10T12:00:00Z",
            "before_state": '{"quantity_on_hand": 50}',
            "after_state": '{"quantity_on_hand": 75}',
            "notes": "Physical count adjustment",
        },
    )
    assert audit_id > 0

    # Attempting to UPDATE an audit record must raise error via trigger
    with pytest.raises((sqlite3.IntegrityError, sqlite3.OperationalError), match="audit_logs is append-only: UPDATE not permitted"):
        mem_db.execute("UPDATE audit_logs SET actor = 'hacker' WHERE audit_id = ?", (audit_id,))

    # Attempting to DELETE an audit record must raise error via trigger
    with pytest.raises((sqlite3.IntegrityError, sqlite3.OperationalError), match="audit_logs is append-only: DELETE not permitted"):
        mem_db.execute("DELETE FROM audit_logs WHERE audit_id = ?", (audit_id,))


def test_forecasts_storage(mem_db: sqlite3.Connection):
    """Verify forecasts can record store/product/date predictions with bounds."""
    cat_id = insert_record(mem_db, "categories", {"name": "Electronics", "created_at": "2026-01-01T00:00:00Z"})
    prod_id = insert_record(mem_db, "products", {"sku": "FC-01", "name": "Tablet", "category_id": cat_id, "unit_cost": 150.0, "unit_price": 299.0, "created_at": "2026-01-01T00:00:00Z"})
    store_id = insert_record(mem_db, "stores", {"name": "Tech Store", "store_type": "retail", "created_at": "2026-01-01T00:00:00Z"})

    fc_id = insert_record(
        mem_db,
        "forecasts",
        {
            "store_id": store_id,
            "product_id": prod_id,
            "forecast_date": "2026-02-01",
            "forecast_horizon_days": 30,
            "predicted_quantity": 42.5,
            "lower_bound": 35.0,
            "upper_bound": 50.0,
            "confidence_level": 0.95,
            "model_version": "v1.0.0",
            "generated_at": "2026-01-15T00:00:00Z",
        },
    )
    assert fc_id > 0

    row = fetch_one(mem_db, "SELECT * FROM forecasts WHERE forecast_id = ?", (fc_id,))
    assert row["predicted_quantity"] == 42.5
    assert row["confidence_level"] == 0.95
