"""Authentication service for STOCKSENSE."""

from fastapi import HTTPException, status
from backend.auth.security import create_access_token, verify_password
from backend.database import get_db
from backend.schemas.auth import LoginRequest, TokenResponse, UserResponse
from smartstock.db.operations import fetch_one

def authenticate_user(login_data: LoginRequest) -> TokenResponse:
    """Validate username and password, returning JWT and user profile."""
    with get_db() as conn:
        row = fetch_one(
            conn,
            "SELECT user_id, username, password_hash, role, is_active, created_at FROM users WHERE username = ?",
            (login_data.username,),
        )
        if not row:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect username or password",
            )
        
        if not row["is_active"]:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Account is inactive. Please contact your administrator.",
            )
            
        if not verify_password(login_data.password, row["password_hash"]):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect username or password",
            )

        token = create_access_token(
            data={"sub": row["username"], "user_id": row["user_id"], "role": row["role"]}
        )
        
        user_resp = UserResponse(
            user_id=row["user_id"],
            username=row["username"],
            role=row["role"],
            is_active=row["is_active"],
            created_at=row["created_at"],
        )
        
        return TokenResponse(access_token=token, token_type="bearer", user=user_resp)
