"""Unit tests for SQLite connection management."""

import sqlite3
import pytest

from smartstock.config import Config
from smartstock.db.connection import get_connection, open_connection


def test_connection_pragmas(tmp_path):
    """Verify that foreign keys and WAL mode are configured on new connections."""
    db_file = str(tmp_path / "test.db")
    cfg = Config(db_path=db_file)

    with get_connection(cfg) as conn:
        fk_status = conn.execute("PRAGMA foreign_keys").fetchone()[0]
        assert fk_status == 1

        journal_mode = conn.execute("PRAGMA journal_mode").fetchone()[0]
        assert journal_mode.lower() in ("wal", "memory")

        assert conn.row_factory == sqlite3.Row


def test_connection_commits_on_clean_exit(tmp_path):
    """Verify that get_connection commits transactions when no exception occurs."""
    db_file = str(tmp_path / "test.db")
    cfg = Config(db_path=db_file)

    with get_connection(cfg) as conn:
        conn.execute("CREATE TABLE t (x INT);")
        conn.execute("INSERT INTO t VALUES (42);")

    # Re-open in a new connection and verify data persisted
    with get_connection(cfg) as conn:
        row = conn.execute("SELECT x FROM t;").fetchone()
        assert row["x"] == 42


def test_connection_rolls_back_on_error(tmp_path):
    """Verify that get_connection rolls back transactions when an exception is raised."""
    db_file = str(tmp_path / "test.db")
    cfg = Config(db_path=db_file)

    with get_connection(cfg) as conn:
        conn.execute("CREATE TABLE t (x INT);")

    with pytest.raises(RuntimeError):
        with get_connection(cfg) as conn:
            conn.execute("INSERT INTO t VALUES (99);")
            raise RuntimeError("Simulated failure")

    with get_connection(cfg) as conn:
        rows = conn.execute("SELECT x FROM t;").fetchall()
        assert len(rows) == 0


def test_open_connection_manual_lifecycle():
    """Verify open_connection creates a configured connection with manual lifecycle."""
    cfg = Config(db_path=":memory:")
    conn = open_connection(cfg)
    try:
        fk = conn.execute("PRAGMA foreign_keys").fetchone()[0]
        assert fk == 1
        assert conn.row_factory == sqlite3.Row
    finally:
        conn.close()
