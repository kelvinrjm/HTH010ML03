"""SmartStock AI — Database Layer package."""

from smartstock.db.connection import get_connection, open_connection
from smartstock.db.operations import (
    DatabaseOperationError,
    delete_records,
    fetch_all,
    fetch_many,
    fetch_one,
    insert_record,
    list_tables,
    table_exists,
    update_records,
)
from smartstock.db.schema import (
    MigrationError,
    MigrationTimeoutError,
    get_current_version,
    init_database,
    init_schema,
    list_pending_migrations,
)

__all__ = [
    "get_connection",
    "open_connection",
    "init_schema",
    "init_database",
    "get_current_version",
    "list_pending_migrations",
    "MigrationError",
    "MigrationTimeoutError",
    "fetch_one",
    "fetch_many",
    "fetch_all",
    "insert_record",
    "update_records",
    "delete_records",
    "table_exists",
    "list_tables",
    "DatabaseOperationError",
]
