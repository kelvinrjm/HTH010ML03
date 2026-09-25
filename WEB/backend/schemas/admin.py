"""Pydantic schemas for administration, audit logs, and system health."""

from typing import List, Optional
from pydantic import BaseModel

class SystemHealthResponse(BaseModel):
    database_status: str
    database_engine: str
    foreign_keys_enforced: bool
    wal_mode_enabled: bool
    schema_version: int
    table_count: int
    total_records: dict
    active_ml_model: Optional[str] = None
    server_time_utc: str
    status: str

class AuditLogItem(BaseModel):
    audit_id: int
    event_type: str
    entity_type: str
    entity_id: str
    actor: str
    occurred_at: str
    before_state: Optional[str] = None
    after_state: Optional[str] = None
    notes: Optional[str] = ""

class AuditLogResponse(BaseModel):
    total_count: int
    page: int
    page_size: int
    items: List[AuditLogItem]

class AppSettingItem(BaseModel):
    key: str
    value: str
    description: str
    updated_at: str
