"""Unit tests for reusable database CRUD helper operations."""

import sqlite3

from smartstock.db.operations import (
    delete_records,
    fetch_all,
    fetch_many,
    fetch_one,
    insert_record,
    table_exists,
    update_records,
)


def test_crud_lifecycle_on_stores(mem_db: sqlite3.Connection):
    """Test insert, fetch, update, and delete on the stores table."""
    # 1. Insert store
    store_id = insert_record(
        mem_db,
        "stores",
        {
            "name": "Downtown Flagship",
            "location": "Building A, Main Street",
            "store_type": "retail",
            "is_active": 1,
            "created_at": "2026-01-01T00:00:00Z",
        },
    )
    assert store_id > 0

    # 2. Fetch one
    row = fetch_one(mem_db, "SELECT * FROM stores WHERE store_id = ?", (store_id,))
    assert row is not None
    assert row["name"] == "Downtown Flagship"
    assert row["store_type"] == "retail"

    # 3. Update record
    updated_count = update_records(
        mem_db,
        "stores",
        {"name": "Downtown Flagship - Renovated"},
        where="store_id = ?",
        where_params=(store_id,),
    )
    assert updated_count == 1

    row_after = fetch_one(mem_db, "SELECT name FROM stores WHERE store_id = ?", (store_id,))
    assert row_after["name"] == "Downtown Flagship - Renovated"

    # 4. Fetch many
    for i in range(5):
        insert_record(
            mem_db,
            "stores",
            {
                "name": f"Branch {i}",
                "location": f"Zone {i}",
                "store_type": "retail",
                "is_active": 1,
                "created_at": "2026-01-01T00:00:00Z",
            },
        )

    stores_subset = fetch_many(mem_db, "SELECT * FROM stores ORDER BY store_id", limit=3)
    assert len(stores_subset) == 3

    all_stores = fetch_all(mem_db, "SELECT * FROM stores")
    assert len(all_stores) == 6

    # 5. Delete records
    deleted_count = delete_records(mem_db, "stores", where="name LIKE ?", where_params=("Branch %",))
    assert deleted_count == 5

    remaining = fetch_all(mem_db, "SELECT * FROM stores")
    assert len(remaining) == 1


def test_table_exists_helper(mem_db: sqlite3.Connection):
    """Verify table_exists identifies existing and non-existent tables correctly."""
    assert table_exists(mem_db, "products") is True
    assert table_exists(mem_db, "users") is True
    assert table_exists(mem_db, "non_existent_table_xyz") is False
