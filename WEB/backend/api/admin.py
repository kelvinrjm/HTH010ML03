"""Administrative controls, system health, and audit trail router."""

from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from backend.auth.dependencies import CurrentUser, require_admin
from backend.database import get_db
from backend.schemas.admin import AuditLogItem, AuditLogResponse, SystemHealthResponse
from backend.services.system_health_service import get_system_health
from smartstock.db.operations import fetch_all, fetch_one

router = APIRouter(prefix="/admin", tags=["Administration"])

@router.get("/system-health", response_model=SystemHealthResponse)
def get_health_diagnostics(admin: CurrentUser = Depends(require_admin)):
    """Admin-only: System and database technical health metrics."""
    return get_system_health()

@router.get("/audit-logs", response_model=AuditLogResponse)
def get_audit_trail(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=50, ge=1, le=200),
    entity_type: Optional[str] = None,
    admin: CurrentUser = Depends(require_admin),
):
    """Admin-only: Query the append-only tamper-evident audit log."""
    offset = (page - 1) * page_size
    with get_db() as conn:
        where = "1=1"
        params = []
        if entity_type:
            where += " AND entity_type = ?"
            params.append(entity_type)

        count_row = fetch_one(conn, f"SELECT COUNT(*) as total FROM audit_logs WHERE {where}", params)
        total = count_row["total"] if count_row else 0

        query = f"""
            SELECT audit_id, event_type, entity_type, entity_id, actor, occurred_at, before_state, after_state, notes
            FROM audit_logs
            WHERE {where}
            ORDER BY occurred_at DESC
            LIMIT ? OFFSET ?
        """
        params.extend([page_size, offset])
        rows = fetch_all(conn, query, params)

        items = [
            AuditLogItem(
                audit_id=r["audit_id"],
                event_type=r["event_type"],
                entity_type=r["entity_type"],
                entity_id=r["entity_id"],
                actor=r["actor"],
                occurred_at=r["occurred_at"],
                before_state=r["before_state"],
                after_state=r["after_state"],
                notes=r["notes"],
            )
            for r in rows
        ]
        return AuditLogResponse(total_count=total, page=page, page_size=page_size, items=items)

@router.get("/users")
def get_users_list(admin: CurrentUser = Depends(require_admin)):
    """Admin-only: List registered users."""
    with get_db() as conn:
        return fetch_all(conn, "SELECT user_id, username, role, is_active, created_at FROM users ORDER BY username ASC")
