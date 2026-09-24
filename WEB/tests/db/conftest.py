"""Pytest fixtures for SmartStock AI database tests."""

import sqlite3
from typing import Generator

import pytest

from smartstock.config import Config
from smartstock.db.connection import open_connection
from smartstock.db.schema import init_schema


@pytest.fixture
def mem_config() -> Config:
    """Return a Config object pointing to an in-memory database."""
    return Config(db_path=":memory:")


@pytest.fixture
def mem_db(mem_config: Config) -> Generator[sqlite3.Connection, None, None]:
    """Yield a freshly initialized in-memory database with all migrations applied."""
    conn = open_connection(mem_config)
    init_schema(conn, mem_config)
    yield conn
    conn.close()
