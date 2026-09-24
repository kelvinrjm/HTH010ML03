-- Migration: 0002_create_users
-- Description: Create users table for authentication and access control.

CREATE TABLE users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('admin', 'manager', 'viewer')),
    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at    TEXT    NOT NULL   -- ISO-8601 UTC
);

CREATE INDEX idx_users_username  ON users (username);
CREATE INDEX idx_users_is_active ON users (is_active);
