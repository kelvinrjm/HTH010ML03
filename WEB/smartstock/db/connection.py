"""SmartStock AI — SQLite connection management.

Provides two entry points:

* ``get_connection`` — a context-manager that owns the full connection
  lifecycle (open → configure → yield → commit/rollback → close).
* ``open_connection`` — opens and configures a raw connection without
  managing its lifecycle; intended for test fixtures and long-lived
  sessions that need explicit control.
"""

from __future__ import annotations

import contextlib
import os
import sqlite3
from typing import Generator

from smartstock.config import DEFAULT_CONFIG, Config


def _configure(conn: sqlite3.Connection) -> None:
    """Apply standard PRAGMAs and row factory to an open connection."""
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")


@contextlib.contextmanager
def get_connection(
    config: Config = DEFAULT_CONFIG,
) -> Generator[sqlite3.Connection, None, None]:
    """Context manager that opens, configures, and closes a SQLite connection.

    On clean exit the transaction is committed.  On any exception the
    transaction is rolled back; the exception is then re-raised unchanged.

    Parameters
    ----------
    config:
        Application configuration.  ``config.db_path`` determines which
        database file to open (``":memory:"`` is supported for tests).

    Yields
    ------
    sqlite3.Connection
        A fully configured connection with ``foreign_keys`` ON,
        ``journal_mode`` WAL, and ``row_factory`` set to ``sqlite3.Row``.

    Raises
    ------
    sqlite3.OperationalError
        Re-raised (with the path embedded in the message) when the
        database file cannot be opened.
    """
    if config.db_path != ":memory:":
        parent_dir = os.path.dirname(os.path.abspath(config.db_path))
        if parent_dir and not os.path.exists(parent_dir):
            os.makedirs(parent_dir, exist_ok=True)

    try:
        conn = sqlite3.connect(config.db_path)
    except sqlite3.OperationalError as exc:
        raise sqlite3.OperationalError(
            f"Cannot open database at '{config.db_path}': {exc}"
        ) from exc

    try:
        _configure(conn)
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def open_connection(config: Config = DEFAULT_CONFIG) -> sqlite3.Connection:
    """Open and configure a SQLite connection without managing its lifecycle.

    The caller is responsible for committing, rolling back, and closing
    the returned connection.  This function is intended for use in pytest
    fixtures and other contexts that need fine-grained lifecycle control.

    Parameters
    ----------
    config:
        Application configuration.  ``config.db_path`` determines which
        database file to open (``":memory:"`` is supported for tests).

    Returns
    -------
    sqlite3.Connection
        A fully configured connection with ``foreign_keys`` ON,
        ``journal_mode`` WAL, and ``row_factory`` set to ``sqlite3.Row``.

    Raises
    ------
    sqlite3.OperationalError
        Re-raised (with the path embedded in the message) when the
        database file cannot be opened.
    """
    if config.db_path != ":memory:":
        parent_dir = os.path.dirname(os.path.abspath(config.db_path))
        if parent_dir and not os.path.exists(parent_dir):
            os.makedirs(parent_dir, exist_ok=True)

    try:
        conn = sqlite3.connect(config.db_path)
    except sqlite3.OperationalError as exc:
        raise sqlite3.OperationalError(
            f"Cannot open database at '{config.db_path}': {exc}"
        ) from exc

    _configure(conn)
    return conn
