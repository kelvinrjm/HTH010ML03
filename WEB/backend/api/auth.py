"""Authentication router."""

from fastapi import APIRouter, Depends
from backend.auth.dependencies import CurrentUser, get_current_user
from backend.schemas.auth import LoginRequest, TokenResponse, UserResponse
from backend.services.auth_service import authenticate_user

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/login", response_model=TokenResponse)
def login(credentials: LoginRequest):
    """Authenticate and obtain JWT access token."""
    return authenticate_user(credentials)

@router.get("/me", response_model=UserResponse)
def get_me(user: CurrentUser = Depends(get_current_user)):
    """Retrieve profile for the currently logged-in user."""
    return UserResponse(
        user_id=user.user_id,
        username=user.username,
        role=user.role,
        is_active=1,
        created_at="",
    )
