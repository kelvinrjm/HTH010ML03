"""STOCKSENSE FastAPI Authentication Dependencies.

Protects routes via Bearer token verification and role-based access control.
"""

from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from backend.auth.security import decode_access_token
from backend.database import get_db
from smartstock.db.operations import fetch_one

security = HTTPBearer(auto_error=False)


class CurrentUser:
    def __init__(self, user_id: int, username: str, role: str):
        self.user_id = user_id
        self.username = username
        self.role = role


def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
) -> CurrentUser:
    """Validate JWT token and return the current user model."""
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required. Please provide a valid Bearer token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = credentials.credentials
    payload = decode_access_token(token)
    if not payload or "sub" not in payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    username: str = payload["sub"]
    
    with get_db() as conn:
        row = fetch_one(
            conn,
            "SELECT user_id, username, role, is_active FROM users WHERE username = ?",
            (username,),
        )
        if not row or not row["is_active"]:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User account is inactive or not found.",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return CurrentUser(user_id=row["user_id"], username=row["username"], role=row["role"])


def require_admin(current_user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    """Ensure the authenticated user holds the ADMIN role."""
    if current_user.role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin privileges required to access this resource.",
        )
    return current_user


def require_staff(current_user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    """Ensure the user holds at least STAFF / MANAGER privileges."""
    if current_user.role not in ("admin", "manager", "staff"):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Staff or Admin privileges required.",
        )
    return current_user
