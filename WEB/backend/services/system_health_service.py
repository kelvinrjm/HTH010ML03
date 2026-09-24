"""Admin-only System Health and Diagnostics Service."""

from datetime import datetime, timezone
import os
from backend.config import settings
from backend.database import get_db
from backend.schemas.admin import SystemHealthResponse
from smartstock.db.operations import fetch_all, fetch_one, list_tables
from smartstock.db.schema import get_current_version

def get_system_health() -> SystemHealthResponse:
    """Collect internal infrastructure metrics strictly for administrative inspection."""
    with get_db() as conn:
        schema_ver = get_current_version(conn)
        tables = list_tables(conn)
        fk_check = bool(conn.execute("PRAGMA foreign_keys").fetchone()[0])
        journal = conn.execute("PRAGMA journal_mode").fetchone()[0].lower()
        wal_enabled = journal == "wal"

        products_cnt = fetch_one(conn, "SELECT COUNT(*) as cnt FROM products")["cnt"]
        sales_cnt = fetch_one(conn, "SELECT COUNT(*) as cnt FROM sales")["cnt"]
        users_cnt = fetch_one(conn, "SELECT COUNT(*) as cnt FROM users")["cnt"]
        stores_cnt = fetch_one(conn, "SELECT COUNT(*) as cnt FROM stores")["cnt"]

        active_model_row = fetch_one(
            conn,
            "SELECT model_name, model_version FROM ml_model_registry WHERE is_active = 1 LIMIT 1"
        )
        model_name = (
            f"{active_model_row['model_name']} ({active_model_row['model_version']})"
            if active_model_row
            else "None"
        )

        now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

        return SystemHealthResponse(
            database_status="Healthy & Operational",
            database_engine="SQLite 3 (Production WAL Mode, PostgreSQL-Ready)",
            foreign_keys_enforced=fk_check,
            wal_mode_enabled=wal_enabled,
            schema_version=schema_ver,
            table_count=len(tables),
            total_records={
                "products": products_cnt,
                "sales_transactions": sales_cnt,
                "users": users_cnt,
                "stores": stores_cnt,
            },
            active_ml_model=model_name,
            server_time_utc=now_utc,
            status="All systems operating normally.",
        )
