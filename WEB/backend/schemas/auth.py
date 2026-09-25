"""Pydantic schemas for authentication and user accounts."""

from pydantic import BaseModel, Field
from typing import Optional

class LoginRequest(BaseModel):
    username: str = Field(..., example="admin")
    password: str = Field(..., example="Admin@123")

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserResponse"

class UserResponse(BaseModel):
    user_id: int
    username: str
    role: str
    is_active: int
    created_at: str

TokenResponse.model_rebuild()
