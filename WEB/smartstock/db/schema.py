"""SmartStock AI — Database schema initialization and migration manager.

Discovers and applies versioned SQL migrations in ascending order.
Ensures atomic transactions per migration, records applied migrations
in `schema_version`, and provides safe initialization for runtime and test environments.
"""

from __future__ import annotations

import logging
import os
import sqlite3
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import List

from smartstock.config import DEFAULT_CONFIG, Config
from smartstock.db.connection import get_connection

logger = logging.getLogger(__name__)

MIGRATIONS_DIR = Path(__file__).parent / "migrations"


class MigrationError(Exception):
    """Raised when a database migration script fails to apply."""

    def __init__(self, version: int, description: str, cause: Exception):
        self.version = version
        self.description = description
        self.cause = cause
        super().__init__(
            f"Failed to apply migration {version:04d} ({description}): {cause}"
        )


class MigrationTimeoutError(Exception):
    """Raised when migrations exceed the configured timeout."""

    pass


def get_current_version(conn: sqlite3.Connection) -> int:
    """Return the highest applied migration version number, or 0 if uninitialized."""
    try:
        cursor = conn.execute(
            "SELECT MAX(version) FROM schema_version"
        )
        row = cursor.fetchone()
        if row and row[0] is not None:
            return int(row[0])
        return 0
    except sqlite3.OperationalError:
        # schema_version table does not exist yet
        return 0


def get_applied_versions(conn: sqlite3.Connection) -> set[int]:
    """Return the set of all migration versions applied so far."""
    try:
        cursor = conn.execute("SELECT version FROM schema_version")
        return {int(row[0]) for row in cursor.fetchall()}
    except sqlite3.OperationalError:
        return set()


def list_pending_migrations(conn: sqlite3.Connection) -> List[Path]:
    """Discover all SQL migration files and return unapplied ones sorted by version."""
    if not MIGRATIONS_DIR.exists():
        return []

    applied = get_applied_versions(conn)
    all_files = sorted(MIGRATIONS_DIR.glob("*.sql"))

    pending: List[Path] = []
    for f in all_files:
        prefix = f.stem.split("_")[0]
        try:
            ver = int(prefix)
            if ver not in applied:
                pending.append(f)
        except ValueError:
            logger.warning("Skipping non-versioned SQL file: %s", f.name)

    return pending


def init_schema(conn: sqlite3.Connection, config: Config = DEFAULT_CONFIG) -> None:
    """Apply all pending migrations in ascending order within configured timeout.

    Parameters
    ----------
    conn:
        Active sqlite3.Connection.
    config:
        Application configuration containing timeout and path settings.

    Raises
    ------
    MigrationError:
        If any migration file fails execution.
    MigrationTimeoutError:
        If total migration execution exceeds `config.db_migration_timeout_seconds`.
    """
    start_time = time.monotonic()
    timeout = config.db_migration_timeout_seconds

    # 1. Bootstrap schema_version if not present
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS schema_version (
            version     INTEGER PRIMARY KEY,
            description TEXT    NOT NULL,
            applied_at  TEXT    NOT NULL
        );
        """
    )
    conn.commit()

    pending = list_pending_migrations(conn)
    if not pending:
        logger.info("Database schema is already up to date.")
        return

    logger.info("Applying %d pending migrations...", len(pending))

    for migration_file in pending:
        # Check timeout
        elapsed = time.monotonic() - start_time
        if elapsed > timeout:
            raise MigrationTimeoutError(
                f"Schema migration timed out after {elapsed:.1f}s (max {timeout}s)"
            )

        prefix = migration_file.stem.split("_", 1)[0]
        description = (
            migration_file.stem.split("_", 1)[1]
            if "_" in migration_file.stem
            else migration_file.stem
        )
        version = int(prefix)

        sql_content = migration_file.read_text(encoding="utf-8")

        # Execute migration
        try:
            conn.executescript(sql_content)

            now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
            conn.execute(
                "INSERT INTO schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                (version, description, now_utc),
            )
            conn.commit()
            logger.info("Applied migration %04d: %s", version, description)
        except Exception as exc:
            try:
                conn.rollback()
            except sqlite3.Error:
                pass
            logger.error("Migration failed at version %04d: %s", version, exc)
            raise MigrationError(version, description, exc) from exc


def init_database(config: Config = DEFAULT_CONFIG) -> None:
    """Ensure the target database directory exists and run all migrations.

    Safe to run repeatedly; idempotent.
    """
    if config.db_path != ":memory:":
        parent_dir = os.path.dirname(os.path.abspath(config.db_path))
        if parent_dir and not os.path.exists(parent_dir):
            os.makedirs(parent_dir, exist_ok=True)

    with get_connection(config) as conn:
        init_schema(conn, config)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    init_database()
    print("Database initialization complete.")
