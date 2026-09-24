"""SmartStock AI — Reusable Database Utility Functions.

Provides safe, parameterized helper functions for standard CRUD operations:
- fetch_one
- fetch_many
- fetch_all
- insert_record
- update_records
- delete_records
- table_exists

All functions accept an active `sqlite3.Connection` and use parameterized queries
to prevent SQL injection.
"""

from __future__ import annotations

import logging
import sqlite3
from typing import Any, Dict, List, Optional, Sequence, Union

logger = logging.getLogger(__name__)


class DatabaseOperationError(sqlite3.Error):
    """Raised when a generic database operation fails."""

    pass


def fetch_one(
    conn: sqlite3.Connection,
    query: str,
    params: Union[Sequence[Any], Dict[str, Any]] = (),
) -> Optional[sqlite3.Row]:
    """Execute a SELECT query and return a single row, or None if no result.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    query:
        SQL query string with ? placeholders or named parameters.
    params:
        Parameters matching query placeholders.
    """
    try:
        cursor = conn.execute(query, params)
        return cursor.fetchone()
    except sqlite3.Error as exc:
        logger.error("fetch_one failed on query '%s': %s", query, exc)
        raise DatabaseOperationError(f"Database query error: {exc}") from exc


def fetch_many(
    conn: sqlite3.Connection,
    query: str,
    params: Union[Sequence[Any], Dict[str, Any]] = (),
    limit: int = 100,
) -> List[sqlite3.Row]:
    """Execute a SELECT query and return up to `limit` rows.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    query:
        SQL query string.
    params:
        Parameters matching placeholders.
    limit:
        Maximum number of rows to retrieve.
    """
    try:
        cursor = conn.execute(query, params)
        return cursor.fetchmany(limit)
    except sqlite3.Error as exc:
        logger.error("fetch_many failed on query '%s': %s", query, exc)
        raise DatabaseOperationError(f"Database query error: {exc}") from exc


def fetch_all(
    conn: sqlite3.Connection,
    query: str,
    params: Union[Sequence[Any], Dict[str, Any]] = (),
) -> List[sqlite3.Row]:
    """Execute a SELECT query and return all matching rows.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    query:
        SQL query string.
    params:
        Parameters matching placeholders.
    """
    try:
        cursor = conn.execute(query, params)
        return cursor.fetchall()
    except sqlite3.Error as exc:
        logger.error("fetch_all failed on query '%s': %s", query, exc)
        raise DatabaseOperationError(f"Database query error: {exc}") from exc


def insert_record(
    conn: sqlite3.Connection,
    table: str,
    data: Dict[str, Any],
) -> int:
    """Insert a new record into `table` and return the newly generated primary key (lastrowid).

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    table:
        Target table name.
    data:
        Mapping of column names to values.

    Returns
    -------
    int:
        The rowid of the newly inserted record.
    """
    if not data:
        raise ValueError("Cannot insert empty data mapping")

    # Clean table name to protect against unsafe identifiers
    table_clean = table.strip()
    columns = list(data.keys())
    placeholders = ["?"] * len(columns)
    values = [data[col] for col in columns]

    col_clause = ", ".join(f'"{col}"' for col in columns)
    val_clause = ", ".join(placeholders)
    sql = f'INSERT INTO "{table_clean}" ({col_clause}) VALUES ({val_clause})'

    try:
        cursor = conn.execute(sql, values)
        last_id = cursor.lastrowid
        if last_id is None:
            raise DatabaseOperationError(f"Failed to obtain lastrowid after inserting into {table}")
        return last_id
    except sqlite3.IntegrityError:
        raise
    except sqlite3.Error as exc:
        logger.error("insert_record failed on table '%s': %s", table, exc)
        raise DatabaseOperationError(f"Insert error on table '{table}': {exc}") from exc


def update_records(
    conn: sqlite3.Connection,
    table: str,
    data: Dict[str, Any],
    where: str,
    where_params: Sequence[Any] = (),
) -> int:
    """Update existing records in `table` matching `where` clause.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    table:
        Target table name.
    data:
        Mapping of columns to update with new values.
    where:
        WHERE condition string (e.g. "product_id = ? AND store_id = ?").
    where_params:
        Parameters for the WHERE clause.

    Returns
    -------
    int:
        Count of updated rows.
    """
    if not data:
        raise ValueError("Cannot perform update with empty data mapping")
    if not where:
        raise ValueError("A WHERE clause is required to update records safely")

    table_clean = table.strip()
    columns = list(data.keys())
    set_clause = ", ".join(f'"{col}" = ?' for col in columns)
    values = [data[col] for col in columns] + list(where_params)

    sql = f'UPDATE "{table_clean}" SET {set_clause} WHERE {where}'

    try:
        cursor = conn.execute(sql, values)
        return cursor.rowcount
    except sqlite3.IntegrityError:
        raise
    except sqlite3.Error as exc:
        logger.error("update_records failed on table '%s': %s", table, exc)
        raise DatabaseOperationError(f"Update error on table '{table}': {exc}") from exc


def delete_records(
    conn: sqlite3.Connection,
    table: str,
    where: str,
    where_params: Sequence[Any] = (),
) -> int:
    """Delete records from `table` matching `where` clause.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    table:
        Target table name.
    where:
        WHERE condition string (e.g. "order_id = ?").
    where_params:
        Parameters for the WHERE clause.

    Returns
    -------
    int:
        Count of deleted rows.
    """
    if not where:
        raise ValueError("A WHERE clause is required to delete records safely")

    table_clean = table.strip()
    sql = f'DELETE FROM "{table_clean}" WHERE {where}'

    try:
        cursor = conn.execute(sql, where_params)
        return cursor.rowcount
    except sqlite3.Error as exc:
        logger.error("delete_records failed on table '%s': %s", table, exc)
        raise DatabaseOperationError(f"Delete error on table '{table}': {exc}") from exc


def table_exists(conn: sqlite3.Connection, table_name: str) -> bool:
    """Check whether a table or view exists in the database."""
    row = fetch_one(
        conn,
        "SELECT 1 FROM sqlite_master WHERE type IN ('table', 'view') AND name = ?",
        (table_name,),
    )
    return row is not None


def list_tables(conn: sqlite3.Connection) -> List[str]:
    """Return a list of all table names in the database (excluding internal sqlite tables)."""
    rows = fetch_all(
        conn,
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
    )
    return [row["name"] for row in rows]
