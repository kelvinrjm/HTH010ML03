"""STOCKSENSE Database Access Layer.

Integrates with the SmartStock AI foundation database tier, providing
connection pooling and context-managed execution for FastAPI services.
"""

from contextlib import contextmanager
import sqlite3
from typing import Generator

from backend.config import settings
from smartstock.config import Config
from smartstock.db.connection import get_connection as _get_connection, open_connection as _open_connection
from smartstock.db.schema import init_database as _init_database

db_config = Config(db_path=settings.DB_PATH)


def init_db() -> None:
    """Initialize the database and run all pending migrations."""
    _init_database(db_config)


@contextmanager
def get_db() -> Generator[sqlite3.Connection, None, None]:
    """Yield a managed SQLite connection configured with WAL mode and foreign keys enabled."""
    with _get_connection(db_config) as conn:
        yield conn
